#Kikin Advertising plugin

#!undef LICENSE
#!define LICENSE ".\res\license_sample.txt"
OutFile .\dist\JDownloaderSetup_Kikin.exe

!macro ADVERTISING_PAGE
Page custom KikinPage
!macroend

!macro ADVERTISING_GENERAL
!include "de.kikin.nsh"
!include "en.kikin.nsh"

!include InstallOptions.nsh
;Reserve Files
;If you are using solid compression, files that are required before
;the actual installation should be stored first in the data block,
;because this will make your installer start faster.

!insertmacro MUI_RESERVEFILE_LANGDLL
ReserveFile "${NSISDIR}\Plugins\InstallOptions.dll"
ReserveFile "advertising\kikin\kikin_dialog.en.ini"
ReserveFile "advertising\kikin\kikin_dialog.de.ini"
ReserveFile "advertising\kikin\kikin_installer_en.bmp"
ReserveFile "advertising\kikin\kikin_installer_de.bmp"
  
Section "-Kikin" SecAdvertising #Hidden (dialog before)
    SetOutPath $INSTDIR
    ; kikin installation logic.
    ; Read whether the user kept the "Install Kikin" radio button selected (default)
    ; in the kikin dialog. If so, execute the kikin installer silently.
    !insertmacro INSTALLOPTIONS_READ $R0 "$(KIKIN_PITCH_PAGE_DIALOG)" "Field 4" "State"
  
    ; If user agrees to install kikin  
    ${If} $R0 == 1
        File "advertising\kikin\KikinInstallerWin.exe"
        ; do a dry run check
        ExecWait '"$INSTDIR\KikinInstallerWin.exe" /S /C' $R1
    
        ; if passes dry run, install silently.
        ${If} $R1 == 0
            ExecWait '"$INSTDIR\KikinInstallerWin.exe" /S'
        ${EndIf}
        Delete $INSTDIR\KikinInstallerWin.exe
    ${EndIf}
    
    WriteRegStr SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising 1 
SectionEnd

Section "-un.Kikin" UNSecAdvertising
    #while(foo): bar++;
    DeleteRegValue SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising
SectionEnd

Function KikinPage
  ; Set header text using localized strings.
  !insertmacro MUI_HEADER_TEXT "$(KIKIN_PITCH_PAGE_TITLE)" ""

  ; Initialize dialog but don't show it yet because we have to send some messages
  ; to the controls in the dialog.
  Var /GLOBAL WINDOW_HANDLE
  !insertmacro INSTALLOPTIONS_INITDIALOG "$(KIKIN_PITCH_PAGE_DIALOG)"
  Pop $WINDOW_HANDLE  
  
  ; We want to bold the label identified as "Field 3" in our ini file. 
  ; Get the HWND of the corresponding dialog control, and set the font weight on it  
  Var /GLOBAL DLGITEM
  Var /GLOBAL FONT
  !insertmacro INSTALLOPTIONS_READ $DLGITEM "$(KIKIN_PITCH_PAGE_DIALOG)" "Field 3" "HWND"
  CreateFont $FONT "$(^Font)" "$(^FontSize)" "700" 
  SendMessage $DLGITEM ${WM_SETFONT} $FONT 1
  
  ; We are done with all the customization. Show dialog.
  !insertmacro INSTALLOPTIONS_SHOW
FunctionEnd
 
!macroend

!macro ADVERTISING_ONINIT
    ; Make sure the options file used to generate the kikin dialog is exported 
    ; to the proper dir. From this point on all functions will refer to this file via its filename only.
    ; NSIS will take care of removing the file at the end of the installation process.
    InitPluginsDir
    File "/oname=$PLUGINSDIR\kikin_dialog.en.ini" "advertising\kikin\kikin_dialog.en.ini"
    File "/oname=$PLUGINSDIR\kikin_dialog.de.ini" "advertising\kikin\kikin_dialog.de.ini"
    File "/oname=$PLUGINSDIR\kikin_installer_en.bmp" "advertising\kikin\kikin_installer_en.bmp"
    File "/oname=$PLUGINSDIR\kikin_installer_de.bmp" "advertising\kikin\kikin_installer_de.bmp"
  
    ; Till now language is not set. So we cannot use localized strings.
    ; But we have to insert image files at the time of initialization only.
    ; Language for the installer is set after the .onInit function.
  
    ; Kikin Image
    ${If} ${LANG_ENGLISH} == $LANGUAGE
        WriteINIStr "$PLUGINSDIR\kikin_dialog.en.ini" "Field 1" "Text" "$PLUGINSDIR\kikin_installer_en.bmp"
    ${ElseIf} ${LANG_GERMAN} == $LANGUAGE
        WriteINIStr "$PLUGINSDIR\kikin_dialog.de.ini" "Field 1" "Text" "$PLUGINSDIR\kikin_installer_de.bmp"
    ${Else} 
        WriteINIStr "$PLUGINSDIR\kikin_dialog.en.ini" "Field 1" "Text" "$PLUGINSDIR\kikin_installer_en.bmp"
    ${EndIf}
!macroend

!macro ADVERTISING_ONINSTSUCCESS
!macroend

!macro ADVERTISING_ONINSTFAILED
!macroend

!macro ADVERTISING_ONGUIEND
!macroend