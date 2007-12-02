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
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.ProgressController;
import jd.utils.JDUtilities;

public class JUnrar {
    private File                    passwordList        = new File(JDUtilities.getJDHomeDirectory(), "passwordlist.xml");

    private HashMap<File, String>   files;

    public HashMap<String, Integer> passwordlist;

    public String                   standardPassword    = null;

    private static final String     allOk               = "(?s).*[\\s]+All OK[\\s].*";

    private static final int        MAX_REC_DEPTHS      = 2;
/**
 * Flag zeigt and ass das archiv nicht Passwortgeschützt ist
 */
    private static final byte PROTECTION_NONE = 0;
    /**
     * Flag zeigt an dass im Archiv die Files extra geschützt sind. Der zugriff auf die FIlelist ist möglich.
     */
    private static final byte PROTECTION_SINGLE_FILE = 1;
    /**
     * Zeigt an dass das archiv als ganzes gecshützt ist. die Filelist ist ohne Passwort nicht zugänglich
     */

    private static final byte PROTECTION_ARCHIVE = 2;

    public String                   unrar               = null;

    public int                      maxFilesize         = 2;

    public boolean                  overwriteFiles      = false, autoDelete = true;

    private Vector<String>          srcFolders;

    private ProgressController      progress;



    private byte                 archivProtected     = 0;

    public boolean useExtendedPasswordSearch=true;

    public static Logger            logger              = JDUtilities.getLogger();

