#OpenCandy Advertising Plugin

!undef LICENSE
!define LICENSE ".\res\license_opencandy.txt" # /redef doesn't work for some reason
OutFile ".\dist\JDownloaderSetup_OpenCandy.exe"

!macro ADVERTISING_PAGE

PageEx custom
 PageCallbacks OpenCandyPageStartFn OpenCandyPageLeaveFn 
PageExEnd

!macroend

!macro ADVERTISING_GENERAL

!include "nsdialogs.nsh"
!AddIncludeDir ".\advertising\OpenCandy"
!include "OCSetupHlp.nsh"
!include "OpenCandyKeys.nsh"

Section "-Sample" SecAdvertising #Hidden (dialog before)
    ${If} ${UAC_IsAdmin}
        !insertmacro OpenCandyInstallDLL
        WriteRegStr SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising 1 
    ${EndIf}
SectionEnd

Section "-un.Sample" UNSecAdvertising
    DeleteRegValue SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising
SectionEnd



!macroend

!macro ADVERTISING_ONINIT
    ${If} ${UAC_IsAdmin}
        !insertmacro OpenCandyInit "$(^Name)" ${OpenCandyKey1} ${OpenCandyKey2} "${REGKEY}"
    ${EndIf}
!macroend

!macro ADVERTISING_ONINSTSUCCESS
    ${If} ${UAC_IsAdmin}
        !insertmacro OpenCandyOnInstSuccess
    ${EndIf}
!macroend

!macro ADVERTISING_ONINSTFAILED
    ${If} ${UAC_IsAdmin}
        !insertmacro OpenCandyOnInstFailed
    ${EndIf}
!macroend

!macro ADVERTISING_ONGUIEND
    ${If} ${UAC_IsAdmin}
        !insertmacro OpenCandyOnGuiEnd
    ${EndIf}
!macroend
