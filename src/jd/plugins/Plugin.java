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

package jd.plugins;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.captcha.pixelgrid.Captcha;
import jd.config.ConfigContainer;
import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.parser.HTMLParser;
import jd.unrar.JUnrar;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese abstrakte Klasse steuert den Zugriff auf weitere Plugins. Alle Plugins
 * müssen von dieser Klasse abgeleitet werden.
 * 
 * Alle Plugins verfügen über einen Event Mechanismus
 */
public abstract class Plugin implements ActionListener {
    public static void main(String args[]) {

    }

    protected static final String END_OF_LINK = "[^\"]*";

    /**
     * Der Defualt Accept-language-header. Achtung nicht Ändern. Wird dieser
     * Header geändert müssen die Regexes der Plugins angepasst werden
     */
    public static final String ACCEPT_LANGUAGE = "de, en-gb;q=0.9, en;q=0.8";

    public void actionPerformed(ActionEvent e) {
        return;
    }

    /**
     * Versionsinformationen
     */
    public static final String VERSION = "jDownloader_20070830_0";

    /**
     * Zeigt an, ob das Plugin abgebrochen werden soll
     */
    public ConfigContainer config;

    protected RequestInfo requestInfo;

    protected CRequest request = new CRequest();

    //public boolean aborted = false;

    public boolean collectCaptchas() {
        return true;
    }

    /**
     * Wenn das Captcha nicht richtig erkannt wurde kann wird ein Dialog zu
     * Captchaeingabe gezeigt ist useUserinputIfCaptchaUnknown wird dieser
     * dialog nicht gezeigt
     * 
     * @return
     */
    public boolean useUserinputIfCaptchaUnknown() {
        return true;
    }

    /**
     * Liefert den Namen des Plugins zurück
     * 
     * @return Der Name des Plugins
     */
    public abstract String getPluginName();

    /**
     * Liefert eine einmalige ID des Plugins zurück
     * 
     * @return Plugin ID
     */
    public abstract String getPluginID();

    /**
     * Hier wird der Author des Plugins ausgelesen
     * 
     * @return Der Author des Plugins
     */
    public abstract String getCoder();

    /**
     * Liefert die Versionsbezeichnung dieses Plugins zurück
     * 
     * @return Versionsbezeichnung
     */
    public abstract String getVersion();

    /**
     * Ein regulärer Ausdruck, der anzeigt, welche Links von diesem Plugin
     * unterstützt werden
     * 
     * @return Ein regulärer Ausdruck
     * @see Pattern
     */
    public abstract Pattern getSupportedLinks();

