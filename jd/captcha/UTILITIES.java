package jd.captcha;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.MessageDigest;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;







/*
 * Diese Klasse beinhaltet mehrere Hilfsfunktionen
 */

public class UTILITIES {
	/** *********************DEBUG*************************** */
	/*
	 * public static String getPreString() Diese Funktion gibt einen Zeitstring
	 * zur anzeige aufd er Konsole aus
	 */
	public static String getPreString() {
		Calendar c = Calendar.getInstance();

		return c.get(Calendar.DAY_OF_MONTH) + "." + c.get(Calendar.MONTH) + "."
				+ c.get(Calendar.YEAR) + " - " + c.get(Calendar.HOUR_OF_DAY)
				+ ":" + c.get(Calendar.MINUTE) + ":" + c.get(Calendar.SECOND)
				+ " (" + c.get(Calendar.MILLISECOND) + ") : ";
	}

	/*
	 * public static void trace(String args) Diese Funktion gibt einen String
	 * auf der Konsole aus
	 */
	public static void trace(String args) {

		System.out.println(getPreString() + args);
	}

	/*
	 * public static void trace(int args) Diese Funktion gibt einen Integer auf
	 * der Konsole aus
	 */
	public static void trace(int args) {

		System.out.println(getPreString() + args);
	}

	/*
	 * public static void trace(Boolean args) Diese Funktion gibt einen boolean
	 * Wert aus
	 */
	public static void trace(Boolean args) {

		System.out.println(getPreString() + args);
	}

	/*
	 * public static void trace(float args) Diese Funktion gibt einen float Wert
	 * aus
	 */

	public static void trace(float args) {

		System.out.println(getPreString() + args);
	}

	/*
	 * public static void trace(double args) Diese Funktion gibt einen Double
	 * Wert aus
	 */
	public static void trace(double args) {

		System.out.println(getPreString() + args);
	}

	/*
	 * public static void trace(Object args) Diese Funktion gibt über die
	 * toString Methode ein Object auf der Konsole aus
	 */
	public static void trace(Object args) {
		if (args == null) {
			args = "[" + "] NULL";
		}

		System.out.println(getPreString() + args.toString());
	}

	/*
	 * public static void trace(String[] args) Diese Funktion gibt ein String
	 * Array aus
	 */
	public static void trace(String[] args) {
		int i;
		for (i = 0; i < args.length; i++) {

			System.out.println(getPreString() + i + ". " + args[i]);
		}
	}

	/*
	 * public static void trace(int[] args) Diese Funktion gibt ein Integer
	 * Array aus
	 */
	public static void trace(int[] args) {
		int i;
		for (i = 0; i < args.length; i++) {

			System.out.println(getPreString() + i + ". " + args[i]);
		}
	}

	/*
	 * public static void trace(Object[] args) Diese Funktion gibt ein Object
	 * Array aus
	 */
	public static void trace(Object[] args) {
		int i;
		for (i = 0; i < args.length; i++) {

			System.out.println(getPreString() + i + ". " + args[i].toString());
		}
	}

