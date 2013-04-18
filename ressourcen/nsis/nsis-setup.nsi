Name JDownloader

RequestExecutionLevel user

# Config

#TODO Better UAC Handling http://nsis.sourceforge.net/UAC_plug-in
!macro onBundleInstallOK
      inetc::get /SILENT /useragent "JDownloaderSetup_inetc_$$Revision$$" "http://jdownloader.org/scripts/inst.php?ad=${ADVERTISING_PLUGIN}&do=bundleok" ".a.log"
    Delete ".a.log"

!macroend

!macro onBundleInstallFailed
  inetc::get /SILENT /useragent "JDownloaderSetup_inetc_$$Revision$$" "http://jdownloader.org/scripts/inst.php?ad=${ADVERTISING_PLUGIN}&do=bundlefail" ".a.log"
    Delete ".a.log"


!macroend


!define COMPANY "AppWork GmbH"
!define URL http://www.jdownloader.org
!define APPNAME "JDownloader"
!define APPNAME_SHORT "JDownloader" # Name without spaces etc (Jay Downloader -> JayDownloader)
#Advertising plugins might overwrite this!
!define LICENSE ".\res\license.txt"
OutFile .\..\..\dist\JDownloaderSetup.exe
SetCompressor lzma

#Advertising
!ifndef ADVERTISING_PLUGIN
    !define ADVERTISING_PLUGIN "example"
!endif
!include ".\advertising\${ADVERTISING_PLUGIN}\script.nsh"
    
#Disable version display for JD (Autoupdate)
#VERSION2 is needed for VIAddVersionKey
#!define VERSION 1.0
!define VERSION2 1.0.0.0

# Just don't edit below this line.
InstallDir "$PROGRAMFILES\${APPNAME_SHORT}" #Necessary for correct append behaviour on user selection
!define INSTDIR_USER "$PROFILE\${APPNAME_SHORT}"
!define INSTDIR_ADMIN "$PROGRAMFILES\${APPNAME_SHORT}"
!define REGKEY "Software\${APPNAME_SHORT}"

# MUI Symbol Definitions
!define MUI_ICON .\res\install.ico
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_FINISHPAGE_RUN $INSTDIR\JDownloader.exe
!define MUI_UNICON .\res\uninstall.ico
!define MUI_UNFINISHPAGE_NOAUTOCLOSE

# Java Check
!define JRE_VERSION "1.6"
!define JRE_SILENT 0
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=42746"

# Included files
!AddPluginDir plugins
!include "Sections.nsh"
!include "MUI2.nsh"
!include "LogicLib.nsh"
!include "FileAssociation.nsh"
!include "ProtocolAssociation.nsh"
!include "UAC.nsh"
!include "JREDyna.mod.nsh"
!include "Utilities.nsh"

# Variables

Var ADMINATINSTALL
# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE ${LICENSE}
#!insertmacro MUI_PAGE_COMPONENTS
!define MUI_PAGE_CUSTOMFUNCTION_LEAVE dirLeave
!define MUI_PAGE_CUSTOMFUNCTION_PRE Directory_PreFunction
!insertmacro MUI_PAGE_DIRECTORY 

!define MUI_PAGE_CUSTOMFUNCTION_PRE Directory_PreFunction
  !insertmacro CUSTOM_PAGE_JREINFO
!insertmacro ADVERTISING_PAGE  
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!addincludedir ".\languages\"

!insertmacro MUI_LANGUAGE English
!include "en.nsis-setup.nsh"
!insertmacro MUI_LANGUAGE German
!include "de.nsis-setup.nsh"

!insertmacro ADVERTISING_GENERAL

# Installer attributes
Function Directory_PreFunction
  StrCpy $R8 1 ;This is the third page
  
FunctionEnd
CRCCheck on
XPStyle on
ShowInstDetails  hide
ShowUninstDetails hide
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductName "${APPNAME}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileDescription "${APPNAME} Setup for Windows"
VIAddVersionKey /LANG=${LANG_ENGLISH} LegalCopyright "${COMPANY}"
#Disable version display for JD (Autoupdate)
#VIProductVersion must be set for invoking VIAddVersionKey
VIProductVersion "${VERSION2}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileVersion "${VERSION}"
#VIAddVersionKey /LANG=${LANG_ENGLISH} ProductVersion "${VERSION}"


