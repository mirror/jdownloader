//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import jd.HostPluginWrapper;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.event.ControlEvent;
import jd.gui.swing.components.ConvertDialog;
import jd.gui.swing.components.ConvertDialog.ConversionMode;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.http.JDProxy;
import jd.http.URLConnectionAdapter;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

/**
 * Diese abstrakte Klasse steuert den Zugriff auf weitere Plugins. Alle Plugins
 * müssen von dieser Klasse abgeleitet werden.
 * 
 * Alle Plugins verfügen über einen Event Mechanismus
 */
public abstract class Plugin implements ActionListener {

    public static final String ACCEPT_LANGUAGE = "de, en-gb;q=0.9, en;q=0.8";

    protected static Logger logger = JDLogger.getLogger();

    public static ConversionMode showDisplayDialog(ArrayList<ConversionMode> displaymodes, String name, CryptedLink link) throws DecrypterException {
        link.getProgressController().setStatusText(JDL.L("gui.linkgrabber.waitinguserio", "Waiting for user input"));
        synchronized (JDUtilities.USERIO_LOCK) {
            ConversionMode temp = ConvertDialog.displayDialog(displaymodes, name);
            link.getProgressController().setStatusText(null);
            if (temp == null) throw new DecrypterException(JDL.L("jd.plugins.Plugin.aborted", "Decryption aborted!"));
            return temp;
        }
    }

    public static String getUserInput(String message, CryptedLink link) throws DecrypterException {
        String password = JDUtilities.getUserInput(message, link);
        if (password == null) throw new DecrypterException(DecrypterException.PASSWORD);
        return password;
    }

