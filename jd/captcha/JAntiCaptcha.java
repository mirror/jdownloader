package jd.captcha;

import java.awt.GridBagLayout;
import java.awt.Image;
import java.io.File;
import java.io.FileFilter;
import java.io.StringWriter;
import java.util.Vector;
import java.util.logging.Logger;

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

/**
 * Diese Klasse stellt alle public Methoden zur captcha erkennung zur Verfügung.
 * Sie verküpft Letter und captcha Klassen. Gleichzeitig dient sie als
 * Parameter-Dump.
 * 
 * @author coalado
 * 
 */
public class JAntiCaptcha {
	/**
	 * Logger
	 */
	private static Logger logger = UTILITIES.getLogger();

	/**
	 * Prozentwert. Ab dieser Schwelle an Korektheit wird ein Letter als 100%
	 * richtig gewertet
	 */
	private double letterSearchLimitValue = 0.15;

	/**
	 * Name des Authors der entsprechenden methode. Wird aus der jacinfo.xml
	 * Datei geladen
	 */
	private String methodAuthor;

	/**
	 * Methodenname. Wird aus der jacinfo.xml geladen
	 */
	private String methodName;

	/**
	 * Bildtyp. Falls dieser von jpg unterschiedlich ist muss zuerst konvertiert
	 * werden
	 */
	private String imageType;

	/**
	 * Anzahl der Buchstaben im Captcha. Wird aus der jacinfo.xml gelesen
	 */
	private int letterNum;

	/**
	 * Pfad zum SourceBild (Standalone). Wird aus der jacinfo.xml gelesen
	 */
	private String sourceImage;

	/**
	 * Vector für die Befehle die für die Vorverarbeitung des Captchas verwendet
	 * werden. (script.jas)
	 */
	private Vector<String[]> captchaPrepareCommands;

	/**
	 * Vector für die Befehle die für die Ekennung allgemein gelten (script.jas)
	 */
	private Vector<String[]> jacCommands;

	/**
	 * Vector für die Befehle die für die Zeichenerkennung und verarbeitung
	 * gelten. (script.jas)
	 */
	private Vector<String[]> letterCommands;

	/**
	 * Pfad zur Resulotfile. dort wird der Captchacode hineingeschrieben.
	 * (standalone mode)
	 */
	private String resultFile;

	/**
	 * XML Dokument für die MTH File
	 */

	private Document mth;

	/**
	 * Vector mit den Buchstaben aus der MTHO File
	 */
	private Vector<Letter> letterDB;

	/**
	 * Parameter: Linke Pixelgrenze ab der Nach Buchstaben gesucht wird
	 */
	private int leftPadding = 0;

	/**
	 * Parameter: Gibt die Anzahl der Reihen(Pixel) an die zur peak detection
	 * verwendet werden sollen
	 */
	private int gapWidthPeak = 1;

	/**
	 * Gibt an ob 0: Helligkeit 1: Stättigung 2: Farbton Zur Bildverarbeitung
	 * als Kriterum verwendet werden sollen
	 */
	private int HSBType = 2;

	/**
	 * Parameter: Gibt die Anzahl der reihen an die zur Average Detection
	 * verwendet werden sollen
	 */
	private int gapWidthAverage = 1;

	/**
	 * Parameter: gapAndAverageLogic=true: Es werden Lücken verwendet bei denen
	 * Peak und Average detection zusammenfallen (AND) gapAndAverageLogic=false:
	 * Es werden sowohl peak als Auch Average Lücken verwendet (nur in
	 * Ausnahmefällen) (OR)
	 */
	private boolean gapAndAverageLogic = true;

	/**
	 * Parameter: Der Kontrastwert für die Average Detection. ~1
	 */
	private double gapDetectionAverageContrast = 1.3;

	/**
	 * Parameter: Der Kontrastwert für die Peak Detection. ~0.25
	 */
	private double gapDetectionPeakContrast = 0.25;

	/**
	 * Parameter: Average Detection verwenden
	 */
	private boolean useAverageGapDetection = false;

	/**
	 * Parameter: Peak Detection verwenden
	 */
	private boolean usePeakGapdetection = true;

	/**
	 * Parameter: Kontrollwert über die minimale Buchstabenbreite
	 */
	private int minimumLetterWidth = 10;

	/**
	 * Parameter: Wert gibt an um welchen faktor die Fingerprints verkleinert
	 * werden. So groß wie möglich, so klein wie nötig Wenn dieser Wert
	 * verändert wird, wrd die MTH File unbrauchbar und muss neu trainiert
	 * werden
	 */
	private int simplifyFaktor = 1;

	/**
	 * Parameter: Wert gibt meistens den höchsten möglichen farbwert an. Durch
	 * diesen Wert wird geteilt um die Dateigröße der MTH kleiner zu halten
	 * 
	 */
	private int colorValueFaktor = 0xffffff;

	/**
	 * Parameter: Allgemeiner Bildkontrastparameter ~0.8 bis 1.2
	 */
	private double relativeContrast = 0.85;

	/**
	 * Parameter: Gibt die Tolleranz beim Säubern des Hintergrunds an ~0.05-0.5
	 */
	private double backgroundSampleCleanContrast = 0.1;

	/**
	 * Parameter: Gibt für dieverse SW Umwandlungen den Schwellwert an
	 */
	private double blackPercent = 0.1;