    /**
     * Erstellt ein SupportPattern. dabei gibt es 2 Platzhalter: [*]= Dieser
     * Platzhalter ist optional. hier KANN ein beliebiges zeichen doer keins
     * stehen; [+] Hier muss mindestens ein beliebiges zeichen stehen Da die
     * links in einer liste "link1"\r\n"link2"\"\n... untersucht werden kann
     * hier eineinfaches pattern verwendet werden. UNd es werden trotzdem
     * zuverlässig alle treffer gefunden.
     * 
     * @param patternString
     * @return Gibt ein patternzurück mit dem links gesucht und überprüft werden
     *         können
     */
    public @Deprecated static Pattern getSupportPattern(String patternString) {
        patternString = patternString.replaceAll("\\[\\*\\]", ".*");
      
        patternString = patternString.replaceAll("\\[\\+\\]", ".+");
        patternString = patternString.replaceAll("\\[\\w\\]", "[\\w\\.]*?");
        if (patternString.endsWith(".*") || patternString.endsWith(".+")) {
            patternString = patternString.substring(0, patternString.length() - 2) + "[^\"]" + patternString.substring(patternString.length() - 1);
        }

        return Pattern.compile(patternString, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Verwendet den JDcontroller um ein ControlEvent zu broadcasten
     * 
     * @param controlID
     * @param param
     */
    public void fireControlEvent(int controlID, Object param) {

        JDUtilities.getController().fireControlEvent(new ControlEvent(this, controlID, param));
    }

    /**
     * Liefert den Anbieter zurück, für den dieses Plugin geschrieben wurde
     * 
     * @return Der unterstützte Anbieter
     */
    public abstract String getHost();

    /**
     * Führt einen Botcheck für den captcha file aus
     * 
     * @param file
     * @return true:istBot; false: keinBot
     */
    public abstract boolean doBotCheck(File file);

    protected File getLocalCaptchaFile(Plugin plugin) {
        return getLocalCaptchaFile(plugin, ".jpg");
    }

    /**
     * Gibt die Date zurück in die der aktuelle captcha geladne werden soll.
     * 
     * @param plugin
     * @return Gibt einen Pfadzurück der für die nächste Captchadatei reserviert
     *         ist
     */
    public static File getLocalCaptchaFile(Plugin plugin, String extension) {
        if (extension == null) extension = ".jpg";
        Calendar calendar = Calendar.getInstance();
        String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS.", calendar)+new Random().nextInt(999);
        // File dest = JDUtilities.getResourceFile("captchas/" +
        // plugin.getPluginName() + "/captcha_" + (new Date().getTime()) +
        // ".jpg");
        File dest = JDUtilities.getResourceFile("captchas/" + plugin.getPluginName() + "/" + date + extension);        
        return dest;
    }

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit Plugins.
     * Beispielsweise kann so der JDController die Premiumnutzung abschalten
     * wenn er fehler feststellt
     */
    public static final String PROPERTY_USE_PREMIUM = "USE_PREMIUM";

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit Plugins.
     * Beispielsweise kann so der JDController die Premiumnutzung abschalten
     * wenn er fehler feststellt
     */
    public static final String PROPERTY_PREMIUM_PASS = "PREMIUM_PASS";

    /**
     * Property name für die Config. Diese sollten möglichst einheitlich sein.
     * Einheitliche Properties erlauben einheitliches umspringen mit Plugins.
     * Beispielsweise kann so der JDController die Premiumnutzung abschalten
     * wenn er fehler feststellt
     */
    public static final String PROPERTY_PREMIUM_USER = "PREMIUM_USER";

    public static final int DOWNLOAD_ERROR_INVALID_OUTPUTFILE = 0;

    public static final int DOWNLOAD_ERROR_OUTPUTFILE_ALREADYEXISTS = 2;

    public static final int DOWNLOAD_ERROR_DOWNLOAD_INCOMPLETE = 3;

    public static final int DOWNLOAD_ERROR_RENAME_FAILED = 4;

    public static final int DOWNLOAD_SUCCESS = 5;

    public static final int DOWNLOAD_ERROR_FILENOTFOUND = 6;

    public static final int DOWNLOAD_ERROR_SECURITY = 7;

    public static final int DOWNLOAD_ERROR_UNKNOWN = 8;

    public static final int DOWNLOAD_ERROR_OUTPUTFILE_IN_PROGRESS = 9;

//    private static final int DOWNLOAD_ERROR_0_BYTE_TOLOAD = 10;

    public static final int CAPTCHA_JAC = 0;

    public static final int CAPTCHA_USER_INPUT = 1;

    public static SubConfiguration CONFIGS = null;

    /**
     * Führt den aktuellen Schritt aus
     * 
     * @param step
     * @param parameter
     * @return der gerade ausgeführte Schritt
     */
    public abstract PluginStep doStep(PluginStep step, Object parameter);

    /**
     * Hier werden alle notwendigen Schritte des Plugins hinterlegt
     */
    protected Vector<PluginStep> steps;

    /**
     * Enthält den aktuellen Schritt des Plugins
     */
    protected PluginStep currentStep = null;

    /**
     * Properties zum abspeichern der einstellungen
     */
//    private Property properties;

    private String statusText;

    private long initTime;

    private Captcha lastCaptcha;

    private int captchaDetectionID = -1;

    /**
     * Ein Logger, um Meldungen darzustellen
     */
    public static Logger logger = JDUtilities.getLogger();

    protected Plugin() {

        this.initTime = System.currentTimeMillis();
        steps = new Vector<PluginStep>();
        config = new ConfigContainer(this);
        // Lädt die Konfigurationseinstellungen aus der Konfig
        if (this.getPluginName() == null) {
            logger.severe("ACHTUNG: die Plugin.getPluginName() Funktion muss einen Wert wiedergeben der zum init schon verfügbar ist, also einen static wert");
        }

    }

    /**
     * Zeigt, daß diese Plugin gestoppt werden soll
     */
//    public void abort() {
//        aborted = true;
//
//    }

//    public boolean hasBeenInterrupted(){
//    
//        if(Thread.currentThread().isInterrupted()){
//            Thread.currentThread().interrupt();
//            return true;
//        }
//        return false;
//    }
    /**
     * Initialisiert das Plugin vor dem ersten Gebrauch
     */
    public void init() {
        currentStep = null;
    }

    /**
     * Gibt das Konfigurationsobjekt der INstanz zurück. Die Gui kann daraus
     * Dialogelement zaubern
     * 
     * @return gibt die aktuelle Configuration INstanz zurück
     */
    public ConfigContainer getConfig() {
        return config;
    }

    /**
     * Gibt ausgehend vom aktuellen step den nächsten zurück
     * 
     * @param currentStep
     *            Der aktuelle Schritt
     * @return nächster step
     */
    public PluginStep nextStep(PluginStep currentStep) {

        if (steps != null && steps.size() > 0) {

            if (currentStep == null) {
                currentStep = steps.firstElement();

            } else {
                int index = steps.indexOf(currentStep) + 1;
                if (steps.size() > index) {
                    currentStep = steps.elementAt(index);

                } else {
                    currentStep = null;
                }
            }
        } else {
            currentStep = null;
        }

        logger.finer("next: " + this.currentStep + "->" + currentStep);
        return (this.currentStep = currentStep);

    }

    /**
     * Gibt den nächsten schritt zurück OHNE dabei den internen Stepzähler zu
     * ändern.
     * 
     * @param step
     * @return
     */
    public PluginStep getNextStep(PluginStep step) {
        if (steps != null && steps.size() > 0) {

            if (currentStep == null) {
                return steps.firstElement();

            } else {
                int index = steps.indexOf(currentStep) + 1;
                if (steps.size() > index) {
                    return steps.elementAt(index);

                } else {
                    return null;
                }
            }
        } else {
            return null;
        }

    }

    /**
     * Gibt ausgehend von übergebenem Schritt den vorherigen zurück
     * 
     * @param currentStep
     * @return Gibt den vorherigen step relativ zu currentstep zurück
     */
    public PluginStep previousStep(PluginStep currentStep) {

        if (steps != null || steps.size() > 0) {

            if (currentStep == null) {
                currentStep = steps.lastElement();

            } else {
                int index = steps.indexOf(currentStep) - 1;
                if (index >= 0) {
                    currentStep = steps.elementAt(index);

                } else {
                    currentStep = null;
                }
            }
        } else {
            currentStep = null;
        }

        logger.info("previous: " + currentStep + "<-" + this.currentStep);
        return (this.currentStep = currentStep);
    }

    /**
     * @author JD-Team Setzt den Pluginfortschritt zurück. Wird Gebraucht um
     *         einen Download nochmals zu starten, z.B. nach dem reconnect
     */
    public void resetSteps() {
        currentStep = null;
        for (int i = 0; i < steps.size(); i++) {
            steps.elementAt(i).setStatus(PluginStep.STATUS_TODO);
        }
        // firePluginDataChanged();
    }

    // private void firePluginDataChanged() {
    // JDUtilities.getController().fireControlEvent(new ControlEvent(this,
    // ControlEvent.CONTROL_DOWNLOADLINKS_CHANGED));
    // }
    /**
     * Create MenuItems erlaubt es den Plugins eine MenuItemliste zurückzugeben.
     * Die Gui kann diese Menüpunkte dann darstellen. Die Gui muss das Menu bei
     * jedem zugriff neu aufbauen, weil sich das MenuItem Array geändert haben
     * kann. MenuItems sind Datenmodelle für ein TreeMenü.
     */
    public abstract ArrayList<MenuItem> createMenuitems();

    /**
     * @author olimex Fügt Map als String mit Trennzeichen zusammen TODO:
     *         auslagern
     * @param map
     *            Map
     * @param delPair
     *            Trennzeichen zwischen Key und Value
     * @param delMap
     *            Trennzeichen zwischen Map-Einträgen
     * @return Key-value pairs
     */
    public static String joinMap(Map<String, String> map, String delPair, String delMap) {
        StringBuffer buffer = new StringBuffer();
        boolean first = true;
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (first)
                first = false;
            else
                buffer.append(delMap);
            buffer.append(entry.getKey());
            buffer.append(delPair);
            buffer.append(entry.getValue());
        }
        return buffer.toString();
    }


    /**
     * @author JD-Team
     * @return Gibt den aktuellen Schritt oder null zurück
     */
    public PluginStep getCurrentStep() {
        return currentStep;
    }

    /**
     * Hier wird geprüft, ob das Plugin diesen Text oder einen Teil davon
     * handhaben kann. Dazu wird einfach geprüft, ob ein Treffer des Patterns
     * vorhanden ist.
     * 
     * @param data
     *            der zu prüfende Text
     * @return wahr, falls ein Treffer gefunden wurde.
     */
    public synchronized boolean canHandle(String data) {
        if (data == null) return false;
        Pattern pattern = getSupportedLinks();
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) { return true; }
        }
        return false;
    }

    /**
     * Diese Funktion schneidet alle Vorkommnisse des vom Plugin unterstützten
     * Pattern aus
     * 
     * @param data
     *            Text, aus dem das Pattern ausgeschnitter werden soll
     * @return Der resultierende String
     */
    public String cutMatches(String data) {
        return data.replaceAll(getSupportedLinks().pattern(), "--CUT--");
    }

    /**
     * Holt den Dateinamen aus einem Content-Disposition header. wird dieser
     * nicht gefunden, wird der dateiname aus der url ermittelt
     * 
     * @param urlConnection
     * @return Filename aus dem header (content disposition) extrahiert
     */
    public String getFileNameFormHeader(HTTPConnection urlConnection) {

        String ret;
        if (urlConnection.getHeaderField("content-disposition") == null || urlConnection.getHeaderField("content-disposition").indexOf("filename=") < 0) {

            return getFileNameFormURL(urlConnection.getURL());
        }

        String cd = urlConnection.getHeaderField("content-disposition").toLowerCase();
        ret = urlConnection.getHeaderField("content-disposition").substring(cd.indexOf("filename=") + 9);
        while (ret.startsWith("\"") || ret.startsWith("'"))
            ret = ret.substring(1);
        while (ret.endsWith("\"") || ret.endsWith("'")|| ret.endsWith(";"))
            ret = ret.substring(0, ret.length() - 1);
        try {

            ret = URLDecoder.decode(ret, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        ;
        return ret;
    }

    protected String getFileNameFormURL(URL url) {
        int index = Math.max(url.getFile().lastIndexOf("/"), url.getFile().lastIndexOf("\\"));
        return url.getFile().substring(index + 1);
    }
    protected void sleep(int i, DownloadLink downloadLink) throws InterruptedException {
        while (i > 0 && !downloadLink.getDownloadLinkController().isAborted()) {

            i -= 1000;
            downloadLink.setStatusText(String.format(JDLocale.L("gui.downloadlink.status.wait", "wait %s min"), JDUtilities.formatSeconds(i / 1000)));
            downloadLink.requestGuiUpdate();
            Thread.sleep(1000);

        }

    }

    /**
     * Diese Methode erstellt einen einzelnen String aus einer HashMap mit
     * Parametern für ein Post-Request.
     * 
     * @param parameters
     *            HashMap mit den Parametern
     * @return Codierter String
     */
    protected String createPostParameterFromHashMap(HashMap<String, String> parameters) {
        StringBuffer parameterLine = new StringBuffer();
        String parameter;
        Iterator<String> iterator = parameters.keySet().iterator();
        String key;
        while (iterator.hasNext()) {
            key = iterator.next();
            parameter = parameters.get(key);
            try {
                if (parameter != null) parameter = URLEncoder.encode(parameter, "US-ASCII");
            } catch (UnsupportedEncodingException e) {
            }
            parameterLine.append(key);
            parameterLine.append("=");
            parameterLine.append(parameter);
            if (iterator.hasNext()) parameterLine.append("&");
        }
        return parameterLine.toString();
    }

   
    /**
     * verwendet die erste Acaptcha Interaction um den captcha auszuwerten
     * 
     * @param file
     * @param plugin
     * @return captchacode
     */
    public static String getCaptchaCode(File file, Plugin plugin) {
        String captchaText = null;
        captchaText = JDUtilities.getCaptcha(plugin, null, file, false);
        return captchaText;
    }

    /**
     * gibt das interne properties objekt zurück indem die Plugineinstellungen
     * gespeichert werden
     * 
     * @return internes property objekt
     */
    public SubConfiguration getProperties() {

        if (!JDUtilities.getResourceFile("config/" + this.getPluginName() + ".cfg").exists()) {
            SubConfiguration cfg = JDUtilities.getSubConfig(this.getPluginName());
            if (JDUtilities.getConfiguration().getProperty("PluginConfig_" + this.getPluginName()) != null) {
                cfg.setProperties(((Property) JDUtilities.getConfiguration().getProperty("PluginConfig_" + this.getPluginName())).getProperties());
                cfg.save();
                return cfg;
            }
            return JDUtilities.getSubConfig(this.getPluginName());
        } else {
            return JDUtilities.getSubConfig(this.getPluginName());
        }

    }

    /**
     * 
     * @return gibt den namen des Links an der gerade verarbeitet wird
     */
    public abstract String getLinkName();

    /**
     * Gibt den Statustext des Plugins zurück. kann von der GUI aufgerufen
     * werden
     * 
     * @return Statustext
     */
    public String getStatusText() {
        if (this.statusText == null) this.statusText = "";
        return this.statusText;
    }

    /**
     * Setzte den Statustext des Plugins.
     * 
     * @param value
     */
    public void setStatusText(String value) {
        statusText = value;
    }

    public String getInitID() {
        return this.initTime + "<ID";
    }

    /**
     * Gibt einen Cookie mit Hilfe der cookies.txt von Mozilla bzw. FireFox aus
     * 
     * @param link
     * @param cookiefile
     * @return
     */
    public static String parseMozillaCookie(URL link, File cookiefile) {
        if (cookiefile.isFile()) {
            try {
                Pattern cookiePattern = Pattern.compile(".*?[\\s]+(TRUE|FALSE)[\\s]+/(.*?)[\\s]+(TRUE|FALSE)[\\s]+[0-9]{10}[\\s]+(.*?)[\\s]+(.*)", Pattern.CASE_INSENSITIVE);
                HashMap<String, String> inp = new HashMap<String, String>();
                String thisLine;
                FileInputStream fin = new FileInputStream(cookiefile);
                BufferedReader myInput = new BufferedReader(new InputStreamReader(fin));
                String hostname = link.getHost().toLowerCase();
                String hostname2 = hostname + ".*";
                if (hostname.matches(".*?\\..*?\\..*?")) hostname = hostname.replaceFirst(".*?\\.", ".");
                hostname = "\\.?" + hostname + ".*";
                String path = link.getPath();
                while ((thisLine = myInput.readLine()) != null) {
                    if (thisLine.toLowerCase().matches(hostname) || thisLine.toLowerCase().matches(hostname2)) {
                        Matcher matcher = cookiePattern.matcher(thisLine);
                        if (matcher.find()) {
                            String path2 = matcher.group(2);
                            if (!path2.matches("[\\s]*")) {
                                path2 = "/?" + path2 + ".*";
                                if (path.matches(path2)) inp.put(matcher.group(4), matcher.group(5));
                            } else
                                inp.put(matcher.group(4), matcher.group(5));
                        }
                    }
                }
                return joinMap(inp, "=", "; ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Gibt die Passwörter als String aus bsp. {"Passwort1","Passwort2"}
     * 
     * @param data
     * @return
     */
    public static String findPassword(String data) {
        Vector<String> passwords = HTMLParser.findPasswords(data);
        return JUnrar.passwordArrayToString(passwords.toArray(new String[passwords.size()]));
    }

    public void setLastCaptcha(Captcha captcha) {
        this.lastCaptcha = captcha;

    }

    public Captcha getLastCaptcha() {
        return lastCaptcha;
    }

    public void setCaptchaDetectID(int captchaJac) {
        captchaDetectionID = captchaJac;

    }

    public int getCaptchaDetectionID() {
        return captchaDetectionID;
    }

    public void setCaptchaDetectionID(int captchaDetectionID) {
        this.captchaDetectionID = captchaDetectionID;
    }

    public void clean() {
        this.lastCaptcha = null;
        this.requestInfo = null;
        System.gc();
        System.runFinalization();

    }

    public Vector<PluginStep> getSteps() {
        return steps;
    }

}
