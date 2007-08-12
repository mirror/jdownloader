
###############################---Parameter Dokumentation---#################################
#######Kommentare mit #........
#-------------------------------------------------------------------------------------------#
################ => lettersearchlimitvalue [double] (0-0.30)
#gibt einen Faktor an ab welchem ein Buchstabe als Perfekt erkannt gilt
# Beispiel: || param.lettersearchlimitvalue = 0.15;
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => objectColorContrast [double] (0.2-0.7)
#Kontrastwert um zu erkennen ob ein neuer Pixel zu eiem Objekt passt
# Beispiel: || param.objectColorContrast = 0.3;
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => objectDetectionContrast [double] (0.2-0.7)
#Kontrastwert zur erkennung eines ObjektPixels (Kontrast  Objekt/hintergrund)
# Beispiel: || param.objectDetectionContrast = 0.5;
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => useObjectDetection [boolean] (true/false)
#Object Erkennung aktivieren
# Beispiel: || param.useObjectDetection = true;
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => minimumObjectArea [int] (50-500)
#Minimale Objektfläche für die Objekterkennung
# Beispiel: || param.minimumObjectArea = 200;
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => trainonlyunknown [boolean] (true/false)
#Gibt an ob beim training bekante buchstabben erneut trainiert werden sollen
# Beispiel: || param.trainonlyunknown = true;
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => scanvariance [Integer](0-10)
#Parameter: Scan-Parameter. Gibt an um wieviele Pixel Letter und
#Vergleichsletter gegeneinander verschoben werden um die beste
#Übereinstimung zu finden. Hohe werte verlangemmen die Erkennung deutlich
# Beispiel: || param.scanvariance = 2;
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => bordervariance [Integer](0-10)
#Parameter: Scan-Parameter. Gibt an um wieviele Pixel sich Letter und
#Vergleichsletter unterscheiden dürfen um verglichen zu werden. Hohe Werte
#machen das ganze Langsam
# Beispiel: || param.bordervariance = 2;
#-------------------------------------------------------------------------------------------#				
#-------------------------------------------------------------------------------------------#
################ => leftpadding [Integer] (ab 0)
#Linkes Padding
# Beispiel: || param.leftpadding = 5;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => simplifyfaktor [Integer] (ab 0)
# Parameter: Wert gibt an um welchen faktor die Fingerprints verkleinert
# werden. So groß wie möglich, so klein wie nötig Wenn dieser Wert
# verändert wird, wrd die MTH File unbrauchbar und muss neu trainiert
# werden
# Beispiel: || param.simplifyfaktor= 3;
#-------------------------------------------------------------------------------------------#				
#-------------------------------------------------------------------------------------------#
################ => letternum [Integer] (ab 1)
#Anzahl der Buchstaben in den captchas (sollte in der jacinfo.xml stehen
# Beispiel: || param.letternum = 4;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => sourceimage [String]
#!!STANDALONE ONLY!!
#pfad zum SourceImage. sollte in der jacinfo.xml stehen
# Beispiel: || param.sourceimage = rscaptcha.jpg;
#-------------------------------------------------------------------------------------------#					
#-------------------------------------------------------------------------------------------#
################ => resultfile [String]
#!!STANDALONE ONLY!!
#pfad zur Zieldatei (rapid.txt)
# Beispiel: || param.resultfile= result.txt;
#-------------------------------------------------------------------------------------------#						
#-------------------------------------------------------------------------------------------#
################ => gapwidthpeak [Integer](1-5)
# Parameter: Gibt die Anzahl der Reihen(Pixel) an die zur peak detection
# verwendet werden sollen
# Beispiel: || param.gapwidthpeak = 1;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => gapwidthaverage [Integer] (1-6)
# Parameter: Gibt die Anzahl der reihen an die zur Average Detection
# verwendet werden sollen
# Beispiel: || param.gapwidthaverage = 2;
#-------------------------------------------------------------------------------------------#							
#-------------------------------------------------------------------------------------------#
################ => hsbtype [Integer] (0/1/2)
#Verwendetes farbkriterium. Dabei können auch mehrere verwendet werden.Sie werden der wichtigkeit nach aneinandergereiht
# Beispiel: || 
#param.hsbtype = b; : helligkeit
#param.hsbtype = bhs; : kriterien (in wichtigkeit absteigend: helligkeit, farbton, sättigung
#-------------------------------------------------------------------------------------------#				
#-------------------------------------------------------------------------------------------#
################ => gapandaveragelogic [boolean] (true/false)
# Parameter: gapAndAverageLogic=true: Es werden Lücken verwendet bei denen
# Peak und Average detection zusammenfallen (AND) gapAndAverageLogic=false:
# Es werden sowohl peak als Auch Average Lücken verwendet (nur in
# Ausnahmefällen) (OR)
# Beispiel: || param.gapandaveragelogic = true;
#-------------------------------------------------------------------------------------------#				
#-------------------------------------------------------------------------------------------#
################ => gapdetectionaveragecontrast [double](0.6 - 1.4)
#Kontrast Parameter für die gapaverageErkennung
# Beispiel: || param.gapdetectionaveragecontrast=0.85;
#-------------------------------------------------------------------------------------------#			
#-------------------------------------------------------------------------------------------#
################ => useaveragegapdetection [boolean] (true/false)
#gap Average detection ( Helle linien erkennung) 
# Beispiel: || param.useaveragegapdetection=false;
#-------------------------------------------------------------------------------------------#				
#-------------------------------------------------------------------------------------------#
################ => usepeakgapdetection [boolean] (true/false)
#Gap peak Detection (Flanken Erkennung) aktivieren
# Beispiel: || param.usepeakgapdetection = true;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => minimumletterwidth [int] (3-20)
#Minimale Buchstabenbreite
# Beispiel: || param.minimumletterwidth=10;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => colorvaluefaktor [int] (ab 1) 16777215 für RGB(jpg source)
# Parameter: Wert gibt meistens den höchsten möglichen farbwert an. Durch
# diesen Wert wird geteilt um die Dateigröße der MTH kleiner zu halten
# Beispiel: || param.colorvaluefaktor=16777215;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => relativecontrast [double] (0.8 - 1.2)
#Parameter: Allgemeiner Bildkontrastparameter ~0.8 bis 1.2
# Beispiel: || param.relativecontrast=0.90;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => backgroundsamplecleancontrast [double] (0.05 - 0.5)
#Parameter: Gibt die Tolleranz beim Säubern des Hintergrunds an ~0.05-0.5
# Beispiel: || param.backgroundsamplecleancontrast=0.15;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => blackpercent [double] (0.05 - 0.30)
#Parameter: Gibt für dieverse SW Umwandlungen den Schwellwert an
# Beispiel: || param.blackpercent=0.15;
#-------------------------------------------------------------------------------------------#	
#-------------------------------------------------------------------------------------------#
################ => gaps [int[]]
#Werte-Array Wird gaps != null, so werden die Werte als Trennpositionen
#für die letter detection verwendet. Alle anderen Erkennungen werden dann
#ignoriert
# Beispiel: || param.gaps={25,60,85};
#-------------------------------------------------------------------------------------------#
############################---Captcha prepare Dokumentation---##############################
#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.cleanBackgroundBySample(int xPosition, int yPosition, int ausschnittBreite, int ausschnittHöhe);
# Nimmt an der angegebenen Positiond en farbwert auf und entfernt desen aus
# dem ganzen Bild
# 
# @param px
# @param py
# @param width
# @param height
# Beispiel: || captcha.prepare.cleanBackgroundBySample(3, 3, 5, 5);
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.cleanWithMask(String maskenPfad, int ersatzBreite, int ersatzHöhe);
# Entfernt Störungen über eine Maske und ersetzt diese mit den umliegenden
# pixeln
# 
# @param mask
# Maske
# @param width
# breite des Ersatzfeldes
# @param height
# Höhe des Ersatzfeldes
# Beispiel: || captcha.prepare.cleanWithMask(rsmask.jpg, 5, 5);
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.clean();
# Entfernt von allen 4 Seiten die Zeilen und Reihen bis nur noch der
# content übrig ist
# 
# @return true/False
# Beispiel: || captcha.prepare.clean();
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.toBlackAndWhite();
# Erzeugt ein REINEs Sw Bild mit dme bilddurchschnitt als grenze
# erzeugen
# Beispiel: || captcha.prepare.toBlackAndWhite();
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.toBlackAndWhite(double Kontrastfaktor);
# Erzeugt ein schwarzweiß bild
# 
# @param faktor
# Schwellwert für die Kontrasterkennung
# Beispiel: || captcha.prepare.toBlackAndWhite(0.25);
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.reduceWhiteNoise(int Effektradius);
# Entfernt weißes Rauschen
# 
# @param faktor
# Stärke des Effekts
# Beispiel: || captcha.prepare.reduceWhiteNoise(5);
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.reduceWhiteNoise(int Effektradius, double Kontrastfaktor);
# Entfernt weißes Rauschen
# 
# @param faktor
# Prüfradius
# @param contrast
# Kontrasteinstellungen.je kleiner, desto mehr Pixel werden als
# störung erkannt, Je kleiner, desto höher wird der resultierende kontrast
# Beispiel: || captcha.prepare.reduceWhiteNoise(5, 0.6);
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.reduceBlackNoise(int Effektradius);
# Entfernt Schwarze Störungen
# 
# @param faktor
# Stärke
# Beispiel: || captcha.prepare.reduceBlackNoise(4)
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.reduceBlackNoise(int Effektradius, double Kontrastfaktor);
# Entfernt schwarze Störungen
# 
# @param faktor
# prüfradius
# @param contrast
# Kontrasteinstellungen
# Beispiel: || captcha.prepare.reduceBlackNoise(4, 0.85);
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.invert();
#Erstellt das negativ
# 
# Beispiel: || captcha.prepare.invert();
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.blurIt(int Effektradius);
# Lässt das Bild verschwimmen
# 
# @param faktor
# Stärke des Effekts
# Beispiel: || captcha.prepare.blurIt(5);
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.sampleDown(int Effektradius);
# Macht das Bild gröber und sw.
# 
# @param faktor
# Grobheit.
# Beispiel: || captcha.prepare.sampleDown(3);
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.saveImageasJpg(String zielPfad);
# Speichert das Bild asl JPG ab
# 
# @param file
# Zielpfad
# Beispiel: || captcha.prepare.saveImageasJpg("preparedImage.jpg");
#-------------------------------------------------------------------------------------------#
#-------------------------------------------------------------------------------------------#
################ => captcha.prepare.convertPixel(String frabstring);
# Legt den verwendeten farbbereich fest. Es Steht RGB un hsb zur verfügung
# R:Rot|G:Grün|B:Blau   oder h:Frabton/s:Sättigung/b:helligkeit
# Es kann nur jeweils ein farbraum verwendet werden. Im farbraum können aber alle 3 kriterien beliebig aneinandergereiht werden (1-3 kriterien) die wichtigsten zuerst
# 
# Beispiel: || captcha.prepare.convertPixel("hs");
#-------------------------------------------------------------------------------------------#
#
##################################---ENDE Dokumentation---###################################