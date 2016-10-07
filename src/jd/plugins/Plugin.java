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
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Icon;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.SubConfiguration;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkchecker.LinkCheckerThread;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.controlling.reconnect.ipcheck.BalancedWebIPCheck;
import jd.controlling.reconnect.ipcheck.IPCheckException;
import jd.controlling.reconnect.ipcheck.OfflineException;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.BrowserSettingsThread;
import jd.http.ProxySelectorInterface;
import jd.http.StaticProxySelector;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.utils.JDUtilities;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Exceptions;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.auth.Login;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.dialog.AskCrawlerPasswordDialogInterface;
import org.jdownloader.gui.dialog.AskDownloadPasswordDialogInterface;
import org.jdownloader.gui.dialog.AskForCryptedLinkPasswordDialog;
import org.jdownloader.gui.dialog.AskForPasswordDialog;
import org.jdownloader.gui.dialog.AskForUserAndPasswordDialog;
import org.jdownloader.gui.dialog.AskUsernameAndPasswordDialogInterface;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.UserIOProgress;
import org.jdownloader.plugins.config.AccountConfigInterface;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;
import org.jdownloader.translate._JDT;

/**
 * Diese abstrakte Klasse steuert den Zugriff auf weitere Plugins. Alle Plugins müssen von dieser Klasse abgeleitet werden.
 *
 * Alle Plugins verfügen über einen Event Mechanismus
 */
public abstract class Plugin implements ActionListener {
    public static final String                                    HTTP_LINKS_HOST     = "http links";
    public static final String                                    DIRECT_HTTP_HOST    = "DirectHTTP";
    public static final String                                    FTP_HOST            = "ftp";
    /* to keep 0.95xx comp */
    /* switch this on every stable update */
    // protected static Logger logger = jd.controlling.JDLogger.getLogger();
    /* after 0.95xx */
    protected LogInterface                                        logger              = LogController.TRASH;
    protected final CopyOnWriteArrayList<File>                    cleanUpCaptchaFiles = new CopyOnWriteArrayList<File>();
    private static final HashMap<String, HashMap<String, Object>> CACHE               = new HashMap<String, HashMap<String, Object>>();
    private CrawledLink                                           currentLink         = null;

    public void setLogger(LogInterface logger) {
        if (logger == null) {
            logger = LogController.TRASH;
        }
        //
        this.logger = logger;
    }

    public abstract void runCaptchaDDosProtection(String id) throws InterruptedException;

    protected String getBrowserReferrer() {
        final LinkCrawler crawler = getCrawler();
        if (crawler != null) {
            return crawler.getReferrerUrl(getCurrentLink());
        }
        return null;
    }

    public CrawledLink getCurrentLink() {
        return currentLink;
    }

    public void setCurrentLink(CrawledLink currentLink) {
        this.currentLink = currentLink;
    }

    public LogInterface getLogger() {
        return logger;
    }

