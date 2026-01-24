#ifndef NETWORKWORKER_H
#define NETWORKWORKER_H

#include <QThread>
#include <QTcpSocket>
#include <QString>
#include <QObject>

class NetworkWorker : public QThread
{
    Q_OBJECT
public:
    explicit NetworkWorker(QObject *parent = nullptr);
    ~NetworkWorker();

    void setServerInfo(const QString &host, quint16 port);
    void sendFile(const QString &fileName, const QString &content);
    void requestFile(const QString &fileName);
    void sendUsername(const QString &username);

signals:
    void fileSent(const QString &fileName);
    void fileReceived(const QString &fileName, const QString &content);
    void errorOccurred(const QString &msg);
    void activeEditorsReceived(const QString &);

protected:
    void run() override;

private:
    QString serverHost;
    quint16 serverPort;
    QString pendingFileName;
    QString pendingFileContent;
    enum Operation { None, SendFile, RequestFile, SendUsername } op;
    QTcpSocket *socket;
};

#endif // NETWORKWORKER_H
