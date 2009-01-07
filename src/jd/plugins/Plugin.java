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
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.captcha.pixelgrid.Captcha;
import jd.config.ConfigContainer;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.event.ControlEvent;
import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.utils.JDUtilities;

/**
 * Diese abstrakte Klasse steuert den Zugriff auf weitere Plugins. Alle Plugins
 * müssen von dieser Klasse abgeleitet werden.
 * 
 * Alle Plugins verfügen über einen Event Mechanismus
 */
public abstract class Plugin implements ActionListener {

    public static final String ACCEPT_LANGUAGE = "de, en-gb;q=0.9, en;q=0.8";

    public static final int CAPTCHA_JAC = 0;

    public static final int CAPTCHA_USER_INPUT = 1;

    public static SubConfiguration CONFIGS = null;

    private boolean acceptOnlyURIs = true;
    /**
     * Ein Logger, um Meldungen darzustellen
     */
    public static Logger logger = JDUtilities.getLogger();

    /**
     * Gibt die Passwörter als String aus bsp. {"Passwort1","Passwort2"}
     * 
     * @param data
     * @return
     */
    public static String findPassword(String data) {
        Vector<String> passwords = HTMLParser.findPasswords(data);
        return JDUtilities.passwordArrayToString(passwords.toArray(new String[passwords.size()]));
    }

