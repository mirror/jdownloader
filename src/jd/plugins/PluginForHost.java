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

import java.awt.MouseInfo;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import jd.PluginWrapper;
import jd.captcha.JACMethod;
import jd.config.SubConfiguration;
import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterfaceFactory;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Exceptions;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.controlling.DownloadLinkWalker;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.accounts.AccountFactory;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.translate._JDT;

/**
 * Dies ist die Oberklasse fuer alle Plugins, die von einem Anbieter Dateien herunterladen koennen
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {
    private static Pattern[] PATTERNS = new Pattern[] {
                                      /**
                                       * these patterns should split filename and fileextension (extension must include the point)
                                       */
                                      // multipart rar archives
            Pattern.compile("(.*)(\\.pa?r?t?\\.?[0-9]+.*?\\.rar$)", Pattern.CASE_INSENSITIVE),
            // normal files with extension
            Pattern.compile("(.*)(\\..*?$)", Pattern.CASE_INSENSITIVE) };

    private LazyHostPlugin   lazyP    = null;

    public LazyHostPlugin getLazyP() {
        return lazyP;
    }

    public void setLazyP(LazyHostPlugin lazyP) {
        this.lazyP = lazyP;
    }

    public void errLog(Throwable e, Browser br, DownloadLink link) {
        LogSource errlogger = LogController.getInstance().getLogger("PluginErrors");
        try {
            errlogger.severe("HosterPlugin out of date: " + this + " :" + getVersion());
            errlogger.severe("URL was: " + link.getDownloadURL());
            if (e != null) errlogger.log(e);
        } finally {
            errlogger.close();
        }
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
        File captchaFile = null;
        try {
            captchaFile = getLocalCaptchaFile();
            try {
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
            } catch (Exception e) {
                logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            final String captchaCode = getCaptchaCode(method, captchaFile, downloadLink);
            return captchaCode;
        } finally {
            captchaFile.delete();
        }
    }

    protected String getCaptchaCode(final File captchaFile, final DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(getHost(), captchaFile, downloadLink);
    }

    protected String getCaptchaCode(final String methodname, final File captchaFile, final DownloadLink downloadLink) throws PluginException {

        return getCaptchaCode(methodname, captchaFile, 0, downloadLink, null, null);
    }

    protected String getCaptchaCode(final String method, File file, final int flag, final DownloadLink link, final String defaultValue, final String explain) throws PluginException {
        final LinkStatus linkStatus = link.getLinkStatus();
        final String status = linkStatus.getStatusText();
        int latest = linkStatus.getLatestStatus();
        try {
            linkStatus.addStatus(LinkStatus.WAITING_USERIO);
            linkStatus.setStatusText(_JDT._.gui_downloadview_statustext_jac());
            try {
                final BufferedImage img = ImageProvider.read(file);
                linkStatus.setStatusIcon(new ImageIcon(IconIO.getScaledInstance(img, 16, 16)));
            } catch (Throwable e) {
                e.printStackTrace();
            }

            String orgCaptchaImage = link.getStringProperty("orgCaptchaFile", null);
            if (orgCaptchaImage != null && new File(orgCaptchaImage).exists()) {
                file = new File(orgCaptchaImage);
            }
            BasicCaptchaChallenge c = new BasicCaptchaChallenge(method, file, defaultValue, explain, this, flag) {

                @Override
                public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                    boolean ret;
                    switch (skipRequest) {

                    case BLOCK_ALL_CAPTCHAS:

                        return true;

                    case BLOCK_HOSTER:

                        return PluginForHost.this.getHost().equals(Challenge.getHost(challenge));

                    case BLOCK_PACKAGE:

                        ret = link.getFilePackage() == Challenge.getDownloadLink(challenge).getFilePackage();
                        ret &= ((PluginForHost) link.getDefaultPlugin()).hasCaptcha(link, null);
                        return ret;
                    case REFRESH:
                    case SINGLE:
                    default:
                        return false;

                    }
                }

            };

            if (CaptchaBlackList.getInstance().matches(c)) {
                logger.warning("Cancel. Blacklist Matching");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }

            ChallengeResponseController.getInstance().handle(c);

            if (!c.isSolved()) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            return c.getResult().getValue();
        } catch (InterruptedException e) {
            logger.warning(Exceptions.getStackTrace(e));

            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } catch (SkipException e) {

            switch (e.getSkipRequest()) {

            case BLOCK_ALL_CAPTCHAS:

                DownloadController.getInstance().set(new DownloadLinkWalker() {

                    @Override
                    public boolean accept(DownloadLink link) {

                        boolean ret = ((PluginForHost) link.getDefaultPlugin()).hasCaptcha(link, null);
                        return ret;
                    }

                    @Override
                    public boolean accept(FilePackage fp) {
                        return true;
                    }

                    @Override
                    public void handle(DownloadLink link) {
                        link.setSkipped(true);
                    }

                });
                HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));

                break;

            case BLOCK_HOSTER:
                DownloadController.getInstance().set(new DownloadLinkWalker() {

                    @Override
                    public boolean accept(DownloadLink link) {
                        boolean ret = link.getHost().equals(getHost());

                        ret &= ((PluginForHost) link.getDefaultPlugin()).hasCaptcha(link, null);
                        return ret;
                    }

                    @Override
                    public boolean accept(FilePackage fp) {
                        return true;
                    }

                    @Override
                    public void handle(DownloadLink link) {
                        link.setSkipped(true);
                    }

                });
                HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                break;

            case BLOCK_PACKAGE:
                DownloadController.getInstance().set(new DownloadLinkWalker() {

                    @Override
                    public boolean accept(DownloadLink link) {
                        boolean ret = link.getFilePackage() == getDownloadLink().getFilePackage();
                        ret &= ((PluginForHost) link.getDefaultPlugin()).hasCaptcha(link, null);
                        return ret;
                    }

                    @Override
                    public boolean accept(FilePackage fp) {
                        return true;
                    }

                    @Override
                    public void handle(DownloadLink link) {
                        link.setSkipped(true);
                    }

                });

                HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));

                break;

            case SINGLE:
                getDownloadLink().setSkipped(true);

                HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                break;
            case REFRESH:
                // we should forward the refresh request to a new pluginstructure soon. For now. the plugin will just retry
                break;
            }

            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } finally {
            linkStatus.removeStatus(LinkStatus.WAITING_USERIO);
            linkStatus.addStatus(latest);
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

    protected DownloadInterfaceFactory         customizedDownloadFactory                    = null;

    public DownloadInterfaceFactory getCustomizedDownloadFactory() {
        return customizedDownloadFactory;
    }

    public void setCustomizedDownloadFactory(DownloadInterfaceFactory customizedDownloadFactory) {
        this.customizedDownloadFactory = customizedDownloadFactory;
    }

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
        try {
            dl.getConnection().disconnect();
        } catch (Throwable e) {
        } finally {
            setDownloadInterface(null);
        }
        try {
            br.disconnect();
        } catch (Throwable e) {
        } finally {
            br = null;
        }
        super.clean();
    }

    public void setDownloadInterface(DownloadInterface dl) {
        DownloadInterface oldDl = this.dl;
        this.dl = dl;
        if (oldDl != null && oldDl != dl) {
            try {
                oldDl.close();
            } catch (final Throwable e) {
                LogSource.exception(getLogger(), e);
            }
        }
    }

    protected void setBrowserExclusive() {
        if (br == null) return;
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
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
     * Hier werden Treffer fuer Downloadlinks dieses Anbieters in diesem Text gesucht. Gefundene Links werden dann in einem ArrayList
     * zurueckgeliefert
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
                     * use this REGEX to cut of following http links, (?=https?:|$|\r|\n|)
                     */
                    /* we use null as ClassLoader to make sure all share the same ProtoTypeClassLoader */
                    final DownloadLink link = new DownloadLink(getLazyP().getPrototype(null), null, getHost(), file, true);
                    links.add(link);
                } catch (Throwable e) {
                    LogSource.exception(logger, e);
                }
            }
        }
        if (links != null && fp != null && fp != FilePackage.getDefaultFilePackage()) {
            fp.addLinks(links);
        }
        return links;
    }

    /*
     * OVERRIDE this function if you need to modify the link, ATTENTION: you have to use new browser instances, this plugin might not have
     * one!
     */
    public void correctDownloadLink(final DownloadLink link) throws Exception {
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigroeße, Dateiname, verfuegbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfuegbar)
     * @throws IOException
     */
    public abstract AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception;

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    /*
     * Integer.Min_Value will result in no download at all (eg no free supported)
     * 
     * <=0 will result in unlimited
     * 
     * return max possible simultan downloads for given link and account,overwrite this if you want special handling, eg for multihost
     */
    public int getMaxSimultanDownload(DownloadLink link, final Account account) {
        int max;
        if (account == null) {
            max = getMaxSimultanFreeDownloadNum();
            if (max == 0 || max == Integer.MIN_VALUE) {
                /* no free downloads are possible */
                return 0;
            }
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
        if (max == Integer.MIN_VALUE) return 0;
        if (max <= 0) return Integer.MAX_VALUE;
        return max;
    }

    /*
     * Override this if you want special handling for DownloadLinks to bypass MaxSimultanDownloadNum
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
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    public abstract void handleFree(DownloadLink link) throws Exception;

    /**
     * By overriding this method, a plugin is able to return a HostPluginInfoGenerator. <br>
     * <b>Attention: Until next stable update, we have to return Object here.</b>
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

    /*
     * finer controlling if we can download the link with given account, eg link is only downloadable for premium ones
     */
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return true;
    }

    public boolean enoughTrafficFor(DownloadLink downloadLink, Account account) {
        AccountInfo ai = null;
        if (account != null && (ai = account.getAccountInfo()) != null) {
            if (ai.getTrafficLeft() >= 0 && ai.getTrafficLeft() < downloadLink.getDownloadSize()) return false;
        }
        return true;
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
                if (account.getHoster().equalsIgnoreCase(downloadLink.getHost())) {
                    handlePremium(downloadLink, account);
                } else {
                    handleMultiHost(downloadLink, account);
                }
            } else {
                /* without account */
                handleFree(downloadLink);
            }
        } finally {
            try {
                downloadLink.getDownloadLinkController().getConnectionHandler().removeConnectionHandler(dl.getManagedConnetionHandler());
            } catch (final Throwable e) {
            }
            clean();
        }
    }

    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        /*
         * fetchAccountInfo must fill ai.setProperty("multiHostSupport", ArrayList<String>); to signal all supported multiHosts
         * 
         * please synchronized on accountinfo and the ArrayList<String> when you change something in the handleMultiHost function
         * 
         * in fetchAccountInfo we don't have to synchronize because we create a new instance of AccountInfo and fill it
         * 
         * if you need customizable maxDownloads, please use getMaxSimultanDownload to handle this you are in multihost when account host
         * does not equal link host!
         * 
         * 
         * 
         * will update this doc about error handling
         */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurueck (variablen intialisieren etc)
     */
    public abstract void reset();

    public abstract void resetDownloadlink(DownloadLink link);

    @Deprecated
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
        PluginProgress progress = new PluginProgress(i, i, null);
        progress.setIcon(NewTheme.I().getIcon("wait", 16));
        progress.setProgressSource(this);
        downloadLink.setPluginProgress(progress);
        try {
            while (i > 0 && dlc != null && !dlc.isAborted()) {
                i -= 1000;
                progress.setCurrent(i);
                downloadLink.getLinkStatus().setStatusText(message + _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(i / 1000)));
                synchronized (this) {
                    this.wait(1000);
                }
            }
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.TODO);
        } finally {
            downloadLink.setPluginProgress(null);
            downloadLink.getLinkStatus().setStatusText(null);
        }

    }

    /**
     * may only be used from within the plugin, because it only works when SingleDownloadController is still running
     */
    protected boolean isAborted(DownloadLink downloadLink) {
        SingleDownloadController dlc = downloadLink.getDownloadLinkController();
        return (dlc != null && dlc.isAborted());
    }

    public Browser getBrowser() {
        return br;
    }

    /**
     * Gibt die Url zurueck, unter welcher ein PremiumAccount gekauft werden kann
     * 
     * @return
     */
    public String getBuyPremiumUrl() {
        if (premiumurl != null) return premiumurl;
        return premiumurl;
    }

    public boolean isPremiumEnabled() {
        return enablePremium;
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

    public DomainInfo getDomainInfo() {
        String host = getCustomFavIconURL();
        if (host == null) host = getHost();
        return DomainInfo.getInstance(host);
    }

    public static void main(String[] args) throws IOException {
        try {
            File home = new File(Application.getRessourceURL(PluginForHost.class.getName().replace(".", "/") + ".class").toURI()).getParentFile().getParentFile().getParentFile().getParentFile();

            File hostPluginsDir = new File(home, "src/jd/plugins/hoster/");

            for (File f : hostPluginsDir.listFiles()) {
                if (f.getName().endsWith(".java")) {

                    StringBuilder method = new StringBuilder();
                    String src = IO.readFileToString(f);

                    if (src.toLowerCase().contains("captcha")) {

                        if (new Regex(src, "(boolean\\s+hasCaptcha\\(\\s*DownloadLink .*?\\,\\s*Account .*?\\))").matches()) {
                            continue;
                        }

                        if (src.contains("enablePremium")) {
                            method.append("\r\n/* NO OVERRIDE!! We need to stay 0.9*compatible */");
                            method.append("\r\npublic boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {");
                            method.append("\r\nif (acc == null) {");
                            method.append("\r\n/* no account, yes we can expect captcha */");
                            method.append("\r\nreturn true;");
                            method.append("\r\n}");

                            method.append("\r\n if (Boolean.TRUE.equals(acc.getBooleanProperty(\"free\"))) {");
                            method.append("\r\n/* free accounts also have captchas */");
                            method.append("\r\nreturn true;");
                            method.append("\r\n}");
                            method.append("\r\nreturn false;");
                            method.append("\r\n}");

                        } else {
                            method.append("\r\n/* NO OVERRIDE!! We need to stay 0.9*compatible */");
                            method.append("\r\npublic boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {");
                            method.append("\r\nreturn true;");
                            method.append("\r\n}");
                        }

                    } else {

                    }

                    if (method.length() > 0) {

                        src = src.substring(0, src.lastIndexOf("}")) + method.toString() + "\r\n}";
                        f.delete();
                        IO.writeStringToFile(f, src);
                    }

                }
            }
            System.out.println(1);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /* can we expect a captcha if we try to load link with acc */
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        /* you must distinguish between different acc types! */
        /* acc=null is no account */
        /* best save the information in accountinformation! */
        return false;
    }

    /* do we have anticaptcha available for this host */
    /* ONLY override if you have customized this */
    public boolean hasAutoCaptcha() {
        return JACMethod.hasMethod(getHost());
    }

    /**
     * plugins may change the package identifier used for auto package matching. some hosters replace chars, shorten filenames...
     * 
     * @param packageIdentifier
     * @return
     */
    public String filterPackageID(String packageIdentifier) {
        return packageIdentifier;
    }

    /**
     * Some hosters have bad filenames. Rapidshare for example replaces all special chars and spaces with _. Plugins can try to autocorrect
     * this based on other downloadlinks
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

    public char[] getFilenameReplaceMap() {
        return new char[0];
    }

    public String autoFilenameCorrection(HashMap<Object, Object> cache, String originalFilename, DownloadLink downloadLink, ArrayList<DownloadLink> dlinks) {
        try {
            // cache = null;
            String MD5 = downloadLink.getMD5Hash();
            String SHA1 = downloadLink.getSha1Hash();
            // auto partname correction
            /*
             * this holds the filename split into name, extension(. included)
             */
            String[] fileNameSplit = null;
            /*
             * this holds the Pattern got used to split the filename
             */
            Pattern pattern = null;
            if (cache != null) {
                /* load from cache */
                fileNameSplit = (String[]) cache.get(AUTO_FILE_NAME_CORRECTION_NAME_SPLIT + originalFilename);
                pattern = (Pattern) cache.get(AUTO_FILE_NAME_CORRECTION_NAME_SPLIT_PATTERN + originalFilename);
            }
            char[] originalReplaces = getFilenameReplaceMap();
            // find first match
            if (pattern == null) {
                for (Pattern p : PATTERNS) {
                    fileNameSplit = new Regex(originalFilename, p).getRow(0);
                    if (fileNameSplit != null) {
                        /*
                         * regex matched, so we should now have filename, extension in fileNameSplit
                         */
                        pattern = p;
                        if (cache != null) {
                            /* update cache */
                            cache.put(AUTO_FILE_NAME_CORRECTION_NAME_SPLIT + originalFilename, fileNameSplit);
                            cache.put(AUTO_FILE_NAME_CORRECTION_NAME_SPLIT_PATTERN + originalFilename, pattern);
                        }
                        break;
                    }
                }
            }
            if (fileNameSplit == null) {
                /*
                 * no valid pattern found,lets split filename into name/extension as fallback
                 */
                fileNameSplit = CrossSystem.splitFileName(originalFilename);
                pattern = null;
            }
            String filteredName = filterPackageID(fileNameSplit[0]);
            String prototypesplit;
            String newName;
            for (DownloadLink next : dlinks) {
                if (downloadLink == next) {
                    /* same link */
                    continue;
                }
                if (next.getHost().equals(getHost())) {
                    /* same host */
                    continue;
                }
                String prototypeName = next.getNameSetbyPlugin();
                if (prototypeName.equals(originalFilename)) {
                    /* same name */
                    continue;
                }
                if (prototypeName.equalsIgnoreCase(originalFilename)) {
                    /* same name but different upper/lower cases */
                    newName = fixCase(cache, originalFilename, prototypeName);
                    if (newName != null) return newName;
                }
                /*
                 * this holds the filename that got extracted with same pattern as the originalFilename
                 */
                prototypesplit = null;
                if (cache != null && pattern != null) {
                    /* load prototype split from cache if available */
                    prototypesplit = (String) cache.get(prototypeName + pattern.toString());
                }
                if (prototypesplit == null) {
                    /* no prototypesplit available yet, create new one */
                    if (pattern != null) {
                        /*
                         * a pattern does exist, we must use the same one to make sure the *filetypes* match (eg . part01.rar and .r01 with
                         * same filename
                         */
                        prototypesplit = new Regex(prototypeName, pattern).getMatch(0);
                    } else {
                        /* no pattern available, lets use fallback */
                        prototypesplit = CrossSystem.splitFileName(prototypeName)[0];
                    }
                    if (prototypesplit == null) {
                        /*
                         * regex did not match, different *filetypes*
                         */
                        continue;
                    }
                    if (cache != null && pattern != null) {
                        /* update cache */
                        cache.put(prototypeName + pattern.toString(), prototypesplit);
                    }
                }

                if (isHosterManipulatesFilenames() && fileNameSplit[0].length() == prototypesplit.length() && filteredName.equalsIgnoreCase(filterPackageID(prototypesplit))) {
                    newName = getFixedFileName(originalFilename, originalReplaces, prototypesplit, next.getDefaultPlugin().getFilenameReplaceMap());
                    if (newName != null) {
                        String caseFix = fixCase(cache, newName + fileNameSplit[1], prototypeName);
                        if (caseFix != null) {
                            /* we had to fix the upper/lower cases */
                            return caseFix;
                        }
                        /* we have new name, add extension to it */
                        return newName + fileNameSplit[1];
                    }
                }

                if ((!StringUtils.isEmpty(MD5) && MD5.equalsIgnoreCase(next.getMD5Hash())) || (!StringUtils.isEmpty(SHA1) && SHA1.equalsIgnoreCase(next.getSha1Hash()))) {
                    // 100% mirror! ok and now? these files should have the
                    // same filename!!
                    return next.getName();
                }
            }

        } catch (Throwable e) {
            LogController.CL().log(e);
        }

        return null;
    }

    protected String getFixedFileName(String originalFilename, char[] originalReplaces, String prototypeName, char[] prototypeReplaces) {
        if (originalReplaces.length == 0 && prototypeReplaces.length == 0) {
            /* no replacements available */
            return null;
        }
        StringBuilder sb = new StringBuilder();
        mainLoop: for (int i = 0; i < prototypeName.length(); i++) {
            char oC = originalFilename.charAt(i);
            char pC = prototypeName.charAt(i);
            if (Character.toLowerCase(oC) != Character.toLowerCase(pC)) {
                for (char oCC : originalReplaces) {
                    /*
                     * first we check if char from Original is on replacement List, if so, we use char from prototype
                     */
                    if (oC == oCC) {
                        sb.append(pC);
                        continue mainLoop;
                    }
                }
                for (char pCC : prototypeReplaces) {
                    /*
                     * then we check if char from prototype is on replacement List, if so, we use char from original
                     */
                    if (pC == pCC) {
                        sb.append(oC);
                        continue mainLoop;
                    }
                }
                return null;
            } else {
                sb.append(oC);
            }
        }
        return sb.toString();
    }

    protected String fixCase(HashMap<Object, Object> cache, String originalFilename, String prototypeName) {
        if (cache != null) {
            Object ret = cache.get(originalFilename + "_" + prototypeName);
            if (ret != null) return (String) ret;
        }
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

    /**
     * Some hoster manipulate the filename after upload. rapidshare for example, replaces special chars and spaces with _
     * 
     * @return
     */
    public boolean isHosterManipulatesFilenames() {
        return false;
    }

    public int getMaxRetries(DownloadLink link, Account acc) {
        return JsonConfig.create(GeneralSettings.class).getMaxPluginRetries();
    }

    /**
     * If a plugin want's to define it's one premium info dialog or premiuminfo panel. overwrite this methods
     * 
     * @param dialog
     * @return
     */
    public JComponent layoutPremiumInfoPanel(AbstractDialog dialog) {
        return null;
    }

    /**
     * Can be overridden to support special accounts like login tokens instead of username/password
     * 
     * @return
     */
    public AccountFactory getAccountFactory() {
        // this should be plugincode as soon as we can ignore 0.9 compatibility
        if (getHost().equalsIgnoreCase("letitbit.net")) { return new LetitBitAccountFactory(); }
        return new DefaultAccountFactory();
    }

}