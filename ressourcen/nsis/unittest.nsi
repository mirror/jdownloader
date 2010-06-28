Name "JDownloader Setup Unittest"

RequestExecutionLevel admin

OutFile unittest.exe
!include "Sections.nsh"
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "Utilities.nsh"
!include "Registry.nsh"
!insertmacro MUI_PAGE_LICENSE ".\ressourcen\unittest-info.txt"
Page custom "Select" "Validate"
!insertmacro MUI_PAGE_INSTFILES

!define APPNAME_SHORT "JDownloader"

Function "Select"
    InstallOptions::dialog "$PLUGINSDIR\unittest.ini"
FunctionEnd

Function "Validate"
    ReadINIStr $0 "$PLUGINSDIR\unittest.ini" "field 3" "state"
    ReadINIStr $1 "$PLUGINSDIR\unittest.ini" "field 5" "state"
    StrCmp $0 "" 0 +3
    MessageBox MB_OK|MB_ICONSTOP "You must input the current installation state..."
    Abort
    IfFileExists "$1\$0.txt" 0 +3
    MessageBox MB_OK|MB_ICONSTOP "There's already a dump for this installation state"
    Abort
    StrCmp $1 "" 0 +3
    MessageBox MB_OK|MB_ICONSTOP "You must select a folder to save the file..."
    Abort
    StrCpy $INSTDIR $1
FunctionEnd

Function ".onInit"
InitPluginsDir
File /oname=$PLUGINSDIR\unittest.ini ".\ressourcen\unittest.ini"
WriteIniStr "$PLUGINSDIR\unittest.ini" "field 5" "state" "$EXEDIR\output"
FunctionEnd

Function ".onInstSuccess"
ReadINIStr $0 "$PLUGINSDIR\unittest.ini" "field 3" "state"
ExecShell "open" "$INSTDIR\$0.txt"
FunctionEnd

CRCCheck on
XPStyle on
ShowInstDetails  show
Var success

!macro dump REGKEY
    ${registry::SaveKey} "${REGKEY}" "$INSTDIR\$0.txt" "/A=1" $success
!macroend

!macro dump2 REGKEY
    !insertmacro dump "HKLM\${REGKEY}"
    !insertmacro dump "HKCU\${REGKEY}"
!macroend

!macro dump3 REGKEY
    !insertmacro dump "HKCR\${REGKEY}"
    !insertmacro dump "HKCU\${REGKEY}"
!macroend

Function WriteToFile
 Exch $0 ;file to write to
 Exch
 Exch $1 ;text to write
 
  FileOpen $0 $0 a #open file
   FileSeek $0 0 END #go to end
   FileWrite $0 $1 #write to file
  FileClose $0
 
 Pop $1
 Pop $0
FunctionEnd
 
!macro WriteToFile String File
 Push "${String}"
 Push "${File}"
  Call WriteToFile
!macroend
!define WriteToFile "!insertmacro WriteToFile"

Section DumpRegistry
    ReadINIStr $0 "$PLUGINSDIR\unittest.ini" "field 3" "state"
    SetOutPath $INSTDIR 
    
    DetailPrint "Dumping general registry information"
    ${WriteToFile} ";Dumping general registry information$\r$\n" "$INSTDIR\$0.txt"
    !insertmacro dump2 "Software\${APPNAME_SHORT}"
    !insertmacro dump2 "Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}"
    
    
    #fileassoc
    DetailPrint "Dumping file association registry information"
    ${WriteToFile} ";Dumping file association registry information$\r$\n" "$INSTDIR\$0.txt"
    !insertmacro dump "HKCR\.dlc"
    !insertmacro dump "HKCR\JDownloader DLC Container"
    !insertmacro dump "HKCU\Software\Classes\.dlc"
    !insertmacro dump "HKCU\Software\Classes\JDownloader DLC Container"
    
    #protocolassoc
    DetailPrint "Dumping protocol association registry information"
    ${WriteToFile} ";Dumping protocol association registry information$\r$\n" "$INSTDIR\$0.txt"
    !insertmacro dump "HKCR\metalink"
    !insertmacro dump "HKCU\Software\Classes\metalink"
SectionEnd

