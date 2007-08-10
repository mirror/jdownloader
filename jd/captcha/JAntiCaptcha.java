package jd.captcha;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.io.File;
import java.io.FileFilter;
import java.io.StringWriter;
import java.util.Date;
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

public class JAntiCaptcha {

	/*
	 * private static double letterSearchLimitValue Faktor, über den sich
	 * einstellen lässt ab welcher Grenze ein Buchstabe als sicher richtig
	 * erkannt wird. Übliche werte sind von 0(es wird immer der beste Wert
	 * gesucht) bis 0.3 (der algo ist sehr tollerant bei der Suche)
	 */
	private double letterSearchLimitValue = 0.15;

	private String methodAuthor;

	private String methodName;

	private String imageType;

	private int letterNum;

	private String sourceImage;

	private Vector<String[]> captchaPrepareCommands;

	private Vector<String[]> jacCommands;

	private Vector<String[]> letterCommands;

	private String resultFile;

	private String methodSettingsFile;

	private String methodScriptFile;

	private Document mth;

	private Vector<Letter> letterDB;

	private String methodLettersFile;

	private int leftPadding = 0;

	private int gapWidthPeak = 1;

	private int gapWidthAverage = 1;

	private boolean gapAndAverageLogic = true;

	private double gapDetectionAverageContrast = 1.3;

	private double gapDetectionPeakContrast = 0.25;

	private boolean useAverageGapDetection = false;

	private boolean usePeakGapdetection = true;

	private int minimumLetterWidth = 10;

	private int simplifyFaktor = 1;

	private int colorValueFaktor = 0xffffff;

	private double relativeContrast = 0.85;

	private double backgroundSampleCleanContrast = 0.1;

	private double blackPercent = 0.1;

	private int[] gaps;

	private String method;

	private boolean trainOnlyUnknown = true;

	private BasicWindow frame1;

	private BasicWindow frame2;

	private BasicWindow frame3;

	private int borderVariance = 0;

	private int scanVariance = 0;

	public JAntiCaptcha(String method) {
		this.method = method;
		String[] path = { "methods", method };
		if (!new File(UTILITIES.getFullPath(path)).exists()) {
			UTILITIES.trace("ERROR: Die Methode " + method
					+ " kann nicht gefunden werden");
		}
		getJACInfo();
		parseScriptFile();
		executeJacCommands();
		loadMTHFile();

		if (this.getResultFile() != null && this.getSourceImage() != null
				&& new File(this.getSourceImage()).exists()) {
			String hash = UTILITIES
					.getLocalHash(new File(this.getSourceImage()));
			UTILITIES.trace(hash);
			if (hash.equals("dab07d2b7f1299f762454cda4c6143e7")) {
				UTILITIES.trace("BOT ERKANNT");
				UTILITIES.writeLocalFile(new File(this.getResultFile()), "");

			} else {
				Image captchaImage = loadImage(new File(this.getSourceImage()));
				Captcha captcha = createCaptcha(captchaImage);
				String code = this.checkCaptcha(captcha);
				if (code.indexOf("null") >= 0) {
					UTILITIES.trace("BOT ERKANNT");
					code = "";
					UTILITIES.writeLocalFile(new File(this.getResultFile()),
							code);

				} else {
					UTILITIES.writeLocalFile(new File(this.getResultFile()),
							code);
					String fileName = new File(this.getSourceImage()).getName();

					String[] newPath = { "methods", method, "checked",
							captcha.valityValue + "_" + fileName };
					new File(UTILITIES
							.getFullPath(newPath)).getParentFile().mkdirs();
					new File(this.getSourceImage()).renameTo(new File(UTILITIES
							.getFullPath(newPath)));
				}
			}

		}
		// saveMSTFile();
		// trainAllCaptchas();
		// while(true){
		// String path="captchas/share_gulli_"+(new Date().getTime())+".jpg";
		// NET.downloadBinary(path, "http://share.gulli.com/captcha");
		// File f= new File(path);
		//		
		// trainCaptcha(f,3);
		// }
		//		
		// BasicWindow w;
		//
		// Captcha c;
		//
		// c = Captcha
		// .getCaptcha(f);
		//	
		// w = //BasicWindow.showImage(c.getImage(), "captcha");
		// prepare(c);
		// Letter[] letters=c.getLetters(3);
		// //BasicWindow.showImage(letters[0].getImage());
		// //BasicWindow.showImage(letters[1].getImage());
		// //BasicWindow.showImage(letters[2].getImage());
		//
		// UTILITIES.trace(checkCaptcha(c));
		// UTILITIES.wait(999999999);
		// prepare(c);
		// w = //BasicWindow.showImage(c.getImage(), "Img vor");
		// prepare(c);
		// Letter[] letters = c.getLetters(5);
		//		
		// w = //BasicWindow.showImage(letters[0].getImage(), "Letter 0 "
		// + getLetter2(letters[0])+"-"+getLetter(letters[0]));
		// w.setSize(300, 80);
		// w.setLocation(50, 100);
		// w = //BasicWindow.showImage(letters[1].getImage(), "Letter 1 "
		// + getLetter2(letters[1])+"-"+getLetter(letters[1]));
		// w.setSize(300, 80);
		// w.setLocation(50, 200);
		// w = //BasicWindow.showImage(letters[2].getImage(), "Letter 2 "
		// + getLetter2(letters[2])+"-"+getLetter(letters[2]));
		// w.setSize(300, 80);
		// w.setLocation(50, 300);
		// w = //BasicWindow.showImage(letters[3].getImage(), "Letter 3 "
		// + getLetter2(letters[3])+"-"+getLetter(letters[3]));
		// w.setSize(300, 80);
		// w.setLocation(50, 400);
		// w = //BasicWindow.showImage(letters[4].getImage(), "Letter 4 "
		// + getLetter2(letters[4])+"-"+getLetter(letters[4]));
		// w.setSize(300, 80);
		// w.setLocation(50, 500);
	}

