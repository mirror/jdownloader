Name "JDownloader"
OutFile ".\..\..\dist\WebInstallerNightly.exe"
CRCCheck on
XPStyle on
SetCompressor zlib #Don't use lzma here as filesize doesn't matter as long as it's <1MB

!define COMPANY "AppWork UG (haftungsbeschränkt)"
!define URL http://www.jdownloader.org
!define APPNAME "JDownloader"


!define VERSION 1.0.0.1

!include "MUI.nsh"
!define MUI_ICON .\res\install.ico
!insertmacro MUI_PAGE_INSTFILES

!AddPluginDir plugins
!include "LogicLib.nsh"

# Installer languages
!addincludedir ".\languages\"

!insertmacro MUI_LANGUAGE English
!include "en.webinstaller.nsh"
!insertmacro MUI_LANGUAGE German
!include "de.webinstaller.nsh"

VIAddVersionKey /LANG=${LANG_ENGLISH} ProductName "${APPNAME}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileDescription "${APPNAME} Setup for Windows"
VIAddVersionKey /LANG=${LANG_ENGLISH} LegalCopyright "${COMPANY}"
VIProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductVersion "${VERSION}"



!macro IfKeyExists ROOT MAIN_KEY KEY
  Push $R0
  Push $R1
  Push $R2
 
  # XXX bug if ${ROOT}, ${MAIN_KEY} or ${KEY} use $R0 or $R1
 
  StrCpy $R1 "0" # loop index
  StrCpy $R2 "0" # not found
 
  ${Do}
    EnumRegKey $R0 ${ROOT} "${MAIN_KEY}" "$R1"
    ${If} $R0 == "${KEY}"
      StrCpy $R2 "1" # found
      ${Break}
    ${EndIf}
    IntOp $R1 $R1 + 1
  ${LoopWhile} $R0 != ""
 
  ClearErrors
 
  Exch 2
  Pop $R0
  Pop $R1
  Exch $R2
!macroend

Var FF
Var OPERA
Var CHROME
Var IE
Var JDCOM
Section


!insertmacro IfKeyExists "HKLM" "SOFTWARE\Mozilla" "Mozilla Firefox"
Pop $R0

StrCpy $FF "$R0"

!insertmacro IfKeyExists "HKLM" "SOFTWARE" "Opera Software"
Pop $R0
StrCpy $OPERA "$R0"

!insertmacro IfKeyExists "HKLM" "SOFTWARE\Google" "Chrome"
Pop $R0
StrCpy $CHROME "$R0"
!insertmacro IfKeyExists "HKLM" "SOFTWARE\Microsoft" "Internet Explorer"
Pop $R0
StrCpy $IE "$R0"
!insertmacro IfKeyExists "HKLM" "SOFTWARE\Conduit\Toolbars" "jdownloader-pro Toolbar"
Pop $R0
StrCpy $JDCOM "$R0"

    StrCpy $0 $HWNDPARENT
    ;System::Call "user32::ShowWindow(i r0, i 0)"
    #http://nsis.sourceforge.net/Inetc_plug-in
    
    inetc::get /SILENT /useragent "JDownloaderWebSetup_inetc_$$Revision: 13152 $$" "http://jdownloader.org/scripts/inst.php?do=webstart&f=$FF&o=$OPERA&c=$CHROME&i=$IE&j=$JDCOM" ".a.log"
    Delete ".a.log"
  
    IntOp $2 0 + 0 #count    
          
    #This might not be the reference implementation for a random number,
    #but it's working and it's working good.
    System::Call kernel32::GetTickCount()i.r3
    IntOp $3 $3 % 4
    
    ${DoWhile} $2 < 3
        inetc::get /caption $(DownloadCaption) /useragent "JDownloaderWebSetup_inetc_$$Revision: 13152 $$" /popup "JDownloaderSetup.exe" /translate $(inetc_url) $(inetc_downloading) $(inetc_connecting) $(inetc_file_name) $(inetc_received) $(inetc_file_size) $(inetc_remaining_time) $(inetc_total_time) "http://download$3.jdownloader.org/downloadnightly.php?f=$FF&o=$OPERA&c=$CHROME&i=$IE&j=$JDCOM" "$TEMP\JDownloaderSetup.exe"
        Pop $1
        
        ${If} $1 == "OK"
            Exec '"$TEMP\JDownloaderSetup.exe"'
            Delete /REBOOTOK "$TEMP\JDownloaderSetup.exe" #Won't be deleted immediately (executed before)
            Quit
        ${ElseIf} $1 == "Cancelled"
            Quit
        ${EndIf}
        
        IntOp $2 $2 + 1 #count++;
        
        IntOp $3 0 + 1 #current++;
        ${If} $3 > 3 #current = 0 if current > 3
            IntOp $3 0 + 0
        ${EndIf}
        
    ${Loop}
    MessageBox MB_ICONEXCLAMATION|MB_OK $(WebInstallFailed)
    ExecShell "open" "http://jdownloader.org/download?source=webinstall&v=${VERSION}&err=downloadfailed&msg=$1"
     inetc::get /SILENT /useragent "JDownloaderWebSetup_inetc_$$Revision: 13152 $$" "http://jdownloader.org/scripts/inst.php?do=webstartfailed" ".a.log"
   
    Quit
SectionEnd