# Installer sections
Section $(SecJDMain_TITLE) SecJDMain
    SectionIn RO

    #Java Check
    ${If} ${UAC_IsAdmin}
      Call DownloadAndInstallJREIfNecessary
    ${EndIf}
    
    #Copy files
    SetOutPath $INSTDIR    
    SetOverwrite on
    File /r .\..\..\dist\JDownloader\*
    
    #Create shortcuts
    SetOutPath "$SMPROGRAMS\$(^Name)"
    CreateShortcut "$SMPROGRAMS\$(^Name)\$(^Name).lnk" $INSTDIR\JDownloaderD3D.exe
    CreateShortcut "$SMPROGRAMS\$(^Name)\$(HelpLink).lnk" "http://jdownloader.org/knowledge/index"
    SetOutPath $DESKTOP
    CreateShortcut "$DESKTOP\$(^Name).lnk" $INSTDIR\JDownloaderD3D.exe
    
    #make files writeable for user
    ${If} ${UAC_IsAdmin}
      AccessControl::EnableFileInheritance "$INSTDIR\"
      AccessControl::GrantOnFile "$INSTDIR\" "(S-1-1-0)" "FullAccess"
      AccessControl::GrantOnFile "$INSTDIR\license.txt" "(S-1-1-0)" "FullAccess"
    ${EndIf}

    WriteRegStr SHELL_CONTEXT "${REGKEY}\Components" SecJDMain 1
SectionEnd

Section $(SecAssociateFiles_TITLE) SecAssociateFiles
    ${registerExtension} "$INSTDIR\JDownloaderD3D.exe" ".jd" "JDownloader JD File"
    ${registerExtension} "$INSTDIR\JDownloaderD3D.exe" ".jdc" "JDownloader JDContainer File"
    ${registerExtension} "$INSTDIR\JDownloaderD3D.exe" ".dlc" "JDownloader DLC Container"
    ${registerExtension} "$INSTDIR\JDownloaderD3D.exe" ".ccf" "JDownloader CCF Container"
    ${registerExtension} "$INSTDIR\JDownloaderD3D.exe" ".rsdf" "JDownloader RSDF Container"
    ${registerExtension} "$INSTDIR\JDownloaderD3D.exe" ".metalink" "JDownloader Metalink"
    ${registerProtocol}  "$INSTDIR\JDownloaderD3D.exe" "rsdf" "JDownloader RSDF Link"
    ${registerProtocol}  "$INSTDIR\JDownloaderD3D.exe" "ccf" "JDownloader CCF Link"
    ${registerProtocol}  "$INSTDIR\JDownloaderD3D.exe" "dlc" "JDownloader DLC Link"
    ${registerProtocol}  "$INSTDIR\JDownloaderD3D.exe" "metalink" "JDownloader Metalink"
    ${registerProtocol}  "$INSTDIR\JDownloaderD3D.exe" "jd" "JDownloader JD Link"
    ${registerProtocol}  "$INSTDIR\JDownloaderD3D.exe" "jdlist" "JDownloader JDList Link"
    
    WriteRegStr SHELL_CONTEXT "${REGKEY}\Components" SecAssociateFiles 1 
SectionEnd

#This section provides mandatory install functionality
Section -post SecPostInstall
    #Write uninstaller
    SetOutPath $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    SetOutPath "$SMPROGRAMS\$(^Name)"
    CreateShortcut "$SMPROGRAMS\$(^Name)\$(^UninstallLink).lnk" "$INSTDIR\uninstall.exe"
    
    #Write registry
    WriteRegStr SHELL_CONTEXT "${REGKEY}" "Path" $INSTDIR
    #Add uninstall information to Add/Remove Programs in windows
    #see http://nsis.sourceforge.net/Add_uninstall_information_to_Add/Remove_Programs
    WriteRegStr SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}" DisplayName "$(^Name)"
    WriteRegStr SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}" Publisher "${COMPANY}"
    WriteRegStr SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}" URLInfoAbout "${URL}"
    WriteRegStr SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}" DisplayIcon $INSTDIR\JDownloaderD3D.exe
    WriteRegStr SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}" UninstallString $INSTDIR\uninstall.exe
    WriteRegDWORD SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}" NoModify 1
    WriteRegDWORD SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}" NoRepair 1
    #Disable version display for JD (Autoupdate)
    #WriteRegStr SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}" DisplayVersion "${VERSION}"
SectionEnd

# Uninstaller sections
Section /o "-un.$(SecJDMain_TITLE)" UNSecJDMain
    #Remove files
    Push $INSTDIR
    Push "downloads"
    Call un.RmButOne
    RMDir $INSTDIR\downloads #won't delete if not empty
    RMDir $INSTDIR
    
    Delete /REBOOTOK "$SMPROGRAMS\$(^Name)\$(^Name).lnk"
    Delete /REBOOTOK "$SMPROGRAMS\$(^Name)\$(HelpLink).lnk"
    Delete /REBOOTOK "$DESKTOP\$(^Name).lnk"

    DeleteRegValue SHELL_CONTEXT "${REGKEY}\Components" SecJDMain
