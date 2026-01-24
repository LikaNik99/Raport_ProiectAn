' Client Logare Launcher VBScript
' Poate fi folosit direct sau convertit în EXE cu un tool precum Launch4j
' Script-ul ascunde fereastra de comandă și pornește aplicația Java

Set objShell = CreateObject("WScript.Shell")
Set objFSO = CreateObject("Scripting.FileSystemObject")

' Get the directory where this script is located
scriptDir = objFSO.GetParentFolderName(WScript.ScriptFullName)
clientDir = objFSO.GetParentFolderName(scriptDir)

' Paths
jarFile = objFSO.BuildPath(scriptDir, "target\client-login.jar")
javaExe = "C:\Users\VOFF\.jdk\jdk-21.0.8\bin\java.exe"

' Check if JAR exists
If Not objFSO.FileExists(jarFile) Then
    objShell.Popup "Eroare: JAR Client Logare nu a fost găsit." & vbCrLf & vbCrLf & "Cale: " & jarFile & vbCrLf & vbCrLf & "Vă rugăm să compilați mai întâi clientul:" & vbCrLf & "  cd client-logare" & vbCrLf & "  mvn clean package -DskipTests", 0, "Client Logare - Eroare", 48
    WScript.Quit 1
End If

' Check if Java exists
If Not objFSO.FileExists(javaExe) Then
    objShell.Popup "Eroare: Java 21 nu a fost găsit la:" & vbCrLf & javaExe & vbCrLf & vbCrLf & "Vă rugăm să instalați Java 21 sau să actualizați calea.", 0, "Client Logare - Eroare Java", 48
    WScript.Quit 1
End If

' Run the client application with hidden window
objShell.CurrentDirectory = scriptDir
objShell.Run Chr(34) & javaExe & Chr(34) & " -jar " & Chr(34) & jarFile & Chr(34), 0, False

WScript.Quit 0
