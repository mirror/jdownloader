;
; OCSetupHlp.nsh
; --------------
;
; OpenCandy Helper Include File
;
; This file defines a few macros that need to be called
; from your main installer script in order to initialize and
; setup OpenCandy.
;
; Copyright (c) 2008, 2010 - OpenCandy, Inc.
;

!include FileFunc.nsh
!include nsDialogs.nsh
!insertmacro GetParameters

; Local Variables

Var OCUseOfferPage
Var OCPageTitle
Var OCPageDesc
Var OCDetached
Var OCDialog
Var OCPPLabel
Var OCTOULabel
Var OCProductKey
Var OCNoCandy
Var OCRemnant
Var OCOSSupported
Var OCInitcode

;
; Local Functions
; -----------------
;
; These functions are only used here, and
; are not part of the exposed OC API
;

!ifndef NSIS_UNICODE

;
; ConvertMultiByte (converts between codepages)
; ----------------
;
; We use this function to convert from
; UTF-8 (codepage 65001) to the codepage
; NSIS is using ($LANGUAGE) for server-sent
; strings that need to be displayed
;
; Usage:
; ------
;	Push "Blah"            # Input text
;	Push "65001"           # Input text code page (usually CP_UTF8)
;	Push "1036"            # Output text code page (Usually $LANGUAGE)
;	Call ConvertMultiByte  # Actual Function Call
;	Pop $0                 # Retrieve Output text
;

Function ConvertMultiByte

  Exch $2
  Exch
  Exch $1
  Exch
  Exch 2
  Exch $0
  Exch 2
  Push $3
  Push $4
  Push $5
  Push $6
  Push $7
  Push $8
  Push $9

  StrLen $3 $0
  IntOp $3 $3 + 1
  IntOp $4 $3 * 2
  IntOp $5 $3 * 3

  System::Alloc /NOUNLOAD $4
  Pop $6
  System::Call /NOUNLOAD 'kernel32::MultiByteToWideChar(i $1, i 0, t r0, i r3, i r6, i r3) i .r8'

  System::Alloc /NOUNLOAD $5
  Pop $7
  System::Call /NOUNLOAD 'kernel32::WideCharToMultiByte(i $2, i 0, i r6, i r8, i r7, i r5, i 0, i 0) i .r9'

  ; Check if we got an error, which may happen if the desired codepage
  ; is not installed in which case we will fallback to ANSI codepage (0)

  IntCmp $9 0 ANSI_Needed
  Goto ANSI_Not_Needed

ANSI_Needed:

  System::Call /NOUNLOAD 'kernel32::WideCharToMultiByte(i 0, i 0, i r6, i r8, i r7, i r5, i 0, i 0) i .r9'

ANSI_Not_Needed:

  System::Call /NOUNLOAD '*$7(&t$5 .r0)'
  System::Free /NOUNLOAD $6
  System::Free $7

  Pop $9
  Pop $8
  Pop $7
  Pop $6
  Pop $5
  Pop $4
  Pop $3
  Pop $2
  Pop $1

  Exch $0

FunctionEnd

!endif ; NSIS_UNICODE

;
; GetLanguageString
;

