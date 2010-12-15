#Facemoods Advertising plugin

#!undef LICENSE
#!define LICENSE ".\res\license_sample.txt"

OutFile ".\..\..\dist\JDownloaderSetup_${ADVERTISING_PLUGIN}_optout.exe"


!macro ADVERTISING_PAGE
Page custom FacemoodsPage FacemoodsPageLeave
!macroend

!macro ADVERTISING_GENERAL
!include "advertising\${ADVERTISING_PLUGIN}\locale.nsh"

!include InstallOptions.nsh
;Reserve Files
;If you are using solid compression, files that are required before
;the actual installation should be stored first in the data block,
;because this will make your installer start faster.

!insertmacro MUI_RESERVEFILE_LANGDLL
ReserveFile "${NSISDIR}\Plugins\InstallOptions.dll"
ReserveFile "advertising\${ADVERTISING_PLUGIN}\script.en.ini"

ReserveFile "advertising\${ADVERTISING_PLUGIN}\script.de.ini"

ReserveFile "advertising\${ADVERTISING_PLUGIN}\screen.en.bmp"
ReserveFile "advertising\${ADVERTISING_PLUGIN}\screen.de.bmp"
Section "-Facemoods" SecAdvertising #Hidden (dialog before)
    SetOutPath $INSTDIR
    ; Facemoods installation logic.
    ; Read whether the user kept the "Install Facemoods" radio button selected (default)
    ; in the Facemoods dialog. If so, install Facemoods silently.
    !insertmacro INSTALLOPTIONS_READ $R0 "$(BUNDLE_PAGE_DIALOG)" "Field 3" "State"
  
    ; If user agrees to install Facemoods  
    ${If} $R0 == 1
    
     MessageBox MB_OK "$(BUNDLE_CLOSE_BROWSER_MESSAGE)"
        File "advertising\${ADVERTISING_PLUGIN}\installer.exe"
        ; do a dry run check
        ExecWait '"$INSTDIR\installer.exe" /S /mnt /mhp /mds'     
        Delete $INSTDIR\installer.exe  
!insertmacro onBundleInstallOK
${else}
!insertmacro onBundleInstallFailed
    ${EndIf}
    
 
    WriteRegStr SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising 1 
SectionEnd

Section "-un.Facemoods" UNSecAdvertising
    #while(foo): bar++;
    DeleteRegValue SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising
SectionEnd

Function FacemoodsPage
  ; Set header text using localized strings.
 

  
   !insertmacro MUI_HEADER_TEXT "$(BUNDLE_PAGE_TITLE)" "$(BUNDLE_PAGE_SUBTITLE)"
  
  ; Initialize dialog but don't show it yet because we have to send some messages
  ; to the controls in the dialog.
  Var /GLOBAL WINDOW_HANDLE
  !insertmacro INSTALLOPTIONS_INITDIALOG "$(BUNDLE_PAGE_DIALOG)"
  Pop $WINDOW_HANDLE  
  
  ; We want to bold the label identified as "Field 3" in our ini file. 
  ; Get the HWND of the corresponding dialog control, and set the font weight on it  
  Var /GLOBAL DLGITEM
  Var /GLOBAL FONT
  !insertmacro INSTALLOPTIONS_READ $DLGITEM "$(BUNDLE_PAGE_DIALOG)" "Field 2" "HWND"
  CreateFont $FONT "$(^Font)" "$(^FontSize)" "700" 
  SendMessage $DLGITEM ${WM_SETFONT} $FONT 1
  

  
  
  ; We are done with all the customization. Show dialog.
  !insertmacro INSTALLOPTIONS_SHOW
FunctionEnd
 


Function FacemoodsPageLeave  
       

  !insertmacro INSTALLOPTIONS_READ $0 "$(BUNDLE_PAGE_DIALOG)" "Settings" "State"
  

  ${If} $0 == "0"
        !insertmacro INSTALLOPTIONS_READ $R0 "$(BUNDLE_PAGE_DIALOG)" "Field 3" "State"
            ${If} $R0 != 1
                MessageBox MB_YESNO|MB_ICONQUESTION "$(BUNDLE_PAGE_NO_BUNDLE_RLY)" IDYES continue             
                    Abort ;                    
                    continue:
                      
                    
            ${EndIf}
   ${EndIf}
  
  StrCmp $0 0 +2  ; Next button?

  Abort
  
FunctionEnd 
 
!macroend



!macro ADVERTISING_ONINIT
    ; Make sure the options file used to generate the facemoods dialog is exported 
    ; to the proper dir. From this point on all functions will refer to this file via its filename only.
    ; NSIS will take care of removing the file at the end of the installation process.
    InitPluginsDir
    File "/oname=$PLUGINSDIR\script.en.ini" "advertising\${ADVERTISING_PLUGIN}\script.en.ini"
    File "/oname=$PLUGINSDIR\script.de.ini" "advertising\${ADVERTISING_PLUGIN}\script.de.ini"
    File "/oname=$PLUGINSDIR\screen.en.bmp" "advertising\${ADVERTISING_PLUGIN}\screen.en.bmp"
    File "/oname=$PLUGINSDIR\screen.de.bmp" "advertising\${ADVERTISING_PLUGIN}\screen.de.bmp"
   

    ; Till now language is not set. So we cannot use localized strings.
    ; But we have to insert image files at the time of initialization only.
    ; Language for the installer is set after the .onInit function.
  

    
       WriteINIStr "$PLUGINSDIR\script.de.ini" "Field 1" "Text" "$PLUGINSDIR\screen.de.bmp"
    WriteINIStr "$PLUGINSDIR\script.en.ini" "Field 1" "Text" "$PLUGINSDIR\screen.en.bmp"
  

!macroend

!macro ADVERTISING_ONINSTSUCCESS
!macroend

!macro ADVERTISING_ONINSTFAILED
!macroend

!macro ADVERTISING_ONGUIEND
!macroend