	/**
	 * Werte-Array Wird gaps != null, so werden die Werte als Trennpositionen
	 * für die letter detection verwendet. Alle anderen Erkennungen werden dann
	 * ignoriert
	 */
	private int[] gaps;

	/**
	 * ordnername der methode
	 */
	private String method;

	/**
	 * Gibt an ob beim Training nur falscherkannte Buchstaben gespeichert werden
	 * (true) oder alle (False)
	 */
	private boolean trainOnlyUnknown = true;

	/**
	 * Drei fenster die eigentlich nur zur entwicklung sind um Basic GUI
	 * Elemente zu haben
	 */
	@SuppressWarnings("unused")
	private BasicWindow frame1;

	@SuppressWarnings("unused")
	private BasicWindow frame2;

	@SuppressWarnings("unused")
	private BasicWindow frame3;

	/**
	 * Parameter: Scan-Parameter. Gibt an um wieviele Pixel sich Letter und
	 * Vergleichsletter unterscheiden dürfen um verglichen zu werden. Hohe Werte
	 * machen das ganze Langsam
	 */
	private int borderVariance = 0;

	/**
	 * Parameter: Scan-Parameter. Gibt an um wieviele Pixel Letter und
	 * Vergleichsletter gegeneinander verschoben werden um die beste
	 * Übereinstimung zu finden. Hohe werte verlangemmen die Erkennung deutlich
	 */
	private int scanVariance = 0;

	/**
	 * Pfad zum captcha Root
	 */
	public static String jacPath = "captcha";

	public JAntiCaptcha(String method) {
		this.method = method;
		if (isMethodPathValid(method)) {
			getJACInfo();
			parseScriptFile();
			executeParameterCommands();
			loadMTHFile();
		}

	}

	/**
	 * Prüft ob der übergebene Methodname verfügbar ist.
	 * 
	 * @param method
	 * @return true/false
	 */
	private boolean isMethodPathValid(String method) {
		String[] path = { jacPath, "methods", method };
		logger.info("Methods at "+new File(UTILITIES.getFullPath(path)).getAbsolutePath());
		if (!new File(UTILITIES.getFullPath(path)).exists()) {
			logger.severe("Die Methode " + method
					+ " kann nicht gefunden werden. JAC Pfad falsch?");
			return false;
		}
		return true;

	}

	/**
	 * Diese Methode ist eine TestMethode. Sie kann aufgerufen werden wenn JAC
	 * Standalone laufen soll
	 */
	public void executeStandaloneCode() {
		if (this.getResultFile() != null && this.getSourceImage() != null
				&& new File(this.getSourceImage()).exists()) {
			String hash = UTILITIES
					.getLocalHash(new File(this.getSourceImage()));
			logger.info(hash);
			if (hash.equals("dab07d2b7f1299f762454cda4c6143e7")) {
				logger.info("BOT ERKANNT");
				UTILITIES.writeLocalFile(new File(this.getResultFile()), "");

			} else {
				Image captchaImage = UTILITIES.loadImage(new File(this
						.getSourceImage()));
				Captcha captcha = createCaptcha(captchaImage);
				String code = this.checkCaptcha(captcha);
				if (code.indexOf("null") >= 0) {
					logger.info("BOT ERKANNT");
					code = "";
					UTILITIES.writeLocalFile(new File(this.getResultFile()),
							code);

				} else {
					UTILITIES.writeLocalFile(new File(this.getResultFile()),
							code);
					String fileName = new File(this.getSourceImage()).getName();

					String[] newPath = { jacPath, "methods", method, "checked",
							captcha.getValityValue() + "_" + fileName };
					new File(UTILITIES.getFullPath(newPath)).getParentFile()
							.mkdirs();
					new File(this.getSourceImage()).renameTo(new File(UTILITIES
							.getFullPath(newPath)));
				}
			}

		}
	}

