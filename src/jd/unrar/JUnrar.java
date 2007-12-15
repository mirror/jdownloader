package jd.unrar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
import java.util.Vector;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.ProgressController;
import jd.utils.JDUtilities;

public class JUnrar {
    private File passwordList = new File(JDUtilities.getJDHomeDirectory(), "passwordlist.xml");
    private File unpacked = new File(JDUtilities.getJDHomeDirectory(), "unpacked.dat");
    private Vector<File> unpackedlist;
    private HashMap<File, String> files;
    private static Object[][] filesignatures = {{"avi", new Integer[][]{{82, 73, 70, 70}}}, {"mpg", new Integer[][]{{0, 0, 1, 186, -1, 0}}}, {"mpeg", new Integer[][]{{0, 0, 1, 186, -1, 0}}}, {"rar", new Integer[][]{{82, 97, 114, 33, 26, 7}}}, {"wmv", new Integer[][]{{48, 38, 178, 117, 142, 102}}}, {"mp3", new Integer[][]{{73, 68, 51, 3, 0}, {255, 251, 104, -1, 0, -1,}, {255, 251, 64, -1, 0, -1}}}, {"exe", new Integer[][]{{77, 90, 144, 0, 3, 0}}}, {"bz2", new Integer[][]{{66, 90, 104, 54, 49, 65}}}, {"gz", new Integer[][]{{31, 139, 8, 0}}}, {"doc", new Integer[][]{{208, 207, 17, 224, 161, 177}}}, {"pdf", new Integer[][]{{37, 80, 68, 70, 45, 49}}}, {"wma", new Integer[][]{{48, 38, 178, 117, 142, 102}}}, {"jpg", new Integer[][]{{255, 216, 255, 224, 0, 16}, {255, 216, 255, 225, 39, 222}}}, {"m4a", new Integer[][]{{0, 0, 0, 32, 102, 116}}}, {"mdf", new Integer[][]{{0, 255, 255, 255, 255, 255}}}, {"xcf", new Integer[][]{{103, 105, 109, 112, 32, 120}}}};
    private Integer[][] typicalFilesignatures = {{80, 75, 3, 4, -1, 0,}, {82, 73, 70, 70}, {0, 255, 255, 255, 255, 255}, {48, 38, 178, 117, 142, 102}, {208, 207, 17, 224, 161, 177}};
    public HashMap<String, Integer> passwordlist;

    public String standardPassword = null;

    private static final String allOk = "(?s).*[\\s]+All OK[\\s].*";

    private static final int MAX_REC_DEPTHS = 2;
    /**
     * Erkennungsstring für zu große Dateien (dieser String kann keine Dateiname
     * sein) oder Teilarchiv fehlt
     */
    private static final String[] FILE_ERROR = new String[]{null, null};
    /**
     * Flag zeigt and ass das archiv nicht Passwortgeschützt ist
     */
    private static final String[] NO_PROTECTEDFILE = new String[]{null};
    /**
     * Zeigt an dass das Archiv als ganzes gecshützt ist. die Filelist ist ohne
     * Passwort nicht zugänglich
     */
    private static final String[] PASSWORD_PROTECTEDARCHIV = null;

    public String unrar = null;

    public long maxFilesize = 2097152; // entspricht 2 mb

    public boolean overwriteFiles = false, autoDelete = true;

    private Vector<String> srcFolders;

    private ProgressController progress;
    private boolean extendPasswordSearch = false;
    public static Logger logger = JDUtilities.getLogger();
    Vector<String> ret = new Vector<String>();
    /**
     * Konstruktor Hinzufügen von Passwoertern
     * 
     */
    public JUnrar() {
        progress = new ProgressController("Default Unrar",100);
        progress.setStatusText("Unrar-process");
        loadObjects();
    }

    /**
     * Konstruktor zum entpacken aller Rar-Archive im angegebenen Ordner,
     * Passwoerter werden aus der PasswortListe entnommen fals vorhanden
     * 
     * @param path
     */
    public JUnrar(String path) {
        this(new File(path));
    }

    /**
     * Konstruktor zum entpacken aller Rar-Archive im angegebenen Ordner,
     * Passwoerter werden aus der PasswortListe entnommen fals vorhanden
     * 
     * @param path
     */
    public JUnrar(File path) {
        this(fileList(path), null);
    }

