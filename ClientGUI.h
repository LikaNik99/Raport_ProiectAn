#ifndef CLIENTGUI_H
#define CLIENTGUI_H

#include <QMainWindow>
#include <QTextEdit>
#include <QLineEdit>
#include <QListWidget>
#include <QStatusBar>
#include <QToolBar>
#include <QAction>
#include <QFontComboBox>
#include <QComboBox>
#include <QString>
#include <QLabel>
#include <QPropertyAnimation>
#include <QPushButton>
#include "NetworkWorker.h"

// Client GUI principal
class ClientGUI : public QMainWindow 
{
    Q_OBJECT

public:
    explicit ClientGUI(QWidget *parent = nullptr);

    // Seteaza versiunea locala a unui fisier
    void setLocalFileVersion(const QString &file, int version);

public slots:
    // Aplicari de continut venit de la server
    void applyRemoteEdit(const QString &html);
    void applyRemoteClear();
    void clearEditor();

    // Actualizare lista utilizatori si status
    void updateUserList(const QString &list);
    void updateStatus(const QString &msg);
    void onStatusChanged(const QString &statusStr);

    // Acces fisier curent
    QString getCurrentFile() const 
    { 
        return currentFile; 
    }

    void setCurrentFile(const QString &f) 
    { 
        currentFile = f; 
        currentVersion = 0; 
    }

    // Incarcare continut de la server
    void loadFromServer(const QString &html, int serverVersion);

    // Versiuni fisier curent
    int getCurrentVersion() const 
    { 
        return currentVersion; 
    }

    void setCurrentVersion(int v) 
    { 
        currentVersion = v; 
    }

    // Setari primite de la server
    void onServerVersion(const QString &file, int version);
    void loadFromServerCompleted();
    bool isLoadingFromServer() const;

    void setLocalVersion(const QString &file, int version);
    void setServerVersion(const QString &file, int version);

    // Nume utilizator
    void applyUsername();

    // ===== Formatare text =====
    void setBold();
    void setItalic();
    void setUnderline();
    void toggleBulletList();
    void toggleNumberList();
    void insertTable();
    void changeTextColor();
    void changeBackgroundColor();
    void changeFont(const QFont &font);
    void changeFontSize(const QString &size);

    // Out-of-date si sincronizare
    void setOutOfDate(bool outOfDate);
    void updateFromServer(); 

private:
    // ===== Elemente GUI =====
    QTextEdit *editor;
    QLineEdit *usernameBox;
    QListWidget *userList;
    QStatusBar *status;

    // ===== Toolbar pentru formatare =====
    QToolBar *formatBar;
    QAction *boldAction;
    QAction *italicAction;
    QAction *underlineAction;
    QAction *bulletAction;
    QAction *numberAction;
    QAction *tableAction;
    QAction *textColorAction;
    QAction *bgColorAction;
    QFontComboBox *fontBox;
    QComboBox *sizeBox;

    // ===== Actiuni fisier =====
    QAction *saveAction;
    QAction *loadAction;

    // ===== Info despre fisier si starea curenta =====
    bool hasUnsavedChanges;
    bool isSyncing = false;
    int localVersion = 0;
    int serverVersion = 0;
    QString currentFile;
    int currentVersion = 0;
    QLabel *currentFileLabel;
    QLabel *unsavedLabel;
    int localFileVersion = 0;
    int serverFileVersion = 0;
    QMap<QString, int> localFileVersions;
    QMap<QString, int> serverFileVersions;
    bool loadingFromServer = false;
    bool updatingFromServer = false;
    bool applyingServerContent;
    bool ignoreServerStatusTemporarily = false;

    // ===== Indicator sincronizare =====
    QLabel *syncIndicator;
    QPropertyAnimation *pulseAnimation;

    // ===== Indicator out-of-date =====
    QLabel *outOfDateLabel;
    QPushButton *updateBtn;

    // Previne trimiterea recursiva
    bool localEdit;

    // Worker retea
    NetworkWorker *networkWorker;

private slots:
    // Actualizeaza lista editorilor activi
    void updateActiveEditors(const QString &list);

    // Salveaza fisier
    void saveFile();

signals:
    // Semnal emis cand fisierul este incarcat de la server
    void fileLoadedFromServer(const QString &html, int serverVersion);

};

#endif // CLIENTGUI_H
