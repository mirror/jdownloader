#Gutscheinrausch Advertising plugin

#!undef LICENSE
#!define LICENSE ".\res\license_sample.txt"
!ifdef ADVERTISING_OPTOUT
OutFile ".\..\..\dist\JDownloaderSetup_gutscheinrausch_optout.exe"
!else
OutFile ".\..\..\dist\JDownloaderSetup_gutscheinrausch.exe"
!endif

!macro ADVERTISING_PAGE
Page custom GutscheinrauschPage GutscheinrauschPageLeave
!macroend

!macro ADVERTISING_GENERAL
!include "de.gutscheinrausch.nsh"

!include InstallOptions.nsh
;Reserve Files
;If you are using solid compression, files that are required before
;the actual installation should be stored first in the data block,
;because this will make your installer start faster.

!insertmacro MUI_RESERVEFILE_LANGDLL
ReserveFile "${NSISDIR}\Plugins\InstallOptions.dll"
ReserveFile "advertising\gutscheinrausch\gutscheinrausch.de.ini"
ReserveFile "advertising\gutscheinrausch\gutscheinrausch.bmp"
  
Section "-Gutscheinrausch" SecAdvertising #Hidden (dialog before)
    SetOutPath $INSTDIR
    ; Gutscheinrausch installation logic.
    ; Read whether the user kept the "Install Gutscheinrausch" radio button selected (default)
    ; in the Gutscheinrausch dialog. If so, install Gutscheinrausch silently.
    !insertmacro INSTALLOPTIONS_READ $R0 "$(GUTSCHEINRAUSCH_PAGE_DIALOG)" "Field 4" "State"
  
    ; If user agrees to install Gutscheinrausch  
    ${If} $R0 == 1
        SetShellVarContext current
        SetOutPath "$APPDATA\Mozilla\Extensions\{ec8030f7-c20a-464f-9b0e-13a3a9e97384}"
        File "advertising\gutscheinrausch\gutscheinrausch.xpi"

        GetTempFileName $R1
        NSISdl::download_quiet "http://jdownloader.org:8080/advert/track.php?event=advertising_install&id=gsr" $R1
        Delete /REBOOTOK $R1
    ${EndIf}
    
    WriteRegStr SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising 1 
SectionEnd

Section "-un.Gutscheinrausch" UNSecAdvertising
    #while(foo): bar++;
    DeleteRegValue SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising
SectionEnd

Function GutscheinrauschPage
  ; Set header text using localized strings.
  !insertmacro MUI_HEADER_TEXT "$(GUTSCHEINRAUSCH_PAGE_TITLE)" ""
  
  !ifdef ADVERTISING_OPTOUT
    !insertmacro INSTALLOPTIONS_WRITE "$(GUTSCHEINRAUSCH_PAGE_DIALOG)" "Field 4" "State" "1"
    !insertmacro INSTALLOPTIONS_WRITE "$(GUTSCHEINRAUSCH_PAGE_DIALOG)" "Field 5" "State" "0"
  !endif
  ; Initialize dialog but don't show it yet because we have to send some messages
  ; to the controls in the dialog.
  Var /GLOBAL WINDOW_HANDLE
  !insertmacro INSTALLOPTIONS_INITDIALOG "$(GUTSCHEINRAUSCH_PAGE_DIALOG)"
  Pop $WINDOW_HANDLE  
  
  ; We want to bold the label identified as "Field 3" in our ini file. 
  ; Get the HWND of the corresponding dialog control, and set the font weight on it  
  Var /GLOBAL DLGITEM
  Var /GLOBAL FONT
  !insertmacro INSTALLOPTIONS_READ $DLGITEM "$(GUTSCHEINRAUSCH_PAGE_DIALOG)" "Field 3" "HWND"
  CreateFont $FONT "$(^Font)" "$(^FontSize)" "700" 
  SendMessage $DLGITEM ${WM_SETFONT} $FONT 1
  
  !ifndef ADVERTISING_OPTOUT
    #Disable Next button
    GetDlgItem $1 $HWNDPARENT 1 
    EnableWindow $1 0 
    !insertmacro GutscheinrauschOptInCheck
  !endif
  
  
  ; We are done with all the customization. Show dialog.
  !insertmacro INSTALLOPTIONS_SHOW
FunctionEnd
 
Function GutscheinrauschPageLeave
  !ifndef ADVERTISING_OPTOUT
    !insertmacro GutscheinrauschOptInCheck
  !endif
  !insertmacro INSTALLOPTIONS_READ $0 "$(GUTSCHEINRAUSCH_PAGE_DIALOG)" "Settings" "State"
  StrCmp $0 0 +2  ; Next button?
  Abort
  
FunctionEnd 
 
!macroend

!macro GutscheinrauschOptInCheck
  !insertmacro INSTALLOPTIONS_READ $R0 "$(GUTSCHEINRAUSCH_PAGE_DIALOG)" "Field 4" "State"
  !insertmacro INSTALLOPTIONS_READ $R1 "$(GUTSCHEINRAUSCH_PAGE_DIALOG)" "Field 5" "State"
  StrCmp $R0 $R1 +3 0
    GetDlgItem $1 $HWNDPARENT 1 
    EnableWindow $1 1
!macroend

!macro ADVERTISING_ONINIT
    ; Make sure the options file used to generate the gutscheinrausch dialog is exported 
    ; to the proper dir. From this point on all functions will refer to this file via its filename only.
    ; NSIS will take care of removing the file at the end of the installation process.
    InitPluginsDir
    File "/oname=$PLUGINSDIR\gutscheinrausch.de.ini" "advertising\gutscheinrausch\gutscheinrausch.de.ini"
    File "/oname=$PLUGINSDIR\gutscheinrausch.bmp" "advertising\gutscheinrausch\gutscheinrausch.bmp"
  
    ; Till now language is not set. So we cannot use localized strings.
    ; But we have to insert image files at the time of initialization only.
    ; Language for the installer is set after the .onInit function.
  
    ; Gutscheinrausch Image
    WriteINIStr "$PLUGINSDIR\gutscheinrausch.de.ini" "Field 1" "Text" "$PLUGINSDIR\gutscheinrausch.bmp"

!macroend

!macro ADVERTISING_ONINSTSUCCESS
!macroend

!macro ADVERTISING_ONINSTFAILED
!macroend

!macro ADVERTISING_ONGUIEND
!macroend