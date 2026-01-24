#include "ClientGUI.h"
#include "ClientNetwork.h"
#include <QtWidgets>
#include <QTimer>
#include <QPropertyAnimation>
#include "NetworkWorker.h"
#include "FileManager.h"

/* =========================
   CONSTRUCTOR
========================= */

ClientGUI::ClientGUI(QWidget *parent)
    : QMainWindow(parent),
      localEdit(true),
      hasUnsavedChanges(false),
      loadingFromServer(false),
      isSyncing(false),
      applyingServerContent(false)
{
    setWindowTitle("Notes");
    resize(1000, 650);
    currentFile = "";

    status = statusBar();

    QWidget *central = new QWidget();
    QHBoxLayout *mainLayout = new QHBoxLayout();

    /* ===== SIDEBAR ===== */
    QVBoxLayout *sideLayout = new QVBoxLayout();

    usernameBox = new QLineEdit();
    QPushButton *applyBtn = new QPushButton("Set Username");
    QPushButton *clearBtn = new QPushButton("Clear");
    QPushButton *saveBtn = new QPushButton("Save");
    QPushButton *loadBtn = new QPushButton("Load");

    sideLayout->addWidget(usernameBox);
    sideLayout->addWidget(applyBtn);
    sideLayout->addWidget(clearBtn);
    sideLayout->addWidget(saveBtn);
    sideLayout->addWidget(loadBtn);

    currentFileLabel = new QLabel("File: No file");
    unsavedLabel = new QLabel("Status: Saved");

    syncIndicator = new QLabel();
    syncIndicator->setFixedSize(16, 16);
    syncIndicator->setStyleSheet("border-radius:8px; background-color:green;");

    QVBoxLayout *infoLayout = new QVBoxLayout();
    infoLayout->addWidget(currentFileLabel);
    infoLayout->addWidget(unsavedLabel);

    QHBoxLayout *statusLayout = new QHBoxLayout();
    statusLayout->addWidget(syncIndicator);
    statusLayout->addLayout(infoLayout);

    sideLayout->addLayout(statusLayout);

    outOfDateLabel = new QLabel("Up to date");
    outOfDateLabel->setStyleSheet("color: green; font-weight: bold;");
    updateBtn = new QPushButton("Update");
    updateBtn->setVisible(false);

    sideLayout->addWidget(outOfDateLabel);
    sideLayout->addWidget(updateBtn);

    userList = new QListWidget();
    sideLayout->addWidget(userList);
    sideLayout->addStretch();

    editor = new QTextEdit();
    editor->setAcceptRichText(true);

    mainLayout->addLayout(sideLayout, 1);
    mainLayout->addWidget(editor, 4);
    central->setLayout(mainLayout);
    setCentralWidget(central);

    pulseAnimation = new QPropertyAnimation(syncIndicator, "geometry", this);
    pulseAnimation->setDuration(800);
    pulseAnimation->setLoopCount(-1);

    QToolBar *toolbar = addToolBar("Formatting");

    boldAction = toolbar->addAction("B");
    boldAction->setCheckable(true);
    connect(boldAction, &QAction::triggered, this, &ClientGUI::setBold);

    italicAction = toolbar->addAction("I");
    italicAction->setCheckable(true);
    connect(italicAction, &QAction::triggered, this, &ClientGUI::setItalic);

    underlineAction = toolbar->addAction("U");
    underlineAction->setCheckable(true);
    connect(underlineAction, &QAction::triggered, this, &ClientGUI::setUnderline);

    toolbar->addSeparator();

    QAction *bullet = toolbar->addAction("• List");
    connect(bullet, &QAction::triggered, this, &ClientGUI::toggleBulletList);

    QAction *numbered = toolbar->addAction("1. List");
    connect(numbered, &QAction::triggered, this, &ClientGUI::toggleNumberList);

    QAction *table = toolbar->addAction("Table");
    connect(table, &QAction::triggered, this, &ClientGUI::insertTable);

    toolbar->addSeparator();

    QFontComboBox *fontBox = new QFontComboBox();
    connect(fontBox, &QFontComboBox::currentFontChanged,
            this, &ClientGUI::changeFont);
    toolbar->addWidget(fontBox);

    QComboBox *fontSize = new QComboBox();
    for (int i = 8; i <= 32; i += 2)
    {
        fontSize->addItem(QString::number(i));
    }
    connect(fontSize, &QComboBox::currentTextChanged,
            this, &ClientGUI::changeFontSize);
    toolbar->addWidget(fontSize);

    QAction *textColor = toolbar->addAction("Text Color");
    connect(textColor, &QAction::triggered, this, &ClientGUI::changeTextColor);

    QAction *bgColor = toolbar->addAction("Bg Color");
    connect(bgColor, &QAction::triggered, this, &ClientGUI::changeBackgroundColor);

    QAction *saveAction = new QAction(this);
    saveAction->setShortcut(QKeySequence::Save);
    addAction(saveAction);
    connect(saveAction, &QAction::triggered, this, &ClientGUI::saveFile);

    networkWorker = new NetworkWorker(this);
    networkWorker->setServerInfo("127.0.0.1", 8080);

    connect(applyBtn, &QPushButton::clicked, this, &ClientGUI::applyUsername);
    connect(networkWorker, &NetworkWorker::activeEditorsReceived,
            this, &ClientGUI::updateUserList);

    /* =========================
       SIGNALS
    ========================= */

    connect(editor, &QTextEdit::textChanged, this, [this]()
    {
        if (!localEdit || applyingServerContent)
            return;

        hasUnsavedChanges = true;
        unsavedLabel->setText("Status: Unsaved changes");
        syncIndicator->setStyleSheet("background-color:orange;");
        pulseAnimation->start();

        send_edit_to_server(editor->toHtml());
    });

    connect(updateBtn, &QPushButton::clicked, this, &ClientGUI::updateFromServer);

    connect(loadBtn, &QPushButton::clicked, this, [this]()
    {
        QString fileName = QFileDialog::getOpenFileName(this, "Open File");
        if(fileName.isEmpty())
            return;

        currentFile = fileName;
        currentFileLabel->setText("File: " + currentFile);

        loadingFromServer = true;
        unsavedLabel->setText("Status: Loading...");
        status->showMessage("Loading " + currentFile);

        send_load_request(currentFile);
    });

    connect(clearBtn, &QPushButton::clicked, this, &ClientGUI::clearEditor);
    connect(saveBtn, &QPushButton::clicked, this, &ClientGUI::saveFile);
}

