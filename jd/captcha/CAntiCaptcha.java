import java.awt.Component;
import java.awt.Image;
import java.io.File;
import java.io.FileFilter;
import java.io.StringWriter;
import java.util.Properties;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.lagcity.TLH.DEBUG;
import de.lagcity.TLH.GLOBALS;
import de.lagcity.TLH.Locale;
import de.lagcity.TLH.PROPERTY;
import de.lagcity.TLH.STRING;
import de.lagcity.TLH.SWING;
import de.lagcity.TLH.SYSTEM;
import de.lagcity.TLH.XML;

public class CAntiCaptcha {

	private static  double USELETTERSEARCHFAKTOR = 0.15;

	private static boolean DOPREPARE = true;

	public static void main(String[] args) {

		CAntiCaptcha owner = new CAntiCaptcha(new JFrame());

	}

	private long correct;

	private JFrame owner;

	public String METHODAUTHOR;

	public String METHODNAME;

	public String IMAGETYPE;

	public int LETTERNUM;

	public String SOURCEIMAGE;

	public static double COMPAREPERFECTMATCHPERCENT = 0.1;

	public String RESULTTXT;

	private String METHODSETTINGS;

	private Document mth;

	private Vector<Letter> letterDB;

	private String METHODLETTERS;

	public CAntiCaptcha(JFrame owner) {
		this.owner = owner;
		getJACInfo();
		setParameter();
		loadMSTFile(METHODSETTINGS);
		loadMTHFile(METHODLETTERS);
		// saveMSTFile();
		trainAllCaptchas();
		
		 BasicWindow w;
				
		 Captcha c;
			 while(true){
		 c=Captcha.getCaptcha(SYSTEM.fileChooser(GLOBALS.ROOTDIR+"captchas"));
		 w = BasicWindow.showImage(c.getImage(), "captcha");
		 prepare(c);
		DEBUG.trace(checkCaptcha(c));
			 }
//		 prepare(c);
//		 w = BasicWindow.showImage(c.getImage(), "Img vor");
//		 prepare(c);
//		 Letter[] letters = c.getLetters(5);
//		
//		 w = BasicWindow.showImage(letters[0].getImage(), "Letter 0 "
//		 + getLetter2(letters[0])+"-"+getLetter(letters[0]));
//		 w.setSize(300, 80);
//		 w.setLocation(50, 100);
//		 w = BasicWindow.showImage(letters[1].getImage(), "Letter 1 "
//		 + getLetter2(letters[1])+"-"+getLetter(letters[1]));
//		 w.setSize(300, 80);
//		 w.setLocation(50, 200);
//		 w = BasicWindow.showImage(letters[2].getImage(), "Letter 2 "
//		 + getLetter2(letters[2])+"-"+getLetter(letters[2]));
//		 w.setSize(300, 80);
//		 w.setLocation(50, 300);
//		 w = BasicWindow.showImage(letters[3].getImage(), "Letter 3 "
//		 + getLetter2(letters[3])+"-"+getLetter(letters[3]));
//		 w.setSize(300, 80);
//		 w.setLocation(50, 400);
//		 w = BasicWindow.showImage(letters[4].getImage(), "Letter 4 "
//		 + getLetter2(letters[4])+"-"+getLetter(letters[4]));
//		 w.setSize(300, 80);
//		 w.setLocation(50, 500);
			 }
		// getCaptcha()
		// DEBUG.trace(checkCaptcha(new File(SOURCEIMAGE)));
		// String code= checkCaptcha(new File(SOURCEIMAGE));
		// SYSTEM.writeLocalFile(new File(RESULTTXT), code);

	//}