	/**
	 * Diese Methode führt die zuvor eingelesenen JAC Script Befehle aus
	 * 
	 */
	private void executeParameterCommands() {
		if (jacCommands == null || jacCommands.size() == 0) {
			logger.info("KEINE JAC COMMANDS");
			return;
		}
		logger.info("Execute Script.jas Parameter scripts");
		try {
			for (int i = 0; i < jacCommands.size(); i++) {
				String[] cmd = jacCommands.elementAt(i);
				
				if (cmd[0].equals("parameter")) {
					if (cmd[1].equalsIgnoreCase("lettersearchlimitvalue"))
						this.setLetterSearchLimitValue(Double
								.parseDouble(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("trainonlyunknown"))
						this.setTrainOnlyUnknown(cmd[2].equals("true"));
					else if (cmd[1].equalsIgnoreCase("scanvariance"))
						this.setScanVariance(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("bordervariance"))
						this.setBorderVariance(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("leftpadding"))
						this.setLeftPadding(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("simplifyfaktor")){						
						this.setSimplifyFaktor(Integer.parseInt(cmd[2]));
					}
					else if (cmd[1].equalsIgnoreCase("letternum"))
						this.setLetterNum(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("sourceimage"))
						this.setSourceImage(cmd[2]);
					else if (cmd[1].equalsIgnoreCase("resultfile"))
						this.setResultFile(cmd[2]);
					else if (cmd[1].equalsIgnoreCase("gapwidthpeak"))
						this.setGapWidthPeak(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("gapwidthaverage"))
						this.setGapWidthAverage(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("hsbtype"))
						this.setHSBType(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("gapandaveragelogic"))
						this.setGapAndAverageLogic(cmd[2].equals("true"));
					else if (cmd[1].equalsIgnoreCase(
							"gapdetectionaveragecontrast"))
						this.setGapDetectionAverageContrast(Double
								.parseDouble(cmd[2]));
					else if (cmd[1].equalsIgnoreCase(
							"gapdetectionpeakcontrast"))
						this.setGapDetectionPeakContrast(Double
								.parseDouble(cmd[2]));
					else if (cmd[1].equalsIgnoreCase(
							"useaveragegapdetection"))
						this.setUseAverageGapDetection(cmd[2].equals("true"));
					else if (cmd[1].equalsIgnoreCase("usepeakgapdetection"))
						this.setUsePeakGapdetection(cmd[2].equals("true"));
					else if (cmd[1].equalsIgnoreCase("minimumletterwidth"))
						this.setMinimumLetterWidth(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("colorvaluefaktor"))
						this.setColorValueFaktor(Integer.parseInt(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("relativecontrast"))
						this.setRelativeContrast(Double.parseDouble(cmd[2]));
					else if (cmd[1].equalsIgnoreCase(
							"backgroundsamplecleancontrast"))
						this.setBackgroundSampleCleanContrast(Double
								.parseDouble(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("blackpercent"))
						this.setBlackPercent(Double.parseDouble(cmd[2]));
					else if (cmd[1].equalsIgnoreCase("gaps")) {
						cmd[2] = cmd[2].substring(1, cmd[2].length() - 2);
						String[] gaps = cmd[2].split("\\,");
						int[] newGaps = new int[gaps.length];
						for (int ii = 0; ii < gaps.length; ii++)
							newGaps[ii] = Integer.parseInt(gaps[ii]);
						this.setGaps(newGaps);
					}else{
						logger.severe("Error in " + method + "/+script.jas : Parameter not valid: "+cmd[1]+" = "+cmd[2]);
					}

				} else {
					logger.severe("Syntax Error in " + method + "/+script.jas");
				}
			}
		} catch (Exception e) {
			logger.severe("Syntax Error in " + method + "/+script.jas");
		}

	}

	/**
	 * Diese Methode führt die zuvor eingelesenen JAc Script Prepare Befehle aus
	 * 
	 */
	private void executePrepareCommands(Captcha captcha) {
		if (captcha.isPrepared()) {
			// ISt schon prepared
			return;
		}
		logger.info("Execute Script.jas Prepare scripts");
		captcha.setPrepared(true);
		String[] params;
		try {
			for (int i = 0; i < this.captchaPrepareCommands.size(); i++) {
				String[] cmd = captchaPrepareCommands.elementAt(i);	
				logger.info("Execute Function: "+cmd[1]+"("+cmd[2]+")");
				if (cmd[0].equals("parameter")) {
					logger.severe("Syntax Error in " + method + "/+script.jas");
					// captchaPrepareCommands

				} else if (cmd[0].equals("function") && cmd[2] == null) {

					if (cmd[1].equalsIgnoreCase("invert")) {
						captcha.invert();
						continue;
					}
					else if (cmd[1].equalsIgnoreCase("toBlackAndWhite")) {
						captcha.toBlackAndWhite();
						continue;
					}
					else if (cmd[1].equalsIgnoreCase("clean")) {
						captcha.clean();
						continue;
					}else{
						logger.severe("Error in " + method + "/+script.jas : Function not valid: "+cmd[1]+"("+cmd[2]+")");
					}
				} else if (cmd[0].equals("function")
						&& (params = cmd[2].split("\\,")).length == 1) {

					if (cmd[1].equalsIgnoreCase("toBlackAndWhite")) {
						captcha.toBlackAndWhite(Double.parseDouble(params[0]
								.trim()));
						continue;
					}
					else if (cmd[1].equalsIgnoreCase("reduceWhiteNoise")) {
						captcha.reduceWhiteNoise(Integer.parseInt(params[0]
								.trim()));
						continue;
					}
					else if (cmd[1].equalsIgnoreCase("reduceBlackNoise")) {
						captcha.reduceBlackNoise(Integer.parseInt(params[0]
								.trim()));
						continue;
					}
					else if (cmd[1].equalsIgnoreCase("blurIt")) {
						captcha.blurIt(Integer.parseInt(params[0].trim()));
						continue;
					}
					else if (cmd[1].equalsIgnoreCase("sampleDown")) {
						captcha.sampleDown(Integer.parseInt(params[0].trim()));
						continue;
					}
					
					else if (cmd[1].equalsIgnoreCase("saveImageasJpg")) {
						
						captcha.saveImageasJpg(new File(params[0].trim()));
						continue;
					}else{
						logger.severe("Error in " + method + "/+script.jas : Function not valid: "+cmd[1]+"("+cmd[2]+")");
					}

				} else if (cmd[0].equals("function")
						&& (params = cmd[2].split("\\,")).length == 2) {
					if (cmd[1].equalsIgnoreCase("reduceWhiteNoise")) {
						captcha.reduceWhiteNoise(Integer.parseInt(params[0]
								.trim()), Double.parseDouble(params[1].trim()));
						continue;
					}
					else if (cmd[1].equalsIgnoreCase("reduceBlackNoise")) {
						captcha.reduceBlackNoise(Integer.parseInt(params[0]
								.trim()), Double.parseDouble(params[1].trim()));
						continue;
					}
					else if (cmd[1].equalsIgnoreCase("sampleDown")) {
						captcha.sampleDown(Integer.parseInt(params[0].trim()),
								Double.parseDouble(params[1].trim()));
						continue;
					}else{
						logger.severe("Error in " + method + "/+script.jas : Function not valid: "+cmd[1]+"("+cmd[2]+")");
					}

				} else if (cmd[0].equals("function")
						&& (params = cmd[2].split("\\,")).length == 3) {
					if (cmd[1].equalsIgnoreCase("cleanWithMask")) {
						captcha.cleanWithMask(this.createCaptcha(UTILITIES
								.loadImage(new File(params[0].trim()))),
								Integer.parseInt(params[0].trim()), Integer
										.parseInt(params[1].trim()));
						continue;
					}else{
						logger.severe("Error in " + method + "/+script.jas : Function not valid: "+cmd[1]+"("+cmd[2]+")");
					}

				} else if (cmd[0].equals("function")
						&& (params = cmd[2].split("\\,")).length == 4) {

					if (cmd[1].equalsIgnoreCase("cleanBackgroundBySample")) {
						captcha.cleanBackgroundBySample(Integer
								.parseInt(params[0].trim()), Integer
								.parseInt(params[1].trim()), Integer
								.parseInt(params[2].trim()), Integer
								.parseInt(params[3].trim()));
						continue;
					}

				} else if (cmd[0].equals("function")
						&& (params = cmd[2].split("\\,")).length == 5) {

				} else if (cmd[0].equals("function")
						&& (params = cmd[2].split("\\,")).length == 6) {

				}
			}
		} catch (Exception e) {
			logger.severe("Syntax Error in " + method + "/script.jas");
			// e.printStackTrace();
		}
		//BasicWindow.showImage(captcha.getImage());
	}

	/**
	 * Diese Methode liest das script.jas ein. und parsed es
	 * 
	 */
	private void parseScriptFile() {
		logger.info("parsing Script.jas");
		String[] path = { jacPath, "methods", method, "script.jas" };
		String script = UTILITIES.getLocalFile(new File(UTILITIES
				.getFullPath(path)));
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

			if (lines[i].indexOf("#") == 0 || lines[i].trim().length() < 3) {
				// leere Zeile, oder Comment
				continue;
			}
			if (!lines[i].substring(lines[i].length() - 1).equals(";")) {
				logger.info("ERROR : " + method
						+ "/script.jas: Syntax error (; missing?) near line "
						+ i + ": " + lines[i]);
				return;
			}
			lines[i] = lines[i].substring(0, lines[i].length() - 1);
			if ((startAt = lines[i].indexOf("captcha.prepare.")) == 0) {
				pcmd = parseCommand(lines[i].substring(startAt + 16));

				localCaptchaPrepareCommands.add(pcmd);
			} else if ((startAt = lines[i].indexOf("param.")) == 0) {
				pcmd = parseCommand(lines[i].substring(startAt + 6));
				if (!pcmd[0].equals("parameter")) {
					logger.info("ERROR : " + method
							+ "/script.jas: Syntax error near line " + i + ": "
							+ lines[i]);
				}

				localJacCommands.add(pcmd);
			} else if ((startAt = lines[i].indexOf("letter.")) == 0) {
				pcmd = parseCommand(lines[i].substring(startAt + 7));

				localLetterCommands.add(pcmd);
			} else {
				logger.info("ERROR : " + method
						+ "/script.jas: Syntax error near line " + i + ": "
						+ lines[i]);
			}
		}
		this.setCaptchaPrepareCommands(localCaptchaPrepareCommands);
		this.setParameterCommands(localJacCommands);
		this.setLetterCommands(localLetterCommands);
	}

	/**
	 * Diese methode nimmt eine zeile der script.jas entgegen und parsed sie
	 * 
	 * @param cmd
	 *            Zeile eines *.jas scripts
	 * @return Array:{Befehltyp, befehl,parameter}
	 */
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
			if (ret[2].length() == 0)
				ret[2] = null;
		} else if ((matches = UTILITIES.getMatches(cmd, "#°()#")) != null) {
			ret[0] = "function";
			ret[1] = matches[0].trim();
			ret[2] = null;
		}
		return ret;
	}