/* =========================
   APLICARE CONȚINUT SERVER
========================= */

void ClientGUI::updateActiveEditors(const QString &users)
{
    userList->clear();
    for (const QString &u : users.split(','))
    {
        if (!u.isEmpty())
            userList->addItem(u);
    }
}


void ClientGUI::updateUserList(const QString &list)
{
    userList->clear();
    for (const QString &u : list.split(','))
    {
        if (!u.isEmpty())
            userList->addItem(u);
    }
}


void ClientGUI::updateStatus(const QString &msg)
{
    statusBar()->showMessage(msg);
}


void ClientGUI::onServerVersion(const QString &file, int version)
{
    serverFileVersions[file] = version;

    if(file != currentFile)
        return;

    if (applyingServerContent || loadingFromServer || updatingFromServer)
        return;

    int localVersion = localFileVersions.value(file, 0);

    if(localVersion < version)
    {
        setOutOfDate(true);
        unsavedLabel->setText("Status: Out of date");
        syncIndicator->setStyleSheet("background-color:red;");
    }
    else
    {
        setOutOfDate(false);
        unsavedLabel->setText("Status: Synced");
        syncIndicator->setStyleSheet("background-color:green;");
    }
}


void ClientGUI::loadFromServer(const QString &content, int version)
{
    if(currentFile.isEmpty())
        return;

    applyingServerContent = true;
    localEdit = false;

    editor->blockSignals(true);
    editor->setHtml(content);
    editor->blockSignals(false);
    localEdit = true;

    localFileVersions[currentFile] = version;
    serverFileVersions[currentFile] = version;

    hasUnsavedChanges = false;
    loadingFromServer = false;
    updatingFromServer = false;
    ignoreServerStatusTemporarily = false;

    unsavedLabel->setText("Status: Up to date");
    syncIndicator->setStyleSheet("background-color:green;");
    pulseAnimation->stop();

    setOutOfDate(false);
    status->clearMessage();

    applyingServerContent = false;
}


