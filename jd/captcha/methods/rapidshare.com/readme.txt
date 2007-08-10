Jede Methode kann hat folgende Files:

jacinfo.xml:

Dort stehen Infos drin wie JAC arbeiten soll. z.B. 
Sourcefile, Resultfile etc. Wird JAC integriert sollte hier nur Methodenname, Authorname und vor allem!! Buchstabenanzahl stehen


letters.mth

Dort sind die antrainierten Buchstaben als Xml gespeichert. Später könnte man diese DATe um Platz zusparen zippen

script.jas

Die JAC-Script file. hier kann ein einfaches Script angegeben werden.
Vorbearbeitung der captchas und Parameter werden hier gesetzt. Eine genaue Doku der parameter und funktionen wird noch kommen


Optional:
captchas-dir:

hier befinden sich captchas zum trainieren. Für die Veröffentlichung stecken alle Infos darüber in der letters.mth. Der ordner wird also nicht mehr gebraucht
