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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import jd.captcha.pixelgrid.Captcha;
import jd.config.ConfigContainer;
import jd.config.Configuration;
import jd.config.MenuItem;
import jd.config.Property;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.unrar.JUnrar;
import jd.utils.JDUtilities;

/**
 * Diese abstrakte Klasse steuert den Zugriff auf weitere Plugins. Alle Plugins
 * müssen von dieser Klasse abgeleitet werden.
 * 
 * Alle Plugins verfügen über einen Event Mechanismus
 */
public abstract class Plugin implements ActionListener {
    public static void main(String args[]) {
//        try {
//            String url = "http://67oj3rgson.rapidsafe.de/";
//            setReadTimeout(120000);
////            for( byte i=0; i<255;i++){
////                logger.info(i+" : "+new String(new byte[]{i}));
////            }
//            setConnectTimeout(120000);
//            JDUtilities.getSubConfig("DOWNLOAD").save();
//
//            RequestInfo ri = Plugin.getRequest(new URL(url));
//            @SuppressWarnings("unused")
//            String cookie = ri.getCookie();
//            @SuppressWarnings("unused")
//            String[] dat = Plugin.getSimpleMatches(ri.getHtmlCode(), "RapidSafePSC('°=°&t=°','°');");
//            // 1ud22p2l45po=2a8774jfv7ag&t=19e5jrabravg
//            ri = Plugin.postRequest(new URL(url), cookie, url, null, dat[0] + "=" + dat[1] + "&t=" + dat[2], false);
//            dat = Plugin.getSimpleMatches(ri.getHtmlCode(), "RapidSafePSC('°&adminlogin='");
//            // 1ud22p2l45po=2a8774jfv7ag&t=6e8qkspmv5e0&fchk=1
//            ri = Plugin.postRequest(new URL(url), cookie, url, null, dat[0] + "&f=1", false);
//            String flash = Plugin.getSimpleMatch(ri.getHtmlCode(), "<param name=\"movie\" value=\"/°\" />", 0);
//            HTTPConnection con = new HTTPConnection(new URL(url + flash).openConnection());
//            con.setRequestProperty("Cookie", cookie);
//            con.setRequestProperty("Referer", url);
//
//            BufferedInputStream input = new BufferedInputStream(con.getInputStream());
//            StringBuffer sb = new StringBuffer();
//            
//          
//            byte[] b = new byte[1];    
//            Vector<String> key = new Vector<String>();
//            while (input.read(b) != -1) {
//                if(b[0]<0||b[0]>34||b[0]==9||b[0]==10||b[0]==13){
//                    sb.append(new String(b));
//                }else{
//                    sb.append(".");
//                }
//                if(sb.toString().endsWith("–.....")){
//                    input.read(b);
//                    key.add("0x"+Integer.toString((int)b[0], 16)+"("+new String(b)+")");
//                    if(b[0]<0||b[0]>34||b[0]==9||b[0]==10||b[0]==13){
//                        sb.append(new String(b));
//                    }else{
//                        sb.append(".");
//                    }
//                }
//            }
//            input.close();    
//            logger.info(""+sb);
//            logger.info(""+key);
//            //ax5
////            p="ax5.ccax4.ax3.ax7.ax6.ax1.ax2.this.getURL.......(.{4})<\x96.....(.{4})<\x96.....(.{4})<\x96.....(.{4})<\x96.....(.{4})<\x96.....(.{4})<\x96.....(.{4})<\x96";
////            p="ax5.ccax4.ax3.ax7.ax6.ax1.ax2.this.getURL.–.....°...<–.....°...<–.....°...<–.....°...<–.....°...<–.....°...<–.........<–...#......–.....–.......G–...    ...Gb–.....–.......Gbb–.......b–"
//        } catch (Exception e) {
//
//            e.printStackTrace();
//        }
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

    public boolean aborted = false;

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
    public static Pattern getSupportPattern(String patternString) {
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
        String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS", calendar);
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

    private static final int DOWNLOAD_ERROR_0_BYTE_TOLOAD = 10;

    public static final int CAPTCHA_JAC = 0;

    public static final int CAPTCHA_USER_INPUT = 1;

    private static SubConfiguration CONFIGS = null;

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
    private Property properties;

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
    public void abort() {
        aborted = true;

    }

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
     * Sammelt Cookies einer HTTP-Connection und fügt dieser einer Map hinzu
     * 
     * @author olimex
     * @param con
     *            Connection
     * @return HashMap mit allen cookies
     */
    public static HashMap<String, String> collectCookies(HTTPConnection con) {
        Collection<String> cookieHeaders = con.getHeaderFields().get("Set-Cookie");
        HashMap<String, String> cookieMap = new HashMap<String, String>();
        if (cookieHeaders == null) return cookieMap;
        for (String header : cookieHeaders) {
            try {
                StringTokenizer st = new StringTokenizer(header, ";=");
                while (st.hasMoreTokens())
                    cookieMap.put(st.nextToken().trim(), st.nextToken().trim());
            } catch (NoSuchElementException e) {
                // ignore
            }
        }
        return cookieMap;
    }

    /**
     * @author JD-Team Gibt den kompletten Cookiestring zurück, auch wenn die
     *         Cookies über mehrere Header verteilt sind
     * @param con
     * @return cookiestring
     */
    public static String getCookieString(HTTPConnection con) {
        String cookie = "";
        try {
            List<String> list = con.getHeaderFields().get("Set-Cookie");
            ListIterator<String> iter = list.listIterator(list.size());
            boolean last = false;
            while (iter.hasPrevious()) {
                cookie += (last ? "; " : "") + iter.previous().replaceFirst("; expires=.*", "");
                last = true;
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return cookie;
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
     * Findet ein einzelnes Vorkommen und liefert den vollständigen Treffer oder
     * eine Untergruppe zurück
     * 
     * @param data
     *            Der zu durchsuchende Text
     * @param pattern
     *            Das Muster, nach dem gesucht werden soll
     * @param group
     *            Die Gruppe, die zurückgegeben werden soll. 0 ist der
     *            vollständige Treffer.
     * @return Der Treffer
     */
    public String getFirstMatch(String data, Pattern pattern, int group) {
        String hit = null;
        if (data == null) return null;
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find() && group <= matcher.groupCount()) {
                hit = matcher.group(group);
            }
        }
        return hit;
    }

    /**
     * Diese Methode findet alle Vorkommnisse des Pluginpatterns in dem Text,
     * und gibt die Treffer als Vector zurück
     * 
     * @param data
     *            Der zu durchsuchende Text
     * @param pattern
     *            Das Muster, nach dem gesucht werden soll
     * @return Alle Treffer in dem Text
     */
    public Vector<String> getMatches(String data, Pattern pattern) {
        Vector<String> hits = null;
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) {
                hits = new Vector<String>();
                int position = 0;
                while (matcher.find(position)) {
                    hits.add(matcher.group());
                    position = matcher.start() + matcher.group().length();
                }
            }
        }
        return hits;
    }

    /**
     * Zählt, wie oft das Pattern des Plugins in dem übergebenen Text vorkommt
     * 
     * @param data
     *            Der zu durchsuchende Text
     * @param pattern
     *            Das Pattern, daß im Text gefunden werden soll
     * 
     * @return Anzahl der Treffer
     */
    protected int countOccurences(String data, Pattern pattern) {
        int position = 0;
        int occurences = 0;
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            while (matcher.find(position)) {
                occurences++;
                position = matcher.start() + matcher.group().length();
            }
        }
        return occurences;
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
     * Hier kann man den Text zwischen zwei Suchmustern ausgeben lassen
     * Zeilenumbrueche werden dabei auch unterstuetzt
     * 
     * @param data
     *            Der zu durchsuchende Text
     * @param startPattern
     *            der Pattern, bei dem die Suche beginnt
     * @param lastPattern
     *            der Pattern, bei dem die Suche endet
     * 
     * @return der Text zwischen den gefundenen stellen oder, falls nichts
     *         gefunden wurde, der vollständige Text
     */
    public static String getBetween(String data, String startPattern, String lastPattern) {
        Pattern p = Pattern.compile("(?s)" + startPattern + "(.*?)" + lastPattern, Pattern.CASE_INSENSITIVE);
        Matcher match = p.matcher(data);
        if (match.find()) return match.group(1);
        return data;
    }

    /**
     * Diese Methode sucht die vordefinierten input type="hidden" zwischen
     * startpattern und lastpattern und formatiert sie zu einem poststring z.b.
     * würde bei:
     * 
     * <input type="hidden" name="f" value="f50b0f" /> <input type="hidden"
     * name="h" value="390b4be0182b85b0" /> <input type="hidden" name="b"
     * value="9" />
     * 
     * f=f50b0f&h=390b4be0182b85b0&b=9 rauskommen
     * 
     * @param data
     *            Der zu durchsuchende Text
     * @param startPattern
     *            der Pattern, bei dem die Suche beginnt
     * @param lastPattern
     *            der Pattern, bei dem die Suche endet
     * 
     * @return ein String, der als POST Parameter genutzt werden kann und alle
     *         Parameter des Formulars enthält
     */
    public String getFormInputHidden(String data, String startPattern, String lastPattern) {
        return getFormInputHidden(getBetween(data, startPattern, lastPattern));
    }

    /**
     * Diese Methode sucht die vordefinierten input type="hidden" und formatiert
     * sie zu einem poststring z.b. würde bei:
     * 
     * <input type="hidden" name="f" value="f50b0f" /> <input type="hidden"
     * name="h" value="390b4be0182b85b0" /> <input type="hidden" name="b"
     * value="9" />
     * 
     * f=f50b0f&h=390b4be0182b85b0&b=9 ausgegeben werden
     * 
     * @param data
     *            Der zu durchsuchende Text
     * 
     * @return ein String, der als POST Parameter genutzt werden kann und alle
     *         Parameter des Formulars enthält
     */
    public static String getFormInputHidden(String data) {
        return joinMap(getInputHiddenFields(data), "=", "&");
    }

    /**
     * Ermittelt alle hidden input felder in einem HTML Text und gibt die hidden
     * variables als hashmap zurück es wird dabei nur der text zwischen start
     * dun endpattern ausgewertet
     * 
     * @param data
     * @param startPattern
     * @param lastPattern
     * @return hashmap mit hidden input variablen zwischen startPattern und
     *         endPattern
     */
    public HashMap<String, String> getInputHiddenFields(String data, String startPattern, String lastPattern) {
        return getInputHiddenFields(getBetween(data, startPattern, lastPattern));
    }

    /**
     * Gibt alle Hidden fields als hasMap zurück
     * 
     * @param data
     * @return hasmap mit allen hidden fields variablen
     */
    public static HashMap<String, String> getInputHiddenFields(String data) {
        Pattern intput1 = Pattern.compile("(?s)<[ ]?input([^>]*?type=['\"]?hidden['\"]?[^>]*?)[/]?>", Pattern.CASE_INSENSITIVE);
        Pattern intput2 = Pattern.compile("name=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE);
        Pattern intput3 = Pattern.compile("value=['\"]([^'\"]*?)['\"]", Pattern.CASE_INSENSITIVE);
        Pattern intput4 = Pattern.compile("name=([^\\s]*)", Pattern.CASE_INSENSITIVE);
        Pattern intput5 = Pattern.compile("value=([^\\s]*)", Pattern.CASE_INSENSITIVE);
        Matcher matcher1 = intput1.matcher(data);
        Matcher matcher2;
        Matcher matcher3;
        Matcher matcher4;
        Matcher matcher5;
        HashMap<String, String> ret = new HashMap<String, String>();
        boolean iscompl;
        while (matcher1.find()) {
            matcher2 = intput2.matcher(matcher1.group(1) + " ");
            matcher3 = intput3.matcher(matcher1.group(1) + " ");
            matcher4 = intput4.matcher(matcher1.group(1) + " ");
            matcher5 = intput5.matcher(matcher1.group(1) + " ");
            iscompl = false;
            String key, value;
            key = value = null;
            if (matcher2.find()) {
                iscompl = true;
                key = matcher2.group(1);
            } else if (matcher4.find()) {
                iscompl = true;
                key = matcher4.group(1);
            }
            if (matcher3.find() && iscompl)
                value = matcher3.group(1);
            else if (matcher5.find() && iscompl)
                value = matcher5.group(1);
            else
                iscompl = false;
            ret.put(key, value);
        }
        return ret;
    }

    /**
     * Schickt ein GetRequest an eine Adresse
     * 
     * @param link
     *            Die URL, die ausgelesen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo getRequest(URL link) throws IOException {
        return getRequest(link, null, null, false);
    }

    public static int getReadTimeoutFromConfiguration() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, 10000);
    }

    public static int getConnectTimeoutFromConfiguration() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, 10000);
    }

    public static void setReadTimeout(int value) {
        JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_READ_TIMEOUT, value);
    }

    public static void setConnectTimeout(int value) {
        JDUtilities.getSubConfig("DOWNLOAD").setProperty(Configuration.PARAM_DOWNLOAD_CONNECT_TIMEOUT, value);
    }

    /**
     * Schickt ein GetRequest an eine Adresse
     * 
     * @param link
     *            Der Link, an den die GET Anfrage geschickt werden soll
     * @param cookie
     *            Cookie
     * @param referrer
     *            Referrer
     * @param redirect
     *            Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo getRequest(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        // logger.finer("get: "+link+"(cookie: "+cookie+")");
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(getReadTimeoutFromConfiguration());
        httpConnection.setConnectTimeout(getConnectTimeoutFromConfiguration());
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null) httpConnection.setRequestProperty("Referer", referrer);

        // httpConnection.setRequestProperty("Referer", "http://" +
        // link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO User-Agent als Option ins menu
        // hier koennte man mit einer kleinen Datenbank den User-Agent rotieren
        // lassen
        // so ist das Programm nicht so auffallig
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        RequestInfo requestInfo = readFromURL(httpConnection);
        requestInfo.setConnection(httpConnection);
        // logger.finer("getRequest " + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return requestInfo;
    }

    public static RequestInfo headRequest(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        // logger.finer("get: "+link+"(cookie: "+cookie+")");
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(getReadTimeoutFromConfiguration());
        httpConnection.setConnectTimeout(getConnectTimeoutFromConfiguration());
        httpConnection.setRequestMethod("HEAD");
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null) httpConnection.setRequestProperty("Referer", referrer);

        // httpConnection.setRequestProperty("Referer", "http://" +
        // link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        RequestInfo requestInfo = readFromURL(httpConnection);
        requestInfo.setConnection(httpConnection);
        // logger.finer("headRequest " + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return requestInfo;
    }

    /**
     * Führt einen getrequest durch. Gibt die headerinfos zurück, lädt aber die
     * datei noch komplett
     * 
     * @param link
     * @param cookie
     * @param referrer
     * @param redirect
     * @return requestinfos mit headerfields. HTML text wird nicht!! geladen
     * @throws IOException
     */
    public static RequestInfo getRequestWithoutHtmlCode(URL link, String cookie, String referrer, boolean redirect) throws IOException {
        return getRequestWithoutHtmlCode(link, cookie, referrer, redirect, getReadTimeoutFromConfiguration(), getConnectTimeoutFromConfiguration());

    }

    public static RequestInfo getRequestWithoutHtmlCode(URL link, String cookie, String referrer, boolean redirect, int readTimeout, int requestTimeout) throws IOException {
        // logger.finer("get: "+link);
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(readTimeout);
        httpConnection.setConnectTimeout(requestTimeout);
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) {
            httpConnection.setRequestProperty("Cookie", cookie);
        }
        // TODO User-Agent als Option ins menu
        // hier koennte man mit einer kleinen Datenbank den User-Agent rotieren
        // lassen
        // so ist das Programm nicht so auffallig
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        String location = httpConnection.getHeaderField("Location");
        String setcookie = getCookieString(httpConnection);
        int responseCode = 0;
        try {
            responseCode = httpConnection.getResponseCode();
        } catch (IOException e) {
        }

        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        // logger.finer("getReuqest wo " + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return ri;
    }

    /**
     * Führt einen getrequest durch. Gibt die headerinfos zurück, lädt aber die
     * datei noch komplett
     * 
     * @param link
     * @param cookie
     * @param referrer
     * @param redirect
     * @return requestinfos mit headerfields. HTML text wird nicht!! geladen
     * @throws IOException
     */
    public static RequestInfo getRequestWithoutHtmlCode(URL link, String cookie, String referrer, HashMap<String, String> requestProperties, boolean redirect) throws IOException {
        // logger.finer("get: "+link);
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(getReadTimeoutFromConfiguration());
        httpConnection.setConnectTimeout(getConnectTimeoutFromConfiguration());
        httpConnection.setInstanceFollowRedirects(redirect);
        // wenn referrer nicht gesetzt wurde nimmt er den host als referer
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) {
            httpConnection.setRequestProperty("Cookie", cookie);
        }

        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");

        if (requestProperties != null) {
            Set<String> keys = requestProperties.keySet();
            Iterator<String> iterator = keys.iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();

                httpConnection.setRequestProperty(key, requestProperties.get(key));
            }
        }