	/**
	 * Diese methode wird aufgerufen um alle captchas im Ordner
	 * methods/Methodname/captchas zu trainieren
	 * 
	 */
	public void trainAllCaptchas() {

		int successFull = 0;
		int total = 0;
		File[] images = getImages();
		int newLetters;
		for (int i = 0; i < images.length; i++) {

			newLetters = trainCaptcha(UTILITIES.toJPG(images[i]), getLetterNum());
			UTILITIES.toJPG(images[i]).delete();
			logger.info("Erkannt: " + newLetters + "/" + getLetterNum());
			if (newLetters > 0) {
				successFull += newLetters;
				total += getLetterNum();
				logger.info("Erkennungsrate: " + ((100 * successFull / total)));
			}
			//UTILITIES.wait(10000);
		}
		
	}

	/**
	 * MTH File wird geladen und verarbeitet
	 * 
	 */
	private void loadMTHFile() {
		String[] path = { jacPath, "methods", method, "letters.mth" };
		if (new File(UTILITIES.getFullPath(path)).exists() == false) {
			UTILITIES.writeLocalFile(new File(UTILITIES.getFullPath(path)),
					"<jDownloader/>");
			logger.severe("MTH FILE NOT AVAILABLE. Created: " + path);
		}
		mth = UTILITIES.parseXmlFile(UTILITIES.getFullPath(path), false);
		createLetterDBFormMTH();
		sortLetterDB();

	}

