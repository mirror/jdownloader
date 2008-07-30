//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.unrar;

import java.io.File;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.utils.JDUtilities;

public class JUnrar {
	private class ProgressC {
		int pos = 0;
		ProgressController progress = null;

		public ProgressC(String string, int i, boolean progressInTerminal) {
			System.out.println(progressInTerminal);
			if (progressInTerminal) {
				System.out.println(string);
			} else {
				progress = new ProgressController(string, i);
			}
		}

		public void addToMax(int i) {
			if (progress != null)
				progress.addToMax(i);

		}

		public void finalize() {
			if (progress != null)
				progress.finalize();
			else {
				pos = 100;
				System.out.println(100 + " %");

			}
		}

		public void increase(int i) {
			if (progress != null)
				progress.increase(i);
			else {
				pos += i;
				System.out.println(pos + " %");

			}

		}

		public void setRange(int i) {
			if (progress != null)
				progress.setRange(i);

		}

		public void setStatusText(int i) {
			if (progress != null)
				progress.setStatus(i);
			else {
				System.out.println(i + " %");
				pos = i;
			}
		}

		public void setStatusText(String string) {
			if (progress != null)
				progress.setStatusText(string);
			else
				System.out.println(string);

		}
	}

	private static final String allOk = "(?s).*[\\s]+All OK[\\s].*";

	private static SubConfiguration configPasswords = JDUtilities
			.getSubConfig("unrarPasswords");

	// private static final int MAX_REC_DEPTHS = 2; //nichtmehr notwendig da
	// sowieso nur ein unterordner durchscannt werden muss
	/**
	 * Erkennungsstring für zu große Dateien (dieser String kann keine Dateiname
	 * sein) oder Teilarchiv fehlt
	 */
	private static final String[] FILE_ERROR = new String[] { null, null };

	private static Logger logger = JDUtilities.getLogger();

	/**
	 * Flag zeigt and ass das archiv nicht Passwortgeschützt ist
	 */
	private static final String[] NO_PROTECTEDFILE = new String[] { null };

	/**
	 * Zeigt an dass das Archiv als ganzes gecshützt ist. die Filelist ist ohne
	 * Passwort nicht zugänglich
	 */
	private static final String[] PASSWORD_PROTECTEDARCHIV = null;

	private static LinkedList<String> passwordlist;

	private static final String PROPERTY_PASSWORDLIST = "PASSWORDLIST";

	private static final String PROPERTY_TOEXTRACTLIST = "TOEXTRACTLIST";

	private static final String PROPERTY_UNPACKEDLIST = "UNPACKEDLIST";

	private static String unrarErrortext = "Please load the English version of unrar/rar 3.7 or higher from http://www.rarlab.com/rar_add.htm or http://www.rarlab.com/download.htm for your OS";

	/**
	 * Versucht den Programmpfad von unrar bzw unrar.exe zu finden
	 * 
	 * @return
	 */
	public static String autoGetUnrarCommand() {

		String programm = null;
		String OS = System.getProperty("os.name").toLowerCase();
		if ((OS.indexOf("nt") > -1) || (OS.indexOf("windows") > -1)) {
			try {
				File unrarexe = new File(JDUtilities
						.getJDHomeDirectoryFromEnvironment().getAbsolutePath()
						+ System.getProperty("file.separator")
						+ "tools"
						+ System.getProperty("file.separator")
						+ "windows"
						+ System.getProperty("file.separator")
						+ "unrarw32"
						+ System.getProperty("file.separator") + "unrar.exe");
				if (unrarexe.isFile())
					programm = unrarexe.getAbsolutePath();
				else
					return null;
			} catch (Throwable e) {
			}
		} else {
			try {
				String[] charset = System.getenv("PATH").split(":");
				for (int i = 0; i < charset.length; i++) {
					File fi = new File(charset[i], "unrar");
					File fi2 = new File(charset[i], "rar");
					if (fi.isFile()) {
						programm = fi.getAbsolutePath();
						break;
					} else if (fi2.isFile()) {
						programm = fi2.getAbsolutePath();
						break;
					}
				}
			} catch (Throwable e) {
			}
		}
		return programm;
	}

	/**
	 * Führt ein externes programm aus
	 * 
	 * @param command
	 *            grundbefehl
	 * @param parameter
	 *            parameterlisze
	 * @param runIn
	 *            ausführen in
	 * @param waitForReturn
	 *            Sekunden warten bis die Funtion zurückkehrt
	 * @return
	 */
	public static Process createProcess(String command, String[] parameter,
			File runIn) {

		if (parameter == null)
			parameter = new String[] {};
		String[] params = new String[parameter.length + 1];
		params[0] = command;
		System.arraycopy(parameter, 0, params, 1, parameter.length);
		LinkedList<String> tmp = new LinkedList<String>();
		String par = "";
		for (int i = 0; i < params.length; i++) {
			if (params[i] != null && params[i].trim().length() > 0) {
				par += params[i] + " ";
				tmp.add(params[i].trim());
			}
		}
		params = tmp.toArray(new String[] {});
		logger.info("Execute: " + tmp);
		ProcessBuilder pb = new ProcessBuilder(params);
		if (runIn != null) {
			pb.directory(runIn);
		} else {
			if (new File(command).exists()) {
				pb.directory(new File(command).getParentFile());
			}
		}
		// Process process;

		try {

			return pb.start();

		} catch (Exception e) {
			e.printStackTrace();
			logger.severe("Error executing " + command + ": "
					+ e.getLocalizedMessage());
			return null;
		}
	}

