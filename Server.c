// Server.c - Server cu SQLite si sincronizare fisiere
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <stdint.h>
#include <arpa/inet.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/types.h>
#include <sqlite3.h>
#include "Protocol.h"

/* ================= STRUCTURI ================= */
// Structura unui client conectat
typedef struct
{
    int fd;                 // socket-ul clientului
    char username[64];      // numele clientului
    int joined;             // flag daca clientul a dat JOIN / RENAME
    int editing;            // flag daca clientul editeaza
} client_t;

static client_t clients[MAX_CLIENTS];
static sqlite3 *db = NULL; // pointer catre baza de date SQLite

// Variabile globale pentru ultima salvare
static char *last_file_content = NULL;      // continutul ultimului fisier salvat
static uint32_t last_file_version = 0;     // versiunea ultimului fisier
static char last_file_name[256] = "";      // numele ultimului fisier

/* ================= DATABASE ================= */
// Initializare baza de date SQLite si creare tabela daca nu exista
static int db_init()
{
    int rc = sqlite3_open("files.db", &db);
    if(rc)
    {
        fprintf(stderr, "Cannot open database: %s\n", sqlite3_errmsg(db));
        return -1;
    }

    // Creeaza tabela files cu filename, content si version
    const char *sql = "CREATE TABLE IF NOT EXISTS files ("
                      "filename TEXT PRIMARY KEY,"
                      "content BLOB,"
                      "version INTEGER);";

    char *errmsg = NULL;
    rc = sqlite3_exec(db, sql, 0, 0, &errmsg);
    if(rc != SQLITE_OK)
    {
        fprintf(stderr, "SQL error: %s\n", errmsg);
        sqlite3_free(errmsg);
        return -1;
    }

    return 0;
}

// Citeste un fisier din DB
// Returneaza 0 daca gasit, -1 daca nu
static int db_get_file(const char *filename, char **content, int *size, int *version)
{
    if(!filename || !content || !size || !version) return -1;

    const char *sql = "SELECT content, version FROM files WHERE filename = ?;";
    sqlite3_stmt *stmt = NULL;
    if(sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK) return -1;

    sqlite3_bind_text(stmt, 1, filename, -1, SQLITE_STATIC);

    int rc = sqlite3_step(stmt);
    if(rc == SQLITE_ROW)
    {
        const void *blob = sqlite3_column_blob(stmt, 0);
        int sz = sqlite3_column_bytes(stmt, 0);
        *version = sqlite3_column_int(stmt, 1);

        *content = malloc(sz);
        if(!*content)
        {
            sqlite3_finalize(stmt);
            return -1;
        }
        memcpy(*content, blob, sz);
        *size = sz;

        sqlite3_finalize(stmt);
        return 0;
    }
    else
    {
        sqlite3_finalize(stmt);
        *content = NULL;
        *size = 0;
        *version = 0;
        return -1;
    }
}

// Salveaza / actualizeaza un fisier in DB
static int db_save_file(const char *filename, const char *content, int size, int version)
{
    if(!filename || !content || size < 0) return -1;

    const char *sql = "INSERT INTO files(filename, content, version) "
                      "VALUES(?, ?, ?) "
                      "ON CONFLICT(filename) DO UPDATE SET content=excluded.content, version=excluded.version;";
    sqlite3_stmt *stmt = NULL;
    if(sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK) return -1;

    sqlite3_bind_text(stmt, 1, filename, -1, SQLITE_STATIC);
    sqlite3_bind_blob(stmt, 2, content, size, SQLITE_STATIC);
    sqlite3_bind_int(stmt, 3, version);

    int rc = sqlite3_step(stmt);
    sqlite3_finalize(stmt);

    return (rc == SQLITE_DONE) ? 0 : -1;
}

/* ================= UTIL ================= */
// Citeste exact n bytes de la socket
static int read_n(int fd, void *buf, uint32_t len)
{
    char *p = buf;
    uint32_t left = len;
    while(left > 0)
    {
        ssize_t r = recv(fd, p, left, 0);
        if(r <= 0) return r;
        p += r;
        left -= r;
    }
    return len;
}