	private void executeJacCommands() {
		if (jacCommands == null || jacCommands.size() == 0) {
			UTILITIES.trace("KEINE JAC COMMANDS");
			return;
		}
		for (int i = 0; i < jacCommands.size(); i++) {
			String[] cmd = jacCommands.elementAt(i);
			if (cmd[0].equals("parameter")) {
				if (cmd[1].toLowerCase().equals("lettersearchlimitvalue"))
					this.setLetterSearchLimitValue(Double.parseDouble(cmd[2]));
				if (cmd[1].toLowerCase().equals("trainonlyunknown"))
					this.setTrainOnlyUnknown(cmd[2].equals("true"));
				if (cmd[1].toLowerCase().equals("scanvariance"))
					this.setScanVariance(Integer.parseInt(cmd[2]));
				if (cmd[1].toLowerCase().equals("bordervariance"))
					this.setBorderVariance(Integer.parseInt(cmd[2]));
				if (cmd[1].toLowerCase().equals("leftpadding"))
					this.setLeftPadding(Integer.parseInt(cmd[2]));
				if (cmd[1].toLowerCase().equals("simplifyfaktor"))
					this.setSimplifyFaktor(Integer.parseInt(cmd[2]));
			} else if (cmd[0].equals("function") && cmd[2] == null) {

			} else if (cmd[0].equals("function") && cmd[2] != null) {

			}
		}

	}

	private void parseScriptFile() {
		String script = UTILITIES.getLocalFile(new File("methods/"
				+ this.method + "/script.jas"));
		String[] lines = script.split("\r\n");
		if (lines.length == 1)
			lines = script.split("\n\r");
		if (lines.length == 1)
			lines = script.split("\n");
		if (lines.length == 1)
			lines = script.split("\r");
		Vector<String[]> localCaptchaPrepareCommands = new Vector<String[]>();
		Vector<String[]> localJacCommands = new Vector<String[]>();
		Vector<String[]> localLetterCommands = new Vector<String[]>();
		int startAt;
		String[] pcmd;
		for (int i = 0; i < lines.length; i++) {

			lines[i] = lines[i].trim();

			if (lines[i].indexOf("#") == 0 || lines[i].length() < 3) {
				// leere Zeile, oder Comment
				continue;
			}
			if (!lines[i].substring(lines[i].length() - 1).equals(";")) {
				UTILITIES.trace("ERROR : " + method
						+ "/script.jas: Syntax error (; missing?) near line "
						+ i + ": " + lines[i]);
				return;
			}
			lines[i] = lines[i].substring(0, lines[i].length() - 1);
			if ((startAt = lines[i].indexOf("captcha.prepare.")) == 0) {
				pcmd = parseCommand(lines[i].substring(startAt + 16));

				localCaptchaPrepareCommands.add(pcmd);
			} else if ((startAt = lines[i].indexOf("jac.")) == 0) {
				pcmd = parseCommand(lines[i].substring(startAt + 4));

				localJacCommands.add(pcmd);
			} else if ((startAt = lines[i].indexOf("letter.")) == 0) {
				pcmd = parseCommand(lines[i].substring(startAt + 7));

				localLetterCommands.add(pcmd);
			} else {
				UTILITIES.trace("ERROR : " + method
						+ "/script.jas: Syntax error near line " + i + ": "
						+ lines[i]);
			}
		}
		this.setCaptchaPrepareCommands(localCaptchaPrepareCommands);
		this.setJacCommands(localJacCommands);
		this.setLetterCommands(localLetterCommands);
	}

	private String[] parseCommand(String cmd) {
		String[] ret = new String[3];
		String[] matches;
		cmd = "#" + cmd + "#";
		if ((matches = UTILITIES.getMatches(cmd, "#°=°#")) != null) {

			ret[0] = "parameter";
			ret[1] = matches[0].trim();
			ret[2] = matches[1].replaceAll("\\\"", "").trim();
		} else if ((matches = UTILITIES.getMatches(cmd, "#°(°)#")) != null) {
			ret[0] = "function";
			ret[1] = matches[0].trim();
			ret[2] = matches[1].replaceAll("\\\"", "").trim();
		} else if ((matches = UTILITIES.getMatches(cmd, "#°()#")) != null) {
			ret[0] = "function";
			ret[1] = matches[0].trim();
			ret[2] = null;
		}
		return ret;
	}

	// getCaptcha()
	// UTILITIES.trace(checkCaptcha(new File(SOURCEIMAGE)));
	// String code= checkCaptcha(new File(SOURCEIMAGE));
	// UTILITIES.writeLocalFile(new File(RESULTTXT), code);

	// }

	private void setParameter() {

	}

	private void trainAllCaptchas() {

		int successFull = 0;
		int total = 0;
		File[] images = getImages();
		int newLetters;
		for (int i = 0; i < images.length; i++) {

			newLetters = trainCaptcha(images[i], getLetterNum());
			UTILITIES.trace("Erkannt: " + newLetters + "/" + getLetterNum());
			if (newLetters > 0) {
				successFull += newLetters;
				total += getLetterNum();
				UTILITIES.trace("Erkennungsrate: "
						+ ((100 * successFull / total)));
			}

		}
		UTILITIES.wait(10000);
	}