Function GetLanguageString

  ${If} $LANGUAGE == 1078
    StrCpy $0 "af" ; Afrikaans
  ${ElseIf} $LANGUAGE == 1052
    StrCpy $0 "sq" ; Albanian
  ${ElseIf} $LANGUAGE == 1118
    StrCpy $0 "am" ; Amharic
  ${ElseIf} $LANGUAGE == 1025
    StrCpy $0 "ar" ; Arabic - Saudi Arabia
  ${ElseIf} $LANGUAGE == 5121
    StrCpy $0 "ar" ; Arabic - Algeria
  ${ElseIf} $LANGUAGE == 15361
    StrCpy $0 "ar" ; Arabic - Bahrain
  ${ElseIf} $LANGUAGE == 3073
    StrCpy $0 "ar" ; Arabic - Egypt
  ${ElseIf} $LANGUAGE == 2049
    StrCpy $0 "ar" ; Arabic - Iraq
  ${ElseIf} $LANGUAGE == 11265
    StrCpy $0 "ar" ; Arabic - Jordan
  ${ElseIf} $LANGUAGE == 13313
    StrCpy $0 "ar" ; Arabic - Kuwait
  ${ElseIf} $LANGUAGE == 12289
    StrCpy $0 "ar" ; Arabic - Lebanon
  ${ElseIf} $LANGUAGE == 4097
    StrCpy $0 "ar" ; Arabic - Libya
  ${ElseIf} $LANGUAGE == 6145
    StrCpy $0 "ar" ; Arabic - Morocco
  ${ElseIf} $LANGUAGE == 8193
    StrCpy $0 "ar" ; Arabic - Oman
  ${ElseIf} $LANGUAGE == 16385
    StrCpy $0 "ar" ; Arabic - Qatar
  ${ElseIf} $LANGUAGE == 10241
    StrCpy $0 "ar" ; Arabic - Syria
  ${ElseIf} $LANGUAGE == 7169
    StrCpy $0 "ar" ; Arabic - Tunisia
  ${ElseIf} $LANGUAGE == 14337
    StrCpy $0 "ar" ; Arabic - U.A.E.
  ${ElseIf} $LANGUAGE == 9217
    StrCpy $0 "ar" ; Arabic - Yemen
  ${ElseIf} $LANGUAGE == 1067
    StrCpy $0 "hy" ; Armenian - Armenia
  ${ElseIf} $LANGUAGE == 1101
    StrCpy $0 "as" ; Assamese
  ${ElseIf} $LANGUAGE == 2092
    StrCpy $0 "az" ; Azeri (Cyrillic)
  ${ElseIf} $LANGUAGE == 1068
    StrCpy $0 "az" ; Azeri (Latin)
  ${ElseIf} $LANGUAGE == 1069
    StrCpy $0 "eu" ; Basque
  ${ElseIf} $LANGUAGE == 1059
    StrCpy $0 "be" ; Belarusian
  ${ElseIf} $LANGUAGE == 1093
    StrCpy $0 "bn" ; Bengali (India)
  ${ElseIf} $LANGUAGE == 2117
    StrCpy $0 "bn" ; Bengali (Bangladesh)
  ${ElseIf} $LANGUAGE == 5146
    StrCpy $0 "bs" ; Bosnian (Bosnia/Herzegovina)
  ${ElseIf} $LANGUAGE == 1026
    StrCpy $0 "bg" ; Bulgarian
  ${ElseIf} $LANGUAGE == 1109
    StrCpy $0 "my" ; Burmese
  ${ElseIf} $LANGUAGE == 1027
    StrCpy $0 "ca" ; Catalan
  ${ElseIf} $LANGUAGE == 1116
    StrCpy $0 "en" ; Cherokee - United States (no twochar lang code)
  ${ElseIf} $LANGUAGE == 2052
    StrCpy $0 "zh" ; Chinese - People's Republic of China
  ${ElseIf} $LANGUAGE == 4100
    StrCpy $0 "zh" ; Chinese - Singapore
  ${ElseIf} $LANGUAGE == 1028
    StrCpy $0 "zh" ; Chinese - Taiwan
  ${ElseIf} $LANGUAGE == 3076
    StrCpy $0 "zh" ; Chinese - Hong Kong SAR
  ${ElseIf} $LANGUAGE == 5124
    StrCpy $0 "zh" ; Chinese - Macao SAR
  ${ElseIf} $LANGUAGE == 1050
    StrCpy $0 "hr" ; Croatian
  ${ElseIf} $LANGUAGE == 4122
    StrCpy $0 "hr" ; Croatian (Bosnia/Herzegovina)
  ${ElseIf} $LANGUAGE == 1029
    StrCpy $0 "cs" ; Czech
  ${ElseIf} $LANGUAGE == 1030
    StrCpy $0 "da" ; Danish
  ${ElseIf} $LANGUAGE == 1125
    StrCpy $0 "dv" ; Divehi
  ${ElseIf} $LANGUAGE == 1043
    StrCpy $0 "nl" ; Dutch - Netherlands
  ${ElseIf} $LANGUAGE == 2067
    StrCpy $0 "nl" ; Dutch - Belgium
  ${ElseIf} $LANGUAGE == 1126
    StrCpy $0 "en" ; Edo (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1033
    StrCpy $0 "en" ; English - United States
  ${ElseIf} $LANGUAGE == 2057
    StrCpy $0 "en" ; English - United Kingdom
  ${ElseIf} $LANGUAGE == 3081
    StrCpy $0 "en" ; English - Australia
  ${ElseIf} $LANGUAGE == 10249
    StrCpy $0 "en" ; English - Belize
  ${ElseIf} $LANGUAGE == 4105
    StrCpy $0 "en" ; English - Canada
  ${ElseIf} $LANGUAGE == 9225
    StrCpy $0 "en" ; English - Caribbean
  ${ElseIf} $LANGUAGE == 15369
    StrCpy $0 "en" ; English - Hong Kong SAR
  ${ElseIf} $LANGUAGE == 16393
    StrCpy $0 "en" ; English - India
  ${ElseIf} $LANGUAGE == 14345
    StrCpy $0 "en" ; English - Indonesia
  ${ElseIf} $LANGUAGE == 6153
    StrCpy $0 "en" ; English - Ireland
  ${ElseIf} $LANGUAGE == 8201
    StrCpy $0 "en" ; English - Jamaica
  ${ElseIf} $LANGUAGE == 17417
    StrCpy $0 "en" ; English - Malaysia
  ${ElseIf} $LANGUAGE == 5129
    StrCpy $0 "en" ; English - New Zealand
  ${ElseIf} $LANGUAGE == 13321
    StrCpy $0 "en" ; English - Philippines
  ${ElseIf} $LANGUAGE == 18441
    StrCpy $0 "en" ; English - Singapore
  ${ElseIf} $LANGUAGE == 7177
    StrCpy $0 "en" ; English - South Africa
  ${ElseIf} $LANGUAGE == 11273
    StrCpy $0 "en" ; English - Trinidad
  ${ElseIf} $LANGUAGE == 12297
    StrCpy $0 "en" ; English - Zimbabwe
  ${ElseIf} $LANGUAGE == 1061
    StrCpy $0 "et" ; Estonian
  ${ElseIf} $LANGUAGE == 1080
    StrCpy $0 "fo" ; Faroese
  ${ElseIf} $LANGUAGE == 1065
    StrCpy $0 "en" ; Farsi (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1124
    StrCpy $0 "en" ; Filipino (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1035
    StrCpy $0 "fi" ; Finnish
  ${ElseIf} $LANGUAGE == 1036
    StrCpy $0 "fr" ; French - France
  ${ElseIf} $LANGUAGE == 2060
    StrCpy $0 "fr" ; French - Belgium
  ${ElseIf} $LANGUAGE == 11276
    StrCpy $0 "fr" ; French - Cameroon
  ${ElseIf} $LANGUAGE == 3084
    StrCpy $0 "fr" ; French - Canada
  ${ElseIf} $LANGUAGE == 9228
    StrCpy $0 "fr" ; French - Democratic Rep. of Congo
  ${ElseIf} $LANGUAGE == 12300
    StrCpy $0 "fr" ; French - Cote d'Ivoire
  ${ElseIf} $LANGUAGE == 15372
    StrCpy $0 "fr" ; French - Haiti
  ${ElseIf} $LANGUAGE == 5132
    StrCpy $0 "fr" ; French - Luxembourg
  ${ElseIf} $LANGUAGE == 13324
    StrCpy $0 "fr" ; French - Mali
  ${ElseIf} $LANGUAGE == 6156
    StrCpy $0 "fr" ; French - Monaco
  ${ElseIf} $LANGUAGE == 14348
    StrCpy $0 "fr" ; French - Morocco
  ${ElseIf} $LANGUAGE == 58380
    StrCpy $0 "fr" ; French - North Africa
  ${ElseIf} $LANGUAGE == 8204
    StrCpy $0 "fr" ; French - Reunion
  ${ElseIf} $LANGUAGE == 10252
    StrCpy $0 "fr" ; French - Senegal
  ${ElseIf} $LANGUAGE == 4108
    StrCpy $0 "fr" ; French - Switzerland
  ${ElseIf} $LANGUAGE == 7180
    StrCpy $0 "fr" ; French - West Indies
  ${ElseIf} $LANGUAGE == 1127
    StrCpy $0 "ff" ; Fulfulde - Nigeria
  ${ElseIf} $LANGUAGE == 1122
    StrCpy $0 "fy" ; Frisian - Netherlands
  ${ElseIf} $LANGUAGE == 1071
    StrCpy $0 "mk" ; FYRO Macedonian
  ${ElseIf} $LANGUAGE == 2108
    StrCpy $0 "gd" ; Gaelic (Ireland)
  ${ElseIf} $LANGUAGE == 1084
    StrCpy $0 "gd" ; Gaelic (Scotland)
  ${ElseIf} $LANGUAGE == 1110
    StrCpy $0 "gl" ; Galician
  ${ElseIf} $LANGUAGE == 1079
    StrCpy $0 "ka" ; Georgian
  ${ElseIf} $LANGUAGE == 1031
    StrCpy $0 "de" ; German - Germany
  ${ElseIf} $LANGUAGE == 3079
    StrCpy $0 "de" ; German - Austria
  ${ElseIf} $LANGUAGE == 5127
    StrCpy $0 "de" ; German - Liechtenstein
  ${ElseIf} $LANGUAGE == 4103
    StrCpy $0 "de" ; German - Luxembourg
  ${ElseIf} $LANGUAGE == 2055
    StrCpy $0 "de" ; German - Switzerland
  ${ElseIf} $LANGUAGE == 1032
    StrCpy $0 "el" ; Greek
  ${ElseIf} $LANGUAGE == 1140
    StrCpy $0 "gn" ; Guarani - Paraguay
  ${ElseIf} $LANGUAGE == 1095
    StrCpy $0 "gu" ; Gujarati
  ${ElseIf} $LANGUAGE == 1128
    StrCpy $0 "ha" ; Hausa - Nigeria
  ${ElseIf} $LANGUAGE == 1141
    StrCpy $0 "en" ; Hawaiian - United States (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1037
    StrCpy $0 "he" ; Hebrew
  ${ElseIf} $LANGUAGE == 1081
    StrCpy $0 "hi" ; Hindi
  ${ElseIf} $LANGUAGE == 1038
    StrCpy $0 "hu" ; Hungarian
  ${ElseIf} $LANGUAGE == 1129
    StrCpy $0 "ig" ; Ibibio - Nigeria
  ${ElseIf} $LANGUAGE == 1039
    StrCpy $0 "is" ; Icelandic
  ${ElseIf} $LANGUAGE == 1136
    StrCpy $0 "ig" ; Igbo - Nigeria
  ${ElseIf} $LANGUAGE == 1057
    StrCpy $0 "id" ; Indonesian
  ${ElseIf} $LANGUAGE == 1117
    StrCpy $0 "iu" ; Inuktitut
  ${ElseIf} $LANGUAGE == 1040
    StrCpy $0 "it" ; Italian - Italy
  ${ElseIf} $LANGUAGE == 2064
    StrCpy $0 "it" ; Italian - Switzerland
  ${ElseIf} $LANGUAGE == 1041
    StrCpy $0 "ja" ; Japanese
  ${ElseIf} $LANGUAGE == 1099
    StrCpy $0 "kn" ; Kannada
  ${ElseIf} $LANGUAGE == 1137
    StrCpy $0 "kr" ; Kanuri - Nigeria
  ${ElseIf} $LANGUAGE == 2144
    StrCpy $0 "ks" ; Kashmiri
  ${ElseIf} $LANGUAGE == 1120
    StrCpy $0 "ks" ; Kashmiri (Arabic)
  ${ElseIf} $LANGUAGE == 1087
    StrCpy $0 "kk" ; Kazakh
  ${ElseIf} $LANGUAGE == 1107
    StrCpy $0 "km" ; Khmer
  ${ElseIf} $LANGUAGE == 1111
    StrCpy $0 "ki" ; Konkani
  ${ElseIf} $LANGUAGE == 1042
    StrCpy $0 "ko" ; Korean
  ${ElseIf} $LANGUAGE == 1088
    StrCpy $0 "ky" ; Kyrgyz (Cyrillic)
  ${ElseIf} $LANGUAGE == 1108
    StrCpy $0 "lo" ; Lao
  ${ElseIf} $LANGUAGE == 1142
    StrCpy $0 "la" ; Latin
  ${ElseIf} $LANGUAGE == 1062
    StrCpy $0 "lv" ; Latvian
  ${ElseIf} $LANGUAGE == 1063
    StrCpy $0 "lt" ; Lithuanian
  ${ElseIf} $LANGUAGE == 1086
    StrCpy $0 "ms" ; Malay - Malaysia
  ${ElseIf} $LANGUAGE == 2110
    StrCpy $0 "ms" ; Malay - Brunei Darussalam
  ${ElseIf} $LANGUAGE == 1100
    StrCpy $0 "ml" ; Malayalam
  ${ElseIf} $LANGUAGE == 1082
    StrCpy $0 "mt" ; Maltese
  ${ElseIf} $LANGUAGE == 1112
    StrCpy $0 "en" ; Manipuri (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1153
    StrCpy $0 "mi" ; Maori - New Zealand
  ${ElseIf} $LANGUAGE == 1102
    StrCpy $0 "mr" ; Marathi
  ${ElseIf} $LANGUAGE == 1104
    StrCpy $0 "mn" ; Mongolian (Cyrillic)
  ${ElseIf} $LANGUAGE == 2128
    StrCpy $0 "mn" ; Mongolian (Mongolian)
  ${ElseIf} $LANGUAGE == 1121
    StrCpy $0 "ne" ; Nepali
  ${ElseIf} $LANGUAGE == 2145
    StrCpy $0 "ne" ; Nepali - India
  ${ElseIf} $LANGUAGE == 1044
    StrCpy $0 "nb" ; Norwegian (Bokmål)
  ${ElseIf} $LANGUAGE == 2068
    StrCpy $0 "no" ; Norwegian (Nynorsk)
  ${ElseIf} $LANGUAGE == 1096
    StrCpy $0 "or" ; Oriya
  ${ElseIf} $LANGUAGE == 1138
    StrCpy $0 "om" ; Oromo
  ${ElseIf} $LANGUAGE == 1145
    StrCpy $0 "en" ; Papiamentu (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1123
    StrCpy $0 "ps" ; Pashto
  ${ElseIf} $LANGUAGE == 1045
    StrCpy $0 "pl" ; Polish
  ${ElseIf} $LANGUAGE == 1046
    StrCpy $0 "pt" ; Portuguese - Brazil
  ${ElseIf} $LANGUAGE == 2070
    StrCpy $0 "pt" ; Portuguese - Portugal
  ${ElseIf} $LANGUAGE == 1094
    StrCpy $0 "pa" ; Punjabi
  ${ElseIf} $LANGUAGE == 2118
    StrCpy $0 "pa" ; Punjabi (Pakistan)
  ${ElseIf} $LANGUAGE == 1131
    StrCpy $0 "qu" ; Quecha - Bolivia
  ${ElseIf} $LANGUAGE == 2155
    StrCpy $0 "qu" ; Quecha - Ecuador
  ${ElseIf} $LANGUAGE == 3179
    StrCpy $0 "qu" ; Quecha - Peru
  ${ElseIf} $LANGUAGE == 1047
    StrCpy $0 "rm" ; Rhaeto-Romanic
  ${ElseIf} $LANGUAGE == 1048
    StrCpy $0 "ro" ; Romanian
  ${ElseIf} $LANGUAGE == 2072
    StrCpy $0 "ro" ; Romanian - Moldava
  ${ElseIf} $LANGUAGE == 1049
    StrCpy $0 "ru" ; Russian
  ${ElseIf} $LANGUAGE == 2073
    StrCpy $0 "ru" ; Russian - Moldava
  ${ElseIf} $LANGUAGE == 1083
    StrCpy $0 "se" ; Sami (Lappish)
  ${ElseIf} $LANGUAGE == 1103
    StrCpy $0 "sa" ; Sanskrit
  ${ElseIf} $LANGUAGE == 1132
    StrCpy $0 "en" ; Sepedi (NOT FOUND)
  ${ElseIf} $LANGUAGE == 3098
    StrCpy $0 "sr" ; Serbian (Cyrillic)
  ${ElseIf} $LANGUAGE == 2074
    StrCpy $0 "sr" ; Serbian (Latin)
  ${ElseIf} $LANGUAGE == 1113
    StrCpy $0 "" ; Sindhi - India
  ${ElseIf} $LANGUAGE == 2137
    StrCpy $0 "sd" ; Sindhi - Pakistan
  ${ElseIf} $LANGUAGE == 1115
    StrCpy $0 "si" ; Sinhalese - Sri Lanka
  ${ElseIf} $LANGUAGE == 1051
    StrCpy $0 "sk" ; Slovak
  ${ElseIf} $LANGUAGE == 1060
    StrCpy $0 "sl" ; Slovenian
  ${ElseIf} $LANGUAGE == 1143
    StrCpy $0 "so" ; Somali
  ${ElseIf} $LANGUAGE == 1070
    StrCpy $0 "en" ; Sorbian (NOT FOUND)
  ${ElseIf} $LANGUAGE == 3082
    StrCpy $0 "es" ; Spanish - Spain (Modern Sort)
  ${ElseIf} $LANGUAGE == 1034
    StrCpy $0 "es" ; Spanish - Spain (Traditional Sort)
  ${ElseIf} $LANGUAGE == 11274
    StrCpy $0 "es" ; Spanish - Argentina
  ${ElseIf} $LANGUAGE == 16394
    StrCpy $0 "es" ; Spanish - Bolivia
  ${ElseIf} $LANGUAGE == 13322
    StrCpy $0 "es" ; Spanish - Chile
  ${ElseIf} $LANGUAGE == 9226
    StrCpy $0 "es" ; Spanish - Colombia
  ${ElseIf} $LANGUAGE == 5130
    StrCpy $0 "es" ; Spanish - Costa Rica
  ${ElseIf} $LANGUAGE == 7178
    StrCpy $0 "es" ; Spanish - Dominican Republic
  ${ElseIf} $LANGUAGE == 12298
    StrCpy $0 "es" ; Spanish - Ecuador
  ${ElseIf} $LANGUAGE == 17418
    StrCpy $0 "es" ; Spanish - El Salvador
  ${ElseIf} $LANGUAGE == 4106
    StrCpy $0 "es" ; Spanish - Guatemala
  ${ElseIf} $LANGUAGE == 18442
    StrCpy $0 "es" ; Spanish - Honduras
  ${ElseIf} $LANGUAGE == 58378
    StrCpy $0 "es" ; Spanish - Latin America
  ${ElseIf} $LANGUAGE == 2058
    StrCpy $0 "es" ; Spanish - Mexico
  ${ElseIf} $LANGUAGE == 19466
    StrCpy $0 "es" ; Spanish - Nicaragua
  ${ElseIf} $LANGUAGE == 6154
    StrCpy $0 "es" ; Spanish - Panama
  ${ElseIf} $LANGUAGE == 15370
    StrCpy $0 "es" ; Spanish - Paraguay
  ${ElseIf} $LANGUAGE == 10250
    StrCpy $0 "es" ; Spanish - Peru
  ${ElseIf} $LANGUAGE == 20490
    StrCpy $0 "es" ; Spanish - Puerto Rico
  ${ElseIf} $LANGUAGE == 21514
    StrCpy $0 "es" ; Spanish - United States
  ${ElseIf} $LANGUAGE == 14346
    StrCpy $0 "es" ; Spanish - Uruguay
  ${ElseIf} $LANGUAGE == 8202
    StrCpy $0 "es" ; Spanish - Venezuela
  ${ElseIf} $LANGUAGE == 1072
    StrCpy $0 "en" ; Sutu (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1089
    StrCpy $0 "sw" ; Swahili
  ${ElseIf} $LANGUAGE == 1053
    StrCpy $0 "sv" ; Swedish
  ${ElseIf} $LANGUAGE == 2077
    StrCpy $0 "sv" ; Swedish - Finland
  ${ElseIf} $LANGUAGE == 1114
    StrCpy $0 "en" ; Syriac (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1064
    StrCpy $0 "tg" ; Tajik
  ${ElseIf} $LANGUAGE == 1119
    StrCpy $0 "en" ; Tamazight (Arabic) (NOT FOUND)
  ${ElseIf} $LANGUAGE == 2143
    StrCpy $0 "en" ; Tamazight (Latin) (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1097
    StrCpy $0 "ta" ; Tamil
  ${ElseIf} $LANGUAGE == 1092
    StrCpy $0 "tt" ; Tatar
  ${ElseIf} $LANGUAGE == 1098
    StrCpy $0 "te" ; Telugu
  ${ElseIf} $LANGUAGE == 1054
    StrCpy $0 "th" ; Thai
  ${ElseIf} $LANGUAGE == 2129
    StrCpy $0 "bo" ; Tibetan - Bhutan
  ${ElseIf} $LANGUAGE == 1105
    StrCpy $0 "bo" ; Tibetan - People's Republic of China
  ${ElseIf} $LANGUAGE == 2163
    StrCpy $0 "ti" ; Tigrigna - Eritrea
  ${ElseIf} $LANGUAGE == 1139
    StrCpy $0 "ti" ; Tigrigna - Ethiopia
  ${ElseIf} $LANGUAGE == 1073
    StrCpy $0 "ts" ; Tsonga
  ${ElseIf} $LANGUAGE == 1074
    StrCpy $0 "tn" ; Tswana
  ${ElseIf} $LANGUAGE == 1055
    StrCpy $0 "tr" ; Turkish
  ${ElseIf} $LANGUAGE == 1090
    StrCpy $0 "tk" ; Turkmen
  ${ElseIf} $LANGUAGE == 1152
    StrCpy $0 "ug" ; Uighur - China
  ${ElseIf} $LANGUAGE == 1058
    StrCpy $0 "ug" ; Ukrainian
  ${ElseIf} $LANGUAGE == 1056
    StrCpy $0 "ur" ; Urdu
  ${ElseIf} $LANGUAGE == 2080
    StrCpy $0 "ur" ; Urdu - India
  ${ElseIf} $LANGUAGE == 2115
    StrCpy $0 "uz" ; Uzbek (Cyrillic)
  ${ElseIf} $LANGUAGE == 1091
    StrCpy $0 "uz" ; Uzbek (Latin)
  ${ElseIf} $LANGUAGE == 1075
    StrCpy $0 "ve" ; Venda
  ${ElseIf} $LANGUAGE == 1066
    StrCpy $0 "vi" ; Vietnamese
  ${ElseIf} $LANGUAGE == 1106
    StrCpy $0 "cy" ; Welsh
  ${ElseIf} $LANGUAGE == 1076
    StrCpy $0 "xh" ; Xhosa
  ${ElseIf} $LANGUAGE == 1144
    StrCpy $0 "en" ; Yi (NOT FOUND)
  ${ElseIf} $LANGUAGE == 1085
    StrCpy $0 "yi" ; Yiddish
  ${ElseIf} $LANGUAGE == 1130
    StrCpy $0 "yo" ; Yoruba
  ${ElseIf} $LANGUAGE == 1077
    StrCpy $0 "zu" ; Zulu
  ${Else}
    StrCpy $0 "en" ; English is our backup
  ${Endif}
FunctionEnd

SetPluginUnload alwaysoff

!macro CheckNoCandy
	;Fills $0 with 0 if not found 1 if found
	push 	$1
	push 	$2
	push 	$3
	${GetParameters} $0

	StrLen 	$1 $0 ;$1 Len of str
	IntOp  	$2 0 + 0 ;offset
	
OCLookForNoCandy:
	StrCpy 	$3 $0 8 $2
	StrCmp 	$3 "/NOCANDY" OCFoundNoCandy
	IntCmp 	$2 $1 OCNoCandyNotHere 0 OCNoCandyNotHere 
	IntOp  	$2 $2 + 1
    Goto   	OCLookForNoCandy

OCFoundNoCandy:
	IntOp 	$0 0 + 1
	IntOp   $OCNoCandy 0 + 1
	Goto 	OCNoCandyDone
	
OCNoCandyNotHere:
    IntOp 	$0 0 + 0
	IntOp   $OCNoCandy 0 + 0

OCNoCandyDone:
	pop 	$3
	pop 	$2
	pop 	$1
!macroend

!macro GetCandyParameter
	;Fills $0 with "" if not found OID if found
	push 	$1
	push 	$2
	push 	$3
	push	$4
	${GetParameters} $0
	StrLen 	$1 $0 ;$1 Len of str
	IntOp  	$2 0 + 0 ;offset

OCLookForCandyRX:
	StrCpy 	$3 $0 8 $2
	StrCmp 	$3 "/CANDYRX" OCFoundCandyRX
	IntCmp 	$2 $1 OCCandyRXNotHere 0 OCCandyRXNotHere 
	IntOp  	$2 $2 + 1
    	Goto 	OCLookForCandyRX

OCFoundCandyRX:
	IntOp   $1 $1 - $2  
	StrCpy  $3 $0 $1 $2

	;trim anything past a space if found
	IntOp	$2 0 + 0
	StrCpy	$0 ""

OCCandyRxSpaceSearch:
	StrCpy  $4 $3 1 $2
	StrCmp  $4 " " OCCandyRXDone
	IntOp   $2 $2 + 1

	;copy what we got so far
	StrCpy  $0 $3 $2 
	
	IntCmp  $2 $1 OCCandyRXDone OCCandyRxSpaceSearch OCCandyRXDone 

OCCandyRXNotHere:
    	StrCpy  $0 ""

OCCandyRXDone:

	pop	$4
	pop 	$3
	pop 	$2
	pop 	$1

!macroend

!macro OpenCandyInitInternal PublisherName Key Secret Location

  ; -------------------------- OS CHECK -----------------------

  Push $R0
  Push $R1
  
  IntOp $OCOSSupported 0 + 0
 
  ClearErrors
 
  ReadRegStr $R0 HKLM \
  "SOFTWARE\Microsoft\Windows NT\CurrentVersion" CurrentVersion
 
  IfErrors 0 lbl_winnt
 
  ; we are not NT
  GoTo lbl_error
 
  lbl_winnt:
 
  StrCpy $R1 $R0 1
 
  StrCmp $R1 '3' lbl_done
  StrCmp $R1 '4' lbl_done
 
  StrCpy $R1 $R0 3
 
  StrCmp $R1 '5.0' lbl_error
  StrCmp $R1 '5.1' lbl_done
  StrCmp $R1 '5.2' lbl_done
  StrCmp $R1 '6.0' lbl_done
  StrCmp $R1 '6.1' lbl_done
 
  lbl_error:
  IntOp $OCOSSupported 0 + 1
    
  lbl_done:
 
  Pop $R1
  Pop $R0
  IntOP $OCUseOfferPage 0 + 0
  IntOP $OCInitcode 0 + 0
  ; -------------------------- OS CHECK -----------------------

   ${If} $OCOSSupported == 0

	  ; We need to be loaded throughout the setup
	  ; as we will uload ourselves when necessary
	  
	  Push $0
	  Push $1
	  Push $2
	  Push $3
	  Push $4
	  Push $5
	  
	  IntOp 	$OCDetached 0 + 1
	  StrCpy 	$OCProductKey "${Key}"

	  !insertmacro CheckNoCandy
	  
	  IntOp $4 $0 + 0
	  IntOp $5 $0 + 0

	  ${If} $OCRemnant == 0

	  ${If} $4 == 0
		  StrCpy $0 "${PublisherName}"	     ; Publisher

		  StrCpy $1 "${Key}"	; Product "Key"
		  StrCpy $2 "${Secret}"	; Secret

		  ; Get installer language
		  
		  Push $0
		  Call GetLanguageString
		  StrCpy $3 $0
		  Pop $0

		  StrCpy $4 "${Location}" ;Registry location

                  InitPluginsDir
                  SetOutPath "$PLUGINSDIR"
                  File "OCSetupHlp.dll"

!ifdef NSIS_UNICODE
		  System::Call 'OCSetupHlp::OCInit2A(m, m, m, m, m)i(r0, r1, r2 ,r3 ,r4).r5? c'
!else
		  System::Call 'OCSetupHlp::OCInit2A(t, t, t, t, t)i(r0, r1, r2 ,r3 ,r4).r5? c'
!endif
	  ${Else}
		
		  !insertmacro GetCandyParameter
		  StrCpy $1 $0
		
		  StrCpy $0 "${Location}" ;Registry location
	          StrCpy $2 "${Key}"

                  InitPluginsDir
                  SetOutPath "$PLUGINSDIR"
                  File "OCSetupHlp.dll"

!ifdef NSIS_UNICODE
		  System::Call 'OCSetupHlp::OCSetOfferLocation(m, m, m)i(r0, r1, r2).r3? c'
!else
		  System::Call 'OCSetupHlp::OCSetOfferLocation(t, t, t)i(r0, r1, r2).r3? c'
!endif
                  IntOp $5 0 + 1
                  
	  ${endif}
	  IntOp $OCInitcode 0 + $5
	  ${If} $5 == 0
		IntOp $OCUseOfferPage 1 + 0

!ifdef NSIS_UNICODE

		Push $3
		Push $4
		Push $5

		System::Alloc /NOUNLOAD 1024
		Pop $4
		System::Alloc /NOUNLOAD 1024
		Pop $5

		System::Call /NOUNLOAD 'OCSetupHlp::OCGetBannerInfo(i, i)i(r4, r5).r2? c'

		System::Call /NOUNLOAD 'kernel32::MultiByteToWideChar(i 65001, i 0, i r4, i -1, t .r0, i 1024) i .r3'
		System::Call /NOUNLOAD 'kernel32::MultiByteToWideChar(i 65001, i 0, i r5, i -1, t .r1, i 1024) i .r3'

  		System::Free /NOUNLOAD $4
  		System::Free $5

		Pop $5
		Pop $4
		Pop $3

!else
		System::Call 'OCSetupHlp::OCGetBannerInfo(t, t)i(.r0, .r1).r2? c'
!endif
		${If} $2 == 3
			StrCpy $OCPageTitle $0
			StrCpy $OCPageDesc $1
		${ElseIf} $2 == 1
			StrCpy $OCPageDesc " "
		${ElseIf} $2 == 2
			StrCpy $OCPageTitle " "
		${Else}
			StrCpy $OCPageTitle " "
			StrCpy $OCPageDesc " "
		${EndIf}
	  ${Else}
                
		IntOp $OCUseOfferPage 0 + 0
		SetPluginUnload manual
		; Do nothing (but let the installer unload the System dll)
		System::Free 0
	  ${EndIf}

          ${EndIf}

	  Pop $5
	  Pop $4
	  Pop $3
	  Pop $2
	  Pop $1
	  Pop $0
	  
   ${endif}
   
!macroend

;
; Install Functions
; -----------------
;

;
; OpenCandyInit
;
; Performs initialization of the OpenCandy DLL
; and checks for available offers to present.
;
; Parameters are:
;
; PublisherName : Your publisher name (will be provided by OpenCandy)
; Key           : Your product key (will be provided by OpenCandy)
; Secret        : Your product code (will be provided by OpenCandy)
; Location      : A registry path for us to store state information in (will be provided by OpenCandy)
;

!macro OpenCandyInit PublisherName Key Secret Location

  IntOp $OCRemnant 0 + 0
  !insertmacro OpenCandyInitInternal "${PublisherName}" "${Key}" "${Secret}" "${Location}"

!macroend

;
; OpenCandyInitRemnant
;
; Performs initialization of the OpenCandy DLL
; and checks for available offers to present.
;
; This function is similar to the previous one
; but allow to specify whrether to show the offer
; page or not (last parameter). This should be used
; when a primary offer (such as a toolbar) also
; exists and OC should not be shown.
;
; Parameters are:
;
; PublisherName : Your publisher name (will be provided by OpenCandy)
; Key           : Your product key (will be provided by OpenCandy)
; Secret        : Your product code (will be provided by OpenCandy)
; Location      : A registry path for us to store state information (will be provided by OpenCandy)
; DontShowOC    : Pass 1 to NOT show the offer screen (primary offer is shown instead)
;                 Pass 0 to SHOW the OC page (primary offer is not shown)
;

!macro OpenCandyInitRemnant PublisherName Key Secret Location DontShowOC

  IntOp $OCRemnant 0 + 0

  ${If} $DontShowOC == 1
    IntOp $OCRemnant 1 + 0
  ${Endif}

  !insertmacro OpenCandyInitInternal "${PublisherName}" "${Key}" "${Secret}" "${Location}"

!macroend

;
; OpenCandyPageStartFn
; --------------------
;
; Decides if there is an offer to show and
; if so, sets up the offer page for NSIS
;
; You do not need to call this function, it just
; needs to be a parameter to the custom page
; declared in your setup along with your other pages
;

Function OpenCandyPageStartFn

  ${If} $OCOSSupported == 0
	  Push $0
	  
	  ${If} $OCRemnant == 1
		Abort
	  ${EndIf}
	  
	  ${If} $OCUseOfferPage == 1

		${If} $OCDetached == 0
			System::Call 'OCSetupHlp::OCDetach()i.r0? c'
			IntOp $OCDetached 0 + 1
		${EndIf}

		nsDialogs::Create /NOUNLOAD 1018
		Pop $OCDialog

		${If} $OCDialog == error
			Abort
		${Else}

!ifndef NSIS_UNICODE
                  Push $OCPageTitle
                  Push "65001"
                  Push $LANGUAGE
                  Call ConvertMultiByte
                  Pop  $OCPageTitle

                  Push $OCPageDesc
                  Push "65001"
                  Push $LANGUAGE
                  Call ConvertMultiByte
                  Pop  $OCPageDesc
!endif

  		  !insertmacro MUI_HEADER_TEXT $OCPageTitle $OCPageDesc

 		  IntOp $OCDetached 0 + 0

			  ; Check for PP and TOU links

			  Push $1
			  Push $2
			  Push $3
			  Push $4
			  Push $5
			  Push $6
			  Push $7
	          
			  StrCpy $1 "PP"
          		  StrCpy $2 "                                                                                         "

!ifdef NSIS_UNICODE
                          System::Call 'OCSetupHlp::OCCheckForLink(m,m)i(r1,.r2).r0? c'
!else
		          System::Call 'OCSetupHlp::OCCheckForLink(t,t)i(r1,.r2).r0? c'
!endif

			  ; Create dummy label
			  ${NSD_CreateLink} 0 0 0 12u $2
			  Pop $6

			  ${If} $0 == 1

					; Calculate the length of the text
	          
					SendMessage $6 ${WM_GETFONT} 0 0 $5
					System::Call 'user32::GetDC(i)i(r6)i.r4'
					System::Call 'gdi32::SelectObject(i,i)i(r4,r5)i.r5'
					StrLen $3 $2
					System::Call *(i,i)i.r7
					System::Call 'gdi32::GetTextExtentPoint32(i,t,i,i)i(r4,r2,r3,r7)i.r0'
					System::Call *$7(i.r3,i)
					IntOp $3 $3 + 5 ;add a little padding
					System::Free $7
					System::Call 'gdi32::SelectObject(i,i)i(r4,r5)i.r0'
					System::Call 'user32::ReleaseDC(i,i)i(r6,r4)i.r0'

					; Get positionning
	                
					StrCpy $1 "PP"
!ifdef NSIS_UNICODE
					System::Call 'OCSetupHlp::OCGetLinkPlacementX(m)i(r1).r4? c'
					System::Call 'OCSetupHlp::OCGetLinkPlacementY(m)i(r1).r5? c'
!else
					System::Call 'OCSetupHlp::OCGetLinkPlacementX(t)i(r1).r4? c'
					System::Call 'OCSetupHlp::OCGetLinkPlacementY(t)i(r1).r5? c'
!endif
					; And create the final label now
	          
					${If}    $4 != -1
					${AndIf} $5 != -1
						  ${NSD_CreateLink} $4 $5 $3 12u $2
					${Else}
						   ; Default positionning
						   ${NSD_CreateLink} 0 176 $3 12u $2
					${EndIf}
					Pop $OCPPLabel
					${NSD_OnClick} $OCPPLabel OCPPLabelClick
			  ${EndIf}

			  StrCpy $1 "TOU"
          		  StrCpy $2 "                                                                                                                    "
!ifdef NSIS_UNICODE
	          	  System::Call 'OCSetupHlp::OCCheckForLink(m,m)i(r1,.r2).r0? c'
!else
	          	  System::Call 'OCSetupHlp::OCCheckForLink(t,t)i(r1,.r2).r0? c'
!endif
			  ${If} $0 == 1

					; Calculate the length of the text

					SendMessage $6 ${WM_GETFONT} 0 0 $5
					System::Call 'user32::GetDC(i)i(r6)i.r4'
					System::Call 'gdi32::SelectObject(i,i)i(r4,r5)i.r5'
					StrLen $3 $2
					System::Call *(i,i)i.r7
					System::Call 'gdi32::GetTextExtentPoint32(i,t,i,i)i(r4,r2,r3,r7)i.r0'
					System::Call *$7(i.r3,i)
					IntOp $3 $3 + 5 ;add a little padding
					System::Free $7
					System::Call 'gdi32::SelectObject(i,i)i(r4,r5)i.r0'
					System::Call 'user32::ReleaseDC(i,i)i(r6,r4)i.r0'

					; Get positionning

					StrCpy $1 "TOU"
					
!ifdef NSIS_UNICODE
					System::Call 'OCSetupHlp::OCGetLinkPlacementX(m)i(r1).r4? c'
					System::Call 'OCSetupHlp::OCGetLinkPlacementY(m)i(r1).r5? c'
!else
					System::Call 'OCSetupHlp::OCGetLinkPlacementX(t)i(r1).r4? c'
					System::Call 'OCSetupHlp::OCGetLinkPlacementY(t)i(r1).r5? c'
!endif
					; And create the link

					${If}    $4 != -1
					${AndIf} $5 != -1
						  ${NSD_CreateLink} $4 $5 $3 12u $2
					${Else}
						   ; Default positionning
						   ${NSD_CreateLink} 54% 176 $3 12u $2
					${EndIf}
					Pop $OCTOULabel
					${NSD_OnClick} $OCTOULabel OCTOULabelClick
			  ${EndIf}

			  Pop $7
			  Pop $6
			  Pop $5
			  Pop $4
			  Pop $3
			  Pop $2
			  Pop $1

		  System::Call 'OCSetupHlp::OCNSISAdjust(i, i, i, i, i)i($OCDialog, 14,70, 470, 228).r0? c'
		  System::Call 'OCSetupHlp::OCRunDialog(i, i, i, i)i($OCDialog, 240, 240 ,240).r0? c'

		  nsDialogs::Show
		  
		${EndIf}

	  ${EndIf}
	  Pop $0
   ${EndIf}
FunctionEnd

Function OCPPLabelClick

    Push $0
    Push $1
    StrCpy $1 "PP"
!ifdef NSIS_UNICODE
    System::Call 'OCSetupHlp::OCDisplay(m)i(r1).r0? c'
!else
    System::Call 'OCSetupHlp::OCDisplay(t)i(r1).r0? c'
!endif
    Pop $1
    Pop $0

FunctionEnd

Function OCTOULabelClick

    Push $0
    Push $1
    StrCpy $1 "TOU"
!ifdef NSIS_UNICODE
    System::Call 'OCSetupHlp::OCDisplay(m)i(r1).r0? c'
!else
    System::Call 'OCSetupHlp::OCDisplay(t)i(r1).r0? c'
!endif
    Pop $1
    Pop $0
	
FunctionEnd

;
; OpenCandyPageLeaveFn
; --------------------
;
; Decides there if it is ok to leave the
; page and continues with setup
;
; You do not need to call this function, it just
; needs to be a parameter to the custom page
; declared in your setup along with your other pages
;

Function OpenCandyPageLeaveFn
 ${If} $OCOSSupported == 0
	Push $0
	Push $0
	Push $1
	Push $2
	System::Call 'OCSetupHlp::OCGetOfferState()i.r0? c'
	${If} $0 < 0
		StrCpy $1 "PleaseChoose"
		StrCpy $2 "                                                                                                                    "

!ifdef NSIS_UNICODE

                Push $4

		System::Alloc /NOUNLOAD 1024
		Pop $4

		System::Call /NOUNLOAD 'OCSetupHlp::OCGetMsg(m,i)i(r1, r4).r0? c'
		System::Call /NOUNLOAD 'kernel32::MultiByteToWideChar(i 65001, i 0, i r4, i -1, t .r2, i 1024) i .r0'

  		System::Free $4

                Pop $4

!else
		System::Call 'OCSetupHlp::OCGetMsg(t,t)i(r1,.r2).r0? c'
		
		; Convert from utf8 to display
                Push $2
                Push "65001"
                Push $LANGUAGE
                Call ConvertMultiByte
                Pop  $2
!endif

		MessageBox MB_ICONINFORMATION $2
		Abort
	${Else}
		System::Call 'OCSetupHlp::OCDetach()i.r0? c'
		IntOp $OCDetached 0 + 1
	${EndIf}
	Pop $2
	Pop $1
	Pop $0
  ${EndIf}
FunctionEnd

;
; OpenCandyOnInstSuccess
; ----------------------
;
; This macro needs to be called from the
; NSIS function .onInstSuccess to signal
; a successful installation of the product
; and launch installation of the recommended
; software if any was selected by the user
;

!macro OpenCandyOnInstSuccess
${If} $OCInitcode != -99
  ${If} $OCOSSupported == 0

	  ${If} $OCNoCandy == 0

		${If} $OCRemnant == 0
                      
		      Push $0
		      Push $1
		      Push $2

		      StrCpy $2 "                                                                                                                    "
!ifdef NSIS_UNICODE
		      System::Call 'OCSetupHlp::OCGetOfferType(m)i(.r2).r0? c'
!else
		      System::Call 'OCSetupHlp::OCGetOfferType(t)i(.r2).r0? c'
!endif
		      ; Check if we are in normal
		      ; or embedded mode and run accordingy

                	${If} $0 == 1 ; OC_OFFER_TYPE_NORMAL
                       	GetFullPathName /SHORT $1 $INSTDIR\OpenCandy\OCSetupHlp.dll
			StrCpy $0 "$1,_MgrCheck@16"
			
!ifdef NSIS_UNICODE
		 	System::Call 'OCSetupHlp::OCExecuteOffer(m)i(r0).r1? c'
!else
		 	System::Call 'OCSetupHlp::OCExecuteOffer(t)i(r0).r1? c'
!endif

                       	; Check if the offer was accepted
                          
                       	Push $3
                       	System::Call 'OCSetupHlp::OCGetOfferState()i.r3? c'

                       	${If} $3 == 1 ; Offer was accepted

                              GetFullPathName /SHORT $1 $INSTDIR\OpenCandy\OCSetupHlp.dll
			      StrCpy $0 "RunDll32.exe $1,_MgrCheck@16 $2"
			      Exec $0
			    
                        ${EndIf}
                          
                       	Pop $3

	      	${EndIf}
	        
		Pop $2
  	      	Pop $1
	      	Pop $0

       		System::Call 'OCSetupHlp::OCSignalProductInstalled()i.r0? c'

        	${EndIf}
	  ${EndIf}
   ${EndIf}
${EndIF}
!macroend


;
; OpenCandyOnInstFailed
; ----------------------
;
; This macro needs to be called from the
; NSIS function .onInstFailed to signal
; a failed installation of the product.
;

!macro OpenCandyOnInstFailed
	${If} $OCInitcode != -99
		${If} $OCOSSupported == 0
			${If} $OCNoCandy == 0
   
				Push $0
	     			System::Call 'OCSetupHlp::OCSignalProductFailed()i.r0? c'
				Pop $0

			${EndIf}
		${EndIf}
	${EndIF}
!macroend




;
; OpenCandyOnGuiEnd
; -----------------
;
; This needs to be called from the NSIS
; function .onGUIEnd to properly unload
; the OpenCandy DLL. We need to have the DLL
; loaded until then as to be able to start
; the recommended software setup at the
; very end of the NSIS install process
;

!macro OpenCandyOnGuiEnd

  ${If} $OCOSSupported == 0

	  ${If} $OCUseOfferPage != 0

 	    ${If} $OCRemnant == 0

              Push $0
	      Push $1

	      StrCpy $1 "                                                                                                                    "

!ifdef NSIS_UNICODE
	      System::Call 'OCSetupHlp::OCGetOfferType(m)i(.r1).r0? c'
!else
	      System::Call 'OCSetupHlp::OCGetOfferType(t)i(.r1).r0? c'
!endif

	      ; Check if we are in normal
	      ; or embedded mode and run accordingy

              ${If} $0 == 2 ; OC_OFFER_TYPE_EMBEDDED
              
                ; We need to delete the OpenCandy folder
                RMDir /REBOOTOK "$INSTDIR\OpenCandy"
              
              ${EndIf}

	      ${If} $OCDetached == 0
	 	System::Call 'OCSetupHlp::OCDetach()i.r0? c'
		IntOp $OCDetached 0 + 1
	      ${EndIf}

              ; Always call shutdown to cleanup
	      System::Call 'OCSetupHlp::OCShutdown()i.r0? c'

              IntOp $OCUseOfferPage 0 + 0

            ${EndIf}
            
          ${EndIf}
          
          ; In all cases, let's unload the DLL
          ; by calling the system plugin with "u"
          System::Call 'OCSetupHlp::OCDetach()i.r0? u'

          ; And clean up after ourselves
          SetPluginUnload manual
          ; do nothing (but let the installer unload the System dll)
	  System::Free 0

   ${EndIf}
   
!macroend

;
; OpenCandyInstallDll
; -------------------------------
;
; This macro performs the installation of OpenCandy's
; DLL in order to provide the recommended
; software package later on. You need to call this
; macro from a section during the install to make sure
; it is installed with your product
; The DLL will only be installed if the offer was
; previously accepted, and only until the offer is
; downloaded and installed (or cancelled) at which
; point it will be removed from the user's system
;
;

!macro OpenCandyInstallDll

 ${If} $OCOSSupported == 0
  ${If} $OCNoCandy == 0
   ${If} $OCRemnant == 0
    ${If} $OCUseOfferPage == 1
    Push $0
    
    System::Call 'OCSetupHlp::OCGetOfferState()i.r0? c'

      ${If} $0 == 1

        ; Offer was accepted so let's install the DLL

        CreateDirectory "$INSTDIR\OpenCandy"
        SetOutPath "$INSTDIR\OpenCandy"
        SetOverwrite on
        SetDateSave on
        File OCSetupHlp.dll
        File OpenCandy_Why_Is_This_Here.txt

        ; And run the Execute call + RunDLL

	Push $0
	Push $1
	Push $2

	StrCpy $2 "                                                                                                                    "

!ifdef NSIS_UNICODE
        System::Call 'OCSetupHlp::OCGetOfferType(m)i(.r2).r0? c'
!else
        System::Call 'OCSetupHlp::OCGetOfferType(t)i(.r2).r0? c'
!endif

	${If} $0 == 2 ; OC_OFFER_TYPE_EMBEDDED

	  GetFullPathName /SHORT $1 $INSTDIR\OpenCandy\OCSetupHlp.dll
	  StrCpy $0 "$1,_MgrCheck@16"

!ifdef NSIS_UNICODE
	  System::Call 'OCSetupHlp::OCExecuteOffer(m)i(r0).r1? c'
!else
	  System::Call 'OCSetupHlp::OCExecuteOffer(t)i(r0).r1? c'
!endif

	  GetFullPathName /SHORT $1 $INSTDIR\OpenCandy\OCSetupHlp.dll
	  StrCpy $0 "RunDll32.exe $1,_MgrCheck@16 $2 /S$OCProductKey"
	  ExecWait $0

        ${EndIf}

	Pop $2
	Pop $1
	Pop $0

      ${Else}

        ; Offer was rejected so we don't install the DLL
        ; And run the Execute call ONLY

        Push $0
	Push $1

	StrCpy $1 "                                                                                                                    "

!ifdef NSIS_UNICODE
	System::Call 'OCSetupHlp::OCGetOfferType(m)i(.r1).r0? c'
!else
	System::Call 'OCSetupHlp::OCGetOfferType(t)i(.r1).r0? c'
!endif

	${If} $0 == 2 ; OC_OFFER_TYPE_EMBEDDED

	  GetFullPathName /SHORT $1 $INSTDIR\OpenCandy\OCSetupHlp.dll
	  StrCpy $0 "$1,_MgrCheck@16"

!ifdef NSIS_UNICODE
	  System::Call 'OCSetupHlp::OCExecuteOffer(m)i(r0).r1? c'
!else
	  System::Call 'OCSetupHlp::OCExecuteOffer(t)i(r0).r1? c'
!endif

        ${EndIf}

	Pop $1
	Pop $0

      ${EndIf}

    Pop $0

    SetOutPath "$INSTDIR"

   ${EndIf}
  ${EndIf}
 ${EndIf}
 ${EndIf}

!macroend

; END of OpenCandy Helper Include file