// Trimite mesaj catre un client
static int send_message_fd(int fd, uint8_t op, const void *payload, uint32_t payload_len)
{
    uint32_t total_len = 1 + payload_len;
    uint32_t be_len = htonl(total_len);

    if(send(fd, &be_len, sizeof(be_len), 0) != sizeof(be_len)) return -1;
    if(send(fd, &op, 1, 0) != 1) return -1;

    if(payload_len && payload)
    {
        const char *p = payload;
        uint32_t left = payload_len;
        while(left > 0)
        {
            ssize_t s = send(fd, p, left, 0);
            if(s <= 0) return -1;
            p += s;
            left -= s;
        }
    }
    return 0;
}

/* ================= CLIENT MGMT ================= */
// Adauga un client nou
static int add_client(int fd)
{
    for(int i = 0; i < MAX_CLIENTS; i++)
    {
        if(clients[i].fd == 0)
        {
            clients[i].fd = fd;
            clients[i].username[0] = '\0';
            clients[i].joined = 0;
            clients[i].editing = 0;
            return i;
        }
    }
    return -1;
}

// Sterge un client (inchide socket + reseteaza structura)
static void remove_client_index(int i)
{
    if(clients[i].fd)
    {
        close(clients[i].fd);
        memset(&clients[i], 0, sizeof(client_t));
    }
}

/* ================= BROADCAST ================= */
// Actualizeaza lista de utilizatori si trimite tuturor clientilor conectati
static void update_and_broadcast_users(void)
{
    char list[4096] = "";
    int first = 1;
    for(int i = 0; i < MAX_CLIENTS; i++)
    {
        if(clients[i].fd > 0 && clients[i].joined)
        {
            if(!first) strcat(list, ",");
            strcat(list, clients[i].username);
            first = 0;
        }
    }
    for(int i = 0; i < MAX_CLIENTS; i++)
    {
        if(clients[i].fd > 0 && clients[i].joined)
        {
            send_message_fd(clients[i].fd, OP_USERS, list, strlen(list));
        }
    }
}

// Trimite versiunea fisierului catre toti clientii (exceptat skip_fd)
static void broadcast_file_version(const char *filename, int version, int skip_fd)
{
    char msg[64];
    snprintf(msg, sizeof(msg), "%s:%d", filename, version);
    for(int i = 0; i < MAX_CLIENTS; i++)
    {
        if(clients[i].fd > 0 && clients[i].fd != skip_fd)
        {
            send_message_fd(clients[i].fd, OP_STATUS, msg, strlen(msg));
        }
    }
}