void ClientGUI::applyRemoteClear()
{
    applyingServerContent = true;

    localEdit = false;
    editor->blockSignals(true);
    editor->clear();
    editor->blockSignals(false);
    localEdit = true;

    hasUnsavedChanges = false;
    unsavedLabel->setText("Status: Synced");
    syncIndicator->setStyleSheet("border-radius:8px; background-color:green;");
    pulseAnimation->stop();

    setOutOfDate(false);

    applyingServerContent = false;
}


void ClientGUI::applyRemoteEdit(const QString &html)
{
    applyingServerContent = true;

    localEdit = false;
    editor->blockSignals(true);
    editor->setHtml(html);
    editor->blockSignals(false);
    localEdit = true;

    int serverVersion = serverFileVersions.value(currentFile, 0);
    localFileVersions[currentFile] = serverVersion;

    hasUnsavedChanges = false;
    isSyncing = false;
    updatingFromServer = false;
    loadingFromServer = false;

    unsavedLabel->setText("Status: Up to date");
    syncIndicator->setStyleSheet("background-color:green;");
    pulseAnimation->stop();
    status->clearMessage();

    setOutOfDate(false);

    applyingServerContent = false;
}
/* =========================
   STATUS SERVER
========================= */

void ClientGUI::onStatusChanged(const QString &statusStr)
{
    if (applyingServerContent || loadingFromServer || updatingFromServer)
        return;

    QStringList parts = statusStr.split(":");
    if (parts.size() != 2)
        return;

    QString file = parts[0];
    int serverVersion = parts[1].toInt();

    serverFileVersions[file] = serverVersion;

    if (file != currentFile)
        return;

    int localVersion = localFileVersions.value(file, 0);

    if (localVersion < serverVersion)
    {
        setOutOfDate(true);
        unsavedLabel->setText("Status: Out of date");
        syncIndicator->setStyleSheet("background-color:red;");
    }
    else
    {
        setOutOfDate(false);
        unsavedLabel->setText("Status: Synced");
        syncIndicator->setStyleSheet("background-color:green;");
    }
}


/* =========================
   UPDATE FROM SERVER
========================= */

void ClientGUI::updateFromServer()
{
    if(currentFile.isEmpty())
        return;

    updatingFromServer = true;
    loadingFromServer = true;

    unsavedLabel->setText("Status: Loading...");
    syncIndicator->setStyleSheet("background-color:orange;");

    send_load_request(currentFile);
}


/* =========================
   UI HELPERS
========================= */

void ClientGUI::setOutOfDate(bool out)
{
    if (out)
    {
        outOfDateLabel->setText("Out of date");
        outOfDateLabel->setStyleSheet("color:red; font-weight:bold;");
        updateBtn->setVisible(true);
    }
    else
    {
        outOfDateLabel->setText("Up to date");
        outOfDateLabel->setStyleSheet("color:green; font-weight:bold;");
        updateBtn->setVisible(false);
    }
}


/* =========================
   CLEAR / SAVE
========================= */

void ClientGUI::clearEditor()
{
    localEdit = false;
    editor->clear();
    localEdit = true;

    currentFile = "";
    currentFileLabel->setText("File: No file");

    hasUnsavedChanges = false;
    loadingFromServer = false;
    updatingFromServer = false;
    applyingServerContent = false;

    unsavedLabel->setText("Status: Blank");
    syncIndicator->setStyleSheet("background-color:gray;");
    pulseAnimation->stop();

    setOutOfDate(false);
    status->clearMessage();
}


