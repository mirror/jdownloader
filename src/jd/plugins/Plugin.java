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
import java.util.Calendar;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.utils.JDUtilities;

import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.translate._JDT;

/**
 * Diese abstrakte Klasse steuert den Zugriff auf weitere Plugins. Alle Plugins müssen von dieser Klasse abgeleitet werden.
 * 
 * Alle Plugins verfügen über einen Event Mechanismus
 */
public abstract class Plugin implements ActionListener {

    public static final String           HTTP_LINKS_HOST     = "http links";
    public static final String           DIRECT_HTTP_HOST    = "DirectHTTP";
    public static final String           FTP_HOST            = "ftp";

    /* to keep 0.95xx comp */
    /* switch this on every stable update */
    // protected static Logger logger = jd.controlling.JDLogger.getLogger();

    /* afer 0.95xx */
    protected Logger                     logger              = LogController.TRASH;

    protected CopyOnWriteArrayList<File> cleanUpCaptchaFiles = new CopyOnWriteArrayList<File>();

    public void setLogger(Logger logger) {
        if (logger == null) logger = LogController.TRASH;
        this.logger = logger;
    }

    public Logger getLogger() {
        return logger;
    }

    /**
     * Gibt nur den Dateinamen aus der URL extrahiert zurück. Um auf den dateinamen zuzugreifen sollte bis auf Ausnamen immer DownloadLink.getName() verwendet
     * werden
     * 
     * @return Datename des Downloads.
     */
    public static String extractFileNameFromURL(String filename) {
        if (StringUtils.isEmpty(filename)) return null;
        int index = filename.indexOf("?");
        /*
         * cut off get url parameters
         */
        if (index > 0) {
            filename = filename.substring(0, index);
        }
        index = filename.indexOf("#");
        /* cut off anker */
        if (index > 0) {
            filename = filename.substring(0, index);
        }
        index = Math.max(filename.lastIndexOf("/"), filename.lastIndexOf("\\"));
        /*
         * use filename
         */
        filename = filename.substring(index + 1);
        return Encoding.htmlDecode(filename);
    }

    public static String getFileNameFromDispositionHeader(String header) {
        return HTTPConnectionUtils.getFileNameFromDispositionHeader(header);
    }

    /**
     * Holt den Dateinamen aus einem Content-Disposition header. wird dieser nicht gefunden, wird der dateiname aus der url ermittelt
     * 
     * @param urlConnection
     * @return Filename aus dem header (content disposition) extrahiert
     */
    public static String getFileNameFromHeader(final URLConnectionAdapter urlConnection) {
        if (urlConnection.getHeaderField("Content-Disposition") == null || urlConnection.getHeaderField("Content-Disposition").indexOf("filename") < 0) {
            return Plugin.getFileNameFromURL(urlConnection.getURL());
        } else {
            return Plugin.getFileNameFromDispositionHeader(urlConnection.getHeaderField("Content-Disposition"));
        }
    }

    public static String getFileNameFromURL(final URL url) {
        return Plugin.extractFileNameFromURL(url.toExternalForm());
    }

    /**
     * 
     * @param message
     *            The message to be displayed or <code>null</code> to display a Password prompt
     * @param link
     *            the {@link CryptedLink}
     * @return the entered password
     * @throws DecrypterException
     *             if the user aborts the input
     */
    public static String getUserInput(final String message, final CryptedLink link) throws DecrypterException {
        final String password = PluginUtils.askPassword(message, link);
        if (password == null) { throw new DecrypterException(DecrypterException.PASSWORD); }
        return password;
    }

    /**
     * 
     * @param message
     *            The message to be displayed or <code>null</code> to display a Password prompt
     * @param link
     *            the {@link DownloadLink}
     * @return the entered password
     * @throws PluginException
     *             if the user aborts the input
     */
    public static String getUserInput(final String message, final DownloadLink link) throws PluginException {
        final String password = PluginUtils.askPassword(message, link);
        if (password == null) { throw new PluginException(LinkStatus.ERROR_FATAL, _JDT._.plugins_errors_wrongpassword()); }
        return password;
    }

    private volatile ConfigContainer config;

    protected Browser                br = null;