    /**
     * Konstruktor zum entpacken einer bestimmten Datei wenn das Passwort aus
     * der PasswortListe geholt werden soll oder kein Passwort benoetigt wird
     * einfach null als Password setzen
     * 
     * @param file
     * @param password
     */
    public JUnrar(String file, String password) {
        this(new File[]{new File(file)}, password);
    }

    /**
     * Konstruktor zum entpacken einer bestimmten Datei wenn das Passwort aus
     * der PasswortListe geholt werden soll oder kein Passwort benoetigt wird
     * einfach null als Password setzen
     * 
     * @param file
     * @param password
     */
    public JUnrar(File file, String password) {
        this(new File[]{file}, password);
    }

    /**
     * Konstruktor zum entpacken bestimmter Dateien wenn die Passwoerter aus der
     * PasswortListe geholt werden sollen oder keine Passwoerter benoetigt
     * werden einfach null als Password setzen
     * 
     * @param files
     * @param password
     */
    @SuppressWarnings("unchecked")
    public JUnrar(File[] files, String password) {
        progress = new ProgressController("Unrar",100);
        progress.setStatusText("Unrar-process");
        HashMap<File, String> filelist = new HashMap<File, String>();
        for (int i = 0; i < files.length; i++) {
            filelist.put(files[i], password);
        }

        this.files = filelist;
        loadObjects();
    }

    /**
     * Gibt alle Dateien in einem Pfad wieder
     */
    private static File[] fileList(File file) {
        Vector<File> ret = vFileList(file);
        return ret.toArray(new File[ret.size()]);

    }

    private static Vector<File> vFileList(File file) {
        Vector<File> ret = vFileList(file, 0, MAX_REC_DEPTHS);

        return ret;
    }

