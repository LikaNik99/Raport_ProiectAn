#include "ClientNetwork.h"
#include "Protocol.h"
#include "ClientGUI.h"

#include <arpa/inet.h>
#include <unistd.h>
#include <sys/socket.h>
#include <pthread.h>
#include <cstring>
#include <cstdlib>
#include <QDebug>
#include <QMetaObject>
#include <QMap>
#include <QString>

// Mapare versiuni server per fisier
static QMap<QString, int> serverFileVersions;

// Pointer global catre GUI
extern ClientGUI *gui;

// Socket si thread
static int sockfd = -1;
static pthread_t recv_thread;
static int running = 0;

// ======== Send message ========
// Trimite un mesaj cu OP + payload catre server
static int send_message(uint8_t op, const void *payload, uint32_t len)
{
    if (sockfd < 0) return -1;

    uint32_t total_len = 1 + len;
    uint32_t be_len = htonl(total_len);

    if (::send(sockfd, &be_len, sizeof(be_len), 0) != sizeof(be_len)) return -1;
    if (::send(sockfd, &op, 1, 0) != 1) return -1;

    const char *p = (const char*)payload;
    uint32_t left = len;
    while (left > 0)
    {
        ssize_t s = ::send(sockfd, p, left, 0);
        if (s <= 0) return -1;
        p += s;
        left -= s;
    }

    qDebug() << "Sent op" << op << "len" << len;
    return 0;
}

// ======== Connect ========
// Conecteaza clientul la server si trimite username-ul
int connect_to_server(const char *ip, int port, const char *username)
{
    struct sockaddr_in serv{};
    sockfd = socket(AF_INET, SOCK_STREAM, 0);
    if (sockfd < 0)
    {
        qDebug() << "Socket creation failed";
        return -1;
    }

    serv.sin_family = AF_INET;
    serv.sin_port = htons(port);
    if (inet_pton(AF_INET, ip, &serv.sin_addr) <= 0)
    {
        close(sockfd);
        sockfd = -1;
        return -1;
    }

    if (::connect(sockfd, (struct sockaddr*)&serv, sizeof(serv)) < 0)
    {
        qDebug() << "Connect failed";
        close(sockfd);
        sockfd = -1;
        return -1;
    }

    qDebug() << "Connected to server";

    if (username && strlen(username) > 0)
    {
        send_message(OP_JOIN, username, (uint32_t)strlen(username));
    }

    return 0;
}

// ======== Send functions ========
// Trimite text editat catre server
void send_edit_to_server(const QString &text)
{
    QByteArray ba = text.toUtf8();
    send_message(OP_EDIT, ba.constData(), (uint32_t)ba.size());
}

// Trimite cerere de clear
void send_clear_request()
{
    send_message(OP_CLEAR, nullptr, 0);
}

// Schimba username
void send_username_change_request(const QString &name)
{
    QByteArray ba = name.toUtf8();
    send_message(OP_RENAME, ba.constData(), (uint32_t)ba.size());
}

// Salveaza fisier pe server
void send_save_request(const QString &filename, const QString &content)
{
    QString payload = filename + ":" + content;
    QByteArray ba = payload.toUtf8();
    send_message(OP_SAVE, ba.constData(), (uint32_t)ba.size());
}

// Cere fisier de la server
void send_load_request(const QString &filename)
{
    QByteArray ba = filename.toUtf8();
    send_message(OP_LOAD, ba.constData(), (uint32_t)ba.size());
}

// ======== Read exactly ========
// Citeste exact 'len' octeti de la socket
static int read_n(int fd, void *buf, uint32_t len)
{
    char *p = (char*)buf;
    uint32_t left = len;
    while (left > 0)
    {
        ssize_t r = recv(fd, p, left, 0);
        if (r <= 0) return r;
        p += r;
        left -= r;
    }
    return len;
}

