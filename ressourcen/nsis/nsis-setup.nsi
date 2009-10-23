Name JDownloader

RequestExecutionLevel admin

# General Symbol Definitions
!define REGKEY "SOFTWARE\$(^Name)"
!define VERSION 0.89
!define COMPANY "AppWork UG (haftungsbeschränkt)"
!define URL http://www.jdownloader.org

# MUI Symbol Definitions
!define MUI_ICON C:\JD-Install\install.ico
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_FINISHPAGE_RUN $INSTDIR\JDownloader.exe
!define MUI_UNICON C:\JD-Install\uninstall.ico
!define MUI_UNFINISHPAGE_NOAUTOCLOSE

# Included files
!include Sections.nsh
!include MUI2.nsh
!include "FileAssociation.nsh"

# Java Check
!define JRE_VERSION "1.7"
!define JRE_SILENT 0
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=33787"
!include "JREDyna.mod.nsh"


# Variables
Var StartMenuGroup

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE C:\JD-Install\license.txt
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro CUSTOM_PAGE_JREINFO
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English
/*!insertmacro MUI_LANGUAGE German
!insertmacro MUI_LANGUAGE Afrikaans
!insertmacro MUI_LANGUAGE Albanian
!insertmacro MUI_LANGUAGE Arabic
!insertmacro MUI_LANGUAGE Basque
!insertmacro MUI_LANGUAGE Belarusian
!insertmacro MUI_LANGUAGE Bosnian
!insertmacro MUI_LANGUAGE Breton
!insertmacro MUI_LANGUAGE Bulgarian
!insertmacro MUI_LANGUAGE Catalan
!insertmacro MUI_LANGUAGE Croatian
!insertmacro MUI_LANGUAGE Czech
!insertmacro MUI_LANGUAGE Danish
!insertmacro MUI_LANGUAGE Dutch
!insertmacro MUI_LANGUAGE Esperanto
!insertmacro MUI_LANGUAGE Estonian
!insertmacro MUI_LANGUAGE Farsi
!insertmacro MUI_LANGUAGE Finnish
!insertmacro MUI_LANGUAGE French
!insertmacro MUI_LANGUAGE Galician
!insertmacro MUI_LANGUAGE Greek
!insertmacro MUI_LANGUAGE Hebrew
!insertmacro MUI_LANGUAGE Hungarian
!insertmacro MUI_LANGUAGE Icelandic
!insertmacro MUI_LANGUAGE Indonesian
!insertmacro MUI_LANGUAGE Irish
!insertmacro MUI_LANGUAGE Italian
!insertmacro MUI_LANGUAGE Japanese
!insertmacro MUI_LANGUAGE Korean
!insertmacro MUI_LANGUAGE Kurdish
!insertmacro MUI_LANGUAGE Latvian
!insertmacro MUI_LANGUAGE Lithuanian
!insertmacro MUI_LANGUAGE Luxembourgish
!insertmacro MUI_LANGUAGE Macedonian
!insertmacro MUI_LANGUAGE Malay
!insertmacro MUI_LANGUAGE Mongolian
!insertmacro MUI_LANGUAGE Norwegian
!insertmacro MUI_LANGUAGE NorwegianNynorsk
!insertmacro MUI_LANGUAGE Polish
!insertmacro MUI_LANGUAGE Portuguese
!insertmacro MUI_LANGUAGE PortugueseBR
!insertmacro MUI_LANGUAGE Romanian
!insertmacro MUI_LANGUAGE Russian
!insertmacro MUI_LANGUAGE Serbian
!insertmacro MUI_LANGUAGE SerbianLatin
!insertmacro MUI_LANGUAGE SimpChinese
!insertmacro MUI_LANGUAGE Slovak
!insertmacro MUI_LANGUAGE Slovenian
!insertmacro MUI_LANGUAGE Spanish
!insertmacro MUI_LANGUAGE SpanishInternational
!insertmacro MUI_LANGUAGE Swedish
!insertmacro MUI_LANGUAGE Thai
!insertmacro MUI_LANGUAGE TradChinese
!insertmacro MUI_LANGUAGE Turkish
!insertmacro MUI_LANGUAGE Ukrainian
!insertmacro MUI_LANGUAGE Uzbek
!insertmacro MUI_LANGUAGE Welsh*/

