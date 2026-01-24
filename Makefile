# Makefile - builds Server (C) and Client (C++ + Qt5)

# Compilatoare
CC = gcc
CXX = g++
CFLAGS = -Wall -pthread -O2 -fPIC -g
CXXFLAGS = -Wall -pthread -O2 -fPIC -std=c++17 -g

# Qt detection (Qt5 preferred)
PKG_QT5 := $(shell pkg-config --exists Qt5Widgets && echo yes)
ifeq ($(PKG_QT5),yes)
    QT_CFLAGS := $(shell pkg-config --cflags Qt5Widgets Qt5Network)
    QT_LIBS   := $(shell pkg-config --libs Qt5Widgets Qt5Gui Qt5Core Qt5Network)
else
    QT_CFLAGS := -I/usr/include/x86_64-linux-gnu/qt5 \
                 -I/usr/include/x86_64-linux-gnu/qt5/QtWidgets \
                 -I/usr/include/x86_64-linux-gnu/qt5/QtGui \
                 -I/usr/include/x86_64-linux-gnu/qt5/QtCore \
                 -I/usr/include/x86_64-linux-gnu/qt5/QtNetwork
    QT_LIBS := -L/usr/lib/x86_64-linux-gnu -lQt5Widgets -lQt5Gui -lQt5Core -lQt5Network
endif

# Surse
CLIENT_SRC = main_client.cpp ClientNetwork.cpp ClientGUI.cpp NetworkWorker.cpp FileManager.cpp
CLIENT_HEADERS = Protocol.h ClientNetwork.h ClientGUI.h NetworkWorker.h FileManager.h

# Ținte
all: Server Client

# Server (C + SQLite)
Server: Server.c Protocol.h
	$(CC) $(CFLAGS) Server.c -o build/Server -lsqlite3 -pthread

# Generate moc files for Qt classes
moc_ClientGUI.cpp: ClientGUI.h
	moc ClientGUI.h -o build/moc_ClientGUI.cpp

moc_NetworkWorker.cpp: NetworkWorker.h
	moc NetworkWorker.h -o build/moc_NetworkWorker.cpp

moc_FileManager.cpp: FileManager.h
	moc FileManager.h -o build/moc_FileManager.cpp

# Client (C++ + Qt5)
Client: $(CLIENT_SRC) $(CLIENT_HEADERS) moc_ClientGUI.cpp moc_NetworkWorker.cpp moc_FileManager.cpp
	$(CXX) $(CXXFLAGS) $(QT_CFLAGS) $(CLIENT_SRC) build/moc_ClientGUI.cpp build/moc_NetworkWorker.cpp build/moc_FileManager.cpp -o build/Client $(QT_LIBS) -lpthread

# Clean
clean:
	rm -f build/Server build/Client build/moc_ClientGUI.cpp build/moc_NetworkWorker.cpp build/moc_FileManager.cpp