// ======== Receive loop ========
// Thread care receptioneaza mesaje de la server
static void* recv_loop(void*)
{
    while (running)
    {
        uint32_t be_len;
        if (read_n(sockfd, &be_len, sizeof(be_len)) <= 0) break;

        uint32_t total_len = ntohl(be_len);
        if (total_len < 1) continue;

        uint8_t op;
        if (read_n(sockfd, &op, 1) <= 0) break;

        uint32_t payload_len = total_len - 1;
        char *payload = nullptr;
        if (payload_len > 0)
        {
            payload = (char*)malloc(payload_len + 1);
            if (read_n(sockfd, payload, payload_len) <= 0)
            {
                free(payload);
                break;
            }
            payload[payload_len] = '\0';
        }

        qDebug() << "Received op:" << op << "payload:" << (payload ? payload : "");

        if (gui)
        {
            switch(op)
            {
                case OP_EDIT:
                {
                    QString html = QString::fromUtf8(payload ? payload : "", payload_len);
                    QMetaObject::invokeMethod(gui, "applyRemoteEdit",
                                              Qt::QueuedConnection,
                                              Q_ARG(QString, html));
                    break;
                }

                case OP_CLEAR:
                {
                    QMetaObject::invokeMethod(gui, "applyRemoteClear",
                                              Qt::QueuedConnection);
                    break;
                }

                case OP_USERS:
                {
                    QMetaObject::invokeMethod(gui, "updateUserList",
                                              Qt::QueuedConnection,
                                              Q_ARG(QString, QString::fromUtf8(payload ? payload : "", payload_len)));
                    break;
                }

                case OP_FILE:
                {
                    QString html = QString::fromUtf8(payload ? payload : "", payload_len);
                    QMetaObject::invokeMethod(gui, "loadFromServer",
                                              Qt::QueuedConnection,
                                              Q_ARG(QString, html),
                                              Q_ARG(int, -1));
                    break;
                }

                case OP_STATUS:
                {
                    QString str = QString::fromUtf8(payload ? payload : "", payload_len);
                    QStringList parts = str.split(':');
                    if (parts.size() != 2) break;

                    QString file = parts[0];
                    int version = parts[1].toInt();
                    serverFileVersions[file] = version;

                    if (gui)
                    {
                        QMetaObject::invokeMethod(gui, "onServerVersion",
                                                  Qt::QueuedConnection,
                                                  Q_ARG(QString, file),
                                                  Q_ARG(int, version));
                    }

                    if (gui && file == gui->getCurrentFile() && gui->isLoadingFromServer())
                    {
                        QMetaObject::invokeMethod(gui, "setLocalVersion",
                                                  Qt::QueuedConnection,
                                                  Q_ARG(QString, file),
                                                  Q_ARG(int, version));
                        QMetaObject::invokeMethod(gui, "setServerVersion",
                                                  Qt::QueuedConnection,
                                                  Q_ARG(QString, file),
                                                  Q_ARG(int, version));
                        QMetaObject::invokeMethod(gui, "loadFromServerCompleted",
                                                  Qt::QueuedConnection);
                    }

                    break;
                }
                case OP_LOAD_DONE:
                {
                    QString str = QString::fromUtf8(payload, payload_len);
                    QStringList parts = str.split(':');
                    if (parts.size() == 2)
                    {
                        gui->setLocalVersion(parts[0], parts[1].toInt());
                        gui->setServerVersion(parts[0], parts[1].toInt());
                        gui->loadFromServerCompleted();
                    }
                    break;
                }

                default:
                {
                    break;
                }
            }
        }

        if (payload) free(payload);
    }

    if (sockfd >= 0)
    {
        close(sockfd);
        sockfd = -1;
    }

    return nullptr;
}

// ======== Start / stop ========
// Initiaza conexiunea cu serverul
void client_network_init(const char *ip, int port)
{
    connect_to_server(ip, port, "Guest");
}

// Porneste thread-ul de recepție
void client_network_start()
{
    running = 1;
    pthread_create(&recv_thread, nullptr, recv_loop, nullptr);
}

// Opreste thread-ul de recepție
void client_network_stop()
{
    running = 0;
    pthread_cancel(recv_thread);
    pthread_join(recv_thread, nullptr);

    if (sockfd >= 0) close(sockfd);
}