    /**
     * Konstruktor Hinzufügen von Passwoertern
     * 
     */
    public JUnrar() {
        progress = new ProgressController(100);
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
        this(new File[] { new File(file) }, password);
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
        this(new File[] { file }, password);
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
        progress = new ProgressController(100);
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
            }
            else {

                ret.add(list[i]);

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
 * KOnstruktor jkann verwendet werden wenn keine progressbar angezeigt werden soll.
 * @param b
 */
    public JUnrar(boolean b) {
 
        loadObjects();
    }

    @SuppressWarnings("unchecked")
    private void loadObjects() {
        // private int maxFilesize = 500000;
        // public boolean overwriteFiles = false, autoDelete = true;
        if (passwordList.isFile()) {
            this.passwordlist = (HashMap<String, Integer>) Utilities.loadObject(passwordList, true);
        }
        else {

            this.passwordlist = new HashMap<String, Integer>();
            savePasswordList();
        }
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
        }
        else {
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
            }
            catch (Throwable e) {
            }
        }
        else {
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
            }
            catch (Throwable e) {
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
                if (passwords[i] != null && !passwords[i].matches("[\\s]*") && !passwords[i].matches("[\\s]*") && !passwordlist.containsKey(passwords[i])) passwordlist.put(passwords[i], 1);
            }
            passwordlist = (HashMap<String, Integer>) sortByValue(passwordlist);
            savePasswordList();
        }
        else if (password != null && !password.matches("[\\s]*") && !password.matches("[\\s]*") && !passwordlist.containsKey(password)) {
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
                if (thisLine != null && !thisLine.matches("[\\s]*") && !thisLine.matches("[\\s]*") && !passwordlist.containsKey(thisLine)) passwordlist.put(thisLine, 1);
            }
        }
        catch (IOException e) {
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

    private void reorderPasswordList(String password) {
        passwordlist.put(password, passwordlist.get(password) + 1);
        passwordlist = (HashMap<String, Integer>) sortByValue(passwordlist);
    }

    /**
     * Orüft ob ein Archiv mit einem passwort geschützt ist oder
     * passwortgeschützte files hat
     * 
     * @param file
     * @return
     */
    public byte isArchiveProtected(File file) {
    
            Vector<String> params = new Vector<String>();

            params.add("-p-");
            params.add("-v");
            params.add("-ierr");
            params.add("vt");
            params.add(file.getName());
            Process p = createProcess(unrar, params.toArray(new String[] {}), file.getParentFile());
            String str = startInputListener(p, file.getParentFile());

  
        
            if (str.indexOf("*") != -1 ){
                logger.finer("Single File Protection");
                this.archivProtected = PROTECTION_SINGLE_FILE;
            
            }else if(str.indexOf(" (password incorrect ?)") != -1){
                logger.finer("Archiv protection");
                this.archivProtected = PROTECTION_ARCHIVE;
             
            }else{
                this.archivProtected = PROTECTION_NONE;  
            }
           //logger.info(archivProtected+" : "+str);
            return archivProtected;

        
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
        if (password != null) {
            logger.finer("Password is given: " + password);
            progress.increase(1);
            progress.setStatusText("Checkarchive: " + file);
            int z = checkarchiv(file, password);
            if (z > 1) {
                if (z == 2) logger.warning("Password incorrect");

                progress.increase(9);
                return false;
            }
            String str = execprozess(file, password);
       
            if (str.matches(allOk)) {
                logger.finest("All OK");
                addToPasswordlist(password);
                return true;
            }
            else {
                logger.severe(str);
                logger.severe("Please load the English version of unrar from http://www.rarlab.com/rar_add.htm for your OS");
                return false;
            }
        }
        else {
            if (standardPassword != null) {
                int z = checkarchiv(file, standardPassword);
                logger.info("Use standardPassword " + standardPassword);
                if (z > 1) {
                    if (z == 2) logger.warning("Password incorrect using passwordsearch");
                }else{
                String str = execprozess(file, standardPassword);

                if (str.matches(allOk)) {
                    logger.finest("All OK");
                    addToPasswordlist(standardPassword);
                    return true;
                }
                else {
                    logger.warning("Can't extract " + file.getName());
                    logger.finer(str);
                    return false;
                }
                }
            }
            int z = checkarchiv(file, "");
            if (z == 3) return false;
            if (z == 1) {
                String str = execprozess(file, "");

                if (str.matches(allOk)) {
                    logger.finest("All OK");
                    return true;
                }
                else {
                    logger.warning("Can't extract " + file.getName());
                    logger.finer(str);
                }
                return false;
            }
            else {
                long time = -System.currentTimeMillis();
                for (Map.Entry<String, Integer> entry : passwordlist.entrySet()) {

                    password = entry.getKey();
                    z = checkarchiv(file, password);
                    if (z == 3) return false;
                    if (z == 1) {
                        logger.info("Password " + password + " found in " + (time + System.currentTimeMillis()) / 1000 + " sec");
                        String str = execprozess(file, password);

                        if (str.matches(allOk)) {
                            logger.finest("All OK");
                            reorderPasswordList(password);
                            return true;
                        }
                        else{
                            logger.warning("Can't extract " + file.getName());
                            logger.finer(str);   
                        }
                        return false;
                    }
                }
                logger.severe("Can't extract " + file.getName() + "  (it seems like the password isn't in the list?)");

            }
        }
        return false;
    }

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
                    if (lines[i].trim().length() > 0) newText += "<" + lines[i].trim();
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
                }
                else {
                    changeCount = 0;
                }
                text = newText;
            }
            while ((temp != -1));
        }
        catch (Exception e) {
            e.printStackTrace();
            Pattern pattern = Pattern.compile("Extracting  (.*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(buff);
            while (matcher.find()) {
                File delfile = new File(parent, matcher.group(1));
                if (delfile.isFile() && delfile.length() == 0) delfile.delete();

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
     * @param command grundbefehl
     * @param parameter parameterlisze
     * @param runIn ausführen in
     * @param waitForReturn Sekunden warten bis die Funtion zurückkehrt
     * @return
     */
    public static Process createProcess(String command, String[] parameter, File runIn) {

        if (parameter == null) parameter = new String[] {};
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
        params = tmp.toArray(new String[] {});
        logger.info("Execute: " + tmp);
        ProcessBuilder pb = new ProcessBuilder(params);
        if (runIn != null) {
            pb.directory(runIn);
        }
        else {
            if (new File(command).exists()) {
                pb.directory(new File(command).getParentFile());
            }
        }
        Process process;

        try {

            return pb.start();

        }
        catch (Exception e) {
            e.printStackTrace();
            logger.severe("Error executing " + command + ": " + e.getLocalizedMessage());
            return null;
        }
    }

    private int checkarchiv(File file, String password) {
        try {
            byte protection = isArchiveProtected(file);
           
            if (protection==PROTECTION_NONE) return 1;
            logger.finer(file +" is protected "+protection);
            Vector<String> params = new Vector<String>();
            // String ext = ((password != "") ? " -p" + password : " -p-") + "
            // -ierr t -sl" + maxFilesize * 1000000 + " ";
            if (password != "") {
                params.add("-p" + password);

            }
            else {
                params.add("-p-");
            }

            params.add("-o+");

            params.add("-ierr");

            params.add("-sl" + (maxFilesize * 1000000));
            params.add("t");
            params.add(file.getName());
            logger.finer("Check Archiv: " + file);
            Process p = createProcess(unrar, params.toArray(new String[] {}), file.getParentFile());
            String str = startInputListener(p, file.getParentFile());
            if (str.indexOf(" (password incorrect ?)") != -1) {
                logger.finer("Password incorrect");
                return 2;
            }
            else if (str.indexOf("Cannot find volume") != -1) {
                Pattern pattern = Pattern.compile("(Cannot find volume .*)", Pattern.CASE_INSENSITIVE);
                logger.finer("Volume error");
                logger.finer(str);
                return 3;
            }
            else if (str.matches(allOk)) {
                logger.finer("allOK");
                return 1;
            }
            else if (str.indexOf("No files to extract") != -1) {
                //bei aRCHIVEN die als ganzes geschützt sind kommt man hier her nur wenn das passwort richtig eingegeben wurde
                if(protection==PROTECTION_ARCHIVE){
                    logger.finer("Password ok");
                    return 1;
                }else{
             if(useExtendedPasswordSearch){
                // Es gibt kmeine files die klein genug zum check sind.
                int tmp = maxFilesize;
                if (tmp > 5000) {
                    logger.severe("Password could not be checked for file: " + file);
                    return 3;
                }
                maxFilesize += 2000;
                int ret = checkarchiv(file, password);
                maxFilesize = tmp;
                return ret;
             }else{
                    logger.severe("PW could not be found. Activate extended password search to unrar this file");
                    return 3;
                }
                }
            }
            else {
                logger.severe("unknown error");
                logger.severe(str);
                logger.severe("Please load the English version of unrar from http://www.rarlab.com/rar_add.htm for your OS");
                return 3;
            }

        }
        catch (Exception e) {
            e.printStackTrace();
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

            }
            else {
                params.add("-p-");
            }
            if (overwriteFiles) {
                params.add("-o+");
            }
            else {
                params.add("-o-");
            }
            params.add("-ierr");

            params.add("x");

            params.add(file.getName());
            Process p = createProcess(unrar, params.toArray(new String[] {}), file.getParentFile());
            String str = startInputListener(p, file.getParentFile());

            if (autoDelete) {
                if (str.matches(allOk)) {
                    Pattern pattern = Pattern.compile("Extracting from (.*)");
                    Matcher matcher = pattern.matcher(str);
                    while (matcher.find()) {
                        File delfile = new File(file.getParentFile(), matcher.group(1));
                        if (!delfile.isFile() || !delfile.delete()) logger.warning("Can't delete " + delfile.getName());
                    }
                }
            }
            return str;

        }
        catch (Exception e) {
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
        Vector<String> ret = new Vector<String>();
        progress.setStatusText("Filter filelist");
        progress.increase(1);
        for (Map.Entry<File, String> entry : files.entrySet()) {
            File file = entry.getKey();
            if (file.isFile()) {
                String name = file.getName();
                if (name.matches(".*part[0]*[1].rar$")) {
                    logger.finer("Multipart archive: " + entry.getKey());
                    if (extractFile(entry.getKey(), entry.getValue())) {
                        ret.add(entry.getKey().getParentFile().getAbsolutePath());
                    }
                }
                else if (!name.matches(".*part[0-9]+.rar$") && name.matches(".*rar$")) {
                    logger.finer("Single archive: " + entry.getKey());
                    if (extractFile(entry.getKey(), entry.getValue())) {
                        ret.add(entry.getKey().getParentFile().getAbsolutePath());
                    }
                }
                else {
                   // logger.finer("Not an archive: " + entry.getKey());
                }
            }
            else
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
    public void setFolders(Vector<String> folders) {
        this.srcFolders = folders;
        files = new HashMap<File, String>();
        HashMap<File, String> filelist = new HashMap<File, String>();
        progress.setStatusText("Scan download-directories");
        progress.increase(1);
        for (int i = 0; i < srcFolders.size(); i++) {
            if (srcFolders.get(i) != null) {
                Vector<File> list = vFileList(new File(srcFolders.get(i)));
                for (int ii = 0; ii < list.size(); ii++) {
                    filelist.put(list.get(ii), null);
                }
            }
        }
        this.files = filelist;
    }

    public byte getArchiveProtection() {
        return archivProtected;
    }

}