	/*
	 * Diese Funktion scannt letter a mit letter b ab. Als anhaltspunkt dient
	 * dabei der Mittelpunkt von letterb.
	 */
	public static int getDifference(Letter a, Letter b) {

		int bhw = b.getWidth() / 2;
		int bhh = b.getHeight() / 2;
		int minValue = Integer.MAX_VALUE;
		int pointAX;
		int pointAY;
		int pointBX;
		int pointBY;
		int awidth;
		int aheight;
		int peaks=0;
		for (int x = 0; x < a.getWidth(); x++) {
			for (int y = 0; y < a.getHeight(); y++) {
				if(a.getPixelValue(x,y)<a.getMaxPixelValue()*COMPAREPERFECTMATCHPERCENT){
					peaks++;
				}
			}
			}
		// DEBUG.trace(b.sourcehash+" - "+b.decodedValue+" Comp a:"+a.getWidth()+"/"+a.getHeight()+" b: "+b.getWidth()+"/"+b.getHeight());
		int maxSize=Math.min(a.getWidth(),b.getWidth())*Math.min(a.getHeight(),b.getHeight());
		for (int x = 0; x < a.getWidth(); x++) {
			for (int y = 0; y < a.getHeight(); y++) {
				awidth = Math.min(x, bhw)
						+ Math.min(a.getWidth() - x, b.getWidth() - bhw);
				aheight = Math.min(y, bhh)
						+ Math.min(a.getHeight() - y, b.getHeight() - bhh);
				// Die Nullpunkte der Schnittmenge jeweils ins Koordinatensystem
				// von a und b transformiert.
				pointAX = x - Math.min(x, bhw);
				pointAY = y - Math.min(y, bhh);
				pointBX = Math.max(bhw - x, 0);
				pointBY = Math.max(bhh - y, 0);
				// if(b.decodedValue.equals("2")&&
			// b.sourcehash.equals("ba74905c189f6c23f3c7ffcab555cf6d")){
				// if(bhh!=13 ||bhw!=8){
				// DEBUG.trace(b.sourcehash);
				// System.exit(0);
				// }
				// }
				/*
			if(b.getWidth()==5&&b.decodedValue.equals("b")&&
				 b.sourcehash.equals("fbc069be57987318d8db5519d186d917"))DEBUG.trace(x+":"+bhw+" - "+a.getWidth()+":"+b.getWidth());
				 if(b.getWidth()==5&&b.decodedValue.equals("b")&&
				 b.sourcehash.equals("fbc069be57987318d8db5519d186d917"))DEBUG.trace(x+"/"+y+" AUSSCHniTT: "+pointAX+":"+pointAY+" - "+pointBX+":"+pointBY+"	 - "+awidth+":"+aheight);
				
				*/// Die Aktuelle Schnittmenge wird verglichen
				// Der counter mag sinnvoll sein wenn amn spezielle Pixel
				// filternw ill. ansonsten tut es später auch width*height
				long value = 0;
				int count = 0;
				int hit=0;
				for (int ax = 0; ax < awidth; ax++) {
					for (int ay = 0; ay < aheight; ay++) {
						// Dies sind die ZU vergleichenden Pixelwerte
						int va = a.getPixelValue(pointAX + ax, pointAY + ay);
						int vb = b.getPixelValue(pointBX + ax, pointBY + ay);
//						value += Math.abs(va - vb);
						
						if(Math.abs(va - vb)<COMPAREPERFECTMATCHPERCENT*a.getMaxPixelValue() && (va+vb)/2<a.getMaxPixelValue()*COMPAREPERFECTMATCHPERCENT){
							hit++;
						}
					//if(b.getWidth()==5&&b.decodedValue.equals("b")&&  b.sourcehash.equals("fbc069be57987318d8db5519d186d917"))DEBUG.trace("A: "+(pointAX+ax)+"/"+(pointAY+ay)+ " B:"+(pointBX+ax)+"/"+(pointBY+ay) +" __ "+va+" - "+vb);
						// }

						count++;

					}
				}
			//DEBUG.trace(hit+" - "+(awidth*aheight));
				value=peaks-hit;
				if(pointAX==pointBX &&pointAY==pointBY)value*=0.5;
				//value*=(peaks+1)/(hit+1);
				//value /= count;
				//value*=(double)((double)maxSize/(double)(awidth*aheight));
				/*if(b.getWidth()==5&&b.decodedValue.equals("b")&&
				 b.sourcehash.equals("fbc069be57987318d8db5519d186d917"))DEBUG.trace("value	"+value+" -	"+(a.getMaxPixelValue()*COMPAREPERFECTMATCHPERCENT));
				*/
				minValue = (int) Math.min(minValue, value);
				/*if(b.getWidth()==5&&b.decodedValue.equals("b")&&
				 b.sourcehash.equals("fbc069be57987318d8db5519d186d917"))DEBUG.trace(b.decodedValue+": "+minValue);*/
				if (minValue ==0 || minValue<5) {
					// Dieser Wert ist sooo gut. das kann nur ein Treffer sein!
					// ALso wird null zurück gegeben. D.H. Perfecter Treffer fbc069be57987318d8db5519d186d917
				 return minValue;
				}
			}
		}
		// DEBUG.trace("FERTISCH "+minValue);
		return minValue;

	}

