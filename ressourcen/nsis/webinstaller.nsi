Name "JDownloader"
OutFile ".\..\..\dist\WebInstaller.exe"
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

# default section start; every NSIS script has at least one section.
Section
    StrCpy $0 $HWNDPARENT
    System::Call "user32::ShowWindow(i r0, i 0)"
    #http://nsis.sourceforge.net/Inetc_plug-in
    
    IntOp $2 0 + 0 #count    
    
    #This might not be the reference implementation for a random number,
    #but it's working and it's working good.
    System::Call kernel32::GetTickCount()i.r3
    IntOp $3 $3 % 4
    
    ${DoWhile} $2 < 3
        inetc::get /caption $(DownloadCaption) /useragent "JDownloaderSetup_inetc" /popup "JDownloaderSetup.exe" /translate $(inetc_url) $(inetc_downloading) $(inetc_connecting) $(inetc_file_name) $(inetc_received) $(inetc_file_size) $(inetc_remaining_time) $(inetc_total_time) "http://update$3.jdownloader.org/webinstall/JDownloaderSetup.exe" "$TEMP\JDownloaderSetup.exe"
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
    Quit
SectionEnd