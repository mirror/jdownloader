### Macro for selecting uninstaller sections
!macro SELECT_UNSECTION SECTION_NAME UNSECTION_ID
    Push $R0
    
    ReadRegStr $R0 SHELL_CONTEXT "${REGKEY}\Components" "${SECTION_NAME}"
    
    StrCmp $R0 1 0 next${UNSECTION_ID}
    
    !insertmacro SelectSection "${UNSECTION_ID}"
    GoTo done${UNSECTION_ID}
next${UNSECTION_ID}:
    !insertmacro UnselectSection "${UNSECTION_ID}"
done${UNSECTION_ID}:
    Pop $R0
!macroend
###

###Macro for handling Vista/Win7 UAC Stuff, used in onInit an un.onInit
!macro doUAC
    !insertmacro UAC_RunElevated
    ${Switch} $0
    ${Case} 0
        ${IfThen} $1 = 1 ${|} Quit ${|} ;we are the outer process, the inner process has done its work, we are done
        ${IfThen} $3 <> 0 ${|} ${Break} ${|} ;we are admin, let the show go on
    ${Case} 1062
        #MessageBox mb_IconStop|mb_TopMost|mb_SetForeground "Logon service not running, aborting!"
        #Quit
    ${EndSwitch}
!macroend
###

###These Macros search for the correct uninstall directory
!macro checkDir
    ReadRegStr $INSTDIR SHELL_CONTEXT "${REGKEY}" "Path"
    StrCmp $INSTDIR "" 0 UninstDirFound
!macroend

#check if installed with admin rights
!macro INSTDIRfromAdminInstall
    StrCpy $ADMINATINSTALL 1
    SetShellVarContext all
    !insertmacro checkDir
!macroend

#check if installed without adminrights
!macro INSTDIRfromUserInstall
    StrCpy $ADMINATINSTALL 0
    SetShellVarContext current
    !insertmacro checkDir
!macroend
 
#check if current dir might be installation dir, display warning
!macro INSTDIRfromEXEDIR
    StrCpy $ADMINATINSTALL 1 #assume worst case
    SetShellVarContext all
    StrCpy $INSTDIR $EXEDIR #Attention: Danger!! http://nsis.sourceforge.net/Validating_$INSTDIR_before_uninstall
    StrLen $R1 "\${APPNAME_SHORT}"
    StrCpy $R0 $INSTDIR "" -$R1
    StrCmp $R0 "\${APPNAME_SHORT}" +3
        MessageBox MB_YESNO|MB_ICONQUESTION "Unrecognised uninstall path. Continue anyway?" IDYES +2
        Abort
    IfFileExists "$INSTDIR\JDownloader.exe" +3
        MessageBox MB_YESNO|MB_ICONQUESTION "JDownloader.exe not found in uninstall path. Continue anyway?" IDYES +2
        Abort
!macroend

!macro determineInstallDir
    ${If} ${UAC_IsAdmin}
        !insertmacro INSTDIRfromAdminInstall
        !insertmacro INSTDIRfromUserInstall
        !insertmacro INSTDIRfromEXEDIR
    ${Else}
      !insertmacro INSTDIRfromUserInstall
      !insertmacro INSTDIRfromAdminInstall
      !insertmacro INSTDIRfromEXEDIR
    ${EndIf}
    
    ${IfNot} ${UAC_IsAdmin}
        ${If} $ADMINATINSTALL > 0
            MessageBox MB_YESNO|MB_ICONQUESTION $(InstalledAsAdmin) IDYES +2
            Abort
        ${EndIf}
    ${EndIf}
    UninstDirFound:
!macroend
###

###This function removes a dir recursively except one of its subdirs
Function un.RmButOne
 Exch $R0 ; exclude dir
 Exch
 Exch $R1 ; route dir
 Push $R2
 Push $R3
 
  ClearErrors
  FindFirst $R3 $R2 "$R1\*.*"
  IfErrors Exit
 
  Top:
   StrCmp $R2 "." Next
   StrCmp $R2 ".." Next
   StrCmp $R2 $R0 Next
   IfFileExists "$R1\$R2\*.*" 0 DelFile # is it a dir?
   RmDir /r /REBOOTOK "$R1\$R2"
   Goto Next
   DelFile:
    Delete /REBOOTOK "$R1\$R2"
   Next:
    ClearErrors
    FindNext $R3 $R2
    IfErrors Exit
   Goto Top
 
  Exit:
  FindClose $R3
 
 Pop $R3
 Pop $R2
 Pop $R1
 Pop $R0
FunctionEnd
###

