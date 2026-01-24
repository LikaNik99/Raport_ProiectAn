#include "FileManager.h"
#include "NetworkWorker.h"
#include <QFile>
#include <QTextStream>

// Constructor
FileManager::FileManager(QObject *parent) : QObject(parent)
{
}

// Salveaza fisier local
bool FileManager::saveLocal(const QString &fileName, const QString &content)
{
    QFile f(fileName);
    if(!f.open(QIODevice::WriteOnly | QIODevice::Text))
    {
        return false;
    }

    QTextStream out(&f);
    out << content;
    f.close();
    return true;
}

// Incarca fisier local
QString FileManager::loadLocal(const QString &fileName, bool &ok)
{
    QFile f(fileName);
    if(!f.open(QIODevice::ReadOnly | QIODevice::Text))
    {
        ok = false;
        return "";
    }

    QTextStream in(&f);
    QString content = in.readAll();
    f.close();
    ok = true;
    return content;
}

// Salveaza fisier pe server
void FileManager::saveToServer(const QString &fileName, const QString &content)
{
    NetworkWorker *worker = new NetworkWorker();
    worker->setServerInfo(serverHost, serverPort);

    // Conecteaza semnalul de succes
    connect(worker, &NetworkWorker::fileSent, this, [=](const QString &f)
    {
        emit fileSavedToServer(f);
        worker->quit();
        worker->deleteLater();
    });

    // Conecteaza semnalul de eroare
    connect(worker, &NetworkWorker::errorOccurred, this, [=](const QString &msg)
    {
        emit errorOccurred(msg);
        worker->quit();
        worker->deleteLater();
    });

    worker->sendFile(fileName, content);
}

// Incarca fisier de la server (inca neimplementat)
void FileManager::loadFromServer(const QString &fileName)
{
    emit errorOccurred("Server load not implemented yet");
}
