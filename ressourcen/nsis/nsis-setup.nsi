Name JDownloader

RequestExecutionLevel user

# General Symbol Definitions
!define REGKEY "SOFTWARE\$(^Name)"
!define REGKEY2 "Software\$(^Name)"
!define VERSION 0.89
!define VERSION2 0.89.0.0
!define COMPANY "AppWork UG (haftungsbeschränkt)"
!define URL http://www.jdownloader.org
!define APPNAME "JDownloader"
!define SHORTNAME "JDownloader"

!define INSTDIR_USER "$PROFILE\${SHORTNAME}"
!define INSTDIR_ADMIN "$PROGRAMFILES\${SHORTNAME}"

# MUI Symbol Definitions
!define MUI_ICON .\res\install.ico
!define MUI_FINISHPAGE_NOAUTOCLOSE
!define MUI_FINISHPAGE_RUN $INSTDIR\JDownloader.exe
!define MUI_UNICON .\res\uninstall.ico
!define MUI_UNFINISHPAGE_NOAUTOCLOSE

# Java Check
!define JRE_VERSION "1.6"
!define JRE_SILENT 0
!define JRE_URL "http://javadl.sun.com/webapps/download/AutoDL?BundleId=36668"

# Included files
!AddPluginDir plugins
!include Sections.nsh
!include MUI2.nsh
!include "FileAssociation.nsh"
!include "ProtocolAssociation.nsh"
!include "UAC.nsh"
!include "JREDyna.mod.nsh"

# Variables
Var StartMenuGroup
Var ADMINATINSTALL

# Installer pages
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_LICENSE .\res\license.txt
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro CUSTOM_PAGE_JREINFO
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES

# Installer languages
!insertmacro MUI_LANGUAGE English
!insertmacro MUI_LANGUAGE German

# Installer attributes
OutFile .\dist\JDownloaderSetup.exe
#TODO: Switch to current User dir if no admin rights granted.
InstallDir ${INSTDIR_USER}
CRCCheck on
XPStyle on
ShowInstDetails show
VIProductVersion "${VERSION2}"
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductName "${APPNAME}"
VIAddVersionKey /LANG=${LANG_ENGLISH} ProductVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyName "${COMPANY}"
VIAddVersionKey /LANG=${LANG_ENGLISH} CompanyWebsite "${URL}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileVersion "${VERSION}"
VIAddVersionKey /LANG=${LANG_ENGLISH} FileDescription "${APPNAME} Setup for Windows"
VIAddVersionKey /LANG=${LANG_ENGLISH} LegalCopyright "${COMPANY}"
#InstallDirRegKey HKLM "${REGKEY}" Path
ShowUninstDetails show

# Installer sections
Section $(SecJDMain_TITLE) SecJDMain
    SectionIn RO
    
    ${If} ${UAC_IsAdmin}
      call DownloadAndInstallJREIfNecessary
    ${EndIf}
    
    SetOutPath $INSTDIR    
    SetOverwrite on
    File /r .\files\*    
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut $SMPROGRAMS\$StartMenuGroup\JDownloader.lnk $INSTDIR\JDownloader.exe
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\JDownloader Support.lnk" http://jdownloader.org/knowledge/index
    SetOutPath $DESKTOP
    CreateShortcut $DESKTOP\JDownloader.lnk $INSTDIR\JDownloader.exe
    
    ${If} ${UAC_IsAdmin}
      AccessControl::EnableFileInheritance "$INSTDIR\"
      AccessControl::GrantOnFile "$INSTDIR\" "(S-1-1-0)" "FullAccess"
      AccessControl::GrantOnFile "$INSTDIR\license.txt" "(S-1-1-0)" "FullAccess"
    ${EndIf}
    ${If} ${UAC_IsAdmin}
    WriteRegStr HKLM "${REGKEY}\Components" JDownloader 1
    ${Else}
    WriteRegStr HKCU "${REGKEY2}\Components" JDownloader 1
    ${EndIf}
    
SectionEnd

Section $(SecAssociateFiles_TITLE) SecAssociateFiles
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".jd" "JDownloader JD File"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".jdc" "JDownloader JDContainer File"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".dlc" "JDownloader DLC Container"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".ccf" "JDownloader CCF Container"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".rsdf" "JDownloader RSDF Container"
    ${registerExtension} "$INSTDIR\JDownloader.exe" ".metalink" "JDownloader Metalink"
    ${registerProtocol}  "$INSTDIR\JDownloader.exe" "rsdf" "JDownloader RSDF Link"
    ${registerProtocol}  "$INSTDIR\JDownloader.exe" "ccf" "JDownloader CCF Link"
    ${registerProtocol}  "$INSTDIR\JDownloader.exe" "dlc" "JDownloader DLC Link"
    ${registerProtocol}  "$INSTDIR\JDownloader.exe" "metalink" "JDownloader Metalink"
    ${registerProtocol}  "$INSTDIR\JDownloader.exe" "jd" "JDownloader JD Link"
    ${registerProtocol}  "$INSTDIR\JDownloader.exe" "jdlist" "JDownloader JDList Link"
    
    ${If} ${UAC_IsAdmin}
    WriteRegStr HKLM "${REGKEY}\Components" "Associate JDownloader with Containerfiles" 1
    ${Else}
    WriteRegStr HKCU "${REGKEY2}\Components" "Associate JDownloader with Containerfiles" 1
    ${EndIf}
    