void ClientGUI::saveFile()
{
    if (currentFile.isEmpty())
    {
        QString fileName = QFileDialog::getSaveFileName(this, "Save File As");
        if (fileName.isEmpty())
            return;

        currentFile = fileName;
        currentFileLabel->setText("File: " + currentFile);
    }

    FileManager::saveLocal(currentFile, editor->toHtml());
    send_save_request(currentFile, editor->toHtml());

    hasUnsavedChanges = false;

    unsavedLabel->setText("Status: Up to date");
    syncIndicator->setStyleSheet("background-color:green;");
    pulseAnimation->stop();

    setOutOfDate(false);

    status->showMessage("Saved", 1500);
}


void ClientGUI::setLocalVersion(const QString &file, int version)
{
    localFileVersions[file] = version;
}


void ClientGUI::setServerVersion(const QString &file, int version)
{
    serverFileVersions[file] = version;
}


void ClientGUI::loadFromServerCompleted()
{
    loadingFromServer = false;
    updatingFromServer = false;
    hasUnsavedChanges = false;

    unsavedLabel->setText("Status: Up to date");
    syncIndicator->setStyleSheet("background-color:green;");
    pulseAnimation->stop();

    setOutOfDate(false);
}


bool ClientGUI::isLoadingFromServer() const
{
    return loadingFromServer;
}


void ClientGUI::applyUsername()
{
    QString name = usernameBox->text().trimmed();
    if(name.isEmpty())
    {
        status->showMessage("Username cannot be empty", 1500);
        return;
    }
    else
    {
        send_username_change_request(name);
        status->showMessage("Username set: " + name, 1500);
    }
}


/* =========================
   FORMATTING
========================= */

void ClientGUI::setBold()
{
    QTextCharFormat fmt;
    fmt.setFontWeight(boldAction->isChecked() ? QFont::Bold : QFont::Normal);
    editor->mergeCurrentCharFormat(fmt);
}


void ClientGUI::setItalic()
{
    QTextCharFormat fmt;
    fmt.setFontItalic(italicAction->isChecked());
    editor->mergeCurrentCharFormat(fmt);
}


void ClientGUI::setUnderline()
{
    QTextCharFormat fmt;
    fmt.setFontUnderline(underlineAction->isChecked());
    editor->mergeCurrentCharFormat(fmt);
}


void ClientGUI::toggleBulletList()
{
    QTextCursor c = editor->textCursor();
    c.beginEditBlock();
    QTextListFormat l;
    l.setStyle(QTextListFormat::ListDisc);
    c.createList(l);
    c.endEditBlock();
}


void ClientGUI::toggleNumberList()
{
    QTextCursor c = editor->textCursor();
    c.beginEditBlock();
    QTextListFormat l;
    l.setStyle(QTextListFormat::ListDecimal);
    c.createList(l);
    c.endEditBlock();
}


void ClientGUI::insertTable()
{
    QTextCursor c = editor->textCursor();
    c.beginEditBlock();
    c.insertTable(2,2);
    c.endEditBlock();
}


void ClientGUI::changeFont(const QFont &font)
{
    QTextCharFormat fmt;
    fmt.setFont(font);
    editor->mergeCurrentCharFormat(fmt);
}


void ClientGUI::changeFontSize(const QString &size)
{
    QTextCharFormat fmt;
    fmt.setFontPointSize(size.toDouble());
    editor->mergeCurrentCharFormat(fmt);
}


void ClientGUI::changeTextColor()
{
    QColor c = QColorDialog::getColor(Qt::black, this, "Text Color");
    if (c.isValid())
    {
        QTextCharFormat fmt;
        fmt.setForeground(c);
        editor->mergeCurrentCharFormat(fmt);
    }
}


void ClientGUI::changeBackgroundColor()
{
    QColor c = QColorDialog::getColor(Qt::white, this, "Background Color");
    if (c.isValid())
    {
        QTextCharFormat fmt;
        fmt.setBackground(c);
        editor->mergeCurrentCharFormat(fmt);
    }
}