	private String searchLetter(Letter letter) {

		long bestValue = Long.MAX_VALUE;
		String bestResult = "_";
		Letter tmp = new Letter();
		try {

			DEBUG.trace(letterDB.size());
			if (letterDB == null)
				return "_";

			for (int i = 0; i < letterDB.size(); i++) {
				// Get child node
				tmp = letterDB.elementAt(i);
				if(Math.abs(tmp.getWidth()-letter.getWidth())>6 ||Math.abs(tmp.getHeight()-letter.getHeight())>6)continue;
				long value = getDifference(letter, tmp);

			
				if (value < bestValue) {
					bestValue = value;
					bestResult = tmp.decodedValue;
					 DEBUG.trace("new Best "+bestResult + " : " + bestValue+" : "+tmp.sourcehash+" dim:"+tmp.getDim()+" - "+letter.getDim());
						if (value <5) {

							 return tmp.decodedValue;
						}
				}

			}
			this.correct += bestValue;
			// DEBUG.trace("New Method got "+bestResult+" Width a value of
			// "+bestValue);
		} catch (Exception e) {
			DEBUG.error(e);
		}

		return bestResult;

	}

	private void setParameter() {
		/** ************************************Buchstabenerkennung************************************** */
		/*
		 * Captcha.GAPANDAVERAGELOGIC Gibt an wie die Beiden Lückenerkennung
		 * zusammenarbeiten true: AND Verknüpfung. Es werden nur Lücken gezählt
		 * bei denen Peak und Average erkennung übereinstimmen false:OR
		 * Verknüpfung. Es werden sowohl Peak als auch Average Lücken verwendet.
		 */

		Captcha.GAPANDAVERAGELOGIC = true;
		/*
		 * Captcha.USEAVERAGEGAPDETECTION Es werden Reihen die im Vergleich zum
		 * Durchschnisswert hell sind gesucht. Parameter:
		 * Captcha.GAPWIDTHAVERAGE Captcha.GAPDETECTIONAVERAGECONTRAST
		 */
		Captcha.USEAVERAGEGAPDETECTION = false;
		/*
		 * Captcha.GAPWIDTHAVERAGE Gibt an wieviele Reihen zur berechnung
		 * verwendet werden sollen. Dieser Wert sollte auf keinen fall größer
		 * als die Minimal Lückenbreite sein
		 */
		Captcha.GAPWIDTHAVERAGE = 1;
		// je kleiner desto leichter wird eine lücke als wirkliche lücke erkannt
		// Für reine schwarze schrift aufw eißem hintergrund ist 1.2 ein guter
		// wert
		// 0.7 war für svz ganz gut
		/*
		 * Captcha.GAPDETECTIONAVERAGECONTRAST Kontrastparameter wirkt auf die
		 * Durchschnittshelligkeit des ganzen Bildes. und dieser wird dann mit
		 * der reihenhelligkeit verglichen. 1: Lücke falls die Reihenhelligkeit
		 * heller als der Bildgesammtdurchschnitt ist. <1:Lücke dürfen dunkler
		 * sein um erkannt zu werden. (für bilder bei denen der lücken/Bild
		 * Kontrast gering ist) >1: Lücken sind deutlich heller als der rest des
		 * Bildes.
		 * 
		 */
		Captcha.GAPDETECTIONAVERAGECONTRAST = 1.2;
		/*
		 * Captcha.USEPEAKGAPDETECTION Es wird die Fallende Flanke von Dunkel
		 * nach hellgesucht. Dabei wird der Dunkelste Wert einer reihe
		 * verwendet. Eignet sich gut für reine SW Bilder. Parameter:
		 * Captcha.GAPWIDTHPEAK Captcha.GAPDETECTIONPEAKECONTRAST
		 */
		Captcha.USEPEAKGAPDETECTION = true;
		/*
		 * Captcha.GAPWIDTHPEAK Gibt an über wieviele Zeilen nach einem
		 * Dunkelheitspeak gesucht wird. Ist dieser Wert >1 so verschiebt das
		 * die Lückenerkennung nach Links. Allerdings werden dafür Ein-pixel
		 * Lücken nicht als Lücken erkannt
		 */
		Captcha.GAPWIDTHPEAK = 1;

		/*
		 * Captcha.GAPDETECTIONPEAKECONTRAST Kontrasparameter. wirkt aufd en
		 * Bilddurchschnitt. Zeilen-Dunkel-Peak muss dunkler sein als Parameter*
		 * Average Je kleiner der Wert, desto größer muss der Kontrast
		 * zeichen/Hintergrund sein
		 */
		Captcha.GAPDETECTIONPEAKECONTRAST = 0.25;
		/*
		 * Captcha.MINIMUMLETTERWIDTH Gibt die Minimale Zeichenbreite an. Sie
		 * sollte möglichst genau angegeben werden, weil das Fehlerkennungen
		 * verhindert
		 */
		Captcha.MINIMUMLETTERWIDTH = 8;
		/*
		 * Gibt einen Abstand nach links an. Zeichen werden erst ab
		 * Captcha.LEFTPADDING Pixel gesucht
		 * 
		 */
		Captcha.LEFTPADDING = 0;

		/** *****************************************Bildvereinfachungen/Fingerprintparameter****************************************** */
		/*
		 * PixelGrid.SIMPLIFYFAKTOR Zur Fingerprinterstellung wird das Bild um
		 * diesen faktor verkleinert. Nur ganzzahlige werte.
		 */
		PixelGrid.SIMPLIFYFAKTOR = 1;
		/*
		 * PixelGrid.COLORVALUEFAKTOR Farbraumparameter. Je nachdem welcher
		 * farbraum verwendet wird, und je nach anzahl der Farben kann dieser
		 * faktor angepasst werden um weniger Ram zu verbrauchen. MTH File wird
		 * kleiner. Je größer der Wert, desto mehr Farbinformationen können
		 * verloren gehen Reines sw bild: Wert= getMaxPixelValue. => 2 Farben
		 * weiß/schwarz
		 * ACHTUNG!
		 * Problem 1: Setzt man diesen Parameter >1, so wird für die nachfolgenden Pixelwerte der durschnitt aus wert*wert quatraten genommen. Diese sind besonders bei sw bildern deutlich heller als das ausgangsbild
		 *            Der Kontrast sinkt also und man muss andere Werte, z.B. RELATIVECONTRAST anpassen. RELATIVCONTRAST ist aber für sehr vieles zuständig und bereits kleine Änderungen können eine große auswirkung haben (plötzlich weißes oder schwarzes bild.
		 *            Ich empfehle daher eher mit Sampledown zu arbeiten
		 * 
		 */
		PixelGrid.COLORVALUEFAKTOR = 0xffffff;
		// je kleiner, desto mehr wird ausgefiltert. wenn dieser wert kleiner
		// wird
		// sollte GAPDETECTIONAVERAGECONTRAST größer werden
		/*
		 * PixelGrid.RELATIVCONTRAST Filterparameter. Wirk auch auf
		 * GAPDETECTIONAVERAGECONTRAST!! Verkleinert man diesen Wert muss
		 * GAPDETECTIONAVERAGECONTRAST vergrößert werden um die Lückenerkennung
		 * zu erhalten Dieser Wert wirkt auf den kompletten
		 * Bildhelligkeitsdurchschnitt und wird dann zur Kontrasttrennung mit
		 * den Pixelwerten verglichen Je kleiner, desto mehr pixel werden
		 * ausgefiltert. Je größer, desto emhr PIxel gehen in das Bild mit ein.
		 * 
		 */
		PixelGrid.RELATIVCONTRAST = 0.85;
		/*
		 * PixelGrid.BACKGROUNDSAMPLECLEANPERCENT MIt der backgroundSampleClean
		 * Funktion kann eine Position am Bild angegeben werden die den zu
		 * filternden farbwert enthält. Der Wert Gibt die Tolleranz an die beim
		 * Filtern verwendet werden soll. Je größer, desto mehr wird gefiltert
		 * 0.1 ist ein guter wert für einfarbige Hintergründe um möglichst viele
		 * Fragmente zu ignorieren
		 * 
		 */
		PixelGrid.BACKGROUNDSAMPLECLEANPERCENT = 0.1;
		/*
		 * PixelGrid.BLACKPERCENT Die Maskenclean Funktion verwendet eine feste
		 * Maske, (z.B. ein muster) alle Pixel dieser Maske werden durch einen
		 * durchschnisdwert der Umgebung aufgefüllt. Hintergrundmuster lassen
		 * sich dadurch entfernen. Die Maske wird als JPG abgelegt.der Wert
		 * dient dazu innerhalb der Maske zwischen Schwarz und weiß zu
		 * unterscheiden. 0.1 ist für ein Hochqualitatives jpg und ein darauf
		 * festgelegtes Schwarzes Muster gut.
		 */
		PixelGrid.BLACKPERCENT = 0.1;

		/*
		 * In der prepare Funktion sind alle anzuwendenen Filter angegeben. der
		 * Parameter DOPREPARE gibt an ob die Funktion verwendet wird DOPREPARE
		 * =true. Vorverarbeitung aktivieren =false Keine Vorverarbeitung
		 * 
		 * 
		 */
		CAntiCaptcha.DOPREPARE = true;
		// MST File mit den oben angegebenen parametern speichern

	}