	/** ******************************SYSTEM******************************* */
	/*
	 * public static void wait(int ms) Hält den aktuellen Thread um ms
	 * Millisekunden auf pause
	 */
	public static void wait(int ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		e.printStackTrace();
		}
	}

	/*
	 * public static void runCommandAndWait(String command) Führt einen
	 * Shellbefehl aus und wartet bis der Befehl abgearbeitet wurde. Die
	 * rückgaben werden auf der Konsole ausgegeben
	 */

	public static void runCommandAndWait(String command) {
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(command);

			BufferedReader br = new BufferedReader(new InputStreamReader(pr
					.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				trace(line);

			}
		} catch (IOException e) {

		e.printStackTrace();
		}
	}

	/*
	 * public static String runCommandWaitAndReturn(String command) Ruft eine
	 * Shell-Befehl, wartet bis der Befehl beendet wurde und gibt die Rückgaben
	 * als String zurück
	 */
	public static String runCommandWaitAndReturn(String command) {
		String ret = "";
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(command);

			BufferedReader br = new BufferedReader(new InputStreamReader(pr
					.getInputStream()));
			String line;

			while ((line = br.readLine()) != null) {
				ret += line;

			}
		} catch (IOException e) {

		e.printStackTrace();
		}
		return ret;
	}

	/*
	 * public static void runCommand(String command) Führt einen Shell Befehl
	 * auf. Wartet nicht!
	 */
	public static void runCommand(String command) {
		if (command == null) {
			return;
		}
		try {
			Runtime rt = Runtime.getRuntime();
			rt.exec(command);

		} catch (IOException e) {

		e.printStackTrace();
		}
	}

	/** **********************************GUI************************************** */
	/*
	 * public static Image loadImage(File file) Lädt file als Bildatei und
	 * wartet bis file geladen wurde. gibt file als Image zurück
	 */
	public static Image loadImage(File file) {
		JFrame jf = new JFrame();
		Image img = jf.getToolkit().getImage(file.getAbsolutePath());
		MediaTracker mediaTracker = new MediaTracker(jf);
		mediaTracker.addImage(img, 0);
		try {
			mediaTracker.waitForID(0);
		} catch (InterruptedException e) {
			return null;
		}

		mediaTracker.removeImage(img);
		return img;
	}
	/*	public static GridBagConstraints getGBC(int x, int y, int width, int height)
	 * Gibt die default GridBAgConstants zurück
	 */
	public static GridBagConstraints getGBC(int x, int y, int width, int height) {
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.gridheight = height;
		gbc.weightx=0.1;
		gbc.weighty=0;
		gbc.fill = GridBagConstraints.NONE;
		gbc.insets = new Insets(1, 1, 1, 1);
		return gbc;
	};

	/*
	 * public static void showMessage(String msg) Zeigt msg als MessageBox an
	 */
	public static void showMessage(String msg) {
		JOptionPane.showMessageDialog(new JFrame(), msg);
	}

	/*
	 * public static boolean confirm(String msg) Zeigt einen Bestätigungsdialog
	 * an.
	 */
	public static boolean confirm(String msg) {
		return JOptionPane.showConfirmDialog(new JFrame(), msg, "Warning",
				JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0;
	}

	/*
	 * public static String prompt(String msg) Zeigt eine Text-Input Box an
	 */
	public static String prompt(String msg) {
		return JOptionPane.showInputDialog(msg);
	}

	/*
	 * public static String prompt(String msg, String defaultStr) Zeigt eine
	 * Text-Input Box an. inkl Default Eingabe
	 */
	public static String prompt(String msg, String defaultStr) {
		return JOptionPane.showInputDialog(msg, defaultStr);
	}

	/*
	 * public static File directoryChooser Zeigt einen Directory Chooser an
	 */

	public static File directoryChooser(String path) {
		JFileChooser fc = new JFileChooser();

		fc.setApproveButtonText("OK");
		if (path != null)
			fc.setCurrentDirectory(new File(path));

		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int r = fc.showOpenDialog(new JFrame());
		File ret = fc.getSelectedFile();

		return ret;

	}

	/*
	 * public static File fileChooser(String path) zeigt einen filechooser an
	 */

	public static File fileChooser(String path) {
		JFileChooser fc = new JFileChooser();

		fc.setApproveButtonText("OK");
		if (path != null)
			fc.setCurrentDirectory(new File(path));

		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		int r = fc.showOpenDialog(new JFrame());
		File ret = fc.getSelectedFile();

		return ret;

	}

	/** ******************************FILE-SYSTEM************************************ */
	//File Seperator (WIndows /) Linux \ ..
	public static String FS = System.getProperty("file.separator");
	// Application dir
	public static String ROOTDIR=System.getProperty("user.dir") + FS;
	
	/*	public static String getFullPath(String[] entries)
	 * Gibt den Pfad zurück der im array entries übergeben wurde. der passende FS wird eingesetzt
	 */
	public static String getFullPath(String[] entries){
		String ret="";
		for(int i=0;i<entries.length-1;i++){
			ret+=entries[i]+FS;
		}
		ret+=entries[entries.length-1];
		return ret;
	}
	/*
	 * public static String getLocalHash(String file) Gibt einen MD% Hash der
	 * file zurück
	 */
	public static String getLocalHash(String file) {
		return getLocalHash(new File(file));
	}

	/*
	 * public static String getLocalHash(File f) Gibt einen MD% Hash der file
	 * zurück
	 */
	public static String getLocalHash(File f) {
		try {

			MessageDigest md;

			md = MessageDigest.getInstance("md5");

			byte[] b = new byte[1024];

			InputStream in = new FileInputStream(f);
			for (int n = 0; (n = in.read(b)) > -1;) {
				md.update(b, 0, n);

			}
			byte[] digest = md.digest();
			String ret = "";

			for (int i = 0; i < digest.length; i++) {
				String tmp = Integer.toHexString(digest[i] & 0xFF);
				if (tmp.length() < 2)
					tmp = "0" + tmp;
				ret += tmp;
			}
			in.close();
			return ret;

		} catch (Exception e) {

			// DEBUG.error(e);
		}
		return "";
	}

	/*
	 * public static String getLocalFile(File file) Liest file über einen
	 * bufferdReader ein und gibt den Inhalt asl String zurück
	 */
	public static String getLocalFile(File file) {
		BufferedReader f;
		try {
			f = new BufferedReader(new FileReader(file));

			String line;
			String ret = "";

			while ((line = f.readLine()) != null) {
				ret += line + "\r\n";
			}
			f.close();
			return ret;
		} catch (IOException e) {
			// TODO Auto-generated catch block
		e.printStackTrace();
		}
		return "";
	}

	/*
	 * public static boolean writeLocalFileBytes(File file, String content)
	 * schreibt content in file. file wird gelöscht fals sie schon existiert
	 * /byteweise
	 */
	public static boolean writeLocalFileBytes(File file, String content) {

		FileOutputStream f;
		try {

			if (file.isFile()) {
				if (!file.delete()) {
					trace("Konnte Datei nicht löschen " + file);
					return false;
				}

			}
			trace("DIR :" + file.getParent());
			if (file.getParent() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			file.createNewFile();

			f = new FileOutputStream(file);

			for (int i = 0; i < content.length(); i++) {
				f.write((byte) content.charAt(i));
			}
			f.close();
		} catch (FileNotFoundException e) {

			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;

	}

	/*
	 * public static boolean writeLocalFile(File file, String content) Schreibt
	 * über einen BufferedWriter content in file. file wird gelöscht falls sie
	 * schon existiert
	 */
	public static boolean writeLocalFile(File file, String content) {

		try {

			if (file.isFile()) {
				if (!file.delete()) {
					trace("Konnte Datei nicht löschen " + file);
					return false;
				}

			}
			trace("DIR :" + file.getParent());
			if (file.getParent() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			file.createNewFile();

			// FileWriter f = new FileWriter(file);
			BufferedWriter f = new BufferedWriter(new FileWriter(file));
			// DEBUG.trace(file+" - "+content);
			f.write(content);
			f.close();

			return true;

		} catch (Exception e) {
			e.printStackTrace();
			return false;

		}

	}

	/** ************************************XML**************************************** */
	/*
	 * public static Document parseXmlFile(String filename, boolean validating)
	 * liest filename als XML ein und gibt ein XML Document zurück. Parameter
	 * validating: Macht einen validt check
	 */
	public static Document parseXmlFile(String filename, boolean validating) {
		try {
			// Create a builder factory
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			factory.setValidating(validating);

			// Create the builder and parse the file
			Document doc = factory.newDocumentBuilder().parse(
					new File(filename));
			return doc;
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (IOException e) {

			// DEBUG.error(e);
		}
		return null;
	}

	public static String getAttribute(Node childNode,String key){
		NamedNodeMap att = childNode.getAttributes();
		if(att==null ||att.getNamedItem(key)==null){
			trace("ERROR: XML Attribute missing: "+key);
			return null;
		}
		return att.getNamedItem(key).getNodeValue();
	}

	/** ************************************Properties************************************ */
	/*
	 * private static Properties PROPS Globale Property File
	 */
	private static Properties PROPS = new Properties();

	/*
	 * private static String PROPERTYFILE Pfad zur Gloabeln property file
	 */
	private static String PROPERTYFILE = "globals.dat";

	/*
	 * public static String getProperty(String key) Gibt eine Property aus der
	 * globalen propertyfile zurück.
	 */
	public static String getProperty(String key) {
		if (PROPS == null) {
			FileInputStream input;
			try {
				input = new FileInputStream(PROPERTYFILE);

				PROPS.load(input);

				return PROPS.getProperty(key);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
		} else {
			return PROPS.getProperty(key);
		}

	}

	/*
	 * public static String getProperty(String key,String defaultValue) Gibt
	 * eine Eigenschaft aus der globalen Propertyfile zurück. Falls zu dem key
	 * kein Wert existiert wird der defaultwert angelegt und zurückgegeben
	 */
	public static String getProperty(String key, String defaultValue) {
		String ret = getProperty(key);
		if (ret == null)
			setProperty(key, defaultValue);
		return PROPS.getProperty(key);
	}

	/*
	 * public static boolean setProperty(String key, String value) Setzt den
	 * Wert von key in der Globalen propertyfile
	 */
	public static boolean setProperty(String key, String value) {
		if (value == null)
			value = "";
		if (PROPS == null) {
			FileInputStream input;
			try {
				input = new FileInputStream(PROPERTYFILE);

				PROPS.load(input);

				PROPS.setProperty(key, value);

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}

		try {
			FileOutputStream output = new FileOutputStream(PROPERTYFILE);

			try {
				PROPS.store(output, "");
				return true;
			} catch (IOException e) {

				return false;
			}
		} catch (FileNotFoundException e) {
			return false;
		}
	}
	/*****************************************COLOR**************************************/
//	RGB to HSB

	/*public static float[] rgb2hsb(int r, int g, int b)
	 * Wandelt einen farbwert vom RGB Farbraum in HSB um (Hue, Saturation, Brightness)
	 */
		public static float[] rgb2hsb(int r, int g, int b) {
				
				float [] hsbvals = new float[3]; 
				Color.RGBtoHSB(r, g, b, hsbvals);
				return hsbvals;
		}
		/*****************************STRING & PARSE****************************************/
/*public static String[] getMatches(String source, String pattern)
 * Gibt alle treffer in source nach dem pattern  zurück. Platzhalter ist nur !! °
 */
		public static String[] getMatches(String source, String pattern){
		//DEBUG.trace("pattern:   "+STRING.getPattern(pattern));
			Matcher rr = Pattern.compile(
					getPattern(pattern),
					Pattern.DOTALL).matcher(source);
			if (!rr.find()) {
				// Keine treffer
			}
			try{
			String[] ret= new String[rr.groupCount()];
			for(int i=1; i<=rr.groupCount();i++){
				ret[i-1]= rr.group(i);
			}
			return ret;
			}catch(IllegalStateException e){
				
				return null;
			}
		}
		/*public static String getPattern(String str)
		 * Gibt ein Regex pattern zurück. ° dient als Platzhalter!
		 */
		public static String getPattern(String str) {

			String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm 1234567890";
			String ret = "";
			int i;
			for (i = 0; i < str.length(); i++) {
				char letter = str.charAt(i);
				
				if (letter =='°') {
				
					ret += "(.*?)";
				} else if (allowed.indexOf(letter) == -1) {
					
					ret += "\\" + letter;
				} else {
			
					ret += letter;
				}
			}

			return ret;
		}
}