    public Plugin() {
    }

    @Deprecated
    public Plugin(final PluginWrapper wrapper) {
    }

    public void actionPerformed(final ActionEvent e) {
        return;
    }

    /**
     * Hier wird geprüft, ob das Plugin diesen Text oder einen Teil davon handhaben kann. Dazu wird einfach geprüft, ob ein Treffer des Patterns vorhanden ist.
     * 
     * @param data
     *            der zu prüfende Text
     * @return wahr, falls ein Treffer gefunden wurde.
     */
    public boolean canHandle(final String data) {
        if (data == null) { return false; }
        final Pattern pattern = this.getSupportedLinks();
        if (pattern != null) {
            final Matcher matcher = pattern.matcher(data);
            if (matcher.find()) { return true; }
        }
        return false;
    }

    public void clean() {
        br = null;
        for (File clean : cleanUpCaptchaFiles) {
            clean.delete();
        }
    }

    /**
     * Gibt das Konfigurationsobjekt der Instanz zurück. Die Gui kann daraus Dialogelement zaubern
     * 
     * @return gibt die aktuelle Configuration Instanz zurück
     */
    public ConfigContainer getConfig() {
        if (this.config != null) return config;
        synchronized (this) {
            if (this.config != null) return config;
            this.config = new ConfigContainer(null) {
                private static final long serialVersionUID = -30947319320765343L;

                /**
                 * we dont have to catch icon until it is really needed
                 */
                @Override
                public ImageIcon getIcon() {
                    return NewTheme.I().getIcon("warning", 16);
                }

                @Override
                public String getTitle() {
                    return getHost();
                }
            };
        }
        return config;
    }

    public boolean hasConfig() {
        if (config != null && config.getEntries() != null && config.getEntries().size() > 0) { return true; }
        return false;
    }

    /**
     * Liefert den Anbieter zurück, für den dieses Plugin geschrieben wurde
     * 
     * @return Der unterstützte Anbieter
     */
    public abstract String getHost();

    protected File getLocalCaptchaFile() {
        return this.getLocalCaptchaFile(".jpg");
    }

    /**
     * Returns the time in ms until a captcha request times out. this can be different for every plugin.
     * 
     * @PluginDevelopers: Please do not use the @Override Annotation when overriding this method. At least not until 2.0 stable release
     * @return
     */
    public int getCaptchaTimeout() {
        return CFG_CAPTCHA.CFG.getDefaultChallengeTimeout();
    }

    /**
     * Gibt die Datei zurück in die der aktuelle captcha geladen werden soll.
     * 
     * @param plugin
     * @return Gibt einen Pfad zurück der für die nächste Captchadatei reserviert ist
     */
    protected File getLocalCaptchaFile(String extension) {
        if (extension == null) {
            extension = ".jpg";
        }
        final Calendar calendar = Calendar.getInstance();
        final String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS.", calendar) + new Random().nextInt(999);
        final File dest = JDUtilities.getResourceFile("captchas/" + this.getHost() + "_" + date + extension, true);
        cleanUpCaptchaFiles.add(dest);
        return dest;
    }

    /**
     * p gibt das interne properties objekt zurück indem die Plugineinstellungen gespeichert werden
     * 
     * @return internes property objekt
     */
    public abstract SubConfiguration getPluginConfig();

    /**
     * Ein regulärer Ausdruck, der anzeigt, welche Links von diesem Plugin unterstützt werden
     * 
     * @return Ein regulärer Ausdruck
     * @see Pattern
     */
    public abstract Pattern getSupportedLinks();

    /**
     * Liefert die Versionsbezeichnung dieses Plugins zurück
     * 
     * @return Versionsbezeichnung
     */
    public abstract long getVersion();

    /**
     * Initialisiert das Plugin vor dem ersten Gebrauch
     */
    public void init() {
    }

    /**
     * Can be overridden, to return a descriptio of the hoster, or a short help forit's settings
     * 
     * @return
     */
    public String getDescription() {
        return null;
    }

    public boolean pluginAPI(String function, Object input, Object output) throws Exception {
        return false;
    }

    public PluginConfigPanelNG createConfigPanel() {
        return null;
    }

}