SectionEnd

Section -post SEC0002
    
    ${If} ${UAC_IsAdmin}
    WriteRegStr HKLM "${REGKEY}" Path $INSTDIR
    ${Else}
    WriteRegStr HKCU "${REGKEY2}" "Path" $INSTDIR
    ${EndIf}
    SetOutPath $INSTDIR
    WriteUninstaller $INSTDIR\uninstall.exe
    SetOutPath $SMPROGRAMS\$StartMenuGroup
    CreateShortcut "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk" "$INSTDIR\uninstall.exe"
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
    
    ${If} $ADMINATINSTALL > 0
    ReadRegStr $R0 HKLM "${REGKEY}\Components" "${SECTION_NAME}"
    ${Else}
    ReadRegStr $R0 HKCU "${REGKEY2}\Components" "${SECTION_NAME}"
    ${EndIf}
    
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
    
    ${If} $ADMINATINSTALL > 0
    DeleteRegValue HKLM "${REGKEY}\Components" "Associate JDownloader with Containerfiles"
    ${Else}
    DeleteRegValue HKCU "${REGKEY2}\Components" "Associate JDownloader with Containerfiles"
    ${EndIf}
    
SectionEnd

Section /o -un.JDownloader UNSecJDMain
    RMDir /REBOOTOK /r $INSTDIR
    Delete /REBOOTOK $DESKTOP\JDownloader.lnk
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\JDownloader Support.lnk"
    Delete /REBOOTOK $SMPROGRAMS\$StartMenuGroup\JDownloader.lnk
    
    ${If} $ADMINATINSTALL > 0
    DeleteRegValue HKLM "${REGKEY}\Components" JDownloader
    ${Else}
    DeleteRegValue HKCU "${REGKEY2}\Components" JDownloader
    ${EndIf}
    
SectionEnd

Section -un.post UNSEC0002
    Delete /REBOOTOK "$SMPROGRAMS\$StartMenuGroup\$(^UninstallLink).lnk"
    Delete /REBOOTOK $INSTDIR\uninstall.exe
    
    ${If} $ADMINATINSTALL > 0
    DeleteRegKey HKLM "SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\$(^Name)"
    DeleteRegValue HKLM "${REGKEY}" "Path"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}\Components"
    DeleteRegKey /IfEmpty HKLM "${REGKEY}"
    ${Else}
    DeleteRegValue HKCU "${REGKEY2}" "Path"
    DeleteRegKey /IfEmpty HKCU "${REGKEY2}\Components"
    DeleteRegKey /IfEmpty HKCU "${REGKEY2}"    
    ${EndIf}
    

    RmDir /REBOOTOK $SMPROGRAMS\$StartMenuGroup
SectionEnd

# Installer functions
Function .onInit
    InitPluginsDir
    StrCpy $StartMenuGroup JDownloader
    
!insertmacro UAC_RunElevated
${Switch} $0
${Case} 0
    ${IfThen} $1 = 1 ${|} Quit ${|} ;we are the outer process, the inner process has done its work, we are done
    ${IfThen} $3 <> 0 ${|} ${Break} ${|} ;we are admin, let the show go on
${Case} 1062
    #MessageBox mb_IconStop|mb_TopMost|mb_SetForeground "Logon service not running, aborting!"
    #Quit
${EndSwitch}

    ${If} ${UAC_IsAdmin}
    StrCpy $INSTDIR ${INSTDIR_ADMIN}
    ${EndIf}
FunctionEnd