	private void loadMTHFile() {
		String[] path = { "methods", method, "letters.mth" };
		if (new File(UTILITIES.getFullPath(path)).exists() == false) {
			UTILITIES.writeLocalFile(new File(UTILITIES.getFullPath(path)),
					"<jDownloader/>");
			UTILITIES.trace("ERROR: MTH FILE NOT AVAILABLE. Created: " + path);
		}
		mth = UTILITIES.parseXmlFile(UTILITIES.getFullPath(path), false);
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
					// UTILITIES.trace(childNode.getNodeName());
					// UTILITIES.trace(childNode.getTextContent());
					// UTILITIES.trace(att.getNamedItem("captchaHash").getNodeValue());
					// UTILITIES.trace(att.getNamedItem("value").getNodeValue());
					tmp = new Letter();
					tmp.setOwner(this);
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
			UTILITIES
					.trace("ERROR: Fehler mein lesen der MTHO Datei!!. Methode kann nicht funktionieren!");

		}
	}

	private void loadMSTFile(String file) {

	}

	/*
	 * Die Methode erstellet eine Propertydatei mitd en aktuellen Parametern.
	 */
	private void saveMSTFile() {

	}

	/*
	 * Die Methode parst die jacinfo.xml
	 */
	private void getJACInfo() {
		String[] path = { "methods", method, "jacinfo.xml" };
		if (!new File(UTILITIES.getFullPath(path)).exists()) {
			UTILITIES.trace("ERROR: " + UTILITIES.getFullPath(path)
					+ " is missing");
		}
		Document doc = UTILITIES.parseXmlFile(UTILITIES.getFullPath(path),
				false);

		NodeList nl = doc.getFirstChild().getChildNodes();
		for (int i = 0; i < nl.getLength(); i++) {
			// Get child node
			Node childNode = nl.item(i);

			if (childNode.getNodeName().equals("method")) {

				this.setMethodAuthor(UTILITIES
						.getAttribute(childNode, "author"));
				this.setMethodName(UTILITIES.getAttribute(childNode, "name"));

			}
			if (childNode.getNodeName().equals("format")) {

				this.setLetterNum(Integer.parseInt(UTILITIES.getAttribute(
						childNode, "letterNum")));
				this.setImageType(UTILITIES.getAttribute(childNode, "type"));

			}
			if (childNode.getNodeName().equals("source")) {

				this.setSourceImage(UTILITIES.getAttribute(childNode, "file"));

			}
			if (childNode.getNodeName().equals("result")) {

				this.setResultFile(UTILITIES.getAttribute(childNode, "file"));

			}

		}

	}

	/*
	 * Diese Methode lädt ein Bild und wartet bis es geladen ist
	 */
	private Image loadImage(File img) {

		return UTILITIES.loadImage(img);
	}

	// private void prepare(Captcha captcha) {
	// captcha.prepared++;
	// if (captcha.prepared > 1) {
	// UTILITIES.trace("ERROR: Prepare wird doppelt ausgeführt!!!");
	// return;
	// }
	// UTILITIES.trace("prepare");
	// // //BasicWindow.showImage(captcha.getImage(2),"Original als
	// // Helligkeitsabbildung");
	// captcha.cleanBackgroundBySample(3, 3, 3, 3);
	// //BasicWindow.showImage(captcha.getImage(2));
	// // entfernen");
	//	
	// captcha.toBlackAndWhite(0.2);
	//
	//
	//
	// }

	// SVZ Prepare
	private void prepare(Captcha captcha) {
		captcha.prepared++;
		if (captcha.prepared > 1) {
			UTILITIES.trace("ERROR: Prepare wird doppelt ausgeführt!!!");
			return;
		}
		UTILITIES.trace("prepare");
		// //BasicWindow.showImage(captcha.getImage(2),"Original als
		// Helligkeitsabbildung");
		captcha.cleanBackgroundBySample(3, 25, 3, 3);
		// //BasicWindow.showImage(captcha.getImage(2),"Hintergrundfarbe
		// entfernen");
		captcha.cleanWithMask(Captcha.getCaptcha(loadImage(new File(
				"svzgridmask.jpg"))), 3, 3);
		// //BasicWindow.showImage(captcha.getImage(2),"Maskenfilter: Muster
		// entfernen");
		captcha.toBlackAndWhite(1.1);
		// //BasicWindow.showImage(captcha.getImage(2),"BW-Convert: mit Faktor
		// 1.1
		// in SW-Bild umwandlen");
		captcha.reduceWhiteNoise(6);
		// //BasicWindow.showImage(captcha.getImage(2),"ReduceNoise: Weiße
		// Störungen über einen 6 Pixel Durchmesser entfernen");

		// //BasicWindow.showImage(captcha.getImage(2),"ReduceNoise: Weiße
		// Störungen über einen 6 Pixel Durchmesser entfernen");
		captcha.toBlackAndWhite(0.2);
		// //BasicWindow.showImage(captcha.getImage(2),"BW-Convert: mit Faktor
		// 0.2. Elemente die nicht wirklich Schwarz sind werden entfernt");
		captcha.reduceWhiteNoise(3);
		// //BasicWindow.showImage(captcha.getImage(2),"ReduceNoise: Weiße
		// Störungen über einen 3 Pixel Durchmesser entfernen. Buchstaben werden
		// dicker");
		captcha.toBlackAndWhite(0.2);

		// //BasicWindow.showImage(captcha.getImage(2),"BW-Convert: mit Faktor
		// 0.2. Elemente die nicht wirklich Schwarz sind werden entfernt");
		captcha.reduceBlackNoise(3, 0.9);
		// //BasicWindow.showImage(captcha.getImage(2),"ReduceNoise: Schwarze
		// Störungen, Flecken außerhalb der Buchstaben entfernen");
		captcha.toBlackAndWhite(1.2);
		// //BasicWindow.showImage(captcha.getImage(2),"BW-Convert: mit Faktor
		// 1.2. Elemente die leicht grau sind werden zu schwarz");
		//
		// UTILITIES.wait(5000);

	}

	private int trainCaptcha(File captchafile, int letterNum) {

		Image captchaImage = loadImage(captchafile);

		String captchaHash = UTILITIES.getLocalHash(captchafile);
		if (isCaptchaInMTH(captchaHash)) {
			UTILITIES.trace("ERROR captcha schon aufgenommen" + captchafile);
			return -1;
		}

		Captcha captcha = createCaptcha(captchaImage);

		String savedCode = UTILITIES.getProperty(captchaHash);
		String code = null;
		String guess = "unknown";

		Letter[] letters = captcha.getLetters(letterNum);
		if (frame1 != null) {
			frame1.destroy();
		}

		frame1 = BasicWindow.showImage(captcha.getImageWithGaps(1));
		frame1.setLocationByScreenPercent(50, 20);
		if (frame2 != null) {
			frame2.destroy();
		}
		frame2 = new BasicWindow();
		frame2.setTitle("Letters");
		frame2.setLayout(new GridBagLayout());
		frame2.setSize(300, 300);
		frame2.setAlwaysOnTop(true);

		frame2.setLocationByScreenPercent(50, 5);
		if (letters == null) {
			String[] path = { "methods", method, "detectionErrors1",
					captchafile.getName() };
			captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
			UTILITIES
					.trace("ERROR: SELTSAM: 2. Lettererkennung ist fehlgeschlagen!");
			return -1;

		}
		for (int i = 0; i < letters.length; i++) {

			frame2.add(new ImageComponent(letters[i].getImage(this
					.getSimplifyFaktor())), UTILITIES.getGBC(i * 2, 0, 2, 2));

			frame2.add(new ImageComponent(letters[i].getSimplified(
					this.getSimplifyFaktor()).getImage()), UTILITIES.getGBC(
					i * 2, 2, 2, 2));

		}
		frame2.setVisible(true);
		frame2.pack();
		frame2.setSize(300, frame2.getSize().height);
		guess = checkCaptcha(captcha);
		UTILITIES.trace("Decoded Captcha: " + guess + " Vality: "
				+ captcha.valityValue);
		if (guess == null) {
			String[] path = { "methods", method, "detectionErrors2",
					captchafile.getName() };
			captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
			UTILITIES.trace("ERROR: Letter erkennung fehlgeschlagen");
			return -1;

		}

		// String code = (String) JOptionPane.showInputDialog(
		// (Component) GLOBALS.owner, guess, Locale.lc.get("ENTERCAPTCHA",
		// "Captcha"), JOptionPane.QUESTION_MESSAGE, (Icon) UTILITIES
		// / .getImageIcon(captchafile.getAbsolutePath(), "captcha",
		// UTILITIES.ABSOLUTEPATH), null, null);
		// code=null;
		// if(guess.indexOf("_")>=0)return -1;
		// code=guess;
		if (captcha.valityValue > 0) {
			code = UTILITIES
					.prompt("Bitte Captcha Code eingeben (Press enter to confirm "
							+ guess + " / Press ESC to confirm " + savedCode);

		}
		if (code == null && savedCode != null) {
			code = savedCode;

		}
		if (code == null) {
			String[] path = { "methods", method, "detectionErrors3",
					captchafile.getName() };
			captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
			UTILITIES.trace("ERROR: Captcha Input error");
			return -1;
		}
		if (code.length() == 0) {
			code = guess;
		}
		// } else {
		// code = savedCode;
		// if (code == null || code.equals("null")) {
		// captchafile.renameTo(new File("detectionErrors/"
		// + captchafile.getName()));
		// UTILITIES.trace("ERROR: Captcha Input error");
		// return -1;
		// }
		// if (code.length() == 0) {
		// captchafile.renameTo(new File("detectionErrors/"
		// + captchafile.getName()));
		// UTILITIES.trace("ERROR: Captcha Input error2");
		// return -1;
		// }
		// }
		if (code.length() != letters.length) {
			String[] path = { "methods", method, "detectionErrors4",
					captchafile.getName() };
			captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
			UTILITIES.trace("ERROR: Captcha Input error3");
			return -1;
		}
		UTILITIES.setProperty(captchaHash, code);
		int ret = 0;
		for (int i = 0; i < letters.length; i++) {
			if (letters[i] == null || letters[i].getWidth() < 2
					|| letters[i].getHeight() < 2) {
				String[] path = { "methods", method, "detectionErrors5",
						captchafile.getName() };
				captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
				UTILITIES.trace("ERROR: Letter detection error");
				return -1;
			}
			frame2.add(new ImageComponent(letters[i].getImage()));
			frame2.repack();
			// letters[i].printGrid();

			// Create a new DOM document; this method is implemented in
			// e511 Creating an Empty DOM Document

			// Insert the root element node
			Element element = mth.createElement("letter");
			mth.getFirstChild().appendChild(element);

			if (code.length() > i
					&& guess.length() > i
					&& code.substring(i, i + 1).equals(
							guess.substring(i, i + 1))) {
				ret++;
				if (!isTrainOnlyUnknown()) {
					element.appendChild(mth.createTextNode(letters[i]
							.getPixelString()));
					letters[i].printGrid();
					element.setAttribute("value", code.substring(i, i + 1));
					element.setAttribute("captchaHash", captchaHash);
					element.setAttribute("bad", "0");
					element.setAttribute("good", "0");

					letters[i].setOwner(this);
					letters[i].setTextGrid(letters[i].getPixelString());
					letters[i].setSourceHash(captchaHash);
					letters[i].setDecoded(code.substring(i, i + 1));
					letterDB.add(letters[i]);

				}
			} else {
				// Add a text node to the element
				element.appendChild(mth.createTextNode(letters[i]
						.getPixelString()));
				letters[i].printGrid();
				element.setAttribute("value", code.substring(i, i + 1));
				element.setAttribute("captchaHash", captchaHash);
				element.setAttribute("bad", "0");
				element.setAttribute("good", "0");
				letters[i].setOwner(this);
				letters[i].setTextGrid(letters[i].getPixelString());
				letters[i].setSourceHash(captchaHash);
				letters[i].setDecoded(code.substring(i, i + 1));
				letterDB.add(letters[i]);

			}
			// mth.appendChild(element);
		}

		saveMTHFile();
		return ret;
	}

	private Captcha createCaptcha(Image captchaImage) {
		Captcha ret = Captcha.getCaptcha(captchaImage);
		ret.setOwner(this);
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

			String[] path = { "methods", method, "letters.mth" };

			if (!UTILITIES.writeLocalFile(
					new File(UTILITIES.getFullPath(path)), xmlString)) {
				UTILITIES
						.trace("ERROR: MTHO file Konnte nicht gespeichert werden");
			}
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/*
	 * Methode prüft ob das captcha schon aufgenommen wurde.
	 */
	private boolean isCaptchaInMTH(String captchaHash) {
		if (letterDB == null)
			return false;

		for (int i = 0; i < letterDB.size(); i++) {
			if (letterDB.elementAt(i).sourcehash.equals(captchaHash))
				return true;
		}

		return false;

	}

	/*
	 * Versucht einen captcha auszulesen und gibt den Inhalt als String zurück
	 */
	public String checkCaptcha(File captchafile) {
		UTILITIES.trace("check " + captchafile);
		Image captchaImage = loadImage(captchafile);
		Captcha captcha = createCaptcha(captchaImage);
		// captcha.printCaptcha();
		return checkCaptcha(captcha);
	}

	public String checkCaptcha(Captcha captcha) {

		// captcha.printCaptcha();

		// captcha.printCaptcha();
		Letter[] newLetters = new Letter[this.getLetterNum()];
		String ret = "";
		long correct = 0;
		Letter akt;
		Letter[] letters = captcha.getLetters(getLetterNum());
		if (letters == null)
			return null;
		for (int i = 0; i < letters.length; i++) {
			akt = getLetter(letters[i]);
			newLetters[i] = letters[i];
			if (akt == null) {
				letters[i].setDecoded("_");
				letters[i].setValityValue(Integer.MAX_VALUE);
				correct += Integer.MAX_VALUE;

			} else {

				letters[i].setDecoded(akt.decodedValue);
				letters[i].setValityValue(akt.valityValue);
				correct += akt.valityValue;
			}
			if (newLetters[i] != null) {
				ret += akt.decodedValue;
			}
			if (letters[i].getWidth() > 0 && letters[i].getHeight() > 0) {
				// BasicWindow.showImage(letters[i].getImage(1), "cap " + i);
				// letters[i].printGrid();+
			}
		}

		captcha.setDecodedLetters(newLetters);

		captcha.setValityValue((int) (correct / letters.length));

		return ret;
	}

	/*
	 * Vergleicht ein letterobjekt mit der datenbank und gibt den besten treffer
	 * asl String zurück
	 */

	private int scanCompare(Letter a, Letter b) {
		// int difX = Math.abs(a.getWidth() - b.getWidth());
		// int difY = Math.abs(a.getHeight() - b.getHeight());
		int bx, by, va, vb;
		long value = 0;
		int bestValue = Integer.MAX_VALUE;
		int pixel;

		int scanXFrom = Math.min(b.getWidth() - a.getWidth() - scanVariance,
				-scanVariance);
		int scanXTo = Math.max(-scanVariance, a.getWidth() - b.getWidth()
				+ scanVariance);

		int scanYFrom = Math.min(b.getHeight() - a.getHeight() - scanVariance,
				-scanVariance);
		int scanYTo = Math.max(-scanVariance, a.getHeight() - b.getHeight()
				+ scanVariance);
		// UTILITIES.trace(a.getDim()+" - "+b.getDim());
		// a.printGrid();
		// b.printGrid();
		// UTILITIES.trace(scanXFrom+" - "+scanXTo);
		// UTILITIES.trace(scanYFrom+" - "+scanYTo);
		for (int xx = scanXFrom; xx <= scanXTo; xx++) {
			for (int yy = scanYFrom; yy <= scanYTo; yy++) {
				bx = Math.max(0 - xx, 0);
				by = Math.max(0 - yy, 0);
				pixel = 0;
				// UTILITIES.trace("0 x->
				// "+Math.min(a.getWidth(),b.getWidth()-bx));
				// UTILITIES.trace("0 y-> "+Math.min(a.getHeight(),
				// b.getHeight()-by));
				for (int x = 0; x < Math.min(a.getWidth(), b.getWidth() - bx); x++) {
					for (int y = 0; y < Math.min(a.getHeight(), b.getHeight()
							- by); y++) {
						va = a.getPixelValue(x, y);
						vb = b.getPixelValue(x + bx, y + by);
						pixel++;
						value += Math.abs(va - vb);
					}
				}
				if (pixel > 0) {
					value /= pixel;

					bestValue = Math.min((int) value, bestValue);
				}

			}
		}
		return bestValue;
	}

	private Letter getLetter(Letter letter) {
		// UTILITIES.trace(testhash);
		long bestValue = Long.MAX_VALUE;
		String bestResult = "_";
		String lastResult = "";
		Letter res = new Letter();
		int value;
		res.setOwner(this);
		res.setValityValue(Integer.MAX_VALUE);
		// UTILITIES.trace("Detect letter "+letter);

		// UTILITIES.trace(letter.getDim());
		try {

			if (letterDB == null)
				return null;

			for (int i = 0; i < letterDB.size(); i++) {
				// Get child node
				Letter tmp = letterDB.elementAt(i);

				// UTILITIES.trace(tmp.decodedValue+" : "+tmp.getDim());
				if (Math.abs(tmp.getHeight() - letter.getHeight()) > borderVariance
						|| Math.abs(tmp.getWidth() - letter.getWidth()) > borderVariance) {
					continue;
				}

				value = scanCompare(letter, tmp);

				// UTILITIES.trace(" value " + tmp.decodedValue + " : "
				// + (value / 100000));
				if (value < bestValue) {
					bestValue = value;
					bestResult = tmp.decodedValue;
					res = tmp;
					tmp.setValityValue(value);
					// UTILITIES.trace(" old " + bestResult + " : "
					// + (bestValue / 100000));
					// tmp.printGrid();

					if (value == 0
							|| (value <= letterSearchLimitValue
									* tmp.getMaxPixelValue() && bestResult
									.equals(lastResult))) {
						res = tmp;
						// UTILITIES.trace(res.decodedValue
						// + " Grenzwert "
						// + (letterSearchLimitValue * tmp
						// .getMaxPixelValue()) + " " + value);
						// BasicWindow.showImage(res.getImage(1), "Found SUPI
						// WERT with "+value+":" + bestResult);
						tmp.setValityValue(value);
						return tmp;
					}
				}
				lastResult = bestResult;
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return res;

	}

	/*
	 * 
	 * liest den captchaOrdner aus und gibt ein File Array zurück
	 */
	private File[] getImages() {
		String[] path = { "methods", method, "captchas" };

		File dir = new File(UTILITIES.getFullPath(path));

		if (dir == null || !dir.exists()) {
			UTILITIES.trace("Image dir nicht gefunden");
		}

		File[] entries = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				// UTILITIES.trace(pathname.getName());
				if (pathname.getName().endsWith(".jpg")) {

					return true;
				} else {
					return false;
				}
			}

		});
		return entries;

	}

	/**
	 * @return the imageType
	 */
	public String getImageType() {
		return imageType;
	}

	/**
	 * @param imageType
	 *            the imageType to set
	 */
	public void setImageType(String imageType) {
		UTILITIES.trace("SET PARAMETER: [imageType] = " + imageType);
		this.imageType = imageType;
	}

	/**
	 * @return the letterNum
	 */
	public int getLetterNum() {
		return letterNum;
	}

	/**
	 * @param letterNum
	 *            the letterNum to set
	 */
	public void setLetterNum(int letterNum) {
		UTILITIES.trace("SET PARAMETER: [letterNum] = " + letterNum);
		this.letterNum = letterNum;
	}

	/**
	 * @return the method
	 */
	public String getMethod() {
		return method;
	}

	/**
	 * @param method
	 *            the method to set
	 */
	public void setMethod(String method) {
		UTILITIES.trace("SET PARAMETER: [method] = " + method);
		this.method = method;
	}

	/**
	 * @return the methodAuthor
	 */
	public String getMethodAuthor() {
		return methodAuthor;
	}

	/**
	 * @param methodAuthor
	 *            the methodAuthor to set
	 */
	public void setMethodAuthor(String methodAuthor) {
		UTILITIES.trace("SET PARAMETER: [methodAuthor] = " + methodAuthor);
		this.methodAuthor = methodAuthor;
	}

	/**
	 * @return the getMethodLettersFileFile
	 */
	public String getMethodLettersFile() {
		return methodLettersFile;
	}

	/**
	 * @param getMethodLettersFileFile
	 *            the getMethodLettersFileFile to set
	 */
	public void setMethodLettersFile(String getMethodLettersFile) {
		UTILITIES.trace("SET PARAMETER: [getMethodLettersFile] = "
				+ getMethodLettersFile);
		this.methodLettersFile = methodLettersFile;
	}

	/**
	 * @return the methodName
	 */
	public String getMethodName() {
		return methodName;
	}

	/**
	 * @param methodName
	 *            the methodName to set
	 */
	public void setMethodName(String methodName) {
		UTILITIES.trace("SET PARAMETER: [methodName] = " + methodName);
		this.methodName = methodName;
	}

	/**
	 * @return the methodSettingsFile
	 */
	public String getMethodSettingsFile() {
		return methodSettingsFile;
	}

	/**
	 * @param methodSettingsFile
	 *            the methodSettingsFile to set
	 */
	public void setMethodSettingsFile(String methodSettingsFile) {
		UTILITIES.trace("SET PARAMETER: [methodSettingsFile] = "
				+ methodSettingsFile);
		this.methodSettingsFile = methodSettingsFile;
	}

	/**
	 * @return the resultFile
	 */
	public String getResultFile() {
		return resultFile;
	}

	/**
	 * @param resultFile
	 *            the resultFile to set
	 */
	public void setResultFile(String resultFile) {
		UTILITIES.trace("SET PARAMETER: [resultFile] = " + resultFile);
		this.resultFile = resultFile;
	}

	/**
	 * @return the sourceImage
	 */
	public String getSourceImage() {
		return sourceImage;
	}

	/**
	 * @param sourceImage
	 *            the sourceImage to set
	 */
	public void setSourceImage(String sourceImage) {
		UTILITIES.trace("SET PARAMETER: [sourceImage] = " + sourceImage);
		this.sourceImage = sourceImage;
	}

	/**
	 * @return the methodScriptFile
	 */
	public String getMethodScriptFile() {
		return methodScriptFile;
	}

	/**
	 * @param methodScriptFile
	 *            the methodScriptFile to set
	 */
	public void setMethodScriptFile(String methodScriptFile) {
		UTILITIES.trace("SET PARAMETER: [methodScriptFile] = "
				+ methodScriptFile);
		this.methodScriptFile = methodScriptFile;
	}

	/**
	 * @return the captchaPrepareCommands
	 */
	public Vector<String[]> getCaptchaPrepareCommands() {
		return captchaPrepareCommands;
	}

	/**
	 * @param captchaPrepareCommands
	 *            the captchaPrepareCommands to set
	 */
	public void setCaptchaPrepareCommands(
			Vector<String[]> captchaPrepareCommands) {
		this.captchaPrepareCommands = captchaPrepareCommands;
	}

	/**
	 * @return the jacCommands
	 */
	public Vector<String[]> getJacCommands() {
		return jacCommands;
	}

	/**
	 * @param jacCommands
	 *            the jacCommands to set
	 */
	public void setJacCommands(Vector<String[]> jacCommands) {
		this.jacCommands = jacCommands;
	}

	/**
	 * @return the letterCommands
	 */
	public Vector<String[]> getLetterCommands() {
		return letterCommands;
	}

	/**
	 * @param letterCommands
	 *            the letterCommands to set
	 */
	public void setLetterCommands(Vector<String[]> letterCommands) {
		this.letterCommands = letterCommands;
	}

	/**
	 * @return the gapAndAverageLogic
	 */
	public boolean getGapAndAverageLogic() {
		return gapAndAverageLogic;
	}

	/**
	 * @param gapAndAverageLogic
	 *            the gapAndAverageLogic to set
	 */
	public void setGapAndAverageLogic(boolean gapAndAverageLogic) {
		UTILITIES.trace("SET PARAMETER: [gapAndAverageLogic] = "
				+ gapAndAverageLogic);
		this.gapAndAverageLogic = gapAndAverageLogic;
	}

	/**
	 * @return the gapDetectionAverageContrast
	 */
	public double getGapDetectionAverageContrast() {
		return gapDetectionAverageContrast;
	}

	/**
	 * @param gapDetectionAverageContrast
	 *            the gapDetectionAverageContrast to set
	 */
	public void setGapDetectionAverageContrast(
			double gapDetectionAverageContrast) {
		UTILITIES.trace("SET PARAMETER: [gapDetectionAverageContrast] = "
				+ gapDetectionAverageContrast);
		this.gapDetectionAverageContrast = gapDetectionAverageContrast;
	}

	/**
	 * @return the gapDetectionPeakContrast
	 */
	public double getGapDetectionPeakContrast() {
		return gapDetectionPeakContrast;
	}

	/**
	 * @param gapDetectionPeakContrast
	 *            the gapDetectionPeakContrast to set
	 */
	public void setGapDetectionPeakContrast(double gapDetectionPeakContrast) {
		UTILITIES.trace("SET PARAMETER: [gapDetectionPeakContrast] = "
				+ gapDetectionPeakContrast);
		this.gapDetectionPeakContrast = gapDetectionPeakContrast;
	}

	/**
	 * @return the gaps
	 */
	public int[] getGaps() {
		return gaps;
	}

	/**
	 * @param gaps
	 *            the gaps to set
	 */
	public void setGaps(int[] gaps) {
		UTILITIES.trace("SET PARAMETER: [gaps] = " + gaps.toString());
		this.gaps = gaps;
	}

	/**
	 * @return the gapWidthAverage
	 */
	public int getGapWidthAverage() {
		return gapWidthAverage;
	}

	/**
	 * @param gapWidthAverage
	 *            the gapWidthAverage to set
	 */
	public void setGapWidthAverage(int gapWidthAverage) {
		UTILITIES
				.trace("SET PARAMETER: [gapWidthAverage] = " + gapWidthAverage);
		this.gapWidthAverage = gapWidthAverage;
	}

	/**
	 * @return the gapWidthPeak
	 */
	public int getGapWidthPeak() {
		return gapWidthPeak;
	}

	/**
	 * @param gapWidthPeak
	 *            the gapWidthPeak to set
	 */
	public void setGapWidthPeak(int gapWidthPeak) {
		UTILITIES.trace("SET PARAMETER: [gapWidthPeak] = " + gapWidthPeak);
		this.gapWidthPeak = gapWidthPeak;
	}

	/**
	 * @return the leftPadding
	 */
	public int getLeftPadding() {
		return leftPadding;
	}

	/**
	 * @param leftPadding
	 *            the leftPadding to set
	 */
	public void setLeftPadding(int leftPadding) {
		UTILITIES.trace("SET PARAMETER: [leftPadding] = " + leftPadding);
		this.leftPadding = leftPadding;
	}

	/**
	 * @return the letterSearchLimitValue
	 */
	public double getLetterSearchLimitValue() {
		return letterSearchLimitValue;
	}

	/**
	 * @param letterSearchLimitValue
	 *            the letterSearchLimitValue to set
	 */
	public void setLetterSearchLimitValue(double letterSearchLimitValue) {
		UTILITIES.trace("SET PARAMETER: [letterSearchLimitValue] = "
				+ letterSearchLimitValue);
		this.letterSearchLimitValue = letterSearchLimitValue;
	}

	/**
	 * @return the minimumLetterWidth
	 */
	public int getMinimumLetterWidth() {
		return minimumLetterWidth;
	}

	/**
	 * @param minimumLetterWidth
	 *            the minimumLetterWidth to set
	 */
	public void setMinimumLetterWidth(int minimumLetterWidth) {
		UTILITIES.trace("SET PARAMETER: [minimumLetterWidth] = "
				+ minimumLetterWidth);
		this.minimumLetterWidth = minimumLetterWidth;
	}

	/**
	 * @return the useAverageGapDetection
	 */
	public boolean isUseAverageGapDetection() {
		return useAverageGapDetection;
	}

	/**
	 * @param useAverageGapDetection
	 *            the useAverageGapDetection to set
	 */
	public void setUseAverageGapDetection(boolean useAverageGapDetection) {
		UTILITIES.trace("SET PARAMETER: [useAverageGapDetection] = "
				+ useAverageGapDetection);
		this.useAverageGapDetection = useAverageGapDetection;
	}

	/**
	 * @return the usePeakGapdetection
	 */
	public boolean isUsePeakGapdetection() {

		return usePeakGapdetection;
	}

	/**
	 * @param usePeakGapdetection
	 *            the usePeakGapdetection to set
	 */
	public void setUsePeakGapdetection(boolean usePeakGapdetection) {
		UTILITIES.trace("SET PARAMETER: [usePeakGapdetection] = "
				+ usePeakGapdetection);
		this.usePeakGapdetection = usePeakGapdetection;
	}

	/**
	 * @return the backgroundSampleCleanContrast
	 */
	public double getBackgroundSampleCleanContrast() {
		return backgroundSampleCleanContrast;
	}

	/**
	 * @param backgroundSampleCleanContrast
	 *            the backgroundSampleCleanContrast to set
	 */
	public void setBackgroundSampleCleanContrast(
			double backgroundSampleCleanContrast) {
		UTILITIES.trace("SET PARAMETER: [backgroundSampleCleanContrast] = "
				+ backgroundSampleCleanContrast);
		this.backgroundSampleCleanContrast = backgroundSampleCleanContrast;
	}

	/**
	 * @return the blackPercent
	 */
	public double getBlackPercent() {
		return blackPercent;
	}

	/**
	 * @param blackPercent
	 *            the blackPercent to set
	 */
	public void setBlackPercent(double blackPercent) {
		UTILITIES.trace("SET PARAMETER: [blackPercent] = " + blackPercent);
		this.blackPercent = blackPercent;
	}

	/**
	 * @return the colorValueFaktor
	 */
	public int getColorValueFaktor() {
		return colorValueFaktor;
	}

	/**
	 * @param colorValueFaktor
	 *            the colorValueFaktor to set
	 */
	public void setColorValueFaktor(int colorValueFaktor) {
		UTILITIES.trace("SET PARAMETER: [colorValueFaktor] = "
				+ colorValueFaktor);
		this.colorValueFaktor = colorValueFaktor;
	}

	/**
	 * @return the relativeContrast
	 */
	public double getRelativeContrast() {
		return relativeContrast;
	}

	/**
	 * @param relativeContrast
	 *            the relativeContrast to set
	 */
	public void setRelativeContrast(double relativeContrast) {
		UTILITIES.trace("SET PARAMETER: [relativeContrast] = "
				+ relativeContrast);
		this.relativeContrast = relativeContrast;
	}

	/**
	 * @return the simplifyFaktor
	 */
	public int getSimplifyFaktor() {
		return simplifyFaktor;
	}

	/**
	 * @param simplifyFaktor
	 *            the simplifyFaktor to set
	 */
	public void setSimplifyFaktor(int simplifyFaktor) {
		UTILITIES.trace("SET PARAMETER: [simplifyFaktor] = " + simplifyFaktor);
		this.simplifyFaktor = simplifyFaktor;
	}

	/**
	 * @return the borderVariance
	 */
	public int getBorderVariance() {
		return borderVariance;
	}

	/**
	 * @param borderVariance
	 *            the borderVariance to set
	 */
	public void setBorderVariance(int borderVariance) {
		UTILITIES.trace("SET PARAMETER: [borderVariance] = " + borderVariance);
		this.borderVariance = borderVariance;
	}

	/**
	 * @return the scanVariance
	 */
	public int getScanVariance() {
		return scanVariance;
	}

	/**
	 * @param scanVariance
	 *            the scanVariance to set
	 */
	public void setScanVariance(int scanVariance) {
		UTILITIES.trace("SET PARAMETER: [scanVariance] = " + scanVariance);
		this.scanVariance = scanVariance;
	}

	/**
	 * @return the trainOnlyUnknown
	 */
	public boolean isTrainOnlyUnknown() {
		return trainOnlyUnknown;
	}

	/**
	 * @param trainOnlyUnknown
	 *            the trainOnlyUnknown to set
	 */
	public void setTrainOnlyUnknown(boolean trainOnlyUnknown) {
		this.trainOnlyUnknown = trainOnlyUnknown;
	}

}