SectionEnd

Section /o "-un.$(SecAssociateFiles_TITLE)" UNSecAssociateFiles
    ${unregisterExtension} ".jd" "JDownloader JD File"
    ${unregisterExtension} ".jdc" "JDownloader JDContainer File"
    ${unregisterExtension} ".dlc" "JDownloader DLC Container"
    ${unregisterExtension} ".ccf" "JDownloader CCF Container"
    ${unregisterExtension} ".rsdf" "JDownloader RSDF Container"
    ${unregisterExtension} ".metalink" "JDownloader Metalink"
    ${unregisterProtocol}  "rsdf" "JDownloader RSDF Link"
    ${unregisterProtocol}  "ccf" "JDownloader CCF Link"
    ${unregisterProtocol}  "dlc" "JDownloader DLC Link"
    ${unregisterProtocol}  "metalink" "JDownloader Metalink"
    ${unregisterProtocol}  "jd" "JDownloader JD Link"
    ${unregisterProtocol}  "jdlist" "JDownloader JDList Link"
    
    DeleteRegValue SHELL_CONTEXT "${REGKEY}\Components" SecAssociateFiles
SectionEnd

Section -un.post UNSecPostInstall
    #Delete uninstaller
    Delete /REBOOTOK "$INSTDIR\uninstall.exe"
    Delete /REBOOTOK "$SMPROGRAMS\$(^Name)\$(^UninstallLink).lnk"
    RmDir /REBOOTOK "$SMPROGRAMS\$(^Name)"
    
    #Remove entries from registry
    DeleteRegValue SHELL_CONTEXT "${REGKEY}" "Path"
    DeleteRegKey /IfEmpty SHELL_CONTEXT "${REGKEY}\Components"
    DeleteRegKey /IfEmpty SHELL_CONTEXT "${REGKEY}"
    #Remove uninstall information
    DeleteRegKey SHELL_CONTEXT "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\${APPNAME_SHORT}"
SectionEnd

# Installer functions
Function .onInit
    InitPluginsDir
    
    !insertmacro doUAC

    #Installation Context
    ${If} ${UAC_IsAdmin}
        StrCpy $INSTDIR ${INSTDIR_ADMIN}
        SetShellVarContext all
    ${Else}
        StrCpy $INSTDIR ${INSTDIR_USER}
        SetShellVarContext current
    ${EndIf}
        inetc::get /SILENT /useragent "JDownloaderSetup_inetc_$$Revision$$" "http://jdownloader.org/scripts/inst.php?ad=${ADVERTISING_PLUGIN}&do=init" ".a.log"
    Delete ".a.log" 
    
    !insertmacro ADVERTISING_ONINIT
FunctionEnd

Function .onInstSuccess
    !insertmacro ADVERTISING_ONINSTSUCCESS
          inetc::get /SILENT /useragent "JDownloaderSetup_inetc_$$Revision$$" "http://jdownloader.org/scripts/inst.php?ad=${ADVERTISING_PLUGIN}&do=success" ".a.log"
    Delete ".a.log"
    

FunctionEnd

Function .onInstFailed
    !insertmacro ADVERTISING_ONINSTFAILED
        inetc::get /SILENT /useragent "JDownloaderSetup_inetc_$$Revision$$" "http://jdownloader.org/scripts/inst.php?ad=${ADVERTISING_PLUGIN}&do=failed" ".a.log"
    Delete ".a.log"
        
FunctionEnd

Function .onGUIEnd
    !insertmacro ADVERTISING_ONGUIEND
FunctionEnd

# Uninstaller functions
Function un.onInit
    !insertmacro doUAC
    !insertmacro determineInstallDir
    !insertmacro SELECT_UNSECTION SecJDMain ${UNSecJDMain}
    !insertmacro SELECT_UNSECTION SecAssociateFiles ${UNSecAssociateFiles}
    !insertmacro SELECT_UNSECTION SecAdvertising ${UNSecAdvertising}
FunctionEnd

# Sections not displayed
# Section Descriptions
#!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
#!insertmacro MUI_DESCRIPTION_TEXT ${SecJDMain} $(SecJDMain_DESC)
#!insertmacro MUI_DESCRIPTION_TEXT ${SecAssociateFiles} $(SecAssociateFiles_DESC)
#!insertmacro MUI_FUNCTION_DESCRIPTION_END