# Uninstaller functions
Function un.onInit
    
    !insertmacro UAC_RunElevated
    ${Switch} $0
    ${Case} 0
    ${IfThen} $1 = 1 ${|} Quit ${|} ;we are the outer process, the inner process has done its work, we are done
    ${IfThen} $3 <> 0 ${|} ${Break} ${|} ;we are admin, let the show go on
    ${Case} 1062
    #MessageBox mb_IconStop|mb_TopMost|mb_SetForeground "Logon service not running, aborting!"
    #Quit
    ${EndSwitch}
    
    
    ${If} ${UAC_IsAdmin}
      #Uninstall with Admin rights
      StrCpy $ADMINATINSTALL 1
      ReadRegStr $INSTDIR HKLM "${REGKEY}" "Path"
      StrCmp $INSTDIR "" 0 UninstDirFound #installed with admin rights, too
    
      #installed without adminrights then
      StrCpy $ADMINATINSTALL 0
      ReadRegStr $INSTDIR HKCU "${REGKEY2}" "Path"

    ${Else}
      StrCpy $ADMINATINSTALL 0
      ReadRegStr $INSTDIR HKCU "${REGKEY2}" "Path"
      StrCmp $INSTDIR "" 0 UninstDirFound
      
      #installed with adminrights then
      MessageBox MB_ICONEXCLAMATION|mb_TopMost|mb_SetForeground "You're trying to uninstall a product you've installed with admin rights! Software might not be removed completely after uninstall."
      ;Quit
      StrCpy $ADMINATINSTALL 1
      ReadRegStr $INSTDIR HKLM "${REGKEY}" "Path"

    ${EndIf}

UninstDirFound:
  
    StrCpy $StartMenuGroup JDownloader
    
    !insertmacro SELECT_UNSECTION JDownloader ${UNSecJDMain}
    !insertmacro SELECT_UNSECTION "Associate JDownloader with Containerfiles" ${UNSecAssociateFiles}
FunctionEnd

# Section Descriptions
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
!insertmacro MUI_DESCRIPTION_TEXT ${SecJDMain} $(SecJDMain_DESC)
!insertmacro MUI_DESCRIPTION_TEXT ${SecAssociateFiles} $(SecAssociateFiles_DESC)
!insertmacro MUI_FUNCTION_DESCRIPTION_END



##### Internationalization

;;;;; English
#General
LangString ^UninstallLink ${LANG_ENGLISH} "Uninstall $(^Name)"
LangString ^Name ${LANG_ENGLISH} "${APPNAME}"


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
LangString JRE_NOADMIN_TITLE ${LANG_ENGLISH} "JRE Installation Required"
LangString JRE_NOADMIN_HEADLINE ${LANG_ENGLISH} "This application requires Java ${JRE_VERSION} or higher"
LangString JRE_NOADMIN_TEXT ${LANG_ENGLISH} "This application requires installation of the Java Runtime Environment. Your account does not have the administrative rights required for installation. Please ask your system administrator for further instructions."


;;;;; German
#General
LangString ^UninstallLink ${LANG_GERMAN} "Deinstalliere $(^Name)"
LangString ^Name ${LANG_GERMAN} "${APPNAME}"

#Sections
LangString SecJDMain_TITLE ${LANG_GERMAN} "JDownloader (benötigt)"
LangString SecJDMain_DESC ${LANG_GERMAN} "JDownloader - Hauptprogramm"
LangString SecAssociateFiles_TITLE ${LANG_GERMAN} "Verknüpfe JDownloader mit Containerdateien"
LangString SecAssociateFiles_DESC ${LANG_GERMAN} "Verknüpfe JDownloader mit DLC, CCF, RSDF, Click'n'Load and Metalink Dateien"

#JRE Stuff
LangString JRE_INSTALL_TITLE ${LANG_GERMAN} "JRE Installation erforderlich"
LangString JRE_INSTALL_HEADLINE ${LANG_GERMAN} "Diese Anwendung erfordert Java ${JRE_VERSION} oder höher"
LangString JRE_INSTALL_TEXT ${LANG_GERMAN} "Diese Anwendung erfordert die Installation des Java Runtime Environments. Dieses wird im Laufe des Installationsprozesses automatisch heruntergeladen und installiert."
LangString JRE_UPDATE_TITLE ${LANG_GERMAN} "JRE Update erforderlich"
LangString JRE_UPDATE_HEADLINE ${LANG_GERMAN} "Diese Anwendung erfordert Java ${JRE_VERSION} oder höher"
LangString JRE_UPDATE_TEXT ${LANG_GERMAN} "Diese Anwendung erfordert eine aktuellere Version des Java Runtime Environments. Diese wird im Laufe des Installationsprozesses automatisch heruntergeladen und installiert."
LangString JRE_NOADMIN_TITLE ${LANG_GERMAN} "JRE Installation erforderlich"
LangString JRE_NOADMIN_HEADLINE ${LANG_GERMAN} "Diese Anwendung erfordert Java ${JRE_VERSION} oder höher"
LangString JRE_NOADMIN_TEXT ${LANG_GERMAN} "Diese Anwendung erfordert die Installation des Java Runtime Environments. Dieses kann mit den aktuell verfügbaren Systemrechten nicht installiert werden. Bitte wenden Sie sich an ihrem Systemadministrator."