	private void trainAllCaptchas() {

		int successFull = 0;
		int total = 0;
		File[] images = getImages();
		int newLetters;
		for (int i = 0; i < images.length; i++) {

			newLetters = trainCaptcha(images[i], LETTERNUM);
			DEBUG.trace("Erkannt: " + newLetters + "/" + LETTERNUM);
			if (newLetters > 0) {
				successFull += newLetters;
				total += LETTERNUM;
				DEBUG.trace("Erkennungsrate: " + ((100 * successFull / total)));
			}
			System.gc();
		}
	}

	private void loadMTHFile(String path) {
		if (new File(path).exists() == false) {
			SYSTEM.writeLocalFile(new File(path), "<jDownloader/>");
			DEBUG.trace("ERROR: MTH FILE NOT AVAILABLE. Created: " + path);
		}
		mth = XML.parseXmlFile(path, false);
		createLetterDBFormMTH();

	}

	private void createLetterDBFormMTH() {
		letterDB = new Vector<Letter>();
		try {

			if (mth == null || mth.getFirstChild() == null)
				return;
			NodeList nl = mth.getFirstChild().getChildNodes();
			Letter tmp;
			for (int i = 0; i < nl.getLength(); i++) {
				// Get child node
				Node childNode = nl.item(i);
				if (childNode.getNodeName().equals("letter")) {
					NamedNodeMap att = childNode.getAttributes();
					// DEBUG.trace(childNode.getNodeName());
					// DEBUG.trace(childNode.getTextContent());
					// DEBUG.trace(att.getNamedItem("captchaHash").getNodeValue());
					// DEBUG.trace(att.getNamedItem("value").getNodeValue());
					tmp = new Letter();
					if (!tmp.setTextGrid(childNode.getTextContent()))
						continue;
					;

					tmp.setSourceHash(att.getNamedItem("captchaHash")
							.getNodeValue());
					tmp.setDecoded(att.getNamedItem("value").getNodeValue());
					letterDB.add(tmp);
				}
			}
		} catch (Exception e) {
			DEBUG
					.trace("ERROR: Fehler mein lesen der MTHO Datei!!. Methode kann nicht funktionieren!");
			DEBUG.error(e);
		}
	}

