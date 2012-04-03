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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JMenu;

import jd.PluginWrapper;
import jd.captcha.JACMethod;
import jd.config.SubConfiguration;
import jd.controlling.IOPermission;
import jd.controlling.JDLogger;
import jd.controlling.JDPluginLogger;
import jd.controlling.captcha.CaptchaController;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Regex;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging.Log;
import org.jdownloader.DomainInfo;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

/**
 * Dies ist die Oberklasse fuer alle Plugins, die von einem Anbieter Dateien
 * herunterladen koennen
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {
    private static Pattern[] PATTERNS     = new Pattern[] {
                                          // multipart rar archives
            Pattern.compile("(.*)(\\.pa?r?t?\\.?[0-9]+.*?\\.rar$)", Pattern.CASE_INSENSITIVE),
            // normal files with extension
            Pattern.compile("(.*)\\.(.*?$)", Pattern.CASE_INSENSITIVE) };
    private IOPermission     ioPermission = null;

    private LazyHostPlugin   lazyP        = null;

    public LazyHostPlugin getLazyP() {
        return lazyP;
    }

    public void setLazyP(LazyHostPlugin lazyP) {
        this.lazyP = lazyP;
    }

    @Deprecated
    public PluginForHost(final PluginWrapper wrapper) {
        super(wrapper);
        /* defaultPlugin does not need any Browser instance */
        br = null;
        dl = null;
        /* defaultPlugins do not have any working logger */
        /* workaround for all the lazy init issues */
        this.lazyP = (LazyHostPlugin) wrapper.getLazy();
    }

    public void setLogger(JDPluginLogger logger) {
        this.logger = logger;
    }

    public JDPluginLogger getLogger() {

        return (JDPluginLogger) logger;
    }

    public void setBrowser(Browser brr) {
        br = brr;
    }

    protected String getCaptchaCode(final String captchaAddress, final DownloadLink downloadLink) throws IOException, PluginException {
        return getCaptchaCode(getHost(), captchaAddress, downloadLink);
    }

    @Override
    public long getVersion() {
        return lazyP.getVersion();
    }

    @Override
    public Pattern getSupportedLinks() {
        return lazyP.getPattern();
    }

    protected String getCaptchaCode(final String method, final String captchaAddress, final DownloadLink downloadLink) throws IOException, PluginException {
        if (captchaAddress == null) {
            logger.severe("Captcha Adresse nicht definiert");
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final File captchaFile = getLocalCaptchaFile();
        try {
            Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
        } catch (Exception e) {
            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        final String captchaCode = getCaptchaCode(method, captchaFile, downloadLink);
        captchaFile.delete();
        return captchaCode;
    }

    protected String getCaptchaCode(final File captchaFile, final DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(getHost(), captchaFile, downloadLink);
    }

    protected String getCaptchaCode(final String methodname, final File captchaFile, final DownloadLink downloadLink) throws PluginException {

        return getCaptchaCode(methodname, captchaFile, 0, downloadLink, null, null);
    }

    protected String getCaptchaCode(final String method, final File file, final int flag, final DownloadLink link, final String defaultValue, final String explain) throws PluginException {
        final LinkStatus linkStatus = link.getLinkStatus();
        final String status = linkStatus.getStatusText();
        try {
            linkStatus.addStatus(LinkStatus.WAITING_USERIO);
            linkStatus.setStatusText(_JDT._.gui_downloadview_statustext_jac());
            try {
                final BufferedImage img = ImageIO.read(file);

                linkStatus.setStatusIcon(new ImageIcon(IconIO.getScaledInstance(img, 16, 16)));
            } catch (Throwable e) {
                e.printStackTrace();
            }
            final String cc = new CaptchaController(ioPermission, method, file, defaultValue, explain, this).getCode(flag);
            if (cc == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            return cc;
        } finally {
            linkStatus.removeStatus(LinkStatus.WAITING_USERIO);
            linkStatus.setStatusText(status);
            linkStatus.setStatusIcon(null);
        }
    }

    protected DownloadInterface                dl                                           = null;

    private static final HashMap<String, Long> LAST_CONNECTION_TIME                         = new HashMap<String, Long>();
    private static final HashMap<String, Long> LAST_STARTED_TIME                            = new HashMap<String, Long>();
    private static final String                AUTO_FILE_NAME_CORRECTION_NAME_SPLIT         = "AUTO_FILE_NAME_CORRECTION_NAME_SPLIT";
    private static final String                AUTO_FILE_NAME_CORRECTION_NAME_SPLIT_PATTERN = "AUTO_FILE_NAME_CORRECTION_NAME_SPLIT_PATTERN";

    private Long                               WAIT_BETWEEN_STARTS                          = 0L;

    private boolean                            enablePremium                                = false;

    private boolean                            accountWithoutUsername                       = false;

    private String                             premiumurl                                   = null;

    private DownloadLink                       link                                         = null;

    public boolean checkLinks(final DownloadLink[] urls) {
        return false;
    }

    @Override
    public String getHost() {
        return lazyP.getDisplayName();
    }

    @Override
    public SubConfiguration getPluginConfig() {
        return SubConfiguration.getConfig(lazyP.getHost());
    }

    @Override
    public void clean() {
        dl = null;
        br = null;
        super.clean();
    }

    public void setDownloadInterface(DownloadInterface dl) {
        this.dl = dl;
    }

    protected void setBrowserExclusive() {
        if (br == null) return;
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
    }

    public PluginForHost getNewInstance() {
        if (lazyP == null) return null;
        return lazyP.newInstance();
    }

    /** default fetchAccountInfo, set account valid to true */
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        account.setValid(true);
        return ai;
    }

    public boolean getAccountwithoutUsername() {
        return accountWithoutUsername;
    }

    public void setAccountwithoutUsername(boolean b) {
        accountWithoutUsername = b;
    }

    public abstract String getAGBLink();

    protected void enablePremium() {
        enablePremium(null);
    }

    protected void enablePremium(final String url) {
        premiumurl = url;
        enablePremium = true;
    }

    /**
     * Hier werden Treffer fuer Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem ArrayList zurueckgeliefert
     * 
     * @param data
     *            Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein ArrayList mit den gefundenen Downloadlinks
     */
    public ArrayList<DownloadLink> getDownloadLinks(final String data, final FilePackage fp) {
        ArrayList<DownloadLink> links = null;

        final String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
        if (hits != null && hits.length > 0) {
            links = new ArrayList<DownloadLink>(hits.length);
            for (String file : hits) {
                /* remove newlines... */
                file = file.trim();
                /*
                 * this removes the " from HTMLParser.ArrayToString
                 */
                /* only 1 " at start */
                while (file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                /* can have several " at the end */
                while (file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }

                try {
                    /*
                     * use this REGEX to cut of following http links,
                     * (?=https?:|$|\r|\n|)
                     */
                    final DownloadLink link = new DownloadLink(this, null, getHost(), file, true);
                    links.add(link);
                } catch (Throwable e) {
                    JDLogger.exception(e);
                }
            }
        }
        if (links != null && fp != null && fp != FilePackage.getDefaultFilePackage()) {
            fp.addLinks(links);
        }
        return links;
    }

    /*
     * OVERRIDE this function if you need to modify the link, ATTENTION: you
     * have to use new browser instances, this plugin might not have one!
     */
    public void correctDownloadLink(final DownloadLink link) throws Exception {
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigroeße, Dateiname,
     * verfuegbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfuegbar)
     * @throws IOException
     */
    public abstract AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception;

    /**
     * Gibt einen String mit den Dateiinformationen zurueck. Die Defaultfunktion
     * gibt nur den dateinamen zurueck. Allerdings Sollte diese Funktion
     * ueberschrieben werden. So kann ein Plugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param downloadLink
     * @return
     */
    public String getFileInformationString(final DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + Formatter.formatReadable(downloadLink.getDownloadSize()) + ")";
    }

    public int getMaxRetries() {
        return JsonConfig.create(GeneralSettings.class).getMaxPluginRetries();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public int getMaxSimultanDownload(final Account account) {
        int max;
        if (account == null) {
            max = getMaxSimultanFreeDownloadNum();
        } else {
            max = account.getMaxSimultanDownloads();
            if (max < 0) {
                return Integer.MAX_VALUE;
            } else if (max == 0) {
                max = getMaxSimultanPremiumDownloadNum();
            } else {
                return max;
            }
        }
        if (max <= 0) return Integer.MAX_VALUE;
        if (max == Integer.MIN_VALUE) return 0;
        return max;
    }

    /*
     * Override this if you want special handling for DownloadLinks to bypass
     * MaxSimultanDownloadNum
     */
    public boolean bypassMaxSimultanDownloadNum(DownloadLink link, Account acc) {
        return false;
    }

    /* TODO: remove with next major update */
    @Deprecated
    public boolean isPremiumDownload() {
        return true;
    }

    public synchronized long getLastTimeStarted() {
        if (!LAST_STARTED_TIME.containsKey(getHost())) { return 0; }
        return Math.max(0, (LAST_STARTED_TIME.get(getHost())));
    }

    public synchronized void putLastTimeStarted(long time) {
        LAST_STARTED_TIME.put(getHost(), time);
    }

    public synchronized long getLastConnectionTime() {
        if (!LAST_CONNECTION_TIME.containsKey(getHost())) { return 0; }
        return Math.max(0, (LAST_CONNECTION_TIME.get(getHost())));
    }

    public synchronized void putLastConnectionTime(long time) {
        LAST_CONNECTION_TIME.put(getHost(), time);
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        final LinkStatus linkStatus = link.getLinkStatus();
        linkStatus.addStatus(LinkStatus.ERROR_PLUGIN_DEFECT);
        linkStatus.setErrorMessage(_JDT._.plugins_hoster_nopremiumsupport());
    }

    public abstract void handleFree(DownloadLink link) throws Exception;

    /**
     * By overriding this method, a plugin is able to return a
     * HostPluginInfoGenerator. <br>
     * <b>Attention: Until next stable update, we have to return Object
     * here.</b>
     * 
     * @return
     */
    // @Override DO NEVER USE OVERRIDE ON THIS METHOD BEFORE NEXT STABLE UPDATE.
    public Object getInfoGenerator(Account account) {
        AccountInfo ai = account.getAccountInfo();
        HashMap<String, Object> props = null;
        if (ai == null) return null;
        props = ai.getProperties();
        if (props == null || props.size() == 0) return null;
        KeyValueInfoGenerator ret = new KeyValueInfoGenerator(_JDT._.pluginforhost_infogenerator_title(account.getUser(), account.getHoster()));
        for (Entry<String, Object> es : ai.getProperties().entrySet()) {
            String key = es.getKey();
            Object value = es.getValue();

            if (value != null) {
                ret.addPair(key, value.toString());
            }
        }
        return ret;
    }

    public void handle(final DownloadLink downloadLink, final Account account) throws Exception {
        try {
            while (waitForNextStartAllowed(downloadLink)) {
            }
        } catch (InterruptedException e) {
            return;
        }
        putLastTimeStarted(System.currentTimeMillis());
        try {
            if (account != null) {
                /* with account */
                handlePremium(downloadLink, account);
            } else {
                /* without account */
                handleFree(downloadLink);
            }
        } finally {
            try {
                dl.getConnection().disconnect();
            } catch (Throwable e) {
            }
            try {
                br.getHttpConnection().disconnect();
            } catch (Throwable e) {
            }
            try {
                downloadLink.getDownloadLinkController().getConnectionHandler().removeConnectionHandler(dl.getManagedConnetionHandler());
            } catch (final Throwable e) {
            }
            setDownloadInterface(null);
        }
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurueck (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    public abstract void resetDownloadlink(DownloadLink link);

    public void resetPluginGlobals() {
    }

    public int getTimegapBetweenConnections() {
        return 50;
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = interval;
    }

    public boolean waitForNextStartAllowed(final DownloadLink downloadLink) throws InterruptedException {
        final long time = Math.max(0, WAIT_BETWEEN_STARTS - (System.currentTimeMillis() - getLastTimeStarted()));
        if (time > 0) {
            try {
                sleep(time, downloadLink);
            } catch (PluginException e) {
                throw new InterruptedException();
            }
            return true;
        } else {
            return false;
        }
    }

    public boolean waitForNextConnectionAllowed() throws InterruptedException {
        final long time = Math.max(0, getTimegapBetweenConnections() - (System.currentTimeMillis() - getLastConnectionTime()));
        if (time > 0) {
            Thread.sleep(time);
            return true;
        } else {
            return false;
        }
    }

    public void sleep(final long i, final DownloadLink downloadLink) throws PluginException {
        sleep(i, downloadLink, "");
    }

    public void sleep(long i, DownloadLink downloadLink, String message) throws PluginException {
        SingleDownloadController dlc = downloadLink.getDownloadLinkController();
        try {
            while (i > 0 && dlc != null && !dlc.isAborted()) {
                i -= 1000;
                downloadLink.getLinkStatus().setStatusText(message + _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(i / 1000)));
                synchronized (this) {
                    this.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.TODO);
        }
        downloadLink.getLinkStatus().setStatusText(null);
    }

    /**
     * may only be used from within the plugin, because it only works when
     * SingleDownloadController is still running
     */
    protected boolean isAborted(DownloadLink downloadLink) {
        SingleDownloadController dlc = downloadLink.getDownloadLinkController();
        return (dlc != null && dlc.isAborted());
    }

    public Browser getBrowser() {
        return br;
    }

    /**
     * Gibt die Url zurueck, unter welcher ein PremiumAccount gekauft werden
     * kann
     * 
     * @return
     */
    public String getBuyPremiumUrl() {
        if (premiumurl != null) return "http://jdownloader.org/r.php?u=" + Encoding.urlEncode(premiumurl);
        return premiumurl;
    }

    public boolean isPremiumEnabled() {
        return enablePremium;
    }

    /**
     * returns hosterspecific infos. for example the downloadserver
     * 
     * @return
     */
    public String getSessionInfo() {
        return getHost();
    }

    public void setDownloadLink(DownloadLink link) {
        this.link = link;
    }

    public DownloadLink getDownloadLink() {
        return link;
    }

    /* override this if you want to change a link to use this plugin */
    /* dont forget to change host with setHost */
    /* must return true if changing was successful */
    /* if this function needs a browser, it must create an instance on its own */
    public boolean rewriteHost(DownloadLink link) {
        return false;
    }

    public String getCustomFavIconURL() {
        return getHost();
    }

    /**
     * @param ioPermission
     *            the ioPermission to set
     */
    public void setIOPermission(IOPermission ioPermission) {
        this.ioPermission = ioPermission;
    }

    /**
     * @return the ioPermission
     */
    public IOPermission getIOPermission() {
        return ioPermission;
    }

    public DomainInfo getDomainInfo() {
        String host = getCustomFavIconURL();
        if (host == null) host = getHost();
        return DomainInfo.getInstance(host);
    }

    public boolean hasCaptcha() {
        return false;
    }

    public boolean hasAutoCaptcha() {
        return JACMethod.hasMethod(getHost());
    }

    /**
     * plugins may change the package identifier used for auto package matching.
     * some hosters replace chars, shorten filenames...
     * 
     * @param packageIdentifier
     * @return
     */
    public String filterPackageID(String packageIdentifier) {
        return packageIdentifier;
    }

    /**
     * Some hosters have bad filenames. Rapidshare for example replaces all
     * special chars and spaces with _. Plugins can try to autocorrect this
     * based on other downloadlinks
     * 
     * @param cache
     *            TODO
     * @param downloadLink
     * @param dlinks
     * @param orgiginalfilename
     */
    // public String autoFilenameCorrection(String orgiginalfilename,
    // DownloadLink downloadLink, ArrayList<DownloadLink> dlinks) {
    // return null;
    // }

    public String autoFilenameCorrection(HashMap<Object, Object> cache, String originalFilename, DownloadLink downloadLink, ArrayList<DownloadLink> dlinks) {
        try {

            String MD5 = downloadLink.getMD5Hash();
            String SHA1 = downloadLink.getSha1Hash();
            // auto partname correction

            String[] multiPart = cache != null ? (String[]) cache.get(AUTO_FILE_NAME_CORRECTION_NAME_SPLIT + originalFilename) : null;
            Pattern pattern = cache != null ? (Pattern) cache.get(AUTO_FILE_NAME_CORRECTION_NAME_SPLIT_PATTERN + originalFilename) : null;
            // find first match

            if (pattern == null) {
                for (Pattern p : PATTERNS) {
                    multiPart = new Regex(originalFilename, p).getRow(0);
                    if (multiPart != null) {
                        pattern = p;
                        if (cache != null) {
                            cache.put(AUTO_FILE_NAME_CORRECTION_NAME_SPLIT + originalFilename, multiPart);
                            cache.put(AUTO_FILE_NAME_CORRECTION_NAME_SPLIT_PATTERN + originalFilename, pattern);
                        }
                        break;
                    }
                }

                if (multiPart == null) {
                    multiPart = new String[] { originalFilename, "" };
                }
            }
            String filteredName = filterPackageID(multiPart[0]);
            String prototypesplit;
            String newName;
            for (DownloadLink next : dlinks) {

                if (downloadLink == next) {

                    continue;
                }

                if (next.getHost().equals(getHost())) {
                    continue;
                }

                prototypesplit = cache != null ? (String) cache.get(pattern) : null;
                String prototypeName = next.getNameSetbyPlugin();
                if (prototypeName.equals(originalFilename)) {

                    continue;
                }

                if (prototypesplit == null) {
                    if (pattern != null) {
                        prototypesplit = new Regex(prototypeName, pattern).getMatch(0);
                    } else {
                        prototypesplit = prototypeName;
                    }
                    if (cache != null) cache.put(pattern, prototypesplit);

                }

                if (prototypesplit.equalsIgnoreCase(multiPart[0])) {
                    newName = fixCase(cache, originalFilename, prototypeName);
                    if (newName != null) {
                        //

                        return newName;
                    }
                }
                if (isHosterManipulatesFilenames() && multiPart[0].length() == prototypesplit.length() && filteredName.equalsIgnoreCase(filterPackageID(prototypesplit))) {
                    newName = getFixedFileName(originalFilename, prototypesplit);
                    if (newName != null) {

                        String caseFix = fixCase(cache, newName + multiPart[1], prototypeName);
                        if (caseFix != null) {
                            //
                            return caseFix;
                        }
                        return newName + multiPart[1];
                    }
                }

                if ((MD5 != null && MD5.equalsIgnoreCase(next.getMD5Hash())) || (SHA1 != null && SHA1.equalsIgnoreCase(next.getSha1Hash()))) {
                    // 100% mirror! ok and now? these files should have the
                    // same filename!!
                    return next.getName();
                }
            }

        } catch (Throwable e) {
            Log.exception(e);
        }

        return null;
    }

    protected String fixCase(HashMap<Object, Object> cache, String originalFilename, String prototypeName) {
        if (cache != null) {
            Object ret = cache.get(originalFilename + "_" + prototypeName);
            if (ret != null) return (String) ret;
        }
        // String[] multiPart = null;
        // Pattern pattern = null;
        // find first match
        // todo implement part file support
        // for (Pattern p : PATTERNS) {
        // multiPart = new Regex(originalFilename, p).getRow(0);
        // if (multiPart != null) {
        // pattern = p;
        // break;
        // }
        // }
        //
        // if (multiPart == null) {
        // multiPart = new String[] { originalFilename, "" };
        // }
        //
        boolean eic = originalFilename.equals(prototypeName);
        StringBuilder sb = new StringBuilder(prototypeName.length());
        for (int i = 0; i < prototypeName.length(); i++) {
            char c = originalFilename.charAt(i);
            char correctc = prototypeName.charAt(i);
            if (Character.toLowerCase(c) == Character.toLowerCase(correctc)) {
                if (eic) {
                    sb.append(Character.isUpperCase(c) ? c : correctc);
                } else {
                    // for fixcase after rename cases
                    sb.append(correctc);
                }
                // may cause filename errors
            } else if (Character.isDigit(c) && Character.isDefined(correctc)) {
                sb.append(c);
            } else {
                return null;
            }
        }
        if (cache != null) cache.put(originalFilename + "_" + prototypeName, sb.toString());
        return sb.toString();
    }

    protected String getFixedFileName(String originalFilename, String prototypeName) {
        throw new RuntimeException("not implemented");
    }

    /**
     * Some hoster manipulate the filename after upload. rapidshare for example,
     * replaces special chars and spaces with _
     * 
     * @return
     */
    public boolean isHosterManipulatesFilenames() {
        return false;
    }

    /**
     * Can be extended by the plugin to add entries to the contextmenu in the
     * downloadtable
     * 
     * @param m
     * @param downloadLink
     */
    public void extendDownloadTablePropertiesMenu(JMenu m, DownloadLink downloadLink) {
    }

}