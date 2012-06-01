/*
_____________________________________________________________________________
 
                       Protocol Association
                       (c) 2009 JDownloader
_____________________________________________________________________________
 
 Based on code taken from http://nsis.sourceforge.net/File_Association 
 
 Usage in script:
 1. !include "ProtocolAssociation.nsh"
 2. [Section|Function]
      ${ProtocolAssociationFunction} "Param1" "Param2" "..." $var
    [SectionEnd|FunctionEnd]
 
 ProtocolAssociationFunction=[RegisterProtocol|UnRegisterProtocol]
 
_____________________________________________________________________________
 
 ${RegisterProtocol} "[executable]" "[protocol]" "[description]"
 
"[executable]"     ; executable which opens the protocol format
                   ;
"[protocol]"      ; protocol, which represents the protocol to open
                   ;
"[description]"    ; description for the protocol. This will be display in Windows Explorer.
                   ;
 
 
 ${UnRegisterProtocol} "[protocol]" "[description]"
 
"[protocol]"      ; protocol, which represents the protocol format to open
                   ;
"[description]"    ; description for the protocol. This will be display in Windows Explorer.
                   ;
 
_____________________________________________________________________________
 
                         Macros
_____________________________________________________________________________
 
 Change log window verbosity (default: 3=no script)
 
 Example:
 !include "ProtocolAssociation.nsh"
 !insertmacro RegisterProtocol
 ${ProtocolAssociation_VERBOSE} 4   # all verbosity
 !insertmacro UnRegisterProtocol
 ${ProtocolAssociation_VERBOSE} 3   # no script
*/
 
 
!ifndef ProtocolAssociation_INCLUDED
!define ProtocolAssociation_INCLUDED
 
!include Util.nsh
 
!verbose push
!verbose 3
!ifndef _ProtocolAssociation_VERBOSE
  !define _ProtocolAssociation_VERBOSE 3
!endif
!verbose ${_ProtocolAssociation_VERBOSE}
!define ProtocolAssociation_VERBOSE `!insertmacro ProtocolAssociation_VERBOSE`
!verbose pop
 
!macro ProtocolAssociation_VERBOSE _VERBOSE
  !verbose push
  !verbose 3
  !undef _ProtocolAssociation_VERBOSE
  !define _ProtocolAssociation_VERBOSE ${_VERBOSE}
  !verbose pop
!macroend
 
 
 
!macro RegisterProtocolCall _EXECUTABLE _PROTOCOL _DESCRIPTION
  !verbose push
  !verbose ${_ProtocolAssociation_VERBOSE}
  Push `${_DESCRIPTION}`
  Push `${_PROTOCOL}`
  Push `${_EXECUTABLE}`
  ${CallArtificialFunction} RegisterProtocol_
  !verbose pop
!macroend
 
!macro UnRegisterProtocolCall _PROTOCOL _DESCRIPTION
  !verbose push
  !verbose ${_ProtocolAssociation_VERBOSE}
  Push `${_PROTOCOL}`
  Push `${_DESCRIPTION}`
  ${CallArtificialFunction} UnRegisterProtocol_
  !verbose pop
!macroend
 
 
 
!define RegisterProtocol `!insertmacro RegisterProtocolCall`
!define un.RegisterProtocol `!insertmacro RegisterProtocolCall`
 
!macro RegisterProtocol
!macroend
 
!macro un.RegisterProtocol
!macroend
 
!macro RegisterProtocol_
  !verbose push
  !verbose ${_ProtocolAssociation_VERBOSE}
 
  Exch $R2 ;exe
  Exch
  Exch $R1 ;protocol
  Exch
  Exch 2
  Exch $R0 ;desc
  Exch 2
  Push $0
  Push $1
  
${If} ${UAC_IsAdmin}
 
  ReadRegStr $1 HKCR $R1 ""  ; read current protocol association
  StrCmp "$1" "" NoProtocolBackupAdmin  ; is it empty
  StrCmp "$1" $R0 NoProtocolBackupAdmin  ; is it our own
    WriteRegStr HKCR $R1 "backup_val" "$1"  ; backup current value
    ReadRegStr $1 HKCR "$R1\shell\open\command" "" ; backup assoc. exe 
    WriteRegStr HKCR "$R1\shell\open\command" "backup_val" "$1"  
