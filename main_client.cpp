#include <QApplication>
#include <QTimer>
#include <QDebug>
#include "ClientGUI.h"
#include "ClientNetwork.h"

ClientGUI *gui = nullptr; // global pointer folosit în thread-ul de rețea

int main(int argc, char *argv[]) 
{
    QApplication app(argc, argv);

    gui = new ClientGUI();
    gui->show();

    // --- Parse IP and port from command line ---
    QString serverIp = "127.0.0.1"; // default
    int serverPort = 8080;          // default

    if (argc >= 2) {
        serverIp = argv[1];         // first argument = IP
    }
    if (argc >= 3) {
        serverPort = QString(argv[2]).toInt(); // second argument = port
    }

    qDebug() << "Client connecting to" << serverIp << ":" << serverPort;

    // Connect to server after small delay
    QTimer::singleShot(500, [=]() {
        qDebug() << "Starting network init...";
        client_network_init(serverIp.toUtf8().constData(), serverPort);

        QTimer::singleShot(0, []() {
            qDebug("Starting network thread...");
            client_network_start();
        });
    });

    int ret = app.exec();

    client_network_stop();
    return ret;
}