        httpConnection.connect();
        String location = httpConnection.getHeaderField("Location");
        String setcookie = getCookieString(httpConnection);
        int responseCode = 0;
        try {
            responseCode = httpConnection.getResponseCode();
        } catch (IOException e) {
        }

        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        // logger.finer("getRequest wo2 " + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return ri;
    }

    /**
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param link
     *            Der Link, an den die POST Anfrage geschickt werden soll
     * @param parameter
     *            Die Parameter, die übergeben werden sollen
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequest(URL link, String parameter) throws IOException {
        return postRequest(link, null, null, null, parameter, false);
    }

    /**
     * 
     * Schickt ein PostRequest an eine Adresse
     * 
     * @param string
     *            Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie
     *            Cookie
     * @param referrer
     *            Referrer
     * @param requestProperties
     *            Hier können noch zusätliche Properties mitgeschickt werden
     * @param parameter
     *            Die Parameter, die übergeben werden sollen
     * @param redirect
     *            Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequest(URL string, String cookie, String referrer, HashMap<String, String> requestProperties, String parameter, boolean redirect) throws IOException {
        return postRequest(string, cookie, referrer, requestProperties, parameter, redirect, getReadTimeoutFromConfiguration(), getConnectTimeoutFromConfiguration());

    }

    public static RequestInfo postRequest(URL url, String cookie, String referrer, HashMap<String, String> requestProperties, String parameter, boolean redirect, int readTimeout, int requestTimeout) throws IOException {
        // logger.finer("post: "+link+"(cookie:"+cookie+" parameter:
        // "+parameter+")");
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(url.openConnection());
        httpConnection.setReadTimeout(readTimeout);
        httpConnection.setConnectTimeout(requestTimeout);
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + url.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        if (requestProperties != null) {
            Set<String> keys = requestProperties.keySet();
            Iterator<String> iterator = keys.iterator();
            String key;
            while (iterator.hasNext()) {
                key = iterator.next();
                httpConnection.setRequestProperty(key, requestProperties.get(key));
            }
        }
        if (parameter != null) {
            //parameter = parameter.trim();
            httpConnection.setRequestProperty("Content-Length", parameter.length() + "");
        }
        httpConnection.setDoOutput(true);
        httpConnection.connect();

        httpConnection.post(parameter);

        RequestInfo requestInfo = readFromURL(httpConnection);

        requestInfo.setConnection(httpConnection);
        // logger.finer("postRequest " + url + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return requestInfo;
    }
    
    /**
     * Gibt header- und cookieinformationen aus ohne den HTMLCode
     * herunterzuladen
     * 
     * @param link
     *            Der Link, an den die POST Anfrage geschickt werden soll
     * @param cookie
     *            Cookie
     * @param referrer
     *            Referrer
     * @param parameter
     *            Die Parameter, die übergeben werden sollen
     * @param redirect
     *            Soll einer Weiterleitung gefolgt werden?
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo postRequestWithoutHtmlCode(URL link, String cookie, String referrer, String parameter, boolean redirect) throws IOException {
        // logger.finer("post: "+link);
        long timer = System.currentTimeMillis();
        HTTPConnection httpConnection = new HTTPConnection(link.openConnection());
        httpConnection.setReadTimeout(getReadTimeoutFromConfiguration());
        httpConnection.setConnectTimeout(getConnectTimeoutFromConfiguration());
        httpConnection.setInstanceFollowRedirects(redirect);
        if (referrer != null)
            httpConnection.setRequestProperty("Referer", referrer);
        else
            httpConnection.setRequestProperty("Referer", "http://" + link.getHost());
        if (cookie != null) httpConnection.setRequestProperty("Cookie", cookie);
        // TODO das gleiche wie bei getRequest
        httpConnection.setRequestProperty("Accept-Language", ACCEPT_LANGUAGE);
        httpConnection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)");
        httpConnection.setDoOutput(true);
        if (parameter != null) {
            if (parameter == null) parameter = "";
            parameter = parameter.trim();
            httpConnection.setRequestProperty("Content-Length", parameter.length() + "");
            
            httpConnection.connect();
            OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
            wr.write(parameter);
            wr.flush();
            wr.close();
        }
        String location = httpConnection.getHeaderField("Location");
        String setcookie = getCookieString(httpConnection);
        int responseCode = 0;
        try {
            responseCode = httpConnection.getResponseCode();
        } catch (IOException e) {
        }
        RequestInfo ri = new RequestInfo("", location, setcookie, httpConnection.getHeaderFields(), responseCode);
        ri.setConnection(httpConnection);
        // logger.finer("postRequest wo" + link + ": " +
        // (System.currentTimeMillis() - timer) + " ms");
        return ri;
    }

    /**
     * Liest Daten von einer URL. LIst den encoding type und kann plaintext und
     * gzip unterscheiden
     * 
     * @param urlInput
     *            Die URL Verbindung, von der geselen werden soll
     * @return Ein Objekt, daß alle Informationen der Zieladresse beinhält
     * @throws IOException
     */
    public static RequestInfo readFromURL(HTTPConnection urlInput) throws IOException {
        // Content-Encoding: gzip
        BufferedReader rd;
        if (urlInput.getHeaderField("Content-Encoding") != null && urlInput.getHeaderField("Content-Encoding").equalsIgnoreCase("gzip")) {
            rd = new BufferedReader(new InputStreamReader(new GZIPInputStream(urlInput.getInputStream())));
        } else {
            rd = new BufferedReader(new InputStreamReader(urlInput.getInputStream()));
        }
        String line;
        StringBuffer htmlCode = new StringBuffer();
        while ((line = rd.readLine()) != null) {
            htmlCode.append(line + "\n");
        }
        String location = urlInput.getHeaderField("Location");
        String cookie = getCookieString(urlInput);
        int responseCode = 0;
        responseCode = urlInput.getResponseCode();
        RequestInfo requestInfo = new RequestInfo(htmlCode.toString(), location, cookie, urlInput.getHeaderFields(), responseCode);
        rd.close();
        return requestInfo;
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

            int index = Math.max(urlConnection.getURL().getFile().lastIndexOf("/"), urlConnection.getURL().getFile().lastIndexOf("\\"));
            return urlConnection.getURL().getFile().substring(index + 1);
        }

        String cd = urlConnection.getHeaderField("content-disposition").toLowerCase();
        ret = urlConnection.getHeaderField("content-disposition").substring(cd.indexOf("filename=") + 9);
        while (ret.startsWith("\"") || ret.startsWith("'"))
            ret = ret.substring(1);
        while (ret.endsWith("\"") || ret.endsWith("'"))
            ret = ret.substring(0, ret.length() - 1);
        try {

            ret = URLDecoder.decode(ret, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }
        ;
        return ret;
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
     * Gibt den md5hash einer Datei als String aus
     * 
     * @param filepath
     *            Dateiname
     * @return MD5 des Datei
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     */
    public static String md5sum(String filepath) throws NoSuchAlgorithmException, FileNotFoundException {
        File f = new File(filepath);
        return md5sum(f);
    }

    /**
     * Gibt den MD5 hash von file zurück
     * 
     * @param file
     * @return MD5 Hash string
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     */
    public static String md5sum(File file) throws NoSuchAlgorithmException, FileNotFoundException {
        MessageDigest digest = MessageDigest.getInstance("MD5");
        InputStream is = new FileInputStream(file);
        byte[] buffer = new byte[8192];
        int read = 0;
        try {
            while ((read = is.read(buffer)) > 0) {
                digest.update(buffer, 0, read);
            }
            byte[] md5sum = digest.digest();
            BigInteger bigInt = new BigInteger(1, md5sum);
            String output = bigInt.toString(16);
            return output;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process file for MD5", e);
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                throw new RuntimeException("Unable to close input stream for MD5 calculation", e);
            }
        }
    }

    /**
     * Gibt die matches ohne Dublikate als arraylist aus
     * 
     * @param data
     * @param pattern
     * @return StringArray mit den Matches
     */
    public static String[] getUniqueMatches(String data, Pattern pattern) {
        ArrayList<String> set = new ArrayList<String>();
        Matcher m = pattern.matcher(data);
        while (m.find()) {
            if (!set.contains(m.group())) {
                set.add(m.group());
            }
        }
        return (String[]) set.toArray(new String[set.size()]);
    }

    /**
     * public static String[] getMatches(String source, String pattern) Gibt
     * alle treffer in source nach dem pattern zurück. Platzhalter ist nur !! °
     * 
     * @param source
     * @param pattern
     *            als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @return Alle TReffer
     */
    public static String[] getSimpleMatches(Object source, String pattern) {
        // DEBUG.trace("pattern: "+STRING.getPattern(pattern));
        if (source == null || pattern == null) return null;
        Matcher rr = Pattern.compile(getPattern(pattern), Pattern.DOTALL).matcher(source.toString());
        if (!rr.find()) {
            // Keine treffer
        }
        try {
            String[] ret = new String[rr.groupCount()];
            for (int i = 1; i <= rr.groupCount(); i++) {
                ret[i - 1] = rr.group(i);
            }
            return ret;
        } catch (IllegalStateException e) {
            return null;
        }
    }

    /**
     * Durchsucht source mit pattern ach treffern und gibt Treffer id zurück.
     * Bei dem Pattern muss es sich um einen String Handeln der ° als
     * Platzhalter verwendet. Alle newline Chars in source müssen mit einem
     * °-PLatzhalter belegt werden
     * 
     * @param source
     * @param pattern
     * @param id
     * @return String Match
     */
    public static String getSimpleMatch(Object source, String pattern, int id) {

        String[] res = getSimpleMatches(source.toString(), pattern);
        if (res != null && res.length > id) { return res[id]; }
        return null;
    }

    /**
     * public static String getPattern(String str) Gibt ein Regex pattern
     * zurück. ° dient als Platzhalter!
     * 
     * @param str
     * @return REgEx Pattern
     */
    public static String getPattern(String str) {
        String allowed = "QWERTZUIOPÜASDFGHJKLÖÄYXCVBNMqwertzuiopasdfghjklyxcvbnm 1234567890";
        String ret = "";
        int i;
        for (i = 0; i < str.length(); i++) {
            char letter = str.charAt(i);
            // 176 == °
            if (letter == 176) {
                ret += "(.*?)";
            } else if (allowed.indexOf(letter) == -1) {
                ret += "\\" + letter;
            } else {
                ret += letter;
            }
        }
        return ret;
    }

    /**
     * Schreibt alle Treffer von pattern in source in den übergebenen Vector.
     * Als Rückgabe erhält man einen 2D-Vector
     * 
     * @param source
     *            Quelltext
     * @param pattern
     *            als Pattern wird ein Normaler String mit ° als Wildcard
     *            verwendet.
     * @return Treffer
     */
    public static ArrayList<ArrayList<String>> getAllSimpleMatches(Object source, String pattern) {
        return getAllSimpleMatches(source.toString(), Pattern.compile(getPattern(pattern), Pattern.DOTALL));
    }

    /**
     * Schreibt alle treffer von pattern in source in den übergebenen vector Als
     * Rückgabe erhält man einen 2D-Vector
     * 
     * @param source
     *            Quelltext
     * @param pattern
     *            Ein RegEx Pattern
     * @return Treffer
     */
    public static ArrayList<ArrayList<String>> getAllSimpleMatches(Object source, Pattern pattern) {
        ArrayList<ArrayList<String>> ret = new ArrayList<ArrayList<String>>();
        ArrayList<String> entry;
        String tmp;
        for (Matcher r = pattern.matcher(source.toString()); r.find();) {
            entry = new ArrayList<String>();
            for (int x = 1; x <= r.groupCount(); x++) {
                tmp = r.group(x).trim();
                entry.add(JDUtilities.UTF8Decode(tmp));
            }
            ret.add(entry);
        }
        return ret;
    }

    /**
     * Gibt von allen treffer von pattern in source jeweils den id-ten Match
     * einem vector zurück. Als pattern kommt ein Simplepattern zum einsatz
     * 
     * @param source
     * @param pattern
     * @param id
     * @return Matchlist
     */
    public static ArrayList<String> getAllSimpleMatches(Object source, String pattern, int id) {
        pattern = getPattern(pattern);
        ArrayList<String> ret = new ArrayList<String>();
        for (Matcher r = Pattern.compile(pattern, Pattern.DOTALL).matcher(source.toString()); r.find();) {
            if (id <= r.groupCount()) ret.add(r.group(id).trim());
        }
        return ret;
    }

    public static ArrayList<String> getAllSimpleMatches(Object source, Pattern pattern, int id) {

        ArrayList<String> ret = new ArrayList<String>();
        for (Matcher r = pattern.matcher(source.toString()); r.find();) {
            if (id <= r.groupCount()) ret.add(r.group(id).trim());
        }
        return ret;
    }

    /**
     * Gibt über die simplepattern alle den x/y ten treffer aus dem 2D-matches
     * array zurück
     * 
     * @param source
     * @param pattern
     * @param x
     * @param y
     * @return treffer an der stelle x/y im 2d treffer array
     */
    public static String getSimpleMatch(String source, String pattern, int x, int y) {
        ArrayList<ArrayList<String>> ret = getAllSimpleMatches(source, pattern);
        if (ret.get(x) != null && ret.get(x).get(y) != null) { return ret.get(x).get(y); }
        return null;
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

    /**
     * Sucht alle Links heraus
     * 
     * @param data
     *            ist der Quelltext einer Html-Datei
     * @param url
     *            der Link von dem der Quelltext stammt (um die base automatisch
     *            zu setzen)
     * @return Linkliste aus data extrahiert
     */
    /*
     * 
     * public static void testGetHttpLinks() throws IOException { String input =
     * ""; String thisLine; BufferedReader br = new BufferedReader(new
     * FileReader("index.html")); while ((thisLine = br.readLine()) != null)
     * input += thisLine + "\n"; String[] dd = getHttpLinks(input,
     * "http://www.google.de/"); for (int i = 0; i < dd.length; i++)
     * System.out.println(dd[i]); }
     */
    public static String[] getHttpLinks(String data, String url) {
        String[] protocols = new String[] { "h.{2,3}", "https", "ccf", "dlc", "ftp" };
        String protocolPattern = "(";
        for (int i = 0; i < protocols.length; i++) {
            protocolPattern += protocols[i] + ((i + 1 == protocols.length) ? ")" : "|");
        }

        String[] patternStr = { "(?s)<[ ]?base[^>]*?href=['\"]([^>]*?)['\"]", "(?s)<[ ]?base[^>]*?href=([^'\"][^\\s]*)", "(?s)<[ ]?a[^>]*?href=['\"]([^>]*?)['\"]", "(?s)<[ ]?a[^>]*?href=([^'\"][^\\s]*)", "(?s)<[ ]?form[^>]*?action=['\"]([^>]*?)['\"]", "(?s)<[ ]?form[^>]*?action=([^'\"][^\\s]*)", "www[^\\s>'\"\\)]*", protocolPattern + "://[^\\s>'\"\\)]*" };
        url = url == null ? "" : url;
        Matcher m;
        String link;
        Pattern[] pattern = new Pattern[patternStr.length];
        for (int i = 0; i < patternStr.length; i++) {
            pattern[i] = Pattern.compile(patternStr[i], Pattern.CASE_INSENSITIVE);
        }
        String basename = "";
        String host = "";
        LinkedList<String> set = new LinkedList<String>();
        for (int i = 0; i < 2; i++) {
            m = pattern[i].matcher(data);
            if (m.find()) {
                url = JDUtilities.htmlDecode(m.group(1));
                break;
            }
        }
        if (url != null) {
            url = url.replace("http://", "");
            int dot = url.lastIndexOf('/');
            if (dot != -1)
                basename = url.substring(0, dot + 1);
            else
                basename = "http://" + url + "/";
            dot = url.indexOf('/');
            if (dot != -1)
                host = "http://" + url.substring(0, dot);
            else
                host = "http://" + url;
            url = "http://" + url;
        } else
            url = "";
        for (int i = 2; i < 6; i++) {
            m = pattern[i].matcher(data);
            while (m.find()) {
                link = JDUtilities.htmlDecode(m.group(1));
                link = link.replaceAll(protocols[0] + "://", "http://");
                link = link.replaceAll("https?://.*http://", "http://");
                for (int j = 1; j < protocols.length; j++) {
                    link = link.replaceAll("https?://.*" + protocols[j] + "://", protocols[j] + "://");
                }

                if ((link.length() > 6) && (link.substring(0, 7).equals("http://")))
                    ;
                else if (link.length() > 0) {
                    if (link.length() > 2 && link.substring(0, 3).equals("www")) {
                        link = "http://" + link;
                    }
                    if (link.charAt(0) == '/') {
                        link = host + link;
                    } else if (link.charAt(0) == '#') {
                        link = url + link;
                    } else {
                        link = basename + link;
                    }
                }
                if (!set.contains(link)) {
                    set.add(link);
                }
            }
        }
        data = data.replaceAll("(?s)<.*?>", "");
        m = pattern[6].matcher(data);
        while (m.find()) {
            link = "http://" + m.group();
            link = JDUtilities.htmlDecode(link);
            link = link.replaceAll(protocols[0] + "://", "http://");
            link = link.replaceFirst("^www\\..*" + protocols[0] + "://", "http://");
            link = link.replaceAll("https?://.*http://", "http://");
            for (int j = 1; j < protocols.length; j++) {
                link = link.replaceFirst("^www\\..*" + protocols[j] + "://", protocols[j] + "://");
            }
            if (!set.contains(link)) {
                set.add(link);
            }
        }
        m = pattern[7].matcher(data);
        while (m.find()) {
            link = m.group();
            link = JDUtilities.htmlDecode(link);
            link = link.replaceAll(protocols[0] + "://", "http://");
            link = link.replaceAll("https?://.*http://", "http://");
            for (int j = 1; j < protocols.length; j++) {
                link = link.replaceAll("https?://.*" + protocols[j] + "://", protocols[j] + "://");
            }
            // .replaceFirst("h.*?://",
            // "http://").replaceFirst("http://.*http://", "http://");
            if (!set.contains(link)) {
                set.add(link);
            }
        }
        return (String[]) set.toArray(new String[set.size()]);
    }

    /**
     * Gibt alle links die in data gefunden wurden als Stringliste zurück
     * 
     * @param data
     * @return STringliste
     */
    public static String getHttpLinkList(String data) {
        String[] links = getHttpLinks(data, "%HOST%");
        String ret = "";
        for (int i = 0; i < links.length; i++) {
            ret += "\"" + links[i] + "\"\r\n";
        }
        return ret;
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
     * Diese Methode sucht nach passwörtern in einem Datensatz
     * 
     * @param data
     * @return
     */
    public static Vector<String> findPasswords(String data) {
        if (data == null) return new Vector<String>();
        Iterator<String> iter = JUnrar.getPasswordList().iterator();
        Vector<String> ret = new Vector<String>();
        while (iter.hasNext()) {
            String pass = (String) iter.next();
            if (data.contains(pass)) ret.add(pass);
        }
        data = data.replaceAll("(?s)<!-- .*? -->", "").replaceAll("(?s)<script .*?>.*?</script>", "").replaceAll("(?s)<.*?>", "").replaceAll("Spoiler:", "").replaceAll("(no.{0,2}|kein.{0,8}|ohne.{0,8}|nicht.{0,8})(pw|passwort|password|pass)", "").replaceAll("(pw|passwort|password|pass).{0,12}(nicht|falsch|wrong)", "");

        Pattern pattern = Pattern.compile("(pw|passwort|password|pass)[\\s][\\s]*?[\"']([[^\\:\"'\\s]][^\"'\\s]*)[\"']?", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) ret.add(pass);
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s][\\s]*?([[^\\:\"'\\s]][^\"'\\s]*)[\\s]?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 4 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) ret.add(pass);
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?\\:[\\s]*?[\"']([^\"']+)[\"']?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) ret.add(pass);
        }
        pattern = Pattern.compile("(pw|passwort|password|pass)[\\s]?\\:[\\s]*?([^\"'\\s]+)[\\s]?", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(data);
        while (matcher.find()) {
            String pass = matcher.group(2);
            if (pass.length() > 2 && !pass.matches(".*(rar|zip|jpg|gif|png|html|php|avi|mpg)$") && !ret.contains(pass)) ret.add(pass);
        }

        return ret;
    }

    /**
     * Gibt die Passwörter als String aus bsp. {"Passwort1","Passwort2"}
     * 
     * @param data
     * @return
     */
    public static String findPassword(String data) {
        Vector<String> passwords = findPasswords(data);
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

}