    /**
     * verwendet die erste Acaptcha Interaction um den captcha auszuwerten
     * 
     * @param file
     * @param plugin
     * @return captchacode
     * @throws PluginException
     * @throws InterruptedException
     */
    public static String getCaptchaCode(File file, Plugin plugin, DownloadLink link) throws PluginException, InterruptedException {
        String captchaText = JDUtilities.getCaptcha(plugin, null, file, false, link);
        if (captchaText == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        return captchaText;
    }

    public static ConversionMode DisplayDialog(ArrayList<ConversionMode> displaymodes, String name, CryptedLink link) throws InterruptedException {
        link.getProgressController().setProgressText(SimpleGUI.WAITING_USER_IO);
        JDUtilities.acquireUserIO_Semaphore();
        ConversionMode temp = ConvertDialog.DisplayDialog(displaymodes, name);
        JDUtilities.releaseUserIO_Semaphore();
        link.getProgressController().setProgressText(null);
        return temp;
    }

    public static String getCaptchaCode(Plugin plugin, String method, File file, boolean forceJAC, DownloadLink link) throws PluginException, InterruptedException {
        String captchaText = JDUtilities.getCaptcha(plugin, method, file, forceJAC, link);
        if (captchaText == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        return captchaText;
    }

    public static String getCaptchaCode(Plugin plugin, String method, File file, boolean forceJAC, CryptedLink link) throws DecrypterException, InterruptedException {
        String captchaText = JDUtilities.getCaptcha(plugin, method, file, forceJAC, link);
        if (captchaText == null) throw new DecrypterException(DecrypterException.CAPTCHA);
        return captchaText;
    }

    public static String getCaptchaCode(File file, Plugin plugin, CryptedLink link) throws DecrypterException, InterruptedException {
        String captchaText = JDUtilities.getCaptcha(plugin, null, file, false, link);
        if (captchaText == null) throw new DecrypterException(DecrypterException.CAPTCHA);
        return captchaText;
    }

    public static String getUserInput(String message, CryptedLink link) throws DecrypterException, InterruptedException {
        String password = JDUtilities.getUserInput(message, link);
        if (password == null) throw new DecrypterException(DecrypterException.PASSWORD);
        return password;
    }

    public static String getUserInput(String message, DownloadLink link) throws PluginException, InterruptedException {
        String password = JDUtilities.getUserInput(message, link);
        if (password == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        return password;
    }

    public static String getUserInput(String message, String defaultmessage, DownloadLink link) throws PluginException, InterruptedException {
        String password = JDUtilities.getUserInput(message, defaultmessage, link);
        if (password == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        return password;
    }

    public static String getUserInput(String message, String defaultmessage, CryptedLink link) throws DecrypterException, InterruptedException {
        String password = JDUtilities.getUserInput(message, defaultmessage, link);
        if (password == null) throw new DecrypterException(DecrypterException.PASSWORD);
        return password;
    }

    /**
     * Gibt die Date zurück in die der aktuelle captcha geladne werden soll.
     * 
     * @param plugin
     * @return Gibt einen Pfadzurück der für die nächste Captchadatei reserviert
     *         ist
     */
    public static File getLocalCaptchaFile(Plugin plugin, String extension) {
        if (extension == null) {
            extension = ".jpg";
        }
        Calendar calendar = Calendar.getInstance();
        String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS.", calendar) + new Random().nextInt(999);

        File dest = JDUtilities.getResourceFile("captchas/" + plugin.getHost() + "/" + date + extension);
        return dest;
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
                if (hostname.matches(".*?\\..*?\\..*?")) {
                    hostname = hostname.replaceFirst(".*?\\.", ".");
                }
                hostname = "\\.?" + hostname + ".*";
                String path = link.getPath();
                while ((thisLine = myInput.readLine()) != null) {
                    if (thisLine.toLowerCase().matches(hostname) || thisLine.toLowerCase().matches(hostname2)) {
                        Matcher matcher = cookiePattern.matcher(thisLine);
                        if (matcher.find()) {
                            String path2 = matcher.group(2);
                            if (!path2.matches("[\\s]*")) {
                                path2 = "/?" + path2 + ".*";
                                if (path.matches(path2)) {
                                    inp.put(matcher.group(4), matcher.group(5));
                                }
                            } else {
                                inp.put(matcher.group(4), matcher.group(5));
                            }
                        }
                    }
                }
                return HTMLParser.joinMap(inp, "=", "; ");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private int captchaDetectionID = -1;

    /**
     * Zeigt an, ob das Plugin abgebrochen werden soll
     */
    public ConfigContainer config;

    private Captcha lastCaptcha;

    private String statusText;

    protected PluginWrapper wrapper;

    protected Browser br;

    public Plugin(PluginWrapper wrapper) {

        this.br = new Browser();
        this.wrapper = wrapper;
        config = new ConfigContainer(this);

    }

    public PluginWrapper getWrapper() {
        return wrapper;
    }

    public void actionPerformed(ActionEvent e) {
        return;
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
        if (data == null) { return false; }
        Pattern pattern = getSupportedLinks();
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) { return true; }
        }
        return false;
    }

    public void clean() {
        lastCaptcha = null;
        br = new Browser();
        System.gc();
        System.runFinalization();
    }

    public boolean collectCaptchas() {
        return true;
    }

    /**
     * Create MenuItems erlaubt es den Plugins eine MenuItemliste zurückzugeben.
     * Die Gui kann diese Menüpunkte dann darstellen. Die Gui muss das Menu bei
     * jedem zugriff neu aufbauen, weil sich das MenuItem Array geändert haben
     * kann. MenuItems sind Datenmodelle für ein TreeMenü.
     */
    public abstract ArrayList<MenuItem> createMenuitems();

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
     * Verwendet den JDcontroller um ein ControlEvent zu broadcasten
     * 
     * @param controlID
     * @param param
     */
    public void fireControlEvent(int controlID, Object param) {
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, controlID, param));
    }

    public int getCaptchaDetectionID() {
        return captchaDetectionID;
    }

    /**
     * Hier wird der Author des Plugins ausgelesen
     * 
     * @return Der Author des Plugins
     */
    public String getCoder() {
        return "JD-Team";
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
     * Holt den Dateinamen aus einem Content-Disposition header. wird dieser
     * nicht gefunden, wird der dateiname aus der url ermittelt
     * 
     * @param urlConnection
     * @return Filename aus dem header (content disposition) extrahiert
     * @throws Exception
     */
    static public String getFileNameFormHeader(HTTPConnection urlConnection) {
        if (urlConnection.getHeaderField("content-disposition") == null || urlConnection.getHeaderField("content-disposition").indexOf("filename") < 0) { return Plugin.getFileNameFormURL(urlConnection.getURL()); }
        return getFileNameFromDispositionHeader(urlConnection.getHeaderField("content-disposition"));
    }

    static public String getFileNameFromDispositionHeader(String header) {
        // http://greenbytes.de/tech/tc2231/
        if (header == null) return null;
        String orgheader = header;
        String contentdisposition = header;
        String filename = null;
        for (int i = 0; i < 2; i++) {
            if (contentdisposition.contains("filename*")) {
                /* Codierung default */
                contentdisposition = contentdisposition.replaceAll("filename\\*", "filename");
                String format = new Regex(contentdisposition, ".*?=[ \"']*(.+)''").getMatch(0);
                if (format == null) {
                    logger.severe("Content-Disposition: invalid format: " + header);
                    filename = null;
                    return filename;
                }
                contentdisposition = contentdisposition.replaceAll(format + "''", "");
                filename = new Regex(contentdisposition, "filename.*?=[ ]*\"(.+)\"").getMatch(0);
                if (filename == null) filename = new Regex(contentdisposition, "filename.*?=[ ]*'(.+)'").getMatch(0);
                if (filename == null) {
                    header = header.replaceAll("=", "=\"") + "\"";
                    header = header.replaceAll(";\"", "\"");
                    contentdisposition = header;
                } else {
                    try {
                        filename = URLDecoder.decode(filename, format);
                    } catch (Exception e) {
                        logger.severe("Content-Disposition: could not decode filename: " + header);
                        filename = null;
                        return filename;
                    }
                }
            } else if (new Regex(contentdisposition, "=\\?.*?\\?.*?\\?.*?\\?=").matches()) {
                /*
                 * Codierung Encoded Words, TODO: Q-Encoding und mehrfach
                 * tokens, aber noch nicht in freier Wildbahn gesehen
                 */
                String tokens[][] = new Regex(contentdisposition, "=\\?(.*?)\\?(.*?)\\?(.*?)\\?=").getMatches();
                if (tokens.length == 1 && tokens[0].length == 3 && tokens[0][1].trim().equalsIgnoreCase("B")) {
                    /* Base64 Encoded */
                    try {
                        filename = URLDecoder.decode(Encoding.Base64Decode(tokens[0][2].trim()), tokens[0][0].trim());
                    } catch (Exception e) {
                        logger.severe("Content-Disposition: could not decode filename: " + header);
                        filename = null;
                        return filename;
                    }
                }
            } else {
                /* ohne Codierung */
                filename = new Regex(contentdisposition, "filename.*?=[ ]*\"(.+)\"").getMatch(0);
                if (filename == null) filename = new Regex(contentdisposition, "filename.*?=[ ]*'(.+)'").getMatch(0);
                if (filename == null) {
                    header = header.replaceAll("=", "=\"") + "\"";
                    header = header.replaceAll(";\"", "\"");
                    contentdisposition = header;
                }
            }
            if (filename != null) {
                break;
            }
        }
        if (filename != null) filename = filename.trim();
        if (filename == null) logger.severe("Content-Disposition: could not parse header: " + orgheader);
        return filename;
    }

    static public String getFileNameFormURL(URL url) {
        return extractFileNameFromURL(url.toExternalForm());
    }

    /**
     * Gibt nur den Dateinamen aus der URL extrahiert zurück. Um auf den
     * dateinamen zuzugreifen sollte bis auf Ausnamen immer
     * DownloadLink.getName() verwendet werden
     * 
     * @return Datename des Downloads.
     */
    static public String extractFileNameFromURL(String filename) {
        int index = filename.indexOf("?");
        /*
         * erst die Get-Parameter abschneiden
         */
        if (index > 0) filename = filename.substring(0, index);
        index = Math.max(filename.lastIndexOf("/"), filename.lastIndexOf("\\"));
        /*
         * danach den Filename filtern
         */
        filename = filename.substring(index + 1);
        return Encoding.htmlDecode(filename);
    }

    /**
     * Liefert den Anbieter zurück, für den dieses Plugin geschrieben wurde
     * 
     * @return Der unterstützte Anbieter
     */
    public String getHost() {
        return wrapper.getHost();
    }

    public Captcha getLastCaptcha() {
        return lastCaptcha;
    }

    /**
     * 
     * @return gibt den namen des Links an der gerade verarbeitet wird
     */
    public abstract String getLinkName();

    protected File getLocalCaptchaFile(Plugin plugin) {
        return Plugin.getLocalCaptchaFile(plugin, ".jpg");
    }

    /**
     * Liefert eine einmalige ID des Plugins zurück
     * 
     * @return Plugin ID
     */
    public String getPluginID() {
        return getHost() + "-" + getVersion();
    }

    /**
     * p gibt das interne properties objekt zurück indem die Plugineinstellungen
     * gespeichert werden
     * 
     * @return internes property objekt
     */
    public SubConfiguration getPluginConfig() {
        return JDUtilities.getSubConfig(wrapper.getConfigName());
    }

    /**
     * Gibt den Statustext des Plugins zurück. kann von der GUI aufgerufen
     * werden
     * 
     * @return Statustext
     */
    public String getStatusText() {
        if (statusText == null) {
            statusText = "";
        }
        return statusText;
    }

    /**
     * Ein regulärer Ausdruck, der anzeigt, welche Links von diesem Plugin
     * unterstützt werden
     * 
     * @return Ein regulärer Ausdruck
     * @see Pattern
     */
    public Pattern getSupportedLinks() {
        return this.wrapper.getPattern();
    }

    /**
     * Liefert die Versionsbezeichnung dieses Plugins zurück
     * 
     * @return Versionsbezeichnung
     */
    public abstract String getVersion();

    protected String getVersion(String revision) {
        return JDUtilities.getVersion(revision);
    }

    /**
     * Initialisiert das Plugin vor dem ersten Gebrauch
     */
    public void init() {

    }

    public void setCaptchaDetectID(int captchaJac) {
        captchaDetectionID = captchaJac;
    }

    public void setCaptchaDetectionID(int captchaDetectionID) {
        this.captchaDetectionID = captchaDetectionID;
    }

    public void setLastCaptcha(Captcha captcha) {
        lastCaptcha = captcha;
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
     * gibt zurück ob das plugin als supportedLinks nur gültige URIs
     * aktzeotiert. Soll ein PLugin auf andere strings reagieren, z.B. auf
     * Javascript strings, muss setAcceptOnlyURIs(false) gesetzt werden
     * 
     * @return
     */
    public boolean isAcceptOnlyURIs() {
        return acceptOnlyURIs;
    }

    public void setAcceptOnlyURIs(boolean acceptCompleteLinks) {
        this.acceptOnlyURIs = acceptCompleteLinks;
    }

}