	public static void editPasswordlist(String[] passwords) {
		passwordlist = new LinkedList<String>();
		for (int i = 0; i < passwords.length; i++) {
			if (passwords[i] != null && !passwords[i].matches("[\\s]*")
					&& !passwordlist.contains(passwords[i]))
				passwordlist.add(passwords[i]);
		}
		makePasswordListUnique();
		savePasswordList();
	}
	public static String[] getPasswordArray(String password) {
		if (password == null || password.matches("[\\s]*"))
			return new String[] {};
		if (password.matches("[\\s]*\\{[\\s]*\".*\"[\\s]*\\}[\\s]*$")) {
			password = password.replaceFirst("[\\s]*\\{[\\s]*\"", "")
					.replaceFirst("\"[\\s]*\\}[\\s]*$", "");
			return password.split("\"[\\s]*\\,[\\s]*\"");
		}
		return new String[] { password };
	}

	public static LinkedList<String> getPasswordList() {
		loadPasswordlist();
		return passwordlist;

	}

	@SuppressWarnings("unchecked")
	private static void loadPasswordlist() {
		// private int maxFilesize = 500000;
		// public boolean overwriteFiles = false, autoDelete = true;
		if (passwordlist != null)
			return;
		passwordlist = (LinkedList<String>) configPasswords.getProperty(
				PROPERTY_PASSWORDLIST, null);
		if (passwordlist == null)
			passwordlist = new LinkedList<String>();
	}

	public static void makePasswordListUnique() {
		LinkedList<String> pwList = new LinkedList<String>();
		Iterator<String> iter = passwordlist.iterator();
		while (iter.hasNext()) {
			String string = (String) iter.next();
			if (!pwList.contains(string))
				pwList.add(string);
			else
				iter.remove();
		}
	}

	public static String passwordArrayToString(String[] passwords) {
		LinkedList<String> pws = new LinkedList<String>();
		for (int i = 0; i < passwords.length; i++) {
			if (!passwords[i].matches("[\\s]*") && !pws.contains(passwords[i]))
				pws.add(passwords[i]);
		}
		passwords = pws.toArray(new String[pws.size()]);
		if (passwords.length == 0)
			return "";
		if (passwords.length == 1)
			return passwords[0];

		int l = passwords.length - 1;

		String ret = "{\"";
		for (int i = 0; i < passwords.length; i++) {
			if (!passwords[i].matches("[\\s]*"))
				ret += passwords[i] + ((i == l) ? "\"}" : "\",\"");
		}
		return ret;

	}

	public static String[] returnPasswords() {
		loadPasswordlist();
		return passwordlist.toArray(new String[passwordlist.size()]);
	}