    public static String getUserInput(String message, DownloadLink link) throws PluginException {
        String password = JDUtilities.getUserInput(message, link);
        if (password == null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.errors.wrongpassword", "Password wrong"));
        return password;
    }

    public static String getUserInput(String message, String defaultmessage, CryptedLink link) throws DecrypterException {
        String password = JDUtilities.getUserInput(message, defaultmessage, link);
        if (password == null) throw new DecrypterException(DecrypterException.PASSWORD);
        return password;
    }

    protected File getLocalCaptchaFile() {
        return getLocalCaptchaFile(".jpg");
    }

    /**
     * Gibt die Datei zurück in die der aktuelle captcha geladen werden soll.
     * 
     * @param plugin
     * @return Gibt einen Pfad zurück der für die nächste Captchadatei
     *         reserviert ist
     */
    protected File getLocalCaptchaFile(String extension) {
        if (extension == null) {
            extension = ".jpg";
        }
        Calendar calendar = Calendar.getInstance();
        String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS.", calendar) + new Random().nextInt(999);

        File dest = JDUtilities.getResourceFile("captchas/" + this.getHost() + "_" + date + extension, true);
        dest.deleteOnExit();
        return dest;
    }

    protected final ConfigContainer config;

    protected final PluginWrapper wrapper;

    protected Browser br = null;

    public Plugin(final PluginWrapper wrapper) {
        this.wrapper = wrapper;

        if (wrapper instanceof HostPluginWrapper) {
            config = new ConfigContainer(getHost()) {
                private static final long serialVersionUID = -30947319320765343L;

                /**
                 * we dont have to catch icon until it is really needed
                 */
                @Override
                public ImageIcon getIcon() {
                    return ((HostPluginWrapper) wrapper).getIcon();
                }
            };
        } else {
            config = new ConfigContainer(getHost());
        }
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
        if (data == null) return false;
        Pattern pattern = getSupportedLinks();
        if (pattern != null) {
            Matcher matcher = pattern.matcher(data);
            if (matcher.find()) { return true; }
        }
        return false;
    }

    public void clean() {
        JDProxy pr = null;
        if (br != null) {
            pr = br.getProxy();
            br = new Browser();
            if (pr != null) br.setProxy(pr);
        }
    }

    /**
     * Create MenuItems erlaubt es den Plugins eine MenuItemliste zurückzugeben.
     * Die Gui kann diese Menüpunkte dann darstellen. Die Gui muss das Menu bei
     * jedem zugriff neu aufbauen, weil sich das ToolBarAction Array geändert
     * haben kann. MenuItems sind Datenmodelle für ein TreeMenü.
     */
    public abstract ArrayList<MenuAction> createMenuitems();

    /**
     * Diese Funktion schneidet alle Vorkommnisse des vom Plugin unterstützten
     * Pattern aus
     * 
     * @param data
     *            Text, aus dem das Pattern ausgeschnitter werden soll
     * @return Der resultierende String
     */
    public String cutMatches(String data) {
        /* case insensitive pattern */
        return data.replaceAll("(?i)" + getSupportedLinks().pattern(), "--CUT--");
    }

    /**
     * Verwendet den JDController um ein ControlEvent zu broadcasten
     * 
     * @param controlID
     * @param param
     */
    public void fireControlEvent(int controlID, Object param) {
        JDUtilities.getController().fireControlEvent(new ControlEvent(this, controlID, param));
    }

    /**
     * Gibt das Konfigurationsobjekt der Instanz zurück. Die Gui kann daraus
     * Dialogelement zaubern
     * 
     * @return gibt die aktuelle Configuration Instanz zurück
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
     */
    public static String getFileNameFromHeader(URLConnectionAdapter urlConnection) {
        if (urlConnection.getHeaderField("content-disposition") == null || urlConnection.getHeaderField("content-disposition").indexOf("filename") < 0) { return Plugin.getFileNameFromURL(urlConnection.getURL()); }
        return getFileNameFromDispositionHeader(urlConnection.getHeaderField("content-disposition"));
    }

    public static String getFileNameFromDispositionHeader(String header) {
        // http://greenbytes.de/tech/tc2231/
        if (header == null) return null;
        String orgheader = header;
        String contentdisposition = header;
        String filename = null;
        for (int i = 0; i < 2; i++) {
            if (contentdisposition.contains("filename*")) {
                /* Codierung default */
                /*
                 * Content-Disposition: attachment;filename==?UTF-8?B?
                 * RGF2aWQgR3VldHRhIC0gSnVzdCBBIExpdHRsZSBNb3JlIExvdmUgW2FMYnlsb3ZlciBYLUNsdXNpdiBSZW1peF0uTVAz
                 * ?=
                 */
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
            } else if (new Regex(contentdisposition, "=\\?.*?\\?.*?\\?=").matches()) {
                /* Unicode Format wie es 4Shared nutzt */
                String tokens[][] = new Regex(contentdisposition, "=\\?(.*?)\\?(.*?)\\?=").getMatches();
                if (tokens.length == 1 && tokens[0].length == 2) {
                    try {
                        contentdisposition = new String(tokens[0][1].trim().getBytes("ISO-8859-1"), tokens[0][0].trim());
                        continue;
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
        if (filename != null) {
            filename = filename.trim();
            if (filename.startsWith("\"")) {
                logger.info("Using Workaround for broken filename header!");
                filename = filename.substring(1);
            }
        }
        if (filename == null) logger.severe("Content-Disposition: could not parse header: " + orgheader);
        return filename;
    }

    public static String getFileNameFromURL(URL url) {
        return extractFileNameFromURL(url.toExternalForm());
    }

    /**
     * Gibt nur den Dateinamen aus der URL extrahiert zurück. Um auf den
     * dateinamen zuzugreifen sollte bis auf Ausnamen immer
     * DownloadLink.getName() verwendet werden
     * 
     * @return Datename des Downloads.
     */
    public static String extractFileNameFromURL(String filename) {
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
        return SubConfiguration.getConfig(wrapper.getConfigName());
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
        return Formatter.getRevision(revision);
    }

    /**
     * Initialisiert das Plugin vor dem ersten Gebrauch
     */
    public void init() {

    }

    @Deprecated
    public void setAcceptOnlyURIs(boolean acceptCompleteLinks) {
    }

}
