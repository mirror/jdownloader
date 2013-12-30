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
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.regex.Pattern;

import javax.swing.JComponent;

import jd.PluginWrapper;
import jd.captcha.JACMethod;
import jd.config.SubConfiguration;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.controlling.downloadcontroller.SingleDownloadController.WaitingQueueItem;
import jd.controlling.linkcrawler.CrawledLink;
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
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.blacklist.BlockAllDownloadCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByLink;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.accounts.AccountFactory;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.translate._JDT;

/**
 * Dies ist die Oberklasse fuer alle Plugins, die von einem Anbieter Dateien herunterladen koennen
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {
    private static Pattern[]            PATTERNS              = new Pattern[] {
                                                              /**
                                                               * these patterns should split filename and fileextension (extension must include the point)
                                                               */
                                                              // multipart rar archives
            Pattern.compile("(.*)(\\.pa?r?t?\\.?[0-9]+.*?\\.rar$)", Pattern.CASE_INSENSITIVE),
            // normal files with extension
            Pattern.compile("(.*)(\\..*?$)", Pattern.CASE_INSENSITIVE) };

    private LazyHostPlugin              lazyP                 = null;
    /**
     * Is true if the user has answerd a captcha challenge. does not say anything whether if the answer was correct or not
     */
    protected transient ResponseList<?> lastChallengeResponse = null;

    private boolean                     hasCaptchas           = false;

    public void setLastChallengeResponse(ResponseList<?> lastChallengeResponse) {
        this.lastChallengeResponse = lastChallengeResponse;
    }

    public LazyHostPlugin getLazyP() {
        return lazyP;
    }

    public void setLazyP(LazyHostPlugin lazyP) {
        this.lazyP = lazyP;
    }

    // override to let the plugin show an account details dialog
    public void showAccountDetailsDialog(Account account) {
    }

    public void errLog(Throwable e, Browser br, DownloadLink link) {
        LogSource errlogger = LogController.getInstance().getLogger("PluginErrors");
        try {
            errlogger.severe("HosterPlugin out of date: " + this + " :" + getVersion());
            if (link.gotBrowserUrl()) {
                errlogger.severe("URL:" + link.getDownloadURL() + "|BrowserURL:" + link.getBrowserUrl());
            } else {
                errlogger.severe("URL:" + link.getDownloadURL());
            }
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

    public DownloadInterface getDownloadInterface() {
        return dl;
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
            FileCreationManager.getInstance().delete(captchaFile, null);
        }
    }

    protected String getCaptchaCode(final File captchaFile, final DownloadLink downloadLink) throws PluginException {
        return getCaptchaCode(getHost(), captchaFile, downloadLink);
    }

    protected String getCaptchaCode(final String methodname, final File captchaFile, final DownloadLink downloadLink) throws PluginException {

        return getCaptchaCode(methodname, captchaFile, 0, downloadLink, null, null);
    }

    public void invalidateLastChallengeResponse() {
        try {
            ResponseList<?> lLastChallengeResponse = lastChallengeResponse;
            if (lLastChallengeResponse != null) {
                /* TODO: inform other solver that their response was not used */
                AbstractResponse<?> response = lLastChallengeResponse.get(0);
                if (response.getSolver() instanceof ChallengeResponseValidation) {
                    ChallengeResponseValidation validation = (ChallengeResponseValidation) response.getSolver();
                    try {
                        validation.setInvalid(response);
                    } catch (final Throwable e) {
                        LogSource.exception(getLogger(), e);
                    }
                }
            }
        } finally {
            lastChallengeResponse = null;
        }
    }

    public void validateLastChallengeResponse() {
        try {
            ResponseList<?> lLastChallengeResponse = lastChallengeResponse;
            if (lLastChallengeResponse != null) {
                /* TODO: inform other solver that their response was not used */
                AbstractResponse<?> response = lLastChallengeResponse.get(0);
                if (response.getSolver() instanceof ChallengeResponseValidation) {
                    ChallengeResponseValidation validation = (ChallengeResponseValidation) response.getSolver();
                    try {
                        validation.setValid(response);
                    } catch (final Throwable e) {
                        LogSource.exception(getLogger(), e);
                    }
                }
            }
        } finally {
            lastChallengeResponse = null;
        }
    }

    public boolean hasChallengeResponse() {
        return lastChallengeResponse != null;
    }

    protected String getCaptchaCode(final String method, File file, final int flag, final DownloadLink link, final String defaultValue, final String explain) throws PluginException {

        CaptchaStepProgress progress = new CaptchaStepProgress(0, 1, null);
        progress.setProgressSource(this);
        this.hasCaptchas = true;
        try {
            // try {
            // final BufferedImage img = ImageProvider.read(file);
            // progress.setIcon(new ImageIcon(IconIO.getScaledInstance(img, 16, 16)));
            // } catch (Throwable e) {
            // e.printStackTrace();
            // }
            link.setPluginProgress(progress);
            String orgCaptchaImage = link.getStringProperty("orgCaptchaFile", null);
            if (orgCaptchaImage != null && new File(orgCaptchaImage).exists()) {
                file = new File(orgCaptchaImage);
            }
            if (this.getDownloadLink() == null) this.setDownloadLink(link);
            final boolean insideAccountChecker = Thread.currentThread() instanceof AccountCheckerThread;
            BasicCaptchaChallenge c = new BasicCaptchaChallenge(method, file, defaultValue, explain, this, flag) {

                @Override
                public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                    if (insideAccountChecker) {
                        /* we don't want to skip login captcha inside fetchAccountInfo(Thread is AccountCheckerThread) */
                        return false;
                    }
                    Plugin challengePlugin = Challenge.getPlugin(challenge);
                    if (challengePlugin != null && !(challengePlugin instanceof PluginForHost)) {
                        /* we only want block PluginForHost captcha here */
                        return false;
                    }
                    switch (skipRequest) {
                    case BLOCK_ALL_CAPTCHAS:
                        /* user wants to block all captchas (current session) */
                        return true;
                    case BLOCK_HOSTER:
                        /* user wants to block captchas from specific hoster */
                        return StringUtils.equals(link.getHost(), Challenge.getHost(challenge));
                    case BLOCK_PACKAGE:
                        /* user wants to block captchas from current FilePackage */
                        DownloadLink lLink = Challenge.getDownloadLink(challenge);
                        if (lLink == null || lLink.getDefaultPlugin() == null) return false;
                        return link.getFilePackage() == lLink.getFilePackage();
                    default:
                        return false;
                    }
                }
            };
            c.setTimeout(getCaptchaTimeout());
            invalidateLastChallengeResponse();
            if (CaptchaBlackList.getInstance().matches(c)) {
                logger.warning("Cancel. Blacklist Matching");
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            ChallengeResponseController.getInstance().handle(c);
            if (!c.isSolved()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                setLastChallengeResponse(c.getResult());
            }
            return c.getResult().getValue();
        } catch (InterruptedException e) {
            logger.warning(Exceptions.getStackTrace(e));
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } catch (SkipException e) {
            if (getDownloadLink() != null) {
                switch (e.getSkipRequest()) {
                case BLOCK_ALL_CAPTCHAS:
                    CaptchaBlackList.getInstance().add(new BlockAllDownloadCaptchasEntry());

                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    break;
                case BLOCK_HOSTER:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByHost(getDownloadLink().getHost()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    break;

                case BLOCK_PACKAGE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByPackage(getDownloadLink().getParentNode()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    break;
                case SINGLE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(getDownloadLink()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    break;
                case TIMEOUT:
                    if (JsonConfig.create(CaptchaSettings.class).isSkipDownloadLinkOnCaptchaTimeoutEnabled()) {
                        CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(getDownloadLink()));
                        if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) HelpDialog.show(false, true, MouseInfo.getPointerInfo().getLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                case REFRESH:
                    // we should forward the refresh request to a new pluginstructure soon. For now. the plugin will just retry
                    break;
                }
            }
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        } finally {
            link.setPluginProgress(null);
        }
    }

    protected volatile DownloadInterface dl                                           = null;
    private static final String          AUTO_FILE_NAME_CORRECTION_NAME_SPLIT         = "AUTO_FILE_NAME_CORRECTION_NAME_SPLIT";
    private static final String          AUTO_FILE_NAME_CORRECTION_NAME_SPLIT_PATTERN = "AUTO_FILE_NAME_CORRECTION_NAME_SPLIT_PATTERN";

    private long                         WAIT_BETWEEN_STARTS                          = 0;

    private boolean                      enablePremium                                = false;

    private boolean                      accountWithoutUsername                       = false;

    private String                       premiumurl                                   = null;

    private DownloadLink                 link                                         = null;

    protected DownloadInterfaceFactory   customizedDownloadFactory                    = null;

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
        lastChallengeResponse = null;
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
     * Hier werden Treffer fuer Downloadlinks dieses Anbieters in diesem Text gesucht. Gefundene Links werden dann in einem ArrayList zurueckgeliefert
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
            try {
                PluginForHost plugin = getLazyP().getPrototype(null);
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

                    /*
                     * use this REGEX to cut of following http links, (?=https?:|$|\r|\n|)
                     */
                    /* we use null as ClassLoader to make sure all share the same ProtoTypeClassLoader */
                    final DownloadLink link = new DownloadLink(plugin, null, getHost(), file, true);
                    links.add(link);
                }
            } catch (Throwable e) {
                LogSource.exception(logger, e);
            }
        }
        if (links != null && fp != null && fp != FilePackage.getDefaultFilePackage()) {
            fp.addLinks(links);
        }
        return links;
    }

    /*
     * OVERRIDE this function if you need to modify the link, ATTENTION: you have to use new browser instances, this plugin might not have one!
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
     * -1 -> unlimited
     * 
     * <-1 || 0 = no download
     * 
     * 
     * return max possible simultan downloads for given link and account,overwrite this if you want special handling, eg for multihost
     */
    public int getMaxSimultanDownload(DownloadLink link, final Account account) {
        int max;
        if (account == null) {
            max = getMaxSimultanFreeDownloadNum();
            if (max >= 0) {
                /* >=0 = 0 or more downloads */
                return max;
            }
            if (max == -1) {
                /*-1 = unlimited*/
                return Integer.MAX_VALUE;
            }
            /* no downloads */
            return 0;
        } else {
            max = account.getMaxSimultanDownloads();
            if (max >= 1) {
                /* 1 or more downloads */
                return max;
            }
            if (max == 0) {
                /* 0 = use deprecated getMaxSimultanPremiumDownloadNum */
                max = getMaxSimultanPremiumDownloadNum();
                if (max >= 0) {
                    /* >=0 = 0 or more downloads */
                    return max;
                }
                if (max == -1) {
                    /*-1 = unlimited*/
                    return Integer.MAX_VALUE;
                }
                /* no downloads */
                return 0;
            }
            if (max == -1) {
                /*-1 = unlimited*/
                return Integer.MAX_VALUE;
            }
            /* no downloads */
            return 0;
        }
    }

    /* TODO: remove with next major update */
    @Deprecated
    public boolean isPremiumDownload() {
        return true;
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
        Map<String, Object> props = null;
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
            if (ai.isUnlimitedTraffic()) return true;
            if (ai.getTrafficLeft() >= 0 && ai.getTrafficLeft() < downloadLink.getDownloadSize()) return false;
        }
        return true;
    }

    public void handle(final DownloadLink downloadLink, final Account account) throws Exception {
        try {
            waitForNextStartAllowed(downloadLink);
            if (false) {
                if (true) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 5 * 60 * 1000l);
                if (false) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
                } else if (getHost().contains("share")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 5 * 1000l);
                } else if (getHost().contains("upload")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 90 * 5 * 1000l);
                } else if (getHost().contains("cloud")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, new Random().nextInt(5) * 60 * 1000l);
                }
            }
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
         * if you need customizable maxDownloads, please use getMaxSimultanDownload to handle this you are in multihost when account host does not equal link
         * host!
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

    public List<File> deleteDownloadLink(DownloadLink link) {
        List<File> ret = new ArrayList<File>();
        ret.add(new File(link.getFileOutput() + ".part"));
        ret.add(new File(link.getFileOutput()));
        ret.add(new File(link.getFileOutput(false, true)));
        return ret;
    }

    public int getTimegapBetweenConnections() {
        return 50;
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = Math.max(0, interval);
    }

    protected void waitForNextStartAllowed(final DownloadLink downloadLink) throws PluginException, InterruptedException {
        WaitingQueueItem queueItem = downloadLink.getDownloadLinkController().getQueueItem();
        long wait = WAIT_BETWEEN_STARTS;
        if (wait == 0) {
            queueItem.lastStartTimestamp.set(System.currentTimeMillis());
            return;
        }
        PluginProgress progress = new PluginProgress(0, 0, null) {
            private String pluginMessage = null;

            @Override
            public String getMessage(Object requestor) {
                return pluginMessage;
            }

            @Override
            public void updateValues(long current, long total) {
                if (current > 0) {
                    pluginMessage = _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(current / 1000));
                } else {
                    pluginMessage = null;
                }
                super.updateValues(current, total);
            }
        };
        progress.setIcon(NewTheme.I().getIcon("wait", 16));
        progress.setProgressSource(this);
        try {
            long lastQueuePosition = -1;
            long waitQueuePosition = -1;
            long waitMax = 0;
            long waitCur = 0;
            while ((waitQueuePosition = queueItem.indexOf(downloadLink)) >= 0) {
                if (waitQueuePosition != lastQueuePosition) {
                    waitMax = (queueItem.lastStartTimestamp.get() - System.currentTimeMillis()) + ((waitQueuePosition + 1) * wait);
                    waitCur = waitMax;
                    lastQueuePosition = waitQueuePosition;
                }
                if (waitCur <= 0) {
                    break;
                }
                downloadLink.setPluginProgress(progress);
                progress.updateValues(waitCur, waitMax);
                long wTimeout = Math.min(1000, Math.max(0, waitCur));
                synchronized (this) {
                    wait(wTimeout);
                }
                waitCur -= wTimeout;
            }
            if (downloadLink.getDownloadLinkController().isAborting()) throw new PluginException(LinkStatus.ERROR_RETRY);
            queueItem.lastStartTimestamp.set(System.currentTimeMillis());
        } catch (final InterruptedException e) {
            if (downloadLink.getDownloadLinkController().isAborting()) throw new PluginException(LinkStatus.ERROR_RETRY);
            throw e;
        } finally {
            downloadLink.setPluginProgress(null);
        }
    }

    public void waitForNextConnectionAllowed(DownloadLink downloadLink) throws InterruptedException {
        WaitingQueueItem queueItem = downloadLink.getDownloadLinkController().getQueueItem();
        long wait = getTimegapBetweenConnections();
        if (wait <= 0) {
            queueItem.lastConnectionTimestamp.set(System.currentTimeMillis());
            return;
        }
        while (true) {
            long lastConnectionTimestamp = queueItem.lastConnectionTimestamp.get();
            long waitCur = Math.max(0, lastConnectionTimestamp - System.currentTimeMillis() + wait);
            if (waitCur <= 0) {
                queueItem.lastConnectionTimestamp.set(System.currentTimeMillis());
                break;
            }
            if (downloadLink.getDownloadLinkController().isAborting()) throw new InterruptedException("Controller aborted");
            Thread.sleep(waitCur);
            if (queueItem.lastConnectionTimestamp.compareAndSet(lastConnectionTimestamp, System.currentTimeMillis())) break;
        }
        if (downloadLink.getDownloadLinkController().isAborting()) throw new InterruptedException("Controller aborted");
    }

    protected void sleep(final long i, final DownloadLink downloadLink) throws PluginException {
        sleep(i, downloadLink, "");
    }

    @Deprecated
    public void resetPluginGlobals() {
    }

    protected void sleep(long i, DownloadLink downloadLink, final String message) throws PluginException {
        PluginProgress progress = new PluginProgress(i, i, null) {
            private String pluginMessage = message;

            @Override
            public String getMessage(Object requestor) {
                return pluginMessage;
            }

            @Override
            public void setCurrent(long current) {
                if (current > 0) {
                    pluginMessage = _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(current / 1000));
                } else {
                    pluginMessage = message;
                }
                super.setCurrent(current);
            }
        };
        progress.setIcon(NewTheme.I().getIcon("wait", 16));
        progress.setProgressSource(this);
        try {
            downloadLink.setPluginProgress(progress);
            while (i > 0) {
                progress.setCurrent(i);
                synchronized (this) {
                    wait(Math.min(1000, Math.max(0, i)));
                }
                i -= 1000;
            }
        } catch (InterruptedException e) {
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } finally {
            downloadLink.setPluginProgress(null);
        }
        if (downloadLink.getDownloadLinkController().isAborting()) throw new PluginException(LinkStatus.ERROR_RETRY);
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
    /* null = this function is not implemented */
    /* if this function needs a browser, it must create an instance on its own */
    public Boolean rewriteHost(DownloadLink link) {
        return null;
    }

    /* null = this function is not implemented */
    public Boolean rewriteHost(Account acc) {
        return null;
    }

    public String getCustomFavIconURL(DownloadLink link) {
        return getHost();
    }

    public DomainInfo getDomainInfo(DownloadLink link) {
        String host = getCustomFavIconURL(link);
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
                        FileCreationManager.getInstance().delete(f, null);
                        IO.writeStringToFile(f, src);
                    }

                }
            }

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /* can we expect a captcha if we try to load link with acc */
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        /* you must distinguish between different acc types! */
        /* acc=null is no account */
        /* best save the information in accountinformation! */
        return hasCaptchas;
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
     * Some hosters have bad filenames. Rapidshare for example replaces all special chars and spaces with _. Plugins can try to autocorrect this based on other
     * downloadlinks
     * 
     * @param cache
     *            TODO
     * @param downloadable
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
                         * a pattern does exist, we must use the same one to make sure the *filetypes* match (eg . part01.rar and .r01 with same filename
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

    public void resumeDownloadlink(DownloadLink downloadLink) {
    }

    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {

    }

    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        return null;
    }

    public List<LinkVariant> getVariantsByLink(DownloadLink downloadLink) {
        return null;
    }

    public JComponent getVariantPopupComponent(DownloadLink downloadLink) {
        return null;
    }

    public boolean hasVariantToChooseFrom(DownloadLink downloadLink) {
        return false;
    }

    public void extendLinkgrabberContextMenu(JComponent parent, PluginView<CrawledLink> pv) {
    }

    public void extendDownloadsTableContextMenu(JComponent parent, PluginView<DownloadLink> pv) {
    }

}