NoProtocolBackupAdmin:
  WriteRegStr HKCR $R1 "" $R0  ; set our protocol association
  WriteRegStr HKCR $R1 "URL Protocol" ""  ; set it as protocol
 
  ReadRegStr $0 HKCR "$R1\DefaultIcon" ""
  StrCmp $0 "" 0 ProtocolSkipAdmin
    WriteRegStr HKCR "$R1\DefaultIcon" "" "$R2,0"
ProtocolSkipAdmin:
  WriteRegStr HKCR "$R1\shell\open\command" "" '"$R2" "%1"'
 
${Else}
 
  ReadRegStr $1 HKCU "Software\Classes\$R1" ""  ; read current protocol association
  StrCmp "$1" "" NoProtocolBackupUser  ; is it empty
  StrCmp "$1" $R0 NoProtocolBackupUser  ; is it our own
    WriteRegStr HKCU "Software\Classes\$R1" "backup_val" "$1"  ; backup current value
    ReadRegStr $1 HKCU "Software\Classes\$R1\shell\open\command" "" ; backup assoc. exe 
    WriteRegStr HKCU "Software\Classes\$R1\shell\open\command" "backup_val" "$1"  
NoProtocolBackupUser:
  WriteRegStr HKCU "Software\Classes\$R1" "" $R0  ; set our protocol association
  WriteRegStr HKCU "Software\Classes\$R1" "URL Protocol" ""  ; set it as protocol
 
  ReadRegStr $0 HKCU "Software\Classes\$R1\DefaultIcon" ""
  StrCmp $0 "" 0 ProtocolSkipUser
    WriteRegStr HKCU "Software\Classes\$R1\DefaultIcon" "" "$R2,0"
ProtocolSkipUser:
  WriteRegStr HKCU "Software\Classes\$R1\shell\open\command" "" '"$R2" "%1"'

${EndIf}
 
  Pop $1
  Pop $0
  Pop $R2
  Pop $R1
  Pop $R0
 
  !verbose pop
!macroend
 
 
 
!define UnRegisterProtocol `!insertmacro UnRegisterProtocolCall`
!define un.UnRegisterProtocol `!insertmacro UnRegisterProtocolCall`
 
!macro UnRegisterProtocol
!macroend
 
!macro un.UnRegisterProtocol
!macroend
 
!macro UnRegisterProtocol_
  !verbose push
  !verbose ${_ProtocolAssociation_VERBOSE}
 
  Exch $R1 ;desc
  Exch
  Exch $R0 ;protocol
  Exch
  Push $0
  Push $1
 
${If} $ADMINATINSTALL > 0
  ReadRegStr $1 HKCR $R0 ""
  StrCmp $1 $R1 0 NoOwnProtocolAdmin ; only do this if we own it
  ReadRegStr $1 HKCR $R0 "backup_val"
  StrCmp $1 "" 0 ProtocolRestoreAdmin ; if backup="" then delete the whole key
  DeleteRegKey HKCR $R0
  Goto NoOwnProtocolAdmin
 
ProtocolRestoreAdmin:
  WriteRegStr HKCR $R0 "" $1
  DeleteRegValue HKCR $R0 "backup_val"
  ReadRegStr $1 HKCR "$R0\shell\open\command" "backup_val"
  WriteRegStr HKCR "$R0\shell\open\command" "" $1
  DeleteRegValue HKCR "$R0\shell\open\command" "backup_val"
 
NoOwnProtocolAdmin:
${Else}

  ReadRegStr $1 HKCU "Software\Classes\$R0" ""
  StrCmp $1 $R1 0 NoOwnProtocolUser ; only do this if we own it
  ReadRegStr $1 HKCU "Software\Classes\$R0" "backup_val"
  StrCmp $1 "" 0 ProtocolRestoreUser ; if backup="" then delete the whole key
  DeleteRegKey HKCU "Software\Classes\$R0"
  Goto NoOwnProtocolUser
 
ProtocolRestoreUser:
  WriteRegStr HKCU "Software\Classes\$R0" "" $1
  DeleteRegValue HKCU "Software\Classes\$R0" "backup_val"
  ReadRegStr $1 HKCU "Software\Classes\$R0\shell\open\command" "backup_val"
  WriteRegStr HKCU "Software\Classes\$R0\shell\open\command" "" $1
  DeleteRegValue HKCU "Software\Classes\$R0\shell\open\command" "backup_val"
 
NoOwnProtocolUser:
${EndIf}

  Pop $1
  Pop $0
  Pop $R1
  Pop $R0
 
  !verbose pop
!macroend
 
!endif # !ProtocolAssociation_INCLUDED