    /**
     * Siche alle Files in file bis zur rekursionstiefe maxRecDepths
     * 
     * @param file
     * @param recdepths
     * @param maxRecDepths
     * @return
     */
    private static Vector<File> vFileList(File file, int recdepths, int maxRecDepths) {
        Vector<File> ret = new Vector<File>();
        recdepths++;
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
            logger.severe(file.getAbsolutePath() + " is not a Folder2");
            return ret;
        }
        for (int i = 0; i < list.length; i++) {
            if (list[i].isDirectory()) {
                if (recdepths <= maxRecDepths) {
                    ret.addAll(vFileList(list[i], recdepths, maxRecDepths));
                }
            } else {

                ret.add(list[i]);

            }
            if (ret.size() > 300) {
                logger.warning("FileSearch interrupted. Your Download Destinationdirectory may contain to many files.");
                return ret;
            }
        }
        return ret;
    }

    /**
     * Konstruktor zum entpacken bestimmter Dateien, Passwoerter werden aus
     * derconfigfile PasswortListe entnommen fals vorhanden
     * 
     * @param files
     * @param password
     */
    public JUnrar(File[] files) {
        this(files, null);
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
        loadObjects();
    }
    /**
     * KOnstruktor jkann verwendet werden wenn keine progressbar angezeigt
     * werden soll.
     * 
     * @param b
     */
    public JUnrar(boolean b) {

        loadObjects();
    }
    @SuppressWarnings("unchecked")
    private void loadUnpackedList() {
        if (!autoDelete) {
            if (unpacked.isFile()) {
                this.unpackedlist = (Vector<File>) Utilities.loadObject(unpacked, false);
                freeUnpackedList();
            } else {
                this.unpackedlist = new Vector<File>();
                saveUnpackedList();
            }
        }
    }
    private boolean isFileInUnpackedList(File file) {
        for (int i = 0; i < unpackedlist.size(); i++) {
            if (unpackedlist.get(i).equals(file))
                return true;
        }
        return false;

    }
    private void freeUnpackedList() {
        if (!autoDelete) {
            for (int i = 0; i < unpackedlist.size(); i++) {
                File file = unpackedlist.get(i);
                if (!file.exists())
                    unpackedlist.remove(i);
            }
            saveUnpackedList();
        }
    }
    private void saveUnpackedList() {
        if (!autoDelete) {
            Utilities.saveObject(this.unpackedlist, unpacked, false);
        }
    }
    @SuppressWarnings("unchecked")
    private void loadObjects() {
        // private int maxFilesize = 500000;
        // public boolean overwriteFiles = false, autoDelete = true;
        if (passwordList.isFile()) {
            this.passwordlist = (HashMap<String, Integer>) Utilities.loadObject(passwordList, true);
        } else {

            this.passwordlist = new HashMap<String, Integer>();
            savePasswordList();
        }
    }
    private boolean isInFilesignatures(String filename)
    {
        int dot = filename.lastIndexOf('.');
        if (dot == -1)
            return false;

        String extention = filename.substring(dot + 1).toLowerCase();
        for (int i = 0; i < filesignatures.length; i++) {
            if(((String) filesignatures[i][0]).equals(extention))
            {
                typicalFilesignatures=(Integer[][]) filesignatures[i][1];
                logger.info(extention+" is a supported filetype");
                return true;
            }
        }
        return false;
        
    }
    private void savePasswordList() {
        Utilities.saveObject(this.passwordlist, passwordList, true);
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
                logger.severe("Please load the English version of unrar from http://www.rarlab.com/rar_add.htm for your OS");
            }
            return unrar;
        } else {
            return unrar;
        }

    }

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
                File unrarexe = new File(JDUtilities.getJDHomeDirectory().getAbsolutePath() + System.getProperty("file.separator") + "tools" + System.getProperty("file.separator") + "windows" + System.getProperty("file.separator") + "unrarw32" + System.getProperty("file.separator") + "unrar.exe");
                if (unrarexe.isFile())
                    programm = unrarexe.getAbsolutePath();
                else
                    return null;
            } catch (Throwable e) {
            }
        } else {
            try {
                String[] charset = System.getenv("PATH").split(":");
                String Programmname = "unrar";
                for (int i = 0; i < charset.length; i++) {
                    File fi = new File(charset[i], Programmname);
                    if (fi.isFile()) {
                        programm = fi.getAbsolutePath();
                        break;
                    }
                }
            } catch (Throwable e) {
            }
        }
        return programm;
    }

    public String[] returnPasswords() {
        String[] ret = new String[passwordlist.size()];
        int i = 0;
        for (Map.Entry<String, Integer> entry : passwordlist.entrySet()) {
            ret[i++] = entry.getKey();
        }
        return ret;
    }

    public void editPasswordlist(String[] passwords) {
        HashMap<String, Integer> pws = new HashMap<String, Integer>();
        for (int i = 0; i < passwords.length; i++) {
            // lieber auf nummer sicher
            passwords[i] = passwords[i].trim();
            if (passwords[i] != null && !passwords[i].matches("[\\s]*") && !passwords[i].matches("[\\s]*")) {
                if (passwordlist.containsKey(passwords[i]))
                    pws.put(passwords[i], passwordlist.get(passwords[i]));
                else
                    pws.put(passwords[i], 1);
            }
        }
        passwordlist = pws;
        passwordlist = (HashMap<String, Integer>) sortByValue(passwordlist);
        savePasswordList();
    }

    public void addToPasswordlist(String password) {
        if (password.matches("\\{\".*\"\\}$")) {
            password = password.replaceFirst("\\{\"", "").replaceFirst("\"\\}$", "");
            String[] passwords = password.split("\";\"");
            for (int i = 0; i < passwords.length; i++) {
                passwords[i] = passwords[i].trim();
                if (passwords[i] != null && !passwords[i].matches("[\\s]*") && !passwords[i].matches("[\\s]*") && !passwordlist.containsKey(passwords[i]))
                    passwordlist.put(passwords[i], 1);
            }
            passwordlist = (HashMap<String, Integer>) sortByValue(passwordlist);
            savePasswordList();
        } else if (password != null && !password.matches("[\\s]*") && !password.matches("[\\s]*") && !passwordlist.containsKey(password)) {
            password = password.trim();
            passwordlist.put(password, 1);
            passwordlist = (HashMap<String, Integer>) sortByValue(passwordlist);
            savePasswordList();
        }
    }

    @SuppressWarnings("unchecked")
    public void addToPasswordlist(File passwords) {

        try {
            String thisLine;
            FileInputStream fin = new FileInputStream((File) passwords);
            BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
            while ((thisLine = myInput.readLine()) != null) {
                thisLine = thisLine.trim();
                if (thisLine != null && !thisLine.matches("[\\s]*") && !thisLine.matches("[\\s]*") && !passwordlist.containsKey(thisLine))
                    passwordlist.put(thisLine, 1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        passwordlist = (HashMap<String, Integer>) sortByValue(passwordlist);
        savePasswordList();

    }

    @SuppressWarnings("unchecked")
    private static Map sortByValue(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
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
    @SuppressWarnings("unchecked")
    private static Map revSortByValue(Map map) {
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue()).compareTo(((Map.Entry) (o2)).getValue());
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

    private void reorderPasswordList(String password) {
        passwordlist.put(password, passwordlist.get(password) + 1);
        passwordlist = (HashMap<String, Integer>) sortByValue(passwordlist);
    }

    /**
     * prüft ob ein Archiv mit einem passwortgeschützt ist, nicht
     * passwortgeschwützt oder passwortgeschützte Dateien hat und gibt die
     * passwortgeschützten Dateien der Größe nach aufsteigend sortiert aus
     * 
     * @param file
     * @return HashMap
     */
    public String[] getProtectedFiles(File file, String pass) {

        Vector<String> params = new Vector<String>();
        if (pass == null || pass == "")
            params.add("-p-");
        else
            params.add("-p" + pass);
        params.add("-v");
        params.add("-ierr");
        params.add("v");
        params.add(file.getName());
        Process p = createProcess(unrar, params.toArray(new String[]{}), file.getParentFile());
        String str = startInputListener(p, file.getParentFile());
        if (str.indexOf("Cannot find volume") != -1) {
            logger.finer("Volume error");
            logger.finer(str);
            return FILE_ERROR;
        }
        if (str.indexOf("*") != -1) {
            HashMap<String, Integer> protectedFiles = new HashMap<String, Integer>();
            logger.finer("Single File Protection");
            Pattern pattern = Pattern.compile("\\*(.*)" + System.getProperty("line.separator") + ".*?([0-9]+)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(str);
            while (matcher.find()) {
                if (!protectedFiles.containsKey(matcher.group(1)))
                    protectedFiles.put(matcher.group(1), Integer.parseInt(matcher.group(2)));
            }
            if (protectedFiles.size() > 0) {
                protectedFiles = ((HashMap<String, Integer>) revSortByValue(protectedFiles));
                Entry<String, Integer> entry = protectedFiles.entrySet().iterator().next();
                if (entry.getValue() <= maxFilesize) {
                    extendPasswordSearch = false;
                    // die passwortgeschützte Datei wird komplett überprüft
                    // deswegen muss nur die kleinste passwortgeschützte Datei
                    // überprüft werden
                    return new String[]{entry.getKey()};
                } else {
                    logger.finer("There is no protected file matches the maximal filesize try to crack the passwort by filesignatures (not 100% safe)");
                    extendPasswordSearch = true;
                    for (Map.Entry<String, Integer> ent : protectedFiles.entrySet()) {
                        String name = ent.getKey();
                        if(isInFilesignatures(name))
                        {
                           return new String[] {name};
                        }
                    }
                    Set<String> set = protectedFiles.keySet();
                    return set.toArray(new String[set.size()]);
                }
            } else {
                logger.severe(str);
                logger.severe("Please load the English version of unrar from http://www.rarlab.com/rar_add.htm for your OS");
                return FILE_ERROR;
            }

        } else if (str.indexOf(" (password incorrect ?)") != -1) {
            return PASSWORD_PROTECTEDARCHIV;
        } else {
            return NO_PROTECTEDFILE;
        }
        // logger.info(archivProtected+" : "+str);

    }

    /**
     * Entpackt eine Datei
     * 
     * @param file
     * @param password
     * @return
     */

    private boolean extractFile(File file, String password) {
        logger.info("Extracting " + file.getName());
        progress.addToMax(10);
        progress.setStatusText("Extract: " + file);
        String[] z;
        if (password != null) {
            logger.finer("Password is given: " + password);
            progress.increase(1);
            progress.setStatusText("Checkarchive: " + file);

            z = getProtectedFiles(file, password);

            if (z == FILE_ERROR) {
                progress.increase(9);
                return false;
            }
            if (z == PASSWORD_PROTECTEDARCHIV) {
                logger.warning("Password incorrect");
                logger.warning("using passwordsearch");
            } else {
                String str = execprozess(file, password);

                if (str.matches(allOk)) {
                    logger.finest("All OK");
                    return true;
                } else {
                    logger.severe(str);
                    logger.severe("Please load the English version of unrar from http://www.rarlab.com/rar_add.htm for your OS");
                    return false;
                }
            }
        }

        if (standardPassword != null && password != standardPassword) {
            password = standardPassword;
            logger.finer("Password is given: " + password);
            progress.increase(1);
            progress.setStatusText("Checkarchive: " + file);
            z = getProtectedFiles(file, password);

            if (z == FILE_ERROR) {
                progress.increase(9);
                return false;
            }
            if (z == PASSWORD_PROTECTEDARCHIV) {
                logger.warning("Password incorrect");
                logger.warning("using passwordsearch");
            } else {
                String str = execprozess(file, password);

                if (str.matches(allOk)) {
                    logger.finest("All OK");
                    return true;
                } else {
                    logger.severe(str);
                    logger.severe("Please load the English version of unrar from http://www.rarlab.com/rar_add.htm for your OS");
                    return false;
                }
            }
        }

        z = getProtectedFiles(file, null);
        if (z == FILE_ERROR)
            return false;
        if (z == NO_PROTECTEDFILE) {
            String str = execprozess(file, "");

            if (str.matches(allOk)) {
                logger.finest("All OK");
                return true;
            } else {
                logger.warning("Can't extract " + file.getName());
                logger.finer(str);
            }
            return false;
        } else if (z == PASSWORD_PROTECTEDARCHIV) {
            long time = -System.currentTimeMillis();
            for (Map.Entry<String, Integer> entry : passwordlist.entrySet()) {
                password = entry.getKey();
                z = getProtectedFiles(file, password);
                if (z == FILE_ERROR)
                    return false;
                else if (z != PASSWORD_PROTECTEDARCHIV) {
                    logger.info("Password " + password + " found in " + (time + System.currentTimeMillis()) / 1000 + " sec");
                    String str = execprozess(file, password);

                    if (str.matches(allOk)) {
                        logger.finest("All OK");
                        reorderPasswordList(password);
                        return true;
                    } else {
                        logger.warning("Can't extract " + file.getName());
                        logger.finer(str);
                    }
                    return false;
                }

            }
        } else {
            long time = -System.currentTimeMillis();
            String unsafe = null;
            for (Map.Entry<String, Integer> entry : passwordlist.entrySet()) {

                password = entry.getKey();
                int ch = checkarchiv(file, password, z);
                if (ch == 4)
                    unsafe = password;
                if (ch == 3)
                    return false;
                if (ch == 1) {
                    logger.info("Password " + password + " found in " + (time + System.currentTimeMillis()) / 1000 + " sec");
                    String str = execprozess(file, password);

                    if (str.matches(allOk)) {
                        logger.finest("All OK");
                        reorderPasswordList(password);
                        return true;
                    } else {
                        logger.warning("Can't extract " + file.getName());
                        logger.finer(str);
                    }
                    return false;
                }
            }
            if (unsafe != null) {
                logger.info("Password " + password + " found in " + (time + System.currentTimeMillis()) / 1000 + " sec");
                String str = execprozess(file, password);

                if (str.matches(allOk)) {
                    logger.finest("All OK");
                    reorderPasswordList(password);
                    return true;
                } else {
                    logger.warning("Can't extract " + file.getName());
                    logger.finer(str);
                }
                return false;
            }
            logger.severe("Can't extract " + file.getName() + "  (it seems like the password isn't in the list?)");

        }

        return false;
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
    private int startExtendetInputListener(Process p) {
        // Print file geht in stdout auch wenn alle Nachrichten nach stderr
        // umgeleitet werden
        InputStreamReader ipsr = new InputStreamReader(p.getInputStream());
        // Aneinanderhängende Großbuchstaben
        int c = 0;
        // Längste liste aneinanderhängender Großbuchstaben
        int d = 0;
        try {
            // in der Liste stehen die ersten 6 zeichen einer Datei
            Vector<Integer> flist = new Vector<Integer>();
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
            for (int j = 0; j < typicalFilesignatures.length; j++) {
                boolean b = true;
                for (int i = 0; i < typicalFilesignatures[j].length; i++) {
                    if (typicalFilesignatures[j][i] != -1 && !typicalFilesignatures[j][i].equals(fl[i])) {
                        b = false;
                        break;
                    }

                }
                if (b)
                    return 10;

            }
        } catch (Exception e) {

        }
        return d;
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
    private String startInputListener(Process p, File parent) {
        InputStreamReader ipsr = new InputStreamReader(p.getErrorStream());
        StringBuffer buff = new StringBuffer();
        char seperator = System.getProperty("line.separator").charAt(0);
        int changeCount = 0;
        String text = "";
        int max = 1000;
        progress.addToMax(max);
        try {
            int temp;
            do {
                temp = ipsr.read();
                buff.append((char) temp);
                if (((char) temp) == seperator && buff.indexOf(" (password incorrect ?)") != -1) {
                    p.destroy();
                }
                String[] lines = JDUtilities.splitByNewline(buff.toString());

                String newText = "";
                for (int i = lines.length - 1; i >= 0; i--) {
                    if (lines[i].trim().length() > 0)
                        newText += "<" + lines[i].trim();
                }
                if (newText.equals(text)) {
                    changeCount++;
                    if (changeCount == 10) {
                        progress.setStatusText(newText);
                        if (max > 0) {
                            progress.increase(3);
                            max -= 3;

                        }
                    }
                } else {
                    changeCount = 0;
                }
                text = newText;
            } while ((temp != -1));
        } catch (Exception e) {
            Pattern pattern = Pattern.compile("Extracting  (.*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(buff);
            while (matcher.find()) {
                File delfile = new File(parent, matcher.group(1));
                if (delfile.isFile() && delfile.length() == 0)
                    delfile.delete();
            }
        }
        progress.increase(max);
        return buff.toString();
    }

    private void closeEvent() {
        savePasswordList();
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
    public static Process createProcess(String command, String[] parameter, File runIn) {

        if (parameter == null)
            parameter = new String[]{};
        String[] params = new String[parameter.length + 1];
        params[0] = command;
        System.arraycopy(parameter, 0, params, 1, parameter.length);
        Vector<String> tmp = new Vector<String>();
        String par = "";
        for (int i = 0; i < params.length; i++) {
            if (params[i] != null && params[i].trim().length() > 0) {
                par += params[i] + " ";
                tmp.add(params[i].trim());
            }
        }
        params = tmp.toArray(new String[]{});
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
            logger.severe("Error executing " + command + ": " + e.getLocalizedMessage());
            return null;
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
    private int checkarchiv(File file, String password, String[] protection) {
        if (extendPasswordSearch) {
            // Wenn die Passwortgeschützten Dateien zu groß sind dann wird
            // versucht anhand von erkennungsmerkmalen
            // in den Ersten 6 Zeichen der Passwortgeschützten Dateien das
            // Passwort zu knacken
            boolean bool = false;
            for (int g = 0; g < protection.length; g++) {
                Vector<String> params = new Vector<String>();
                params.add("-n" + protection[g]);
                params.add("-p" + password);
                params.add("-ierr");
                params.add("p");
                params.add(file.getName());
                logger.finer("Check Archiv: " + file);
                Process p = createProcess(unrar, params.toArray(new String[]{}), file.getParentFile());
                int st = startExtendetInputListener(p);
                /*
                 * siehe startExtendetInputListener wenn der Wert von
                 * startExtendetInputListener 3 ist wird das Passwort erstmal
                 * als unsicher eingestuft es wird geschaut ob eine andere
                 * passwortgeschützte Datei im Archiv mit dem Passwort auch 3
                 * annimmt wenn ja wird das Passwort als sicher eingestuft
                 */
                if (st > 3 || (st == 3 && bool)) {
                    return 1;
                } else if (st == 3) {
                    bool = true;
                }
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
                Vector<String> params = new Vector<String>();
                params.add("-p" + password);
                params.add("-n" + protection[0]);
                params.add("-ierr");
                params.add("t");
                params.add(file.getName());
                logger.finer("Check Archiv: " + file);
                Process p = createProcess(unrar, params.toArray(new String[]{}), file.getParentFile());
                String str = startInputListener(p, file.getParentFile());
                if (str.indexOf(" (password incorrect ?)") != -1) {
                    logger.finer("Password incorrect");
                    return 2;
                } else if (str.matches(allOk)) {
                    logger.finer("allOK");
                    return 1;
                } else {
                    logger.severe("unknown error");
                    logger.severe(str);
                    logger.severe("Please load the English version of unrar from http://www.rarlab.com/rar_add.htm for your OS");
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
            Vector<String> params = new Vector<String>();
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
            params.add("-ierr");

            params.add("x");

            params.add(file.getName());
            Process p = createProcess(unrar, params.toArray(new String[]{}), file.getParentFile());
            String str = startInputListener(p, file.getParentFile());

            if (str.matches(allOk)) {
                Pattern pattern = Pattern.compile("Extracting from (.*)");
                Matcher matcher = pattern.matcher(str);
                if (autoDelete) {
                    while (matcher.find()) {
                        File delfile = new File(file.getParentFile(), matcher.group(1));
                        if (!delfile.isFile() || !delfile.delete())
                            logger.warning("Can't delete " + delfile.getName());
                    }
                } else {
                    while (matcher.find()) {
                        File ufile = new File(file.getParentFile(), matcher.group(1));
                        unpackedlist.add(ufile);
                    }
                    saveUnpackedList();
                }
                // Entpackte werden nochmal durch JUnrar geschickt um
                // Unterarchive zu entpacken
                pattern = Pattern.compile("Extracting  (.*?)[\\s]+(|OK$)");
                matcher = pattern.matcher(str);
                Vector<File> nfiles = new Vector<File>();
                while (matcher.find()) {
                    nfiles.add(new File(file.getParent() + System.getProperty("file.separator") + matcher.group(1)));
                }
                JUnrar un = new JUnrar(nfiles.toArray(new File[nfiles.size()]));
                un.maxFilesize = maxFilesize;
                un.standardPassword = standardPassword;
                un.autoDelete = autoDelete;
                un.unrar = unrar;
                un.overwriteFiles = overwriteFiles;
                ret.addAll(un.unrar());
            }
            return str;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * Startet den Entpackungsprozess. Es werden alle Zielordner zurückgegeben
     */
    public Vector<String> unrar() {
        unrar = getUnrarCommand();
        logger.info("Starting Unrar (DwD|Coalado)");
        logger.info("Config->unrar: " + unrar);
        logger.info("Config->maxFilesize: " + maxFilesize);
        logger.info("Config->Password: " + standardPassword);
        logger.info("Config->overwriteFiles: " + overwriteFiles);
        logger.info("Config->autoDelete: " + autoDelete);
        if (unrar == null) {
            return null;
        }
        loadUnpackedList();
        progress.setStatusText("Filter filelist");
        progress.increase(1);
        for (Map.Entry<File, String> entry : files.entrySet()) {
            File file = entry.getKey();
            if (file.isFile()) {
                String name = file.getName();
                if (name.matches(".*part[0]*[1].rar$")) {
                    logger.finer("Multipart archive: " + entry.getKey());
                    if ((autoDelete || !isFileInUnpackedList(entry.getKey())) && extractFile(entry.getKey(), entry.getValue())) {
                        ret.add(entry.getKey().getParentFile().getAbsolutePath());
                    }

                } else if (!name.matches(".*part[0-9]+.rar$") && name.matches(".*rar$")) {
                    logger.finer("Single archive: " + entry.getKey());
                    if ((autoDelete || !isFileInUnpackedList(entry.getKey())) && extractFile(entry.getKey(), entry.getValue())) {
                        ret.add(entry.getKey().getParentFile().getAbsolutePath());
                    }
                } else {
                    // logger.finer("Not an archive: " + entry.getKey());
                }
            } else
                logger.fine(file.getName() + " isn't a file");

        }

        closeEvent();
        logger.info("finalize");
        progress.finalize();
        return ret;
    }

    /**
     * Setzt alle Ordner in denen entpackt werden soll
     * 
     * @param folders
     */
    @SuppressWarnings("unchecked")
    public void setFolders(Vector<String> folders) {
        this.srcFolders = folders;
        loadUnpackedList();
        files = new HashMap<File, String>();
        HashMap<File, String> filelist = new HashMap<File, String>();
        if (progress != null)
            progress.setStatusText("Scan download-directories");
        if (progress != null)
            progress.increase(1);
        for (int i = 0; i < srcFolders.size(); i++) {
            if (srcFolders.get(i) != null) {
                Vector<File> list = vFileList(new File(srcFolders.get(i)));
                for (int ii = 0; ii < list.size(); ii++) {
                    if (autoDelete && list.get(ii).getName().matches(".*\\.rar$"))
                        filelist.put(list.get(ii), null);
                    else if (list.get(ii).getName().matches(".*\\.rar$")) {
                        if (!isFileInUnpackedList(list.get(ii))) {
                            filelist.put(list.get(ii), null);
                        }
                    }
                }
            }
        }
        this.files = filelist;
    }

    public HashMap<File, String> getFiles() {
        return files;
    }

}