# Installer attributes
OutFile JDownloaderSetup.exe
#TODO: Switch to current User dir if no admin rights granted.
InstallDir $PROGRAMFILES\JDownloader
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion 0.89.0.0
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductName JDownloader
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileDescription "JDownloader Setup for Windows"
VIAddVersionKey /LANG=${LANG_ENGLISH} LegalCopyright "AppWork UG"
InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

# Installer sections
Section $(SecJDMain_TITLE) SecJDMain
    SectionIn RO
    SetOutPath $INSTDIR
    SetOverwrite on
    File /r C:\JD-Install\files\*
    call DownloadAndInstallJREIfNecessary
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut $SMPROGRAMS\$StartMenuGroup\JDownloader.lnk $INSTDIR\JDownloader.exe
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\JDownloader Support.lnk" http://jdownloader.org/knowledge/index
    SetOutPath $DESKTOP
    CreateShortcut $DESKTOP\JDownloader.lnk $INSTDIR\JDownloader.exe
    WriteRegStr HKLM "${REGKEY}\Components" JDownloader 1
SectionEnd

Section $(SecAssociateFiles_TITLE) SecAssociateFiles
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".jd" "JDownloader JD-File"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".dlc" "JDownloader DLC-Container"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".ccf" "JDownloader CCF-Container"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".rsdf" "JDownloader RSDF-Container"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".metalink" "JDownloader Metalink"
    WriteRegStr HKLM "${REGKEY}\Components" "Associate JDownloader with Containerfiles" 1
SectionEnd

Section -post SEC0002
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    SetOutPath $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk" $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayName "$(^Name)"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayVersion "${VERSION}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" Publisher "${COMPANY}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" URLInfoAbout "${URL}"
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" DisplayIcon $INSTDIR\uninstall.exe
    WriteRegStr HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoModify 1
    WriteRegDWORD HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)" NoRepair 1
SectionEnd

# Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    StrCmp $R0 1 0 next${UNSECTION_ID}
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend

# Uninstaller sections
Section /o "-un.Associate JDownloader with Containerfiles" UNSecAssociateFiles
    ${unregisterExtension} ".jd" "JDownloader JD-File"
    ${unregisterExtension} ".dlc" "JDownloader DLC-Container"
    ${unregisterExtension} ".ccf" "JDownloader CCF-Container"
    ${unregisterExtension} ".rsdf" "JDownloader RSDF-Container"
    ${unregisterExtension} ".metalink" "JDownloader Metalink"
    DeleteRegValue HKLM "${REGKEY}\Components" "Associate JDownloader with Containerfiles"
SectionEnd

Section /o -un.JDownloader UNSecJDMain
    RmDir /r /REBOOTOK $INSTDIR
    Delete /REBOOTOK $DESKTOP\JDownloader.lnk
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\JDownloader Support.lnk"
    Delete /REBOOTOK $SMPROGRAMS\$StartMenuGroup\JDownloader.lnk
    DeleteRegValue HKLM "${REGKEY}\Components" JDownloader
SectionEnd

Section -un.post UNSEC0002
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    DeleteRegValue HKLM "${REGKEY}" Path
    DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"
    RmDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
    RmDir /REBOOTOK $INSTDIR
SectionEnd

# Installer functions
Function .onInit
    InitPluginsDir
    StrCpy $StartMenuGroup JDownloader
FunctionEnd

