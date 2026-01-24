#include "NetworkWorker.h"
#include <QDataStream>
#include <QDebug>
#include <QTcpSocket>

// Constructor
NetworkWorker::NetworkWorker(QObject *parent)
    : QThread(parent), op(None)
{
}

// Destructor
NetworkWorker::~NetworkWorker()
{
    if(socket)
    {
        socket->disconnectFromHost();
    }
    quit();
    wait();
}

// Seteaza informatiile serverului (host si port)
void NetworkWorker::setServerInfo(const QString &host, quint16 port)
{
    serverHost = host;
    serverPort = port;
}

// Trimite un fisier la server
void NetworkWorker::sendFile(const QString &fileName, const QString &content)
{
    pendingFileName = fileName;
    pendingFileContent = content;
    op = SendFile;

    if(!isRunning())
    {
        start();
    }
}

// Cere un fisier de la server
void NetworkWorker::requestFile(const QString &fileName)
{
    pendingFileName = fileName;
    op = RequestFile;

    if(!isRunning())
    {
        start();
    }
}

// Trimite username la server
void NetworkWorker::sendUsername(const QString &username)
{
    pendingFileName = username;
    op = SendUsername;

    if(!isRunning())
    {
        start();
    }
}

// Functie principala a thread-ului
void NetworkWorker::run()
{
    QTcpSocket socket; // socket local in thread-ul curent
    socket.connectToHost(serverHost, serverPort);

    if(!socket.waitForConnected(3000))
    {
        emit errorOccurred("Cannot connect to server");
        op = None;
        return;
    }

    QDataStream out(&socket);
    out.setVersion(QDataStream::Qt_5_0);

    switch(op)
    {
        case SendFile:
        {
            // Construim payload-ul cu nume fisier + continut
            QByteArray payload;
            QDataStream payloadStream(&payload, QIODevice::WriteOnly);
            payloadStream << pendingFileName << pendingFileContent;

            // Trimitem dimensiunea si apoi payload-ul
            out << (quint32)payload.size();
            socket.write(payload);
            socket.flush();
            socket.waitForBytesWritten(3000);

            emit fileSent(pendingFileName);
            qDebug() << "File sent:" << pendingFileName;
            break;
        }

        case SendUsername:
        {
            // Payload-ul este doar UTF-8 al username-ului
            QByteArray payload = pendingFileName.toUtf8();
            out << (quint32)payload.size();
            socket.write(payload);
            socket.flush();
            socket.waitForBytesWritten(3000);

            qDebug() << "Username sent:" << pendingFileName;
            break;
        }

        case RequestFile:
        {
            // Inca neimplementat
            emit errorOccurred("Request file not implemented yet");
            break;
        }

        case None:
        default:
        {
            // Nu avem operatiune de efectuat
            break;
        }
    }

    op = None;

    if(socket.state() == QAbstractSocket::ConnectedState)
    {
        socket.disconnectFromHost();
    }
}
