#Sample Advertising Plugin

#!undef LICENSE
#!define LICENSE ".\res\license_sample.txt"

!macro ADVERTISING_PAGE
!macroend

!macro ADVERTISING_GENERAL

Section "-Sample" SecAdvertising #Hidden (dialog before)
    WriteRegStr SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising 1 
SectionEnd

Section "-un.Sample" UNSecAdvertising
    DeleteRegValue SHELL_CONTEXT "${REGKEY}\Components" SecAdvertising
SectionEnd

!macroend

!macro ADVERTISING_ONINIT
!macroend

!macro ADVERTISING_ONINSTSUCCESS
!macroend

!macro ADVERTISING_ONINSTFAILED
!macroend

!macro ADVERTISING_ONGUIEND
!macroend