	/**
	 * Aus gründen der geschwindigkeit wird die MTH XMl in einen vector
	 * umgewandelt
	 * 
	 */
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
					// logger.info(childNode.getNodeName());
					// logger.info(childNode.getTextContent());
					// logger.info(att.getNamedItem("captchaHash").getNodeValue());
					// logger.info(att.getNamedItem("value").getNodeValue());
					tmp = new Letter();
					tmp.setOwner(this);
					if (!tmp.setTextGrid(childNode.getTextContent()))
						continue;
					;

					tmp.setSourcehash(att.getNamedItem("captchaHash")
							.getNodeValue());
					tmp.setDecodedValue(att.getNamedItem("value")
							.getNodeValue());
					tmp.setBadDetections(Integer.parseInt(UTILITIES.getAttribute(childNode, "bad")));
					tmp.setGoodDetections(Integer.parseInt(UTILITIES.getAttribute(childNode, "good")));
					letterDB.add(tmp);
				}
			}
		} catch (Exception e) {
			logger
					.severe("Fehler mein lesen der MTHO Datei!!. Methode kann nicht funktionieren!");

		}
	}

	/*
	 * Die Methode parst die jacinfo.xml
	 */
	private void getJACInfo() {
		String[] path = { jacPath, "methods", method, "jacinfo.xml" };
		if (!new File(UTILITIES.getFullPath(path)).exists()) {
			logger.severe("" + UTILITIES.getFullPath(path) + " is missing");
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

	/**
	 * Veraltete Prepare Funktionen. Die müssen noch hier bleiben bis ich die
	 * entsprechenen jas-script erstellt habe
	 */

	// private void prepare(Captcha captcha) {
	// captcha.prepared++;
	// if (captcha.prepared > 1) {
	// logger.severe("Prepare wird doppelt ausgeführt!!!");
	// return;
	// }
	// logger.info("prepare");
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
	// private void prepare(Captcha captcha) {
	// captcha.prepared++;
	// if (captcha.prepared > 1) {
	// logger.severe("Prepare wird doppelt ausgeführt!!!");
	// return;
	// }
	// logger.info("prepare");
	// // //BasicWindow.showImage(captcha.getImage(2),"Original als
	// // Helligkeitsabbildung");
	// captcha.cleanBackgroundBySample(3, 25, 3, 3);
	// // //BasicWindow.showImage(captcha.getImage(2),"Hintergrundfarbe
	// // entfernen");
	// captcha.cleanWithMask(Captcha.getCaptcha(loadImage(new File(
	// "svzgridmask.jpg"))), 3, 3);
	// // //BasicWindow.showImage(captcha.getImage(2),"Maskenfilter: Muster
	// // entfernen");
	// captcha.toBlackAndWhite(1.1);
	// // //BasicWindow.showImage(captcha.getImage(2),"BW-Convert: mit Faktor
	// // 1.1
	// // in SW-Bild umwandlen");
	// captcha.reduceWhiteNoise(6);
	// // //BasicWindow.showImage(captcha.getImage(2),"ReduceNoise: Weiße
	// // Störungen über einen 6 Pixel Durchmesser entfernen");
	//
	// // //BasicWindow.showImage(captcha.getImage(2),"ReduceNoise: Weiße
	// // Störungen über einen 6 Pixel Durchmesser entfernen");
	// captcha.toBlackAndWhite(0.2);
	// // //BasicWindow.showImage(captcha.getImage(2),"BW-Convert: mit Faktor
	// // 0.2. Elemente die nicht wirklich Schwarz sind werden entfernt");
	// captcha.reduceWhiteNoise(3);
	// // //BasicWindow.showImage(captcha.getImage(2),"ReduceNoise: Weiße
	// // Störungen über einen 3 Pixel Durchmesser entfernen. Buchstaben werden
	// // dicker");
	// captcha.toBlackAndWhite(0.2);
	//
	// // //BasicWindow.showImage(captcha.getImage(2),"BW-Convert: mit Faktor
	// // 0.2. Elemente die nicht wirklich Schwarz sind werden entfernt");
	// captcha.reduceBlackNoise(3, 0.9);
	// // //BasicWindow.showImage(captcha.getImage(2),"ReduceNoise: Schwarze
	// // Störungen, Flecken außerhalb der Buchstaben entfernen");
	// captcha.toBlackAndWhite(1.2);
	// // //BasicWindow.showImage(captcha.getImage(2),"BW-Convert: mit Faktor
	// // 1.2. Elemente die leicht grau sind werden zu schwarz");
	// //
	// // UTILITIES.wait(5000);
	//
	// }
	/**
	 * Diese methode trainiert einen captcha
	 * 
	 * @param captchafile
	 *            File zum Bild
	 * @param letterNum
	 *            Anzahl der Buchstaben im captcha
	 * @return int -1: Übersprungen Sonst: anzahl der richtig erkanten Letter
	 */
	
	private Document createXMLFromLetterDB(){
		
		Document xml=UTILITIES.parseXmlString("<jDownloader/>",false);
		Letter letter;
		for (int i = 0; i < letterDB.size(); i++) {
			letter=letterDB.elementAt(i);
		Element element = xml.createElement("letter");
		xml.getFirstChild().appendChild(element);
		element.appendChild(xml.createTextNode(letter
				.getPixelString()));
		
		element.setAttribute("value", letter.getDecodedValue());
		element.setAttribute("captchaHash", letter.getSourcehash());
		element.setAttribute("good", letter.getGoodDetections()+"");
		element.setAttribute("bad", letter.getBadDetections()+"");

		}
		return xml;
		
	}
	private int trainCaptcha(File captchafile, int letterNum) {
// Lade das Bild
		Image captchaImage = UTILITIES.loadImage(captchafile);
//Erstelle hashwert
		String captchaHash = UTILITIES.getLocalHash(captchafile);
		
		//Prüfe ob dieser captcha schon aufgenommen wurde und überspringe ihn falls ja
		if (isCaptchaInMTH(captchaHash)) {
			logger.info("ERROR captcha schon aufgenommen" + captchafile);
			return -1;
		}
		//captcha erstellen
		Captcha captcha = createCaptcha(captchaImage);

		
		String code = null;
		String guess = "";
		//Zeige das OriginalBild
		if (frame3 != null) {
			frame3.destroy();
		}
		frame3 = BasicWindow.showImage(captcha.getImage(1));
		frame3.setLocationByScreenPercent(50, 60);
		// Führe das Prepare aus
		executePrepareCommands(captcha);
		//Hole die letters aus dem neuen captcha
		Letter[] letters = captcha.getLetters(letterNum);
		// prüfe auf Erfolgreiche Lettererkennung
		if (letters == null) {
			String[] path = { jacPath, "methods", method, "detectionErrors1",
					captchafile.getName() };
			new File(UTILITIES.getFullPath(path)).getParentFile().mkdirs();
			captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
			logger.severe("2. Lettererkennung ist fehlgeschlagen!");
			return -1;

		}
		//Zeige das After-prepare Bild an
		if (frame1 != null) {
			frame1.destroy();
		}
		frame1 = BasicWindow.showImage(captcha.getImageWithGaps(1));
		frame1.setLocationByScreenPercent(50, 20);
		//Zeige die einzellnen letters an
		if (frame2 != null) {
			frame2.destroy();
		}	
		frame2 = new BasicWindow();
		frame2.setTitle("Letters");
		frame2.setLayout(new GridBagLayout());
		frame2.setSize(300, 300);
		frame2.setAlwaysOnTop(true);
		frame2.setLocationByScreenPercent(50, 5);	
		for (int i = 0; i < letters.length; i++) {
			frame2.add(new ImageComponent(letters[i].getImage(this
					.getSimplifyFaktor())), UTILITIES.getGBC(i * 2, 0, 2, 2));
			frame2.add(new ImageComponent(letters[i].getImage()), UTILITIES.getGBC(
					i * 2, 2, 2, 2));
		}
		frame2.setVisible(true);
		frame2.pack();
		frame2.setSize(300, frame2.getSize().height);
		
		//Decoden. checkCaptcha verwendet dabei die gecachte Erkennung der letters
		guess = checkCaptcha(captcha);
		
		logger.info("Decoded Captcha: " + guess + " Vality: "
				+ captcha.getValityPercent()+" "+(this.getLetterSearchLimitValue()));
		if (guess == null) {
			String[] path = { jacPath, "methods", method, "detectionErrors2",
					captchafile.getName() };
			new File(UTILITIES.getFullPath(path)).getParentFile().mkdirs();
			captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
			logger.severe("Letter erkennung fehlgeschlagen");
			return -1;

		}
		if(captcha.getValityPercent()> (this.getLetterSearchLimitValue()*100)){
		if (captcha.getValityValue() > 0) {
			code = UTILITIES
					.prompt("Bitte Captcha Code eingeben (Press enter to confirm "
							+ guess);

		}
		}else{
			code=guess;
		}
	
		if (code == null) {
			String[] path = { jacPath, "methods", method, "detectionErrors3",
					captchafile.getName() };
			new File(UTILITIES.getFullPath(path)).getParentFile().mkdirs();
			captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
			logger.severe("Captcha Input error");
			return -1;
		}
		if (code.length() == 0) {
			code = guess;
		}
		if (code.length() != letters.length) {
			String[] path = { jacPath, "methods", method, "detectionErrors4",
					captchafile.getName() };
			new File(UTILITIES.getFullPath(path)).getParentFile().mkdirs();
			captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
			logger.severe("Captcha Input error3");
			return -1;
		}
		UTILITIES.setProperty(captchaHash, code);
		int ret = 0;
		for (int i = 0; i < letters.length; i++) {
			if (letters[i] == null || letters[i].getWidth() < 2
					|| letters[i].getHeight() < 2) {
				String[] path = { jacPath, "methods", method,
						"detectionErrors5", captchafile.getName() };
				new File(UTILITIES.getFullPath(path)).getParentFile().mkdirs();
				captchafile.renameTo(new File(UTILITIES.getFullPath(path)));
				logger.severe("Letter detection error");
				return -1;
			}
			frame2.add(new ImageComponent(letters[i].getImage()));
			frame2.repack();

			

			if (code.length() > i
					&& guess.length() > i
					&& code.substring(i, i + 1).equals(
							guess.substring(i, i + 1))) {
				ret++;
				
				
				if(captcha.getDecodedLetters()[i]!=null){
					captcha.getDecodedLetters()[i].markGood();
				}
				if (!isTrainOnlyUnknown()) {
					
					
					letters[i].setOwner(this);
					letters[i].setTextGrid(letters[i].getPixelString());
					letters[i].setSourcehash(captchaHash);
					letters[i].setDecodedValue(code.substring(i, i + 1));
					letterDB.add(letters[i]);
				

				}
			} else {
				if(captcha.getDecodedLetters()[i]!=null &&captcha.getDecodedLetters()[i].getValityPercent()<=20){
					captcha.getDecodedLetters()[i].markBad();
				}
				letters[i].setOwner(this);
				letters[i].setTextGrid(letters[i].getPixelString());
				letters[i].setSourcehash(captchaHash);
				letters[i].setDecodedValue(code.substring(i, i + 1));
				letterDB.add(letters[i]);
				
			}
			// mth.appendChild(element);
		}
		sortLetterDB();
		saveMTHFile();
		return ret;
	}
/**
 * Sortiert die letterDB Nach den bad Detections. Der Sortieralgo gehört dringend überarbeitet!!!
 * Diese Sortieren hilft die GUten Letter zuerst zu prüfen.
 * @TODO Sortoer ALGO ändern. zu langsam!!
 */
	private void sortLetterDB(){
	
			Vector<Letter> ret = new Vector<Letter>();
			for (int i = 0; i < letterDB.size(); i++) {
				Letter akt = letterDB.elementAt(i);
				for (int x = 0; x < ret.size(); x++) {

					if ((akt.getGoodDetections()-5*akt.getBadDetections())>(ret.elementAt(x).getGoodDetections()-5*ret.elementAt(x).getBadDetections())) {
						ret.add(x, akt);
						akt = null;
						break;
					}
				}
				if (akt != null)
					ret.add(akt);

			}
		
			letterDB=ret;
	
	}
	/**
	 * Factory Methode zur Captcha erstellung
	 * 
	 * @param captchaImage
	 *            Image instanz
	 * @return captcha
	 */
	public Captcha createCaptcha(Image captchaImage) {
		if(captchaImage.getWidth(null)<=0 || captchaImage.getHeight(null)<=0) {
			logger.severe("Image Dimensionen zu klein. Image hat keinen Inahlt. Pfad/Url prüfen!");
		}
		Captcha ret = Captcha.getCaptcha(captchaImage, this);
		ret.setOwner(this);
		return ret;
	}

	/**
	 * Speichert die MTH File
	 * 
	 */
	public void saveMTHFile() {
		try {
			Transformer transformer = TransformerFactory.newInstance()
					.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");

			// initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(createXMLFromLetterDB());

			transformer.transform(source, result);

			String xmlString = result.getWriter().toString();


			String[] path = { jacPath, "methods", method, "letters.mth" };

			if (!UTILITIES.writeLocalFile(
					new File(UTILITIES.getFullPath(path)), xmlString)) {
				logger.severe("MTHO file Konnte nicht gespeichert werden");
			}

		} catch (TransformerException e) {
			e.printStackTrace();
		}

	}

	public static String getCaptchaCode(Image img, String method) {
		JAntiCaptcha jac = new JAntiCaptcha(method);
		// BasicWindow.showImage(img);
		Captcha cap = jac.createCaptcha(img);
		// BasicWindow.showImage(cap.getImageWithGaps(2));
		String ret = jac.checkCaptcha(cap);
		logger.info(ret);
		return ret;
	}

	/**
	 * Prüft ob der übergeben hash in der MTH file ist
	 * 
	 * @param captchaHash
	 * @return true/false
	 */
	private boolean isCaptchaInMTH(String captchaHash) {
		if (letterDB == null)
			return false;

		for (int i = 0; i < letterDB.size(); i++) {
			if (letterDB.elementAt(i).getSourcehash().equals(captchaHash))
				return true;
		}

		return false;

	}

	/**
	 * Gibt den erkannten CaptchaText zurück
	 * 
	 * @param captchafile
	 *            Pfad zum Bild
	 * @return CaptchaCode
	 */
	public String checkCaptcha(File captchafile) {
		logger.info("check " + captchafile);
		Image captchaImage = UTILITIES.loadImage(captchafile);
		Captcha captcha = createCaptcha(captchaImage);
		// captcha.printCaptcha();
		return checkCaptcha(captcha);
	}

	/**
	 * prüft den übergebenen Captcha und gibtd en Code als String zurück
	 * das lettersarray des Catchas wird dabei bearbeitet. es werden decoedvalue, avlityvalue und parent gesetzt
	 * @param captcha
	 *            Captcha instanz
	 * @return CaptchaCode
	 */
	public String checkCaptcha(Captcha captcha) {

		//Führe prepare aus
		executePrepareCommands(captcha);
		
		Letter[] newLetters = new Letter[this.getLetterNum()];
		String ret = "";
		long correct = 0;
		Letter akt;
		Letter[] letters = captcha.getLetters(getLetterNum());
		if (letters == null){
			return null;
		}
		for (int i = 0; i < letters.length; i++) {
			akt = getLetter(letters[i]);
			newLetters[i] = akt;
			if (akt == null) {
				letters[i].setDecodedValue("_");
				letters[i].setValityValue(Integer.MAX_VALUE);
				correct += Integer.MAX_VALUE;

			} else {
				letters[i].setParent(akt);
				letters[i].setDecodedValue(akt.getDecodedValue());
				letters[i].setValityValue(akt.getValityValue());
				correct += akt.getValityValue();
			}
			if (newLetters[i] != null) {
				ret += akt.getDecodedValue();
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

	/**
	 * Vergleicht a und b und gibt eine vergleichszahl zurück. Dabei werden a
	 * und b gegeneinander verschoben
	 * 
	 * @param a
	 *            Original Letter
	 * @param b
	 *            Vergleichsletter
	 * @return int 0(super)-0xffffff (ganz übel)
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

		for (int xx = scanXFrom; xx <= scanXTo; xx++) {
			for (int yy = scanYFrom; yy <= scanYTo; yy++) {
				bx = Math.max(0 - xx, 0);
				by = Math.max(0 - yy, 0);
				pixel = 0;
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

	/**
	 * Sucht in der MTH ANch dem besten übereinstimmenem letter
	 * 
	 * @param letter
	 *            (refferenz)
	 * @return Letter. Beste Übereinstimmung
	 */
	private Letter getLetter(Letter letter) {

		long bestValue = Long.MAX_VALUE;
		String bestResult = "_";
		String lastResult = "";
		Letter res = new Letter();
		int value;
		res.setOwner(this);
		res.setValityValue(Integer.MAX_VALUE);

		try {

			if (letterDB == null){
				logger.severe("letterDB nicht vorhanden");
				return null;
			}

			for (int i = 0; i < letterDB.size(); i++) {
				Letter tmp = letterDB.elementAt(i);
				
				if (Math.abs(tmp.getHeight() - letter.getHeight()) > borderVariance
						|| Math.abs(tmp.getWidth() - letter.getWidth()) > borderVariance) {
					continue;
				}
				value = scanCompare(letter, tmp);
				if (value < bestValue) {
					bestValue = value;
					bestResult = tmp.getDecodedValue();
					res = tmp;
					tmp.setValityValue(value);
//					UTILITIES.trace(tmp.getDecodedValue()+": "+value+" < "+(letterSearchLimitValue
//									* tmp.getMaxPixelValue()));
					if (value == 0
							|| (value <= letterSearchLimitValue
									* tmp.getMaxPixelValue() && bestResult
									.equals(lastResult))) {
						res = tmp;
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

	/**
	 * Liest den captchaornder aus
	 * 
	 * @return File Array
	 */
	private File[] getImages() {
		String[] path = { jacPath, "methods", method, "captchas" };

		File dir = new File(UTILITIES.getFullPath(path));

		if (dir == null || !dir.exists()) {
			logger.info("Image dir nicht gefunden");
		}

		File[] entries = dir.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				// logger.info(pathname.getName());
				if (pathname.getName().endsWith(".jpg")||pathname.getName().endsWith(".png")||pathname.getName().endsWith(".gif")) {

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
		logger.info("SET PARAMETER: [imageType] = " + imageType);
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
		logger.info("SET PARAMETER: [letterNum] = " + letterNum);
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
		logger.info("SET PARAMETER: [method] = " + method);
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
		logger.info("SET PARAMETER: [methodAuthor] = " + methodAuthor);
		this.methodAuthor = methodAuthor;
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
		logger.info("SET PARAMETER: [methodName] = " + methodName);
		this.methodName = methodName;
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
		logger.info("SET PARAMETER: [resultFile] = " + resultFile);
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
		logger.info("SET PARAMETER: [sourceImage] = " + sourceImage);
		this.sourceImage = sourceImage;
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
	public void setParameterCommands(Vector<String[]> jacCommands) {
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
		logger.info("SET PARAMETER: [gapAndAverageLogic] = "
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
		logger.info("SET PARAMETER: [gapDetectionAverageContrast] = "
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
		logger.info("SET PARAMETER: [gapDetectionPeakContrast] = "
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
		logger.info("SET PARAMETER: [gaps] = " + gaps.toString());
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
		logger.info("SET PARAMETER: [gapWidthAverage] = " + gapWidthAverage);
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
		logger.info("SET PARAMETER: [gapWidthPeak] = " + gapWidthPeak);
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
		logger.info("SET PARAMETER: [leftPadding] = " + leftPadding);
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
		logger.info("SET PARAMETER: [letterSearchLimitValue] = "
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
		logger.info("SET PARAMETER: [minimumLetterWidth] = "
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
		logger.info("SET PARAMETER: [useAverageGapDetection] = "
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
		logger.info("SET PARAMETER: [usePeakGapdetection] = "
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
		logger.info("SET PARAMETER: [backgroundSampleCleanContrast] = "
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
		logger.info("SET PARAMETER: [blackPercent] = " + blackPercent);
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
		logger.info("SET PARAMETER: [colorValueFaktor] = " + colorValueFaktor);
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
		logger.info("SET PARAMETER: [relativeContrast] = " + relativeContrast);
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
		logger.info("SET PARAMETER: [simplifyFaktor] = " + simplifyFaktor);
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
		logger.info("SET PARAMETER: [borderVariance] = " + borderVariance);
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
		logger.info("SET PARAMETER: [scanVariance] = " + scanVariance);
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

	/**
	 * @return the hSBType
	 */
	public int getHSBType() {
		return HSBType;
	}

	/**
	 * @param type
	 *            the hSBType to set
	 */
	public void setHSBType(int type) {
		HSBType = type;
	}

}