# Uninstaller functions
Function un.onInit
    ReadRegStr $INSTDIR HKLM "${REGKEY}" Path
    StrCpy $StartMenuGroup JDownloader
    !insertmacro SELECT_UNSECTION JDownloader ${UNSecJDMain}
    !insertmacro SELECT_UNSECTION "Associate JDownloader with Containerfiles" ${UNSecAssociateFiles}
FunctionEnd

# Section Descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SecJDMain} $(SecJDMain_DESC)
!insertmacro MUI_DESCRIPTION_TEXT ${SecAssociateFiles} $(SecAssociateFiles_DESC)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

# Installer Language Strings
# TODO Update the Language Strings with the appropriate translations.

LangString ^UninstallLink ${LANG_ENGLISH} "Uninstall $(^Name)"
/*LangString ^UninstallLink ${LANG_GERMAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_AFRIKAANS} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_ALBANIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_ARABIC} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_BASQUE} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_BELARUSIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_BOSNIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_BRETON} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_BULGARIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_CATALAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_CROATIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_CZECH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_DANISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_DUTCH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_ESPERANTO} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_ESTONIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_FARSI} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_FINNISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_FRENCH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_GALICIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_GREEK} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_HEBREW} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_HUNGARIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_ICELANDIC} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_INDONESIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_IRISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_ITALIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_JAPANESE} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_KOREAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_KURDISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_LATVIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_LITHUANIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_LUXEMBOURGISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_MACEDONIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_MALAY} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_MONGOLIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_NORWEGIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_NORWEGIANNYNORSK} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_POLISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_PORTUGUESE} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_PORTUGUESEBR} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_ROMANIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_RUSSIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SERBIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SERBIANLATIN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SIMPCHINESE} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SLOVAK} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SLOVENIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SPANISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SPANISHINTERNATIONAL} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_SWEDISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_THAI} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_TRADCHINESE} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_TURKISH} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_UKRAINIAN} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_UZBEK} "Uninstall $(^Name)"
LangString ^UninstallLink ${LANG_WELSH} "Uninstall $(^Name)"
*/


#---- English


#Sections
LangString SecJDMain_TITLE ${LANG_ENGLISH} "JDownloader (required)"
LangString SecJDMain_DESC ${LANG_ENGLISH} "The main part of JDownloader."
LangString SecAssociateFiles_TITLE ${LANG_ENGLISH} "Associate JDownloader with Containerfiles"
LangString SecAssociateFiles_DESC ${LANG_ENGLISH} "Associate JDownloader with DLC, CCF, RSDF, Click'n'Load and Metalink Fileextensions"

#JRE Stuff
LangString JRE_INSTALL_TITLE ${LANG_ENGLISH} "JRE Installation Required"
LangString JRE_INSTALL_HEADLINE ${LANG_ENGLISH} "This application requires Java ${JRE_VERSION} or higher"
LangString JRE_INSTALL_TEXT ${LANG_ENGLISH} "This application requires installation of the Java Runtime Environment. This will be downloaded and installed as part of the installation."

LangString JRE_UPDATE_TITLE ${LANG_ENGLISH} "JRE Update Required"
LangString JRE_UPDATE_HEADLINE ${LANG_ENGLISH} "This application requires Java ${JRE_VERSION} or higher"
LangString JRE_UPDATE_TEXT ${LANG_ENGLISH} "This application requires a more recent version of the Java Runtime Environment. This will be downloaded and installed as part of the installation."

/*
#---- German

LangString SecJDMain_TITLE ${LANG_GERMAN} "JDownloader (benötigt)"
LangString SecJDMain_DESC ${LANG_GERMAN} "Das JDownloader Grundgerüst. Benötigt."

LangString SecAssociateFiles_TITLE ${LANG_GERMAN} "Associate JDownloader with Containerfiles"
LangString SecAssociateFiles_DESC ${LANG_GERMAN} "Associate JDownloader with DLC, CCF, RSDF, Click'n'Load and Metalink Fileextensions"

*/