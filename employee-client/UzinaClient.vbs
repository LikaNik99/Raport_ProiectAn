Set objShell = CreateObject("WScript.Shell")
Set objFSO = CreateObject("Scripting.FileSystemObject")

' Get the directory where this script is located
scriptDir = objFSO.GetParentFolderName(WScript.ScriptFullName)

' Paths
batchFile = objFSO.BuildPath(scriptDir, "run-client.bat")
jarFile = objFSO.BuildPath(scriptDir, "client-login\target\client-login.jar")

' Check if JAR exists
If Not objFSO.FileExists(jarFile) Then
    objShell.Popup "Error: client-login.jar not found." & vbCrLf & "Please build the client first using Maven." & vbCrLf & vbCrLf & "Command: mvn clean package -DskipTests", 0, "Client Logare - Eroare Build", 48
    WScript.Quit 1
End If

' Run the batch file with hidden window
objShell.Run Chr(34) & batchFile & Chr(34), 0, False

WScript.Quit 0