    protected LinkCrawler getCrawler() {
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            /* not sure why we have this here? */
            final LinkCrawler ret = ((LinkCrawlerThread) Thread.currentThread()).getCurrentLinkCrawler();
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public boolean isHandlingMultipleHosts() {
        if (this instanceof PluginForHost) {
            try {
                final Method method = this.getClass().getMethod("getHost", new Class[] { DownloadLink.class, Account.class });
                final boolean ret = method.getDeclaringClass() != PluginForHost.class;
                return ret;
            } catch (Throwable e) {
            }
        }
        return false;
    }

    public boolean isProxyRotationEnabled(boolean premiumDownload) {
        return !premiumDownload;
    }

    /**
     * Gibt nur den Dateinamen aus der URL extrahiert zurück. Um auf den dateinamen zuzugreifen sollte bis auf Ausnamen immer
     * DownloadLink.getName() verwendet werden
     *
     * @return Datename des Downloads.
     */
    public static String extractFileNameFromURL(final String iFilename) {
        String filename = iFilename;
        if (StringUtils.isEmpty(filename)) {
            return null;
        }
        int index = filename.indexOf("?");
        /*
         * cut off get url parameters
         */
        if (index > 0) {
            filename = filename.substring(0, index);
        }
        index = filename.indexOf("#");
        /* cut off anchor */
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

    public static String getFileNameFromDispositionHeader(final URLConnectionAdapter urlConnection) {
        final String contentDisposition = urlConnection.getHeaderField(HTTPConstants.HEADER_RESPONSE_CONTENT_DISPOSITION);
        return HTTPConnectionUtils.getFileNameFromDispositionHeader(contentDisposition);
    }

    /**
     * Determines file extension, from provided String. <br />
     * Must be a valid URL otherwise failover will be returned <br />
     * This should be more fail proof than getFileNameExtensionFromString
     *
     * @since JD2
     * @author raztoki
     * @throws MalformedURLException
     */
    public static String getFileNameExtensionFromURL(final String url, final String failover) {
        if (url == null) {
            return null;
        }
        try {
            final String output = Plugin.getFileNameFromURL(new URL(url));
            if (output != null && output.contains(".")) {
                return output.substring(output.lastIndexOf("."));
            }
        } catch (final MalformedURLException e) {
        }
        return failover;
    }

    /**
     * Wrapper
     *
     * @since JD2
     * @author raztoki
     * @param url
     * @return
     * @throws MalformedURLException
     */
    public static String getFileNameExtensionFromURL(final String url) {
        return getFileNameExtensionFromURL(url, null);
    }

    /**
     * Determines file extension, from a provided String. <br />
     * Can be used without a VALID URL, or part url or just filename. <br />
     * Will fix file.exe+junk etc.
     *
     * @since JD2
     * @author raztoki
     */
    public static String getFileNameExtensionFromString(final String filename, final String failover) {
        if (filename == null) {
            return null;
        }
        final String output = extractFileNameFromURL(filename);
        if (output != null && output.contains(".")) {
            return output.substring(output.lastIndexOf("."));
        }
        return failover;
    }

    /**
     * Wrapper
     *
     * @since JD2
     * @author raztoki
     * @param filename
     * @return
     */
    public static String getFileNameExtensionFromString(final String filename) {
        return getFileNameExtensionFromString(filename, null);
    }

    /**
     * Holt den Dateinamen aus einem Content-Disposition header. wird dieser nicht gefunden, wird der dateiname aus der url ermittelt
     *
     * @param urlConnection
     * @return Filename aus dem header (content disposition) extrahiert
     */
    public static String getFileNameFromHeader(final URLConnectionAdapter urlConnection) {
        final String fileName = getFileNameFromDispositionHeader(urlConnection);
        if (StringUtils.isEmpty(fileName)) {
            return Plugin.getFileNameFromURL(urlConnection.getURL());
        } else {
            return fileName;
        }
    }

    public static String getFileNameFromURL(final URL url) {
        return Plugin.extractFileNameFromURL(url.getPath());
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
        // final String password = PluginUtils.askPassword(message, link);
        // if (password == null) { throw new DecrypterException(DecrypterException.PASSWORD); }
        // return password;
        // UserIOProgress prg = new UserIOProgress(message);
        // PluginProgress old = null;
        try {
            // old = link.setPluginProgress(prg);
            AskCrawlerPasswordDialogInterface handle = UIOManager.I().show(AskCrawlerPasswordDialogInterface.class, new AskForCryptedLinkPasswordDialog(message, link, getCurrentActivePlugin()));
            if (handle.getCloseReason() == CloseReason.OK) {
                String password = handle.getText();
                if (StringUtils.isEmpty(password)) {
                    throw new DecrypterException(DecrypterException.PASSWORD);
                }
                return password;
            } else {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        } finally {
            // link.compareAndSetPluginProgress(prg, old);
        }
    }

    public static Plugin getCurrentActivePlugin() {
        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof LinkCrawlerThread) {
            //
            return (PluginForDecrypt) ((LinkCrawlerThread) currentThread).getCurrentOwner();
        } else if (currentThread instanceof SingleDownloadController) {
            //
            return ((SingleDownloadController) currentThread).getProcessingPlugin();
        } else if (currentThread instanceof LinkCheckerThread) {
            return ((LinkCheckerThread) currentThread).getPlugin();
        }
        return null;
    }

    protected boolean isConnectionOffline(Throwable e) {
        HTTPProxy proxy = null;
        final BrowserException browserException = Exceptions.getInstanceof(e, BrowserException.class);
        if (browserException != null && browserException.getRequest() != null) {
            proxy = browserException.getRequest().getProxy();
        }
        if (proxy == null) {
            final Plugin plugin = getCurrentActivePlugin();
            if (plugin != null) {
                final Browser br;
                if (plugin instanceof PluginForHost) {
                    br = ((PluginForHost) plugin).getBrowser();
                } else if (plugin instanceof PluginForDecrypt) {
                    br = ((PluginForDecrypt) plugin).getBrowser();
                } else {
                    br = null;
                }
                if (br != null && br.getRequest() != null) {
                    proxy = br.getRequest().getProxy();
                }
            }
        }
        final ProxySelectorInterface proxySelector;
        if (proxy != null) {
            proxySelector = new StaticProxySelector(proxy);
        } else {
            proxySelector = BrowserSettingsThread.getThreadProxySelector();
        }
        final BalancedWebIPCheck onlineCheck = new BalancedWebIPCheck(proxySelector);
        try {
            onlineCheck.getExternalIP();
        } catch (final OfflineException e2) {
            return true;
        } catch (final IPCheckException e2) {
        }
        return false;
    }

    protected boolean isAbort() {
        final Thread currentThread = Thread.currentThread();
        if (currentThread instanceof SingleDownloadController) {
            final SingleDownloadController sdc = (SingleDownloadController) currentThread;
            return sdc.isAborting() || currentThread.isInterrupted();
        }
        return currentThread.isInterrupted();
    }

    /**
     * Show a USername + password dialog
     *
     * @param link
     * @return
     * @throws PluginException
     */
    protected Login requestLogins(String message, DownloadLink link) throws PluginException {
        if (message == null) {
            message = _JDT.T.Plugin_requestLogins_message();
        }
        final UserIOProgress prg = new UserIOProgress(message);
        prg.setProgressSource(this);
        prg.setDisplayInProgressColumnEnabled(false);
        try {
            link.addPluginProgress(prg);
            final AskUsernameAndPasswordDialogInterface handle = UIOManager.I().show(AskUsernameAndPasswordDialogInterface.class, new AskForUserAndPasswordDialog(message, link));
            if (handle.getCloseReason() == CloseReason.OK) {
                String password = handle.getPassword();
                if (StringUtils.isEmpty(password)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, _JDT.T.plugins_errors_wrongpassword());
                }
                String username = handle.getUsername();
                if (StringUtils.isEmpty(username)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, _JDT.T.plugins_errors_wrongusername());
                }
                return new Login(username, password);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, _JDT.T.plugins_errors_wrongpassword());
            }
        } finally {
            link.removePluginProgress(prg);
        }
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
    public static String getUserInput(String message, final DownloadLink link) throws PluginException {
        if (message == null) {
            message = "Please enter the password to continue...";
        }
        final UserIOProgress prg = new UserIOProgress(message);
        prg.setProgressSource(getCurrentActivePlugin());
        prg.setDisplayInProgressColumnEnabled(false);
        try {
            link.addPluginProgress(prg);
            AskDownloadPasswordDialogInterface handle = UIOManager.I().show(AskDownloadPasswordDialogInterface.class, new AskForPasswordDialog(message, link));
            if (handle.getCloseReason() == CloseReason.OK) {
                String password = handle.getText();
                if (StringUtils.isEmpty(password)) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, _JDT.T.plugins_errors_wrongpassword());
                }
                return password;
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, _JDT.T.plugins_errors_wrongpassword());
            }
        } finally {
            link.removePluginProgress(prg);
        }
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

    public static boolean implementsCanHandleString(Plugin plugin) {
        try {
            if (plugin != null) {
                final Method method = plugin.getClass().getMethod("canHandle", String.class);
                final boolean impl = method.getDeclaringClass() != Plugin.class;
                return impl;
            }
        } catch (NoSuchMethodException e) {
        } catch (Throwable e) {
            LogController.CL().log(e);
        }
        return false;
    }

    /**
     * Hier wird geprüft, ob das Plugin diesen Text oder einen Teil davon handhaben kann. Dazu wird einfach geprüft, ob ein Treffer des
     * Patterns vorhanden ist.
     *
     * @param data
     *            der zu prüfende Text
     * @return wahr, falls ein Treffer gefunden wurde.
     */
    public boolean canHandle(final String data) {
        if (data != null) {
            final Matcher matcher = getMatcher();
            synchronized (matcher) {
                try {
                    return matcher.reset(data).find();
                } finally {
                    matcher.reset("");
                }
            }
        }
        return false;
    }

    public abstract Matcher getMatcher();

    public void clean() {
        cleanupLastChallengeResponse();
        br = null;
        for (final File clean : cleanUpCaptchaFiles) {
            if (!clean.delete()) {
                clean.deleteOnExit();
            }
        }
    }

    public CrawledLink convert(final DownloadLink link) {
        return new CrawledLink(link);
    }

    /**
     * Gibt das Konfigurationsobjekt der Instanz zurück. Die Gui kann daraus Dialogelement zaubern
     *
     * @return gibt die aktuelle Configuration Instanz zurück
     */
    public ConfigContainer getConfig() {
        if (this.config != null) {
            return config;
        }
        synchronized (this) {
            if (this.config != null) {
                return config;
            }
            this.config = new ConfigContainer(null) {
                private static final long serialVersionUID = -30947319320765343L;

                /**
                 * we dont have to catch icon until it is really needed
                 */
                @Override
                public Icon getIcon() {
                    return new AbstractIcon(IconKey.ICON_WARNING, 16);
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
        final ConfigContainer lconfig = config;
        if (lconfig != null && lconfig.getEntries() != null && lconfig.getEntries().size() > 0) {
            return true;
        }
        return getConfigInterface() != null;
    }

    @Deprecated
    protected boolean hasOldConfigContainer() {
        final ConfigContainer lconfig = config;
        return lconfig != null && lconfig.getEntries() != null && lconfig.getEntries().size() > 0;
    }

    /**
     * Liefert den Anbieter zurück, für den dieses Plugin geschrieben wurde
     *
     * @return Der unterstützte Anbieter
     */
    public abstract String getHost();

    public File getLocalCaptchaFile() {
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
    public File getLocalCaptchaFile(String extension) {
        if (extension == null) {
            extension = ".jpg";
        }
        final Calendar calendar = Calendar.getInstance();
        final String date = String.format("%1$td.%1$tm.%1$tY_%1$tH.%1$tM.%1$tS.", calendar) + new Random().nextInt(999);
        final File dest = JDUtilities.getResourceFile("captchas/" + this.getHost() + "_" + date + extension, true);
        cleanUpCaptchaFiles.add(dest);
        return dest;
    }

    /*
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
     * Probleme mit ungültigen Zeichen unter Windows und anderen Betriebssystemen * in NTFS und verschiedenen Dateisystemen * * * * * * * *
     * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * Eine ausführliche Ausarbeitung für eine optimale Lösung
     * 
     * @author: niemann.berthold@gmail.com
     * 
     * @date: 13.12.2015
     * 
     * @language: German
     * 
     * @text-edit: 24.08.2016
     * 
     * Allgemeine Notationen ============================ Bei der Benennung von Ordnern und Dateien wird meist der Zeichenvorrat von dem
     * Dateisystem eingeschränkt, da einige Zeichen für bestimmte Funktionen reserviert sind. Es handelt sich hierbei meist um gebräuchliche
     * Zeichen, die auch für aussagekräftige Dateinamen benutzt werden. Es gibt prinzipiell zwei Methoden, um Wörter (Zeichenketten mit
     * gewisser Bedeutung) voneinander abzutrennen, sodass kein Schlangenname entsteht, bei dem man zunächst die Wörter voneinander trennen
     * muss.
     * 
     * Delimiter-separated --------------------------- - Im gewöhnlichen Schriftgebrauch wurde seit dem 7. Jahrhundert die Wortteilung
     * eingeführt. Beim Schreiben wird hierzu der Stift abgehoben und ein gewisser Wortabstand vor dem Wieder-Aufsetzen eingehalten. Auch
     * bei Schreibmaschinen als Leerschlag bezeichnet, heißt es heutzutage Leerzeichen und ist das heute womöglich am häufigsten genutzte
     * Zeichen. Zugleich ist dieses allerdings nicht als Buchstabe anerkannt, da es sich aus dem Auslassen von Buchstaben definiert, was
     * gewissermaßen eine Diskriminierung darstellt. Die Diskriminierung von Frauen könnte man aber auch dadurch legitimieren, dass diese
     * lediglich nicht-Männer wären. Folglich wird das Leerzeichen oft in Kryptografie-Tutorien übergangen und der Buchstabe ‚e‘ wird
     * fälschlicherweise als häufigstes Zeichen in der deutschen Sprache propagiert.
     * 
     * - Im Allgemeinen kennt man von diversen Seiten die Punkt-Notation, die sowohl bei OCHs, als auch bei Torrents und im Usenet
     * gebräuchlich ist. Ein Beispiel ist: >>Selfless.Der.Fremde.in.mir.2015.BDRip.German.AC3D.XViD.PROPER-PS<< Innerhalb der
     * Diskussionsforen und einiger Webseiten gibt es einige Aufschriebe zur Verwendung der Notation und der Begrifflichkeiten, jedoch keine
     * einheitliche Norm. Es ist mehr ein Kollektiv-Gedankengut.
     * 
     * - Snake-Case (Informatik): Insbesondere aus der Linux-Welt, in der noch verstärkt im Terminal gearbeitet wird, ist das Verarbeiten
     * von Dateien mit Leerzeichen etwas umständlich. Aus diesem Grund gibt es hier meist die „Underscore“-Notation. >>Remote_Login_Access<<
     * ________________________________________________ | Einige Anwendungen ersetzen Unterstriche | | beim Anzeigen automatisch durch
     * Leerzeichen! | ‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾‾
     * 
     * - Train-Case (Informatik): Es gibt auch den Viertelgeviertstrich (zu englisch hyphen, ein schlichter Bindestrich) als Separator. Dies
     * ist vor allem von URLs bekannt: >>http://www.spiegel.de/sport/fussball/gladbach-besiegt-bayern-muenchen
     * -suesse-momente-der-macht-a-1066330.html<<
     * 
     * - Es gibt auch freie Trennzeichen-Notationen. So wird für Tabellen oft ein senkrechter Strich im Terminal (Linux) eingesetzt. Dies
     * nutzt der Kommandozeilen-Client von MySQL. + ------------- | ------------- + | First Header | Second Header | | ------------- |
     * ------------- | | Content Cell | Content Cell | | Content Cell | Content Cell |
     * 
     * Ich persönlich setze gerne einen unterbrochenen senkrecht-Strich („¦“) bei Dateinamen ein, um beim Anzeigen eine Spaltenform zu
     * erzeugen.
     * 
     * Letter-case separated --------------------------- - Aus der Informatik ist der ‚CamelCase‘ bekannt. Dabei werden sämtliche
     * Trennzeichen gelöscht und über die Groß- und Kleinschreibung eine Trennung erzeugt. Beim lower camelCase wird mit einem
     * Kleinbuchstaben angefangen: >>diesIstEineVariable<< Beim Upper CamelCase beginnt man mit einem Großbuchstaben:
     * >>DiesIstEineVariable<< Im Allgemeinen gilt festzuhalten, dass meist keine Satzzeichen in dieser Schreibweise eingesetzt werden und
     * die grammatikalische Groß- und Kleinschreibung beim Anwenden verloren geht.
     * 
     * Escapen --------------------------- An vielen Stellen werden ungültige Zeichen meist durch einen Underscore ersetzt. Ungültige
     * Zeichen sind unter Windows : >> < > ? " : | \ / * ␀ << Unter NTFS, ext3 , ext4 und xfs (Linux): >> / ␀ << HFS+ (Mac) erlaubt alle
     * Zeichen. Für URLs sind sämtliche Latin-1-Zeichen außer den folgenden erlaubt: >> ! # $ % & ' ( ) * + , / : ; = ? @ [ ] <<
     * 
     * Einige Dienste ersetzen ungültige Zeichen durch Unterstriche. Dabei gehen jedoch immer Informationen verloren, da unklar ist, welches
     * Zeichen zuvor dort stand. Zugleich wird in der Linux-Welt ein Underscore bereits als reguläres Trennzeichen bzw. als
     * Leerzeichenersatz verwendet. Einige Anwendungen parsen die Dateinamen und wenden automatisch bestimmte Operationen auf diese
     * Unterstriche an (siehe Snake-Case (Informatik))
     * 
     * Das ist vergleichsweise genauso wie bei einer verlustbehafteten mp4-konvertierung. Was einmal weggeworfen wurde, kann nicht
     * wiederhergestellt werden. Sicherlich kann man durch Grammatikregeln und Wörterbücher die Wiederherstellung verbessern, sie wird aber
     * niemals genau das Original wiedergeben können. 
     * 
     * Hierzu ein Beispiel, aus der 7-Bit ASCII-Kodierung, welche nur einen spärlichen Zeichenvorrat hat.
     * 
     * ä => ae ö => oe ü => ue
     * 
     * Würde man dies wieder „dekodieren“ wollen, hieße es umgekehrt
     * 
     * ae => ä oe => ö ue => ü
     * 
     * Das Problem daran ist, dass diese Buchstabenkombinationen nicht frei sind, sondern in der regulären Sprache bereits vergeben sind.
     * 
     * Poesie => Pösie Bruttoertragswert => Bruttörtragswert Zuerst => zürst Soeben => söben Morgenbläue => Morgenbläü Poet => Pöt Quelle =>
     * Qülle
     * 
     * Dies lässt sich mit Fachsprache auf die Spitze treiben und kann schon unter Steganographie fallen,
     * 
     * Anaerob => Anärob OEM => ÖM
     * 
     * kann aber auch einer Sprache ohne Umlaute neue Zeichen geben:
     * 
     * Clue => Clü Fuelwood => Fülwood Aero => Äro bueno => büno
     * 
     * Markierungszeichen --------------------------- Zum Beispiel bei Regex-Abfragen gibt es Schlüsselzeichen, um bestimmte Funktionen zu
     * verwenden: >> /^[a-z0-9A-Z]{6,18}$/ << Um die Zeichen mit Funktionen dennoch regulär verwenden zu können, kann man diese mit einem
     * Backslash „\“ maskieren, sodass deren Funktionalität wegfällt. Aus dem Internet ist für URLs die Prozentdarstellung bekannt, welche
     * auch eine Maskierung darstellt11. Allerdings ist eine Maskierung bei Dateisystemen nicht vorgesehen, wodurch sich Probleme ergeben.
     * 
     * Unicode --------------------------- Eine mögliche Lösung zur Erhaltung der ungültigen Zeichen in Windows-Dateinamen ist, sich den
     * Unicode-Zeichenvorrat zunutze zu machen und unvoreingenommene Unicode-Zeichen einzusetzen. Diese Zeichen sollten selten vorkommen und
     * wie bei l33t (Leetspeak) stellvertretend eingesetzt werden.
     * 
     * -------------------------------------------------------------------------- * Kodierungstabelle V. 3 (13. Dezember 2015) *
     * -------------------------------------------------------------------------- * Eine mögliche Tabelle könnte wie folgt aussehen: * * :
     * (U+003A) => ː (U+02D0) IPA-Längenzeichen * | (U+007C) => ¦ (U+00A6) * < (U+003C) => ≺ (U+227A) * > (U+003E) => ≻ (U+227B) * /
     * (U+002F) => ／ (U+FF0F) * \ (U+005C) => ＼ (U+FF3C) * * (U+002A) => ∗ (U+2217) * ? (U+003F) => ？ (U+FF1F) * ! (U+0021) => ！ (U+FF01) *
     * " (U+0022) => ” (U+201D) Finnisch-Anführungszeichen * * Änderungsbegründung zur vorherigen Version (V. 2): * - Umgedrehte
     * Ausrufezeichen & Fragezeichen ergeben Probleme in Sprachen * wie Spanisch. * * --------------------------- Kodierungstabelle V. 2 (6.
     * Dezember 2015) ---------------------------
     * 
     * : (U+003A) => ː (U+02D0) IPA-Längenzeichen | (U+007C) => ¦ (U+00A6) < (U+003C) => ≺ (U+227A) > (U+003E) => ≻ (U+227B) / (U+002F) => ／
     * (U+FF0F) \ (U+005C) => ＼ (U+FF3C) * (U+002A) => ∗ (U+2217) ? (U+003F) => ¿ (U+00BF) ! (U+0021) => ¡ (U+00A1) " (U+0022) => ” (U+201D)
     * Finnisch-Anführungszeichen
     * 
     * Änderungsbegründung zur vorherigen Version (V. 1): - Das Semikolon findet durchaus Anwendung im üblichen Gebrauch und sollte deswegen
     * nicht den Doppeltpunkt ersetzen. - Für Sprachangaben wie [GER] oder Keywords/Tags werden eckigen Klammern verwendet, weswegen eckige
     * Klammern nicht als Ersatz für die Vergleichszeichen genutzt werden sollten. - Die Schrägstriche wurden angepasst, zuvor waren es
     * unterschiedliche Typen. - Das Sternchen wird nicht weiter durch ein Doppelkreuz (Hashmark) ersetzt, welches insbesondere für Anker
     * auf Webseiten verwendet wird. Stattdessen wird ein, je nach Font, fast identisch aussehendes Sternchen verwendet. - Statt die
     * doppelten Anführungszeichen durch einfache zu ersetzen, welche insbesondere als Apostroph häufig Anwendung findet, werden gewinkelte
     * Anführungszeichen eingesetzt.
     * 
     * --------------------------- Kodierungstabelle V. 1 (2. November 2013) ---------------------------
     * 
     * : (U+003A) => ; (U+003B) Semikolon | (U+007C) => ¦ (U+00A6) < (U+003C) => [ (U+005B) Eckige Klammer auf > (U+003E) => ] (U+005D)
     * Eckige Klammer zu / (U+002F) => ⁄ (U+2044) \ (U+005C) => ∖ (U+2216) * (U+002A) => # (U+0023) Doppelkreuz ? (U+003F) => ¿ (U+00BF) !
     * (U+0021) => ¡ (U+00A1) " (U+0022) => ' (U+0027) Apostroph
     */
    public String encodeUnicode(final String input) {
        if (input != null) {
            String output = input;
            output = output.replace(":", ";");
            output = output.replace("|", "¦");
            output = output.replace("<", "[");
            output = output.replace(">", "]");
            output = output.replace("/", "⁄");
            output = output.replace("\\", "∖");
            output = output.replace("*", "#");
            output = output.replace("?", "¿");
            output = output.replace("!", "¡");
            output = output.replace("\"", "'");
            return output;
        }
        return null;
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

    private WeakReference<PluginConfigPanelNG> configPanel = null;

    public PluginConfigPanelNG getConfigPanel() {
        PluginConfigPanelNG panel = configPanel == null ? null : configPanel.get();
        if (panel == null) {
            panel = createConfigPanel();
            configPanel = new WeakReference<PluginConfigPanelNG>(panel);
        }
        return panel;
    }

    protected PluginConfigPanelNG createConfigPanel() {
        if (getConfigInterface() != null) {
            PluginConfigPanelNG ret = new PluginConfigPanelNG() {
                @Override
                public void updateContents() {
                }

                @Override
                public void save() {
                }
            };
            return ret;
        }
        return null;
    }

    public Class<? extends PluginConfigInterface> getConfigInterface() {
        for (Class<?> cls : getClass().getClasses()) {
            if (PluginConfigInterface.class.isAssignableFrom(cls) && !AccountConfigInterface.class.isAssignableFrom(cls)) {
                PluginHost anno = cls.getAnnotation(PluginHost.class);
                if (anno != null) {
                    if (StringUtils.equals(anno.host(), getHost())) {
                        return (Class<? extends PluginConfigInterface>) cls;
                    }
                } else {
                    return (Class<? extends PluginConfigInterface>) cls;
                }
            }
        }
        return null;
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        // if (AccountController.getInstance().hasAccounts(plg.getHost())) {
        // rly? are there many crawler that require an account?
        return true;
    }

    /**
     * used to disable site testing, for instance names reference is invalid domain (just a place holder). dummyplugins etc.
     *
     * @since JD2
     * @author raztoki
     * @return
     */
    public Boolean siteTesterDisabled() {
        return null;
    }

    /**
     * So plugins can override when one partular site changes away from template defaults. one needs to override.
     *
     * @since JD2
     * @author raztoki
     * @param siteTemplate
     * @param plugin
     * @param br
     * @return
     */
    public Boolean siteTester(final SiteTemplate siteTemplate, final Plugin plugin, final Browser br) {
        return SiteTester.siteTester(siteTemplate, plugin, br);
    }

    /**
     * sets the SiteTemplate defination.
     *
     * @since JD2
     * @author raztoki
     * @return
     */
    public SiteTemplate siteTemplateType() {
        return null;
    }

    /**
     * Used when names url entry contains multiple (sub\.)?domains within URLs. This allows us to return all supported domains.
     *
     * @since JD2
     * @author raztoki
     * @return
     */
    public String[] siteSupportedNames() {
        return null;
    }

    /**
     * extends siteSuportedNames, but allows you to set path, so that siteTemplateConfirmation test is done on the correct URL. Useful when
     * site runs many services on a single domain, but have different base paths for each.
     *
     * @since JD2
     * @author raztoki
     * @return
     */
    public String siteSupportedPath() {
        return null;
    }

    public int getChallengeTimeout(Challenge<?> captchaChallenge) {
        return -1;
    }

    protected List<Challenge<?>> challenges = null;

    /**
     * returns a unmodifiiable List of all challenges done so far in this plugin
     *
     * @return
     */
    public List<Challenge<?>> getChallenges() {
        final List<Challenge<?>> challenges = this.challenges;
        if (challenges == null) {
            return Collections.unmodifiableList(new ArrayList<Challenge<?>>());
        } else {
            return Collections.unmodifiableList(challenges);
        }
    }

    /**
     * returns the current challenge round. if there has been 1 captcha so far, this will return 1
     */
    public int getChallengeRound() {
        final List<Challenge<?>> challenges = this.challenges;
        return challenges == null ? 0 : (challenges.size() - 1);
    }

    /**
     * adds a challenge that has been used in this plugin
     *
     * @param challenge
     */
    public synchronized int addChallenge(Challenge<?> challenge) {
        final List<Challenge<?>> nchallenges = new ArrayList<Challenge<?>>();
        final List<Challenge<?>> old = this.challenges;
        if (old != null) {
            nchallenges.addAll(old);
        }
        nchallenges.add(challenge);
        this.challenges = nchallenges;
        return nchallenges.size() - 1;
    }

    public void invalidateLastChallengeResponse() {
        final List<Challenge<?>> ch = challenges;
        if (ch != null) {
            for (final Challenge<?> c : ch) {
                final SolverJob<?> job = c.getJob();
                if (job != null) {
                    job.invalidate();
                }
            }
        }
    }

    public synchronized void cleanupLastChallengeResponse() {
        final List<Challenge<?>> ch = challenges;
        challenges = null;
        if (ch != null) {
            for (final Challenge<?> c : ch) {
                if (c != null) {
                    try {
                        c.cleanup();
                    } catch (Throwable e) {
                        final LogInterface logger = getLogger();
                        if (logger != null) {
                            logger.log(e);
                        } else {
                            LoggerFactory.getDefaultLogger().log(e);
                        }
                    }
                }
            }
        }
    }

    public void validateLastChallengeResponse() {
        final List<Challenge<?>> ch = challenges;
        if (ch != null && ch.size() > 0) {
            final Challenge<?> latest = ch.get(ch.size() - 1);
            final SolverJob<?> job = latest.getJob();
            if (job != null) {
                job.validate();
            }
        }
    }

    public boolean hasChallengeResponse() {
        final List<Challenge<?>> challenges = this.challenges;
        return challenges != null && challenges.size() > 0;
    }
}