	@SuppressWarnings("unchecked")
	private static Map revSortByValue(Map map) {
		List list = new LinkedList(map.entrySet());
		Collections.sort(list, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((Comparable) ((Map.Entry) (o1)).getValue())
						.compareTo(((Map.Entry) (o2)).getValue());
			}
		});
		// logger.info(list);
		Map result = new LinkedHashMap();
		for (Iterator it = list.iterator(); it.hasNext();) {
			Map.Entry entry = (Map.Entry) it.next();
			result.put(entry.getKey(), entry.getValue());
		}
		return result;
	}

	private static void savePasswordList() {
		if (passwordlist != null) {
			configPasswords.setProperty(PROPERTY_PASSWORDLIST, passwordlist);
			configPasswords.save();
		}
	}

	/**
	 * listet alle Files in file in einem Downloadorder auf
	 * 
	 * @param file
	 * @return
	 */
	private static LinkedList<File> vFileList(File file) {
		LinkedList<File> ret = new LinkedList<File>();
		if (file == null) {
			logger.severe("File is Null");
			return ret;
		}
		if (!file.isDirectory()) {
			logger.severe(file.getAbsolutePath() + " is not a Folder");
			return ret;
		}

		File[] list = file.listFiles();
		if (list == null) {
			logger.severe(file.getAbsolutePath() + " is empty");
			return ret;
		}
		// boolean PARAM_USE_PACKETNAME_AS_SUBFOLDER =
		// JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_USE_PACKETNAME_AS_SUBFOLDER,
		// false);
		for (int i = 0; i < list.length; i++) {
			// Unterordner werden mitaufgelistet
			// Das wird eigentlich nichtmehr benoetigt ist aus
			// sicherheitgruenden jedoch noch drinnen
			if (list[i].isDirectory()) {
				File[] list2 = list[i].listFiles();
				for (int j = 0; j < list2.length; j++) {
					ret.add(list2[j]);
				}

			} else {
				ret.add(list[i]);
			}
			if (ret.size() > 400) {
				logger
						.warning("FileSearch interrupted. Your Download Destinationdirectory may contain to many files.");
				return ret;
			}
		}
		return ret;
	}

	SubConfiguration config = JDUtilities.getSubConfig("unrar");

	private boolean extendPasswordSearch = false;

	public File extractFolder = null;

	public HashMap<File, String> files;

	// Dateien die noch kommen bzw. bei jDownloader gerade heruntergeladen
	// werden
	public String[] followingFiles = null;

	public String link = "";

	public boolean overwriteFiles = false, autoDelete = true,
			deleteInfoFile = false;

	private ProgressC progress;

	public String standardPassword = null;

	private HashMap<File, String> toExtractlist;

	private Integer[][] typicalFilesignatures = { { 80, 75, 3, 4, -1, 0, },
			{ 82, 73, 70, 70 }, { 0, 255, 255, 255, 255, 255 },
			{ 48, 38, 178, 117, 142, 102 }, { 208, 207, 17, 224, 161, 177 } };

	private LinkedList<File> unpackedFiles = new LinkedList<File>();

	private LinkedList<File> unpackedlist;

	public String unrar = null;

	public boolean useToextractlist = true, progressInTerminal = false;

	private LinkedList<Long> volumess = new LinkedList<Long>();

	/**
	 * Konstruktor Hinzufügen von Passwoertern
	 * 
	 */
	public JUnrar() {

		loadPasswordlist();
	}

	/**
	 * KOnstruktor jkann verwendet werden wenn keine progressbar angezeigt
	 * werden soll.
	 * 
	 * @param b
	 */
	public JUnrar(boolean b) {

		loadPasswordlist();
	}

	public JUnrar(File file, String Password) {
		this.files = new HashMap<File, String>();
		this.files.put(file, Password);
		loadPasswordlist();
		// progress = new ProgressController("Default Unrar", 100);
		//progress.setStatusText("Unrar-process");
	}

	/**
	 * Konstruktor zum entpacken bestimmter Dateien mit verschiedenen
	 * Passwoertern in der HashMap steht fuer key die Datei und fuer value das
	 * Passwort, wenn das Passwort aus der PasswortListe geholt werden soll oder
	 * kein Passwort benoetigt wird einfach null als key bzw. Password setzen
	 * 
	 * @param files
	 */
	public JUnrar(HashMap<File, String> files) {
		this.files = files;
		loadPasswordlist();
		// progress = new ProgressController("Default Unrar", 100);
		//progress.setStatusText("Unrar-process");
	}

	public void addToPasswordlist(String password) {
		if (passwordlist == null || passwordlist.size() < 1)
			loadPasswordlist();
		String[] passwords = getPasswordArray(password);
		for (int i = 0; i < passwords.length; i++) {
			passwords[i] = passwords[i].trim();
			if (passwords[i] != null && !passwords[i].matches("[\\s]*")
					&& !passwordlist.contains(passwords[i]))
				passwordlist.add(passwords[i]);
		}
		makePasswordListUnique();
		savePasswordList();
	}

	private void addToToExtractList(File file, String password) {
		if (useToextractlist) {
			if (toExtractlist == null)
				loadToExtractList();
			if (!toExtractlist.containsKey(file)) {
				toExtractlist.put(file, password);
				saveToExtractList();
			}
		}
	}

	/**
	 * checkarchiv wird aufgerufen wenn in dem Archiv eine Einzelne Datei
	 * Passwortgeschützt ist checkarchiv hat gibt 1 aus wenn das Passwort stimmt
	 * 2 wenn das Passwort nicht stimmt 3 wenn ein Fehler aufgetreten ist 4 wenn
	 * das passwort nicht sicher ist
	 * 
	 * @param file
	 * @param password
	 * @param protection
	 * @return
	 */
	private int checkarchiv(File file, String password, String name) {
		if (extendPasswordSearch) {
			// Wenn die Passwortgeschützten Dateien zu groß sind dann wird
			// versucht anhand von erkennungsmerkmalen
			// in den Ersten 6 Zeichen der Passwortgeschützten Dateien das
			// Passwort zu knacken
			boolean bool = false;
			logger.finer("Check Archiv: " + file);
			Process p = createProcess(unrar, new String[] { "-n" + name,
					"-p" + password, "-c-", "-ierr", "p", file.getName() },
					file.getParentFile());
			int st = startExtendedInputListener(p);
			/*
			 * siehe startExtendetInputListener wenn der Wert von
			 * startExtendetInputListener 3 ist wird das Passwort erstmal als
			 * unsicher eingestuft es wird geschaut ob eine andere
			 * passwortgeschützte Datei im Archiv mit dem Passwort auch 3
			 * annimmt wenn ja wird das Passwort als sicher eingestuft
			 */
			if (st > 3 || (st == 3 && bool)) {
				return 1;
			} else if (st == 3) {
				bool = true;
			}
			if (bool)
				return 4;
			else
				return 2;
		} else {
			/*
			 * eine Passwortgeschützte Datei ist klein genug und kann komplett
			 * überprüft werden das garantiert mehr Sicherheit und eine Höhere
			 * Trefferquote bei der Passwortsuche besonders Textdateien machen
			 * bei der Methode mit den erkennungsmerkmalen schwierigkeiten
			 */
			try {
				logger.finer("Check Archiv: " + file);
				Process p = createProcess(unrar, new String[] {
						"-p" + password, "-n" + name, "-c-", "-ierr", "t",
						file.getName() }, file.getParentFile());
				String str = startInputListenerwithoutprogress(p);
				if (str.indexOf(" (password incorrect ?)") != -1) {
					logger.finer("Password incorrect");
					return 2;
				} else if (str.matches(allOk)) {
					logger.finer("allOK");
					return 1;
				} else {
					logger.severe("unknown error");
					logger.severe(str);
					logger.severe(unrarErrortext);
					return 3;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return 3;

	}

	private String execprozess(File file, String password) {

		try {
			LinkedList<String> params = new LinkedList<String>();
			// String ext = ((password != "") ? " -p" + password : " -p-") +
			// ((overwriteFiles) ? " -o+" : " -o-") + " -ierr x ";
			if (password != "") {
				params.add("-p" + password);

			} else {
				params.add("-p-");
			}
			if (overwriteFiles) {
				params.add("-o+");
			} else {
				params.add("-o-");
			}
			params.add("-c-");

			params.add("-ierr");

			params.add("x");
			File parent;
			boolean b = false;
			if (extractFolder != null && extractFolder.isDirectory()) {
				b = true;
				parent = extractFolder;
				params.add(file.getAbsolutePath());
			} else {
				parent = file.getParentFile();
				params.add(file.getName());
			}

			Process p = createProcess(unrar, params.toArray(new String[] {}),
					parent);
			String str = startInputListener(p, parent);

			if (str.matches(allOk)) {
				Pattern pattern = Pattern.compile("Extracting from (.*)");
				Matcher matcher = pattern.matcher(str);
				if (deleteInfoFile) {
					File infoFiles = new File(file.getParentFile(), file
							.getName().replaceFirst(
									"(?i)(\\.part[0-9]+\\.rar|\\.rar)$", "")
							+ ".info");
					if (infoFiles.exists() && infoFiles.delete())
						logger.info(infoFiles.getName() + " removed");
				}
				if (autoDelete) {
					while (matcher.find()) {
						File delfile;
						if (b)
							delfile = new File(matcher.group(1));
						else
							delfile = new File(file.getParentFile(), matcher
									.group(1));

						if (!delfile.isFile()) {
							logger.warning(str);
							logger.warning("Can't find " + delfile.getName());
						} else if (!delfile.delete()) {
							logger.warning(str);
							logger.warning("Can't delete " + delfile.getName());
						}

					}
				} else if (b) {
					while (matcher.find()) {
						File ufile = new File(file.getParentFile(), matcher
								.group(1));
						unpackedlist.add(ufile);
					}
					saveUnpackedList();
				}
				// Entpackte werden nochmal durch JUnrar geschickt um
				// Unterarchive zu entpacken
				pattern = Pattern
						.compile("(...       |Extracting)  (.*?)[\\s]+(|OK$)");
				matcher = pattern.matcher(str);
				HashMap<File, String> nfiles = new HashMap<File, String>();
				while (matcher.find()) {
					File f = new File(parent, matcher.group(2));
					nfiles.put(f, null);
					unpackedFiles.add(f);
				}
				JUnrar un = new JUnrar();
				un.files = nfiles;
				un.standardPassword = standardPassword;
				un.autoDelete = autoDelete;
				un.link = link;
				un.unrar = unrar;
				un.useToextractlist = false;
				un.overwriteFiles = overwriteFiles;
				un.progressInTerminal = progressInTerminal;
				unpackedFiles.addAll(un.unrar());
				Iterator<File> iter = unpackedFiles.iterator();

				while (iter.hasNext()) {
					File file2 = (File) iter.next();
					if (!file2.exists())
						iter.remove();
				}

			}
			return str;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;

	}

	/**
	 * Entpackt eine Datei
	 * 
	 * @param file
	 * @param password
	 * @return
	 */

	private boolean extractFile(File file, String pass) {
		progress.addToMax(100);
		progress.setStatusText(0);
		progress.setRange(100);
		logger.info("Extracting " + file.getName());
		progress.setStatusText("Extract: " + file);

		if (pass != null && !pass.matches("[\\s]*")) {
			String[] passwords = getPasswordArray(pass);
			for (int i = 0; i < passwords.length; i++) {
				passwordlist.addFirst(passwords[i]);
			}
			makePasswordListUnique();
		}

		String[] z = getProtectedFiles(file, null);
		if (z == FILE_ERROR) {
			addToToExtractList(file, pass);
			return false;
		}
		String password;
		if (z == NO_PROTECTEDFILE) {
			String str = execprozess(file, "");

			if (str.matches(allOk)) {
				logger.finest("All OK");
				removeFromToExtractList(file);
				return true;
			} else {
				logger.warning("Can't extract " + file.getName());
				addToToExtractList(file, pass);
				logger.warning(str);
			}
			return false;
		} else if (z == PASSWORD_PROTECTEDARCHIV) {
			long time = -System.currentTimeMillis();
			progress.setStatusText("search password");
			Iterator<String> interator = passwordlist.iterator();
			while (interator.hasNext()) {
				password = (String) interator.next();
				String prePassword = preparePassword(password);
				progress.setStatusText("password:" + password);
				z = getProtectedFiles(file, prePassword);
				if (z == FILE_ERROR) {
					addToToExtractList(file, password);
					return false;
				} else if (z != PASSWORD_PROTECTEDARCHIV) {
					logger.info("Password " + password + " found in "
							+ (time + System.currentTimeMillis()) / 1000
							+ " sec");
					String str = execprozess(file, prePassword);

					if (str.matches(allOk)) {
						logger.finest("All OK");
						removeFromToExtractList(file);
						return true;
					} else {
						logger.warning("Can't extract " + file.getName());
						addToToExtractList(file, pass);
						logger.warning(str);
					}
					return false;
				}

			}
		} else {
			long time = -System.currentTimeMillis();
			String unsafe = null;
			Iterator<String> interator = passwordlist.iterator();
			String name = z[0].replaceAll("[^0-9a-zA-Z\\\\/\\.]", "?");
			while (interator.hasNext()) {
				password = (String) interator.next();
				String prePassword = preparePassword(password);

				int ch = checkarchiv(file, prePassword, name);
				if (ch == 4)
					unsafe = password;
				if (ch == 3) {
					addToToExtractList(file, pass);
					return false;
				}
				if (ch == 1) {
					logger.info("Password " + password + " found in "
							+ (time + System.currentTimeMillis()) / 1000
							+ " sec");
					String str = execprozess(file, prePassword);
					if (str.matches(allOk)) {
						logger.finest("All OK");
						removeFromToExtractList(file);
						return true;
					} else {
						logger.warning("Can't extract " + file.getName());
						addToToExtractList(file, pass);
						logger.warning(str);
					}
					return false;
				}
			}
			if (unsafe != null) {
				password = unsafe;
				String prePassword = preparePassword(password);
				logger.info("Password " + password + " found in "
						+ (time + System.currentTimeMillis()) / 1000 + " sec");
				String str = execprozess(file, prePassword);

				if (str.matches(allOk)) {
					logger.finest("All OK");
					removeFromToExtractList(file);
					return true;
				} else {
					logger.warning("Can't extract " + file.getName());
					addToToExtractList(file, pass);
					logger.warning(str);
				}
				return false;
			}
		}
		new Exception("Can't extract " + file.getName()
				+ "  (it seems like the password isn't in the list?)\r\n"
				+ JDUtilities.getJDTitle() + "\r\n"
				+ System.getProperty("os.name") + "\r\n" + link)
				.printStackTrace();
		return false;
	}

	private void freeToExtractList() {
		if (useToextractlist) {
			HashMap<File, String> toExtractlistTemp = new HashMap<File, String>();
			for (Map.Entry<File, String> entry : toExtractlist.entrySet()) {
				File key = entry.getKey();
				if (key.isFile())
					toExtractlistTemp.put(key, entry.getValue());
			}
			toExtractlist = toExtractlistTemp;
			saveToExtractList();
		}
	}

	private void freeUnpackedList() {
		if (!autoDelete) {
			Iterator<File> iter = unpackedlist.iterator();
			while (iter.hasNext()) {
				File element = (File) iter.next();
				if (!element.exists())
					iter.remove();
			}
			saveUnpackedList();
		}
	}

	/**
	 * prüft ob ein Archiv mit einem passwortgeschützt ist, nicht
	 * passwortgeschwützt oder passwortgeschützte Dateien hat und gibt die
	 * passwortgeschützten Dateien der Größe nach aufsteigend sortiert aus
	 * 
	 * @param file
	 * @return HashMap
	 */
	@SuppressWarnings("unchecked")
	public String[] getProtectedFiles(File file, String pass) {

		String[] params = new String[6];
		if (pass == null || pass == "")
			params[0] = "-p-";
		else
			params[0] = "-p" + pass;

		params[1] = "-v";
		params[2] = "-c-";
		params[3] = "-ierr";
		params[4] = "v";
		params[5] = file.getName();
		Process p = createProcess(unrar, params, file.getParentFile());
		String str = startInputListenerwithoutprogress(p);
		if (str.indexOf("Cannot find volume") != -1) {
			logger.finer("Volume error");
			logger.finer(str);
			return FILE_ERROR;
		}
		if (str.indexOf(" (password incorrect ?)") != -1) {
			return PASSWORD_PROTECTEDARCHIV;
		} else {
			Pattern patternvolumes = Pattern
					.compile(
							"(.*)"
									+ System.getProperty("line.separator")
									+ ".*?([\\d]+).*?[\\d]+.*[\\d]+\\-[\\d]+\\-[\\d]+ [\\d]+:[\\d]+",
							Pattern.CASE_INSENSITIVE);
			Matcher matchervolumes = patternvolumes.matcher(str);
			HashMap<String, Long> protectedFiles = new HashMap<String, Long>();
			String namen = "";
			while (matchervolumes.find()) {

				String name = matchervolumes.group(1);
				if (name.matches("\\*.*")) {
					name = name.replaceFirst(".", "");
					long size = Long.parseLong(matchervolumes.group(2));
					if (!name.equals(namen)) {
						namen = name;
						volumess.add(size);
						if (size > 0)
							protectedFiles.put(name, size);
					}
				} else {
					name = name.replaceFirst(".", "");
					if (!name.equals(namen)) {
						namen = name;
						volumess.add(Long.parseLong(matchervolumes.group(2)));
					}
				}
			}
			if (volumess.size() == 0) {
				logger.severe("can't finde a file in the archiv: "
						+ file.getName());
				logger.severe(str);
				return FILE_ERROR;
			}
			if (protectedFiles.size() == 0) {
				logger.finer("no File Protection");
				return NO_PROTECTEDFILE;
			}
			logger.finer("Single File Protection");
			protectedFiles = ((HashMap<String, Long>) revSortByValue(protectedFiles));
			Entry<String, Long> entry = protectedFiles.entrySet().iterator()
					.next();
			if (2097152 >= entry.getValue()) {
				extendPasswordSearch = false;
				// die passwortgeschützte Datei wird komplett überprüft
				// deswegen muss nur die kleinste passwortgeschützte Datei
				// überprüft werden
				return new String[] { entry.getKey() };
			} else {
				logger
						.finer("There is no protected file matches the maximal filesize try to crack the passwort by filesignatures (not 100% safe)");
				extendPasswordSearch = true;
				for (Map.Entry<String, Long> ent : protectedFiles.entrySet()) {
					String name = ent.getKey();
					if (isInFilesignatures(name)) {
						return new String[] { name };
					}
				}
				Set<String> set = protectedFiles.keySet();
				return set.toArray(new String[set.size()]);
			}

		}

		// logger.info(archivProtected+" : "+str);

	}

	/**
	 * der unrar command kann hier fest gesetzt werden fals nicht gesetzt
	 * versucht Unrarit das Unrarprogramm an Standartorten zu finden der unrar
	 * command kann z.b.: durch textfeld.settext(Unrarit.autoGetUnrarCommand());
	 * in ein Textfeld gelesen werden
	 */
	public String getUnrarCommand() {
		if (unrar == null) {
			unrar = autoGetUnrarCommand();
			if (unrar == null) {
				logger.severe("Can't find unrar command");
				logger.severe(unrarErrortext);
			}
			return unrar;
		} else {
			return unrar;
		}

	}

	private boolean isFileInUnpackedList(File file) {
		Iterator<File> iter = unpackedlist.iterator();
		while (iter.hasNext()) {
			if (iter.next().equals(file))
				return true;
		}
		return false;

	}

	/**
	 * checkt ob eine der folgenden Dateien (Dateien die gerade herunterladen)
	 * dem Dateinamen entspricht fuer jDownloader bedeutet das das auf einen
	 * moeglichen Teil des archives erst gewartet wird
	 */
	private boolean isFollowing(String filename) {
		if (followingFiles != null && followingFiles.length > 0) {
			filename = filename.toLowerCase().replaceFirst(
					"(\\.part[0-9]+\\.rar|\\.rar)$", "");
			for (int i = 0; i < followingFiles.length; i++) {
				if (followingFiles[i]
						.toLowerCase()
						.replaceFirst(
								"(\\.part[0-9]+\\.rar|\\.part[0-9]+\\.rar\\.html|\\.part[0-9]+\\.rar\\.htm|\\.rar|\\.rar\\.html|\\.rar\\.htm|\\.r[0-9]+|\\.r[0-9]+\\.html|\\.r[0-9]+\\.htm)$",
								"").equals(filename)) {
					return true;
				}
			}
		}
		return false;

	}

	private boolean isInFilesignatures(String filename) {
		int dot = filename.lastIndexOf('.');
		if (dot == -1)
			return false;

		String extention = filename.substring(dot + 1).toLowerCase();
		for (int i = 0; i < FileSignatures.filesignatures.length; i++) {
			if (((String) FileSignatures.filesignatures[i][0])
					.equals(extention)) {
				typicalFilesignatures = (Integer[][]) FileSignatures.filesignatures[i][1];
				logger.info(extention + " is a supported filetype");
				return true;
			}
		}
		return false;

	}

	@SuppressWarnings("unchecked")
	private void loadToExtractList() {
		if (useToextractlist) {
			this.toExtractlist = (HashMap<File, String>) config.getProperty(
					PROPERTY_TOEXTRACTLIST, null);
			if (this.toExtractlist == null)
				this.toExtractlist = new HashMap<File, String>();
			else
				freeToExtractList();
		}

	}

	@SuppressWarnings("unchecked")
	private void loadUnpackedList() {
		if (!autoDelete) {
			this.unpackedlist = (LinkedList<File>) config.getProperty(
					PROPERTY_UNPACKEDLIST, null);
			if (unpackedlist == null)
				this.unpackedlist = new LinkedList<File>();
			else
				freeUnpackedList();
		}
	}

	private String preparePassword(String password) {
		String retpw = "";
		for (int i = 0; i < password.length(); i++) {
			char cur = password.charAt(i);
			if (cur == '"') {
				retpw += '\"';
			} else
				retpw += (char) cur;
		}
		return retpw;

	}

	private void removeFromToExtractList(File file) {
		if (useToextractlist) {
			if (toExtractlist == null)
				loadToExtractList();
			if (toExtractlist.containsKey(file)) {
				toExtractlist.remove(file);
				saveToExtractList();
			}
		}

	}

	private void saveToExtractList() {
		if (useToextractlist && toExtractlist != null) {
			config.setProperty(PROPERTY_TOEXTRACTLIST, toExtractlist);
			config.save();
		}
	}

	private void saveUnpackedList() {
		if (!autoDelete && toExtractlist != null) {
			config.setProperty(PROPERTY_UNPACKEDLIST, unpackedlist);
			config.save();
		}
	}

	/**
	 * Setzt alle Ordner in denen entpackt werden soll
	 * 
	 * @param folders
	 */
	public void setFolders(LinkedList<String> folders) {

		loadUnpackedList();
		files = new HashMap<File, String>();
		if (progress != null) {
			progress.setStatusText("Scan download-directories");
			progress.increase(1);
		}
		Iterator<String> iter = folders.iterator();
		while (iter.hasNext()) {
			String element = (String) iter.next();
			if (element != null) {
				Iterator<File> list = vFileList(new File(element)).iterator();
				while (list.hasNext()) {
					File element2 = (File) list.next();
					if (autoDelete && element2.getName().matches(".*\\.rar$"))
						files.put(element2, null);
					else if (element2.getName().matches(".*\\.rar$")) {
						if (!isFileInUnpackedList(element2)) {
							files.put(element2, null);
						}
					}
				}
			}
		}
	}

	/**
	 * gibt 10 zurück wenn die ersten 6 zeichen der zu entpackenden Datei einem
	 * Syntax von filesigs entsprechen, sonst zählt er die Maximale Anzahl
	 * aneinanderliegender Großbuchstaben (Kommen in Signaturen of vor) und gibt
	 * diese zurück
	 * 
	 * @param p
	 * @return int
	 */
	private int startExtendedInputListener(Process p) {
		// Print file geht in stdout auch wenn alle Nachrichten nach stderr
		// umgeleitet werden
		InputStreamReader ipsr = new InputStreamReader(p.getInputStream());
		// Aneinanderhängende Großbuchstaben
		int c = 0;
		// Längste liste aneinanderhängender Großbuchstaben
		int d = 0;
		try {
			// in der Liste stehen die ersten 6 zeichen einer Datei
			LinkedList<Integer> flist = new LinkedList<Integer>();
			int temp;
			for (int i = 0; i < 6; i++) {
				temp = ipsr.read();
				flist.add(temp);
				// prüft ob Es ein Großbuchstabe ist
				if (("" + (char) temp).matches("[A-Z]")) {
					c++;
				} else {
					if (c > d)
						d = c;
					c = 0;
				}

			}
			// Der Prozess wird beendet weil keine Daten mehr benötigt werden
			p.destroy();
			// Wenn In der Liste Mehr als 2 aneinanderhängende Großbuchstaben
			// sind wird davon ausgegangen das das Passwort richtig ist
			if (d > 3)
				return d;

			// Vector zu Array
			Integer[] fl = flist.toArray(new Integer[flist.size()]);

			// Checkt ob die Signatur mit einer der Signaturen von filesigs
			// übereinstimmt wenn ja dann gibt er 10 aus sonst den Wert von d
			if (FileSignatures
					.fileSignatureEquals(typicalFilesignatures, fl))
				return 10;
		} catch (Exception e) {

		}
		return d;
	}

	/*
	 * private Thread delayedProgress(final int steps, final File file, final
	 * Long long1) {
	 * 
	 * return new Thread(new Runnable() {
	 * 
	 * public void run() { long tempfs = 0; long std = long1 / steps; int step =
	 * steps; while (step > 0) { try { Thread.sleep(100); } catch
	 * (InterruptedException e) { 
	 * e.printStackTrace(); } if (file.isFile()) { long size = file.length() -
	 * tempfs;
	 * 
	 * int st = (int) (size / std); if (st > 0) { tempfs += (std * st); step -=
	 * st; progress.increase(st); } } } }
	 * 
	 * }); } /* /** gibt den Inhalt von stderr eines Processes als String aus
	 * bei unrar kann man mit -ierr alle Nachrichten in stderr umleichten wenn
	 * password incorrect in stderr auftaucht bricht er den Prozess sofort ab
	 * und löscht die entpackten Dateien
	 * 
	 * @param p @param parent @return stderr
	 */
	private String startInputListener(Process p, File parent) {
		progress.addToMax(100);
		progress.setStatusText(0);
		progress.setRange(100);
		InputStreamReader ipsr = new InputStreamReader(p.getErrorStream());
		StringBuffer buff = new StringBuffer();
		char seperator = System.getProperty("line.separator").charAt(0);
		long max = 0;
		Iterator<Long> iter = volumess.iterator();
		while (iter.hasNext()) {
			max += iter.next();
		}
		logger.info("" + volumess.size() + " files");
		logger.info(max / 1024 + " kb total size");
		long state = 0;
		// if (volumes > 1)
		// vol = max / volumes;
		String text = "";
		int c = 0;
		int perc = 0;
		String prozent = "";
		boolean b = true;
		int steps = 0;
		int inc = 0;
		boolean dd = false;
		iter = volumess.iterator();
		try {
			int temp;
			do {
				temp = ipsr.read();
				buff.append((char) temp);

				if (((char) temp) == seperator) {
					if (buff.indexOf(" (password incorrect ?)") != -1) {
						p.destroy();
					}
					if (text.trim().length() > 0)
						progress.setStatusText(text);
					b = true;
					text = "";
				} else if (b) {
					text += (char) temp;
					if (text.trim().length() > 0) {

						if (text.matches("[\\s]*Extracting  .*")) {
							b = false;
							long vol = 0;
							if (iter.hasNext())
								vol = iter.next();
							state += vol;
							if (vol > 1000) {
								dd = true;
								steps = (int) (state * 100 / max);
								if (steps > 0) {
									state = 0;
									progress.setStatusText(perc);
									prozent = "";
									inc = 0;
									perc += steps;
								}
							} else {
								dd = false;
							}
							c++;
						}

					}
				}
				if (dd && steps > 0) {
					if (("" + (char) temp).matches("\\%")) {
						if (prozent.matches("[\\d]+")) {
							int as = Integer.parseInt(prozent) * steps / 100
									- inc;
							if (as > 0) {
								progress.increase(as);
								inc += as;
							}
							prozent = "";
						}
					} else if (("" + (char) temp).matches("[\\d]")) {
						prozent += (char) temp;
					} else {
						prozent = "";
					}
				}
			} while ((temp != -1));
		} catch (Exception e) {
			Pattern pattern = Pattern.compile("Extracting  (.*)",
					Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(buff);
			while (matcher.find()) {
				File delfile = new File(parent, matcher.group(1));
				if (delfile.isFile() && delfile.length() == 0)
					delfile.delete();
			}
		}
		progress.increase(100);
		return buff.toString();
	}

	/**
	 * gibt den Inhalt von stderr eines Processes als String aus bei unrar kann
	 * man mit -ierr alle Nachrichten in stderr umleichten wenn password
	 * incorrect in stderr auftaucht bricht er den Prozess sofort ab und löscht
	 * die entpackten Dateien
	 * 
	 * @param p
	 * @param parent
	 * @return stderr
	 */
	private String startInputListenerwithoutprogress(Process p) {
		InputStreamReader ipsr = new InputStreamReader(p.getErrorStream());
		StringBuffer buff = new StringBuffer();
		char seperator = System.getProperty("line.separator").charAt(0);
		try {
			int temp;
			do {
				temp = ipsr.read();
				buff.append((char) temp);
				if (((char) temp) == seperator
						&& buff.indexOf(" (password incorrect ?)") != -1) {
					p.destroy();
				}
			} while ((temp != -1));
		} catch (Exception e) {
		}
		return buff.toString();
	}

	/**
	 * Startet den Entpackungsprozess. Es werden alle Zielordner zurückgegeben
	 */
	public LinkedList<File> unrar() {
		progress = new ProgressC("Default Unrar", 100, progressInTerminal);
		progress.setStatusText("Unrar-process");
		unrar = getUnrarCommand();
		logger.info("Starting Unrar (DwD|JD-Team)");
		logger.info("Config->unrar: " + unrar);
		logger.info("Config->extractFolder: " + extractFolder);
		logger.info("Config->useToextractlist: " + useToextractlist);

		logger.info("Config->overwriteFiles: " + overwriteFiles);
		logger.info("Config->autoDelete: " + autoDelete);
		if (unrar == null) {
			return unpackedFiles;
		}
		loadUnpackedList();
		if (useToextractlist) {
			loadToExtractList();
			files.putAll(toExtractlist);
		}
		progress.setStatusText("Filter filelist");
		progress.increase(1);
		for (Map.Entry<File, String> entry : files.entrySet()) {
			File file = entry.getKey();
			if (file.isFile()) {
				String name = file.getName();
				if (name.matches(".*part[0]*[1].rar$")
						&& (autoDelete || !isFileInUnpackedList(entry.getKey()))) {
					if (isFollowing(name)) {
						if (useToextractlist) {
							String pw = entry.getValue();
							if (pw == null && standardPassword != null)
								pw = standardPassword;
							addToToExtractList(entry.getKey(), pw);
						}
					} else {
						logger.finer("Multipart archive: " + entry.getKey());
						String pw = entry.getValue();
						if (pw == null && standardPassword != null)
							pw = standardPassword;
						logger.info("Password: " + pw);
						extractFile(entry.getKey(), pw);
					}

				} else if (!name.matches(".*part[0-9]+.rar$")
						&& name.matches(".*rar$")
						&& (autoDelete || !isFileInUnpackedList(entry.getKey()))) {
					if (isFollowing(name)) {
						if (useToextractlist) {
							String pw = entry.getValue();
							if (pw == null && standardPassword != null)
								pw = standardPassword;

							addToToExtractList(entry.getKey(), standardPassword);
						}
					} else {
						logger.finer("Single archive: " + entry.getKey());
						String pw = entry.getValue();
						if (pw == null && standardPassword != null)
							pw = standardPassword;
						logger.info("Password: " + pw);
						extractFile(entry.getKey(), pw);
					}
				} else {
					// logger.finer("Not an archive: " + entry.getKey());
				}
			} else
				logger.fine(file.getName() + " isn't a file");

		}
		logger.info("finalize");
		progress.finalize();
		return unpackedFiles;
	}
}