	private void loadMSTFile(String file) {
		Properties mst = PROPERTY.loadPropertyFile(file);
		Captcha.setProperties(mst);
		Letter.setProperties(mst);
		setProperties(mst);
	}

	/*
	 * Die Methode erstellet eine Propertydatei mitd en aktuellen Parametern.
	 */
	private void saveMSTFile() {
		File path = SYSTEM.fileChooser(GLOBALS.ROOTDIR);
		Properties file = PROPERTY.loadPropertyFile(path.getAbsolutePath());
		Captcha.setCurrentProperties(file);
		Letter.setCurrentProperties(file);
		setCurrentProperties(file);
		PROPERTY.savePropertyFile(path.getAbsolutePath(), file);
		SYSTEM.showMessage(Locale.lc.get("MSTFILESAVED") + ": "
				+ path.getAbsolutePath());
	}

	public static void setCurrentProperties(Properties file) {
		file.setProperty("DOPREPARE", DOPREPARE + "");

	}

	public static void setProperties(Properties file) {
		DOPREPARE = file.getProperty("DOPREPARE", DOPREPARE + "")
				.equals("true");

	}

	/*
	 * Die Methode parst die jacinfo.xml
	 */
	private void getJACInfo() {

		Document doc = XML.parseXmlFile("jacinfo.xml", false);

		NodeList nl = doc.getFirstChild().getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			// Get child node
			Node childNode = nl.item(i);

			NamedNodeMap att = childNode.getAttributes();

			if (childNode.getNodeName().equals("method")) {
				if (att.getNamedItem("author") == null) {
					DEBUG
							.trace("ERROR: jacinfo.xml: Attribute Missing: Method/author");
					return;
				}
				if (att.getNamedItem("name") == null) {
					DEBUG
							.trace("ERROR: jacinfo.xml: Attribute Missing: Method/name");
					return;
				}
				if (att.getNamedItem("methodsettingspath") == null) {
					DEBUG
							.trace("ERROR: jacinfo.xml: Attribute Missing: Method/methodsettingspath");
					return;
				}
				if (att.getNamedItem("methodpath") == null) {
					DEBUG
							.trace("ERROR: jacinfo.xml: Attribute Missing: Method/methodpath");
					return;
				}
				METHODAUTHOR = att.getNamedItem("author").getNodeValue();
				METHODNAME = att.getNamedItem("name").getNodeValue();
				METHODSETTINGS = att.getNamedItem("methodsettingspath")
						.getNodeValue();
				METHODLETTERS = att.getNamedItem("methodpath").getNodeValue();

			}
			if (childNode.getNodeName().equals("format")) {
				if (att.getNamedItem("type") == null) {
					DEBUG
							.trace("ERROR: jacinfo.xml: Attribute Missing: format/type");
					return;
				}
				if (att.getNamedItem("letterNum") == null) {
					DEBUG
							.trace("ERROR: jacinfo.xml: Attribute Missing: format/letterNum");
					return;
				}
				LETTERNUM = Integer.parseInt(att.getNamedItem("letterNum")
						.getNodeValue());

			}
			if (childNode.getNodeName().equals("source")) {
				if (att.getNamedItem("file") == null) {
					DEBUG
							.trace("ERROR: jacinfo.xml: Attribute Missing: source/file");
					return;
				}
				SOURCEIMAGE = att.getNamedItem("file").getNodeValue();

			}
			if (childNode.getNodeName().equals("result")) {
				if (att.getNamedItem("file") == null) {
					DEBUG
							.trace("ERROR: jacinfo.xml: Attribute Missing: result/file");
					return;
				}
				RESULTTXT = att.getNamedItem("file").getNodeValue();

			}

		}
		DEBUG.trace("METHODAUTHOR" + " = " + METHODAUTHOR);
		DEBUG.trace("METHODNAME" + " = " + METHODNAME);
		DEBUG.trace("IMAGETYPE" + " = " + IMAGETYPE);
		DEBUG.trace("SOURCEIMAGE" + " = " + SOURCEIMAGE);
		DEBUG.trace("RESULTTXT" + " = " + RESULTTXT);
		DEBUG.trace("METHODSETTINGS" + " = " + METHODSETTINGS);
		DEBUG.trace("METHODLETTERS" + " = " + METHODLETTERS);

	}

	/*
	 * Diese Methode lädt ein Bild und wartet bis es geladen ist
	 */
	private Image loadImage(File img) {

		return SWING.loadImage(img);
	}

	private void prepare(Captcha captcha) {

		captcha.cleanBackgroundBySample(3, 25, 3, 3);

		captcha.cleanWithMask(Captcha.getCaptcha(loadImage(new File(
				"svzgridmask.jpg"))), 3, 3);
		captcha.sampleDown(1);
	}

	private int trainCaptcha(File captchafile, int letterNum) {
		DEBUG.trace(captchafile.getAbsolutePath());
		Image captchaImage = loadImage(captchafile);
		DEBUG.trace(captchafile);
		String captchaHash = SYSTEM.getLocalHash(captchafile);
		if (isCaptchaInMTH(captchaHash)) {
			DEBUG.trace("ERROR captcha schon aufgenommen" + captchafile);
			return -1;
		}

		Captcha captcha = Captcha.getCaptcha(captchaImage);
		// captcha.printCaptcha();
		if (DOPREPARE) {
			prepare(captcha);
		}
		// captcha.blurIt(10);
		// captcha.printCaptcha();
		String savedCode = GLOBALS.getProperty(captchaHash);
		String code;
		String guess = "unknown";
		
		Letter[] letters = captcha.getLetters(letterNum);
		
		if (letters == null) {
			captchafile.renameTo(new File("detectionErrors/"
					+ captchafile.getName()));
			DEBUG
					.trace("ERROR: SELTSAM: 2. Lettererkennung ist efhlgeschlagen!");
			return -1;

		}
		
		if (savedCode == null) {
			guess = checkCaptcha(captchafile);
			if (guess == null) {
				captchafile.renameTo(new File("detectionErrors/"
						+ captchafile.getName()));
				DEBUG.trace("ERROR: Letter erkennung fehlgeschlagen");
				return -1;

			}

			// String code = (String) JOptionPane.showInputDialog(
			// (Component) GLOBALS.owner, guess, Locale.lc.get("ENTERCAPTCHA",
			// "Captcha"), JOptionPane.QUESTION_MESSAGE, (Icon) SYSTEM
			// .getImageIcon(captchafile.getAbsolutePath(), "captcha",
			// SYSTEM.ABSOLUTEPATH), null, null);

			code = (String) JOptionPane.showInputDialog(
					(Component) GLOBALS.owner, guess,
					savedCode == null ? "Bitte eingeben" : "ESC drücken um "
							+ savedCode + " zu übernehmen",
					JOptionPane.QUESTION_MESSAGE, captcha.getAsIconWithGaps(),
					null, null);

			if (code == null && savedCode != null) {
				code = savedCode;

			}
			if (code == null) {
				captchafile.renameTo(new File("detectionErrors/"
						+ captchafile.getName()));
				DEBUG.trace("ERROR: Captcha Input error");
				return -1;
			}
			if (code.length() == 0) {
				code = guess;
			}
		} else {
			code = savedCode;
			if (code == null || code.equals("null")) {
				captchafile.renameTo(new File("detectionErrors/"
						+ captchafile.getName()));
				DEBUG.trace("ERROR: Captcha Input error");
				return -1;
			}
			if (code.length() == 0) {
				captchafile.renameTo(new File("detectionErrors/"
						+ captchafile.getName()));
				DEBUG.trace("ERROR: Captcha Input error2");
				return -1;
			}
		}
		if(code.length()!=letters.length){
			captchafile.renameTo(new File("detectionErrors/"
					+ captchafile.getName()));
			DEBUG.trace("ERROR: Captcha Input error3");
			return -1;
		}
		GLOBALS.setProperty(captchaHash, code);
		int ret = 0;
		for (int i = 0; i < letters.length; i++) {
			if (letters[i] == null
					|| letters[i].getWidth() < 2
					|| letters[i].getHeight() < 2)
				break;
			// letters[i].printGrid();
		
			// Create a new DOM document; this method is implemented in
			// e511 Creating an Empty DOM Document

			// Insert the root element node
			Element element = mth.createElement("letter");
			mth.getFirstChild().appendChild(element);

			if (code.length()>i && guess.length()>i &&code.substring(i, i + 1).equals(guess.substring(i, i + 1))) {
				ret++;
			}
			// Add a text node to the element
			element
					.appendChild(mth
							.createTextNode(letters[i].getPixelString()));
			element.setAttribute("value", code.substring(i, i + 1));
			element.setAttribute("captchaHash", captchaHash);
			DEBUG.trace(letters[i].getPixelString());
			// mth.appendChild(element);
		}

		saveMTHFile();
		return ret;
	}

	public void saveMTHFile() {
		try {
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(mth);

			transformer.transform(source, result);

			String xmlString = result.getWriter().toString();
			new File(METHODLETTERS).renameTo(new File("tmp_" + METHODLETTERS));
			if (SYSTEM.writeLocalFile(new File(METHODLETTERS), xmlString)) {
				new File("tmp_" + METHODLETTERS).delete();
			} else {
				new File("tmp_" + METHODLETTERS).renameTo(new File(
						METHODLETTERS));
				DEBUG
						.trace("ERROR !!!!  XML FILE KONNTE NICHT GESPEICHERT WERDEN");
			}
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			DEBUG.error(e);
		}

	}

	/*
	 * Methode prüft ob das captcha schon aufgenommen wurde.
	 */
	private boolean isCaptchaInMTH(String captchaHash) {
		if (mth == null || mth.getFirstChild() == null)
			return false;
		NodeList nl = mth.getFirstChild().getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			Node childNode = nl.item(i);
			NamedNodeMap att = childNode.getAttributes();
			if (att != null
					&& att.getNamedItem("captchaHash").getNodeValue().equals(
							captchaHash))
				return true;
		}
		return false;

	}

	/*
	 * Versucht einen captcha auszulesen und gibt den Inhalt als String zurück
	 */
	private String checkCaptcha(File captchafile) {
		DEBUG.trace("check " + captchafile);
		Image captchaImage = loadImage(captchafile);
		Captcha captcha = Captcha.getCaptcha(captchaImage);
		// captcha.printCaptcha();
		return checkCaptcha(captcha);
	}
	private String checkCaptcha(Captcha captcha) {
	
		// captcha.printCaptcha();
		if (DOPREPARE) {
			prepare(captcha);
		}

		// captcha.printCaptcha();
		String ret = "";
		correct = 0;
		Letter[] letters = captcha.getLetters(LETTERNUM);
		if (letters == null)
			return null;
		for (int i = 0; i < letters.length; i++) {
			ret += getLetter(letters[i]);
			// letters[i].printGrid();
		}
		DEBUG.trace(ret + " - " + correct);
		return ret;
	}
	/*
	 * Vergleicht ein letterobjekt mit der datenbank und gibt den besten treffer
	 * asl String zurück
	 */
	private String getLetter(Letter letter) {

		long bestValue = Long.MAX_VALUE;
		String bestResult = "_";
		try {
			DEBUG.trace(letterDB.size());
			if (letterDB == null)
				return "_";

			for (int i = 0; i < letterDB.size(); i++) {
				// Get child node
				Letter tmp = letterDB.elementAt(i);

				long value = 0;
				if (tmp.getHeight() != letter.getHeight()
						|| tmp.getWidth() != letter.getWidth()) {

					continue;
				}
				for (int y = 0; y < tmp.getHeight(); y++) {

					for (int x = 0; x < tmp.getWidth(); x++) {

						value += Math.abs(tmp.getPixelValue(x, y)
								- letter.getPixelValue(x, y));
					}
				}
				value /= (tmp.getHeight() * tmp.getWidth());
				if (value == 0) {

					return tmp.decodedValue;
				}
				if (value < bestValue) {
					bestValue = value;
					bestResult = tmp.decodedValue;
					 DEBUG.trace(" old "+bestResult + " : " + bestValue);
				}

			}
			this.correct += bestValue;

		} catch (Exception e) {
			DEBUG.error(e);
		}
		if(bestValue>letter.getMaxPixelValue()*USELETTERSEARCHFAKTOR){
			return searchLetter(letter);
		}
		return bestResult;

	}

	/*
	 * 
	 * liest den captchaOrdner aus und gibt ein File Array zurück
	 */
	private File[] getImages() {
		File dir = new File(GLOBALS.ROOTDIR + GLOBALS.FS + "captchas");

		if (dir == null || !dir.exists()) {
			DEBUG.trace("Image dir nicht gefunden");
		}

		File[] entries = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				// DEBUG.trace(pathname.getName());
				if (pathname.getName().endsWith(".jpg")) {

					return true;
				} else {
					return false;
				}
			}

		});
		return entries;

	}

}