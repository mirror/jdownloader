Name "JDownloader"
OutFile ".\dist\WebInstaller.exe"
CRCCheck on
XPStyle on
SetCompressor zlib #Don't use lzma here as filesize doesn't matter as long as it's <1MB

!define COMPANY "AppWork UG (haftungsbeschränkt)"
!define URL http://www.jdownloader.org
!define APPNAME "JDownloader"


!define VERSION 1.0.0.0

!include "MUI.nsh"
!define MUI_ICON .\res\install.ico
!insertmacro MUI_PAGE_INSTFILES

!AddPluginDir plugins

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
    inetc::get /caption $(DownloadCaption) /popup "JDownloaderSetup.exe" /translate $(inetc_url) $(inetc_downloading) $(inetc_connecting) $(inetc_file_name) $(inetc_received) $(inetc_file_size) $(inetc_remaining_time) $(inetc_total_time) "http://www.wayaround.org/TestSetup2.exe" "$TEMP\JDownloaderSetup.exe"
    Pop $1
    StrCmp "OK" $1 0 +2
    Exec '"$TEMP\JDownloaderSetup.exe"'
    Quit
SectionEnd