/* ================= MAIN ================= */
int main(int argc, char **argv)
{
    int port = (argc > 1) ? atoi(argv[1]) : SERVER_PORT_DEFAULT;
    memset(clients, 0, sizeof(clients));

    if(db_init() != 0)
    {
        fprintf(stderr, "Database init failed\n");
        return 1;
    }

    // Creeaza socket de ascultare
    int listen_fd = socket(AF_INET, SOCK_STREAM, 0);
    if(listen_fd < 0)
    {
        perror("socket");
        return 1;
    }

    int opt = 1;
    setsockopt(listen_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    struct sockaddr_in addr = {0};
    addr.sin_family = AF_INET;
    addr.sin_port = htons(port);
    addr.sin_addr.s_addr = INADDR_ANY;

    if(bind(listen_fd, (struct sockaddr*)&addr, sizeof(addr)) < 0)
    {
        perror("bind");
        return 1;
    }
    if(listen(listen_fd, 10) < 0)
    {
        perror("listen");
        return 1;
    }

    printf("Server listening on port %d\n", port);

    while(1)
    {
        // Folosim select pentru multiple socket-uri
        fd_set rfds;
        FD_ZERO(&rfds);
        FD_SET(listen_fd, &rfds);
        int maxfd = listen_fd;

        for(int i = 0; i < MAX_CLIENTS; i++)
        {
            if(clients[i].fd > 0)
            {
                FD_SET(clients[i].fd, &rfds);
                if(clients[i].fd > maxfd) maxfd = clients[i].fd;
            }
        }

        if(select(maxfd + 1, &rfds, NULL, NULL, NULL) < 0)
        {
            if(errno == EINTR) continue;
            perror("select");
            break;
        }

        // Verifica conexiuni noi
        if(FD_ISSET(listen_fd, &rfds))
        {
            int fd = accept(listen_fd, NULL, NULL);
            if(fd >= 0 && add_client(fd) < 0) close(fd);
        }

        // Proceseaza date de la clientii existenti
        for(int i = 0; i < MAX_CLIENTS; i++)
        {
            int fd = clients[i].fd;
            if(fd <= 0 || !FD_ISSET(fd, &rfds)) continue;

            uint32_t be_len;
            if(read_n(fd, &be_len, 4) <= 0)
            {
                remove_client_index(i);
                update_and_broadcast_users();
                continue;
            }

            uint32_t total_len = ntohl(be_len);
            uint8_t op;
            read_n(fd, &op, 1);

            uint32_t plen = total_len - 1;
            char *payload = plen ? malloc(plen + 1) : NULL;
            if(payload)
            {
                read_n(fd, payload, plen);
                payload[plen] = 0;
            }

            switch(op)
            {
                case OP_JOIN:
                case OP_RENAME:
                    printf("Received op: %d payload: %s from fd %d\n", op, payload, fd);
                    strncpy(clients[i].username, payload, 63);
                    clients[i].joined = 1;
                    printf("Broadcasting users to fd %d\n", clients[i].fd);
                    update_and_broadcast_users();
                    break;

                case OP_EDIT:
                    clients[i].editing = 1;
                    break;

                case OP_CLEAR:
                    clients[i].editing = 1;
                    break;

                case OP_SAVE:
                {
                    if (!payload || plen == 0) break;

                    // Payload: filename + ":" + content
                    char *sep = memchr(payload, ':', plen);
                    if (!sep) break;

                    size_t fname_len = sep - payload;
                    char filename[256];
                    if (fname_len >= sizeof(filename)) fname_len = sizeof(filename) - 1;
                    memcpy(filename, payload, fname_len);
                    filename[fname_len] = '\0';

                    char *file_content = sep + 1;
                    size_t content_len = plen - (fname_len + 1);

                    // Salveaza in baza de date (incrementare versiune)
                    sqlite3_stmt *stmt;
                    const char *sql = "INSERT INTO files (filename, content, version) "
                                    "VALUES (?, ?, 1) "
                                    "ON CONFLICT(filename) DO UPDATE SET "
                                    "content=excluded.content, "
                                    "version=files.version+1;";
                    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) == SQLITE_OK)
                    {
                        sqlite3_bind_text(stmt, 1, filename, -1, SQLITE_STATIC);
                        sqlite3_bind_blob(stmt, 2, file_content, (int)content_len, SQLITE_STATIC);

                        if (sqlite3_step(stmt) != SQLITE_DONE)
                        {
                            fprintf(stderr, "DB save error: %s\n", sqlite3_errmsg(db));
                        }
                        sqlite3_finalize(stmt);
                    }
                    else
                    {
                        fprintf(stderr, "DB prepare error: %s\n", sqlite3_errmsg(db));
                    }

                    // Actualizeaza numele si versiunea ultimei salvari
                    strncpy(last_file_name, filename, 255);
                    last_file_name[255] = '\0';

                    // Obtine versiunea noua din DB
                    char sql_ver[256];
                    snprintf(sql_ver, sizeof(sql_ver), "SELECT version FROM files WHERE filename='%s';", filename);
                    sqlite3_stmt *stmt_ver;
                    if (sqlite3_prepare_v2(db, sql_ver, -1, &stmt_ver, NULL) == SQLITE_OK)
                    {
                        if (sqlite3_step(stmt_ver) == SQLITE_ROW)
                        {
                            last_file_version = sqlite3_column_int(stmt_ver, 0);
                        }
                        sqlite3_finalize(stmt_ver);
                    }

                    // Trimite OP_STATUS catre ceilalti clienti
                    for (int j = 0; j < MAX_CLIENTS; j++)
                    {
                        if (clients[j].fd > 0 && j != i && clients[j].joined)
                        {
                            char version_str[300];
                            snprintf(version_str, sizeof(version_str), "%s:%u", filename, last_file_version);
                            send_message_fd(clients[j].fd, OP_STATUS, version_str, strlen(version_str));
                        }
                    }

                    break;
                }

                case OP_LOAD:
                {
                    char *content = NULL;
                    int size = 0;
                    int version = 0;

                    if (db_get_file(payload, &content, &size, &version) == 0)
                    {
                        // Trimite continut fisier
                        send_message_fd(clients[i].fd, OP_FILE, content, size);

                        // Trimite versiunea fisierului
                        char status[512];
                        snprintf(status, sizeof(status), "%s:%d", payload, version);
                        send_message_fd(clients[i].fd, OP_LOAD_DONE, status, strlen(status));

                        free(content);
                    }
                    break;
                }

                case OP_PING:
                    break;
            }

            free(payload);
        }
    }

    sqlite3_close(db);
    close(listen_fd);
    return 0;
}
