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

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import jd.PluginWrapper;
import jd.captcha.JACMethod;
import jd.config.SubConfiguration;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.ExceptionRunnable;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.SingleDownloadController.WaitingQueueItem;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CheckableLink;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadInterfaceFactory;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.Downloadable;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.MigPanel;
import org.appwork.swing.action.BasicAction;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.IO.SYNC;
import org.appwork.utils.ProgressFeedback;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.ProgressDialog;
import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllDownloadCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByLink;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.controlling.UrlProtection;
import org.jdownloader.controlling.ffmpeg.FFMpegInstallProgress;
import org.jdownloader.controlling.ffmpeg.FFmpeg;
import org.jdownloader.controlling.ffmpeg.FFmpegProvider;
import org.jdownloader.controlling.ffmpeg.FFmpegSetup;
import org.jdownloader.controlling.ffmpeg.FFprobe;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.ExtensionsFilterInterface;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter.VideoExtensions;
import org.jdownloader.controlling.linkcrawler.GenericVariants;
import org.jdownloader.controlling.linkcrawler.LinkVariant;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.dialog.AskToUsePremiumDialog;
import org.jdownloader.gui.dialog.AskToUsePremiumDialogInterface;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo.PluginView;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.BadgeIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.CaptchaStepProgress;
import org.jdownloader.plugins.PluginTaskID;
import org.jdownloader.plugins.SkipReason;
import org.jdownloader.plugins.SkipReasonException;
import org.jdownloader.plugins.SleepPluginProgress;
import org.jdownloader.plugins.accounts.AccountFactory;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.settings.staticreferences.CFG_GENERAL;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.translate._JDT;
import org.jdownloader.updatev2.UpdateController;

/**
 * Dies ist die Oberklasse fuer alle Plugins, die von einem Anbieter Dateien herunterladen koennen
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {
    private static final String      COPY_MOVE_FILE = "CopyMoveFile";

    private static Pattern[]         PATTERNS       = new Pattern[] {

                                                    /**
                                                     * these patterns should split filename and fileextension (extension must include the
                                                     * point)
                                                     */
                                                    // multipart rar archives
            Pattern.compile("(.*)(\\.pa?r?t?\\.?[0-9]+.*?\\.rar$)", Pattern.CASE_INSENSITIVE),
            // normal files with extension
            Pattern.compile("(.*)(\\..*?$)", Pattern.CASE_INSENSITIVE) };

    private LazyHostPlugin           lazyP          = null;
    /**
     * Is true if the user has answered a captcha challenge. does not say anything whether if the answer was correct or not
     */
    protected transient SolverJob<?> lastSolverJob  = null;

    private boolean                  hasCaptchas    = false;
    private boolean                  dlSet          = false;

    public void setLastSolverJob(SolverJob<?> job) {
        this.lastSolverJob = job;
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

    @Deprecated
    public void errLog(Throwable e, Browser br, DownloadLink link) {
        errLog(e, br, null, link, null);
    }

    public void errLog(Throwable e, Browser br, LogSource log, DownloadLink link, Account account) {
        if (e != null && e instanceof PluginException && ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_PLUGIN_DEFECT) {
            final LogSource errlogger = LogController.getInstance().getLogger("PluginErrors");
            try {
                errlogger.severe("HosterPlugin out of date: " + this + " :" + getVersion());
                errlogger.severe("URL:" + link.getPluginPatternMatcher() + "|ContentUrl:" + link.getContentUrl() + "|ContainerUrl:" + link.getContainerUrl() + "|OriginUrl:" + link.getOriginUrl() + "|ReferrerUrl:" + link.getReferrerUrl());
                if (e != null) {
                    errlogger.log(e);
                }
            } finally {
                errlogger.close();
            }
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

    /**
     * @since JD2
     * */
    public void setBrowser(Browser brr) {
        br = brr;
    }

    public DownloadInterface getDownloadInterface() {
        return dl;
    }

    protected String getCaptchaCode(final String captchaAddress, final DownloadLink downloadLink) throws Exception {
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

    protected String getCaptchaCode(final String method, final String captchaAddress, final DownloadLink downloadLink) throws Exception {
        if (captchaAddress == null) {
            logger.severe("Captcha Address nicht definiert");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final File captchaFile = getLocalCaptchaFile();
        final Browser brc = br.cloneBrowser();
        brc.getDownload(captchaFile, captchaAddress);
        final String captchaCode = getCaptchaCode(method, captchaFile, downloadLink);
        return captchaCode;
    }

    protected String getCaptchaCode(final File captchaFile, final DownloadLink downloadLink) throws Exception {
        return getCaptchaCode(getHost(), captchaFile, downloadLink);
    }

    protected String getCaptchaCode(final String methodname, final File captchaFile, final DownloadLink downloadLink) throws Exception {
        return getCaptchaCode(methodname, captchaFile, 0, downloadLink, null, null);
    }

    public void invalidateLastChallengeResponse() {
        try {
            SolverJob<?> lJob = lastSolverJob;
            if (lJob != null) {
                lJob.invalidate();
            }
        } finally {
            lastSolverJob = null;
        }
    }

    private boolean equal(Object a, Object b) {
        if (a == b) {
            return true;
        }
        if (a == null && b != null) {
            return false;
        }
        return a.equals(b);
    }

    public void validateLastChallengeResponse() {
        try {
            SolverJob<?> lJob = lastSolverJob;
            if (lJob != null) {
                lJob.validate();
            }
        } finally {
            lastSolverJob = null;
        }
    }

    public boolean hasChallengeResponse() {
        return lastSolverJob != null;
    }

    protected String getCaptchaCode(final String method, File file, final int flag, final DownloadLink link, final String defaultValue, final String explain) throws Exception {
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            logger.severe("PluginForHost.getCaptchaCode inside LinkCrawlerThread!?");
        }
        final CaptchaStepProgress progress = new CaptchaStepProgress(0, 1, null);
        progress.setProgressSource(this);
        progress.setDisplayInProgressColumnEnabled(false);
        this.hasCaptchas = true;
        try {
            link.addPluginProgress(progress);
            String orgCaptchaImage = link.getStringProperty("orgCaptchaFile", null);
            if (orgCaptchaImage != null && new File(orgCaptchaImage).exists()) {
                file = new File(orgCaptchaImage);
            }

            File copy = Application.getResource("captchas/" + method + "/" + Hash.getMD5(file) + "." + Files.getExtension(file.getName()));
            copy.deleteOnExit();
            copy.getParentFile().mkdirs();
            copy.delete();
            IO.copyFile(file, copy);
            file = copy;
            if (this.getDownloadLink() == null) {
                this.setDownloadLink(link);
            }
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
                        if (lLink == null || lLink.getDefaultPlugin() == null) {
                            return false;
                        }
                        return link.getFilePackage() == lLink.getFilePackage();
                    default:
                        return false;
                    }
                }
            };
            c.setTimeout(getCaptchaTimeout());
            invalidateLastChallengeResponse();
            final BlacklistEntry blackListEntry = CaptchaBlackList.getInstance().matches(c);
            if (blackListEntry != null) {
                logger.warning("Cancel. Blacklist Matching");
                throw new CaptchaException(blackListEntry);
            }
            final SolverJob<String> job = ChallengeResponseController.getInstance().handle(c);
            if (!c.isSolved()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            return c.getResult().getValue();
        } catch (InterruptedException e) {
            LogSource.exception(logger, e);
            throw e;
        } catch (SkipException e) {
            LogSource.exception(logger, e);
            if (getDownloadLink() != null) {
                switch (e.getSkipRequest()) {
                case BLOCK_ALL_CAPTCHAS:
                    CaptchaBlackList.getInstance().add(new BlockAllDownloadCaptchasEntry());

                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case BLOCK_HOSTER:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByHost(getDownloadLink().getHost()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;

                case BLOCK_PACKAGE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByPackage(getDownloadLink().getParentNode()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case SINGLE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(getDownloadLink()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case TIMEOUT:
                    if (JsonConfig.create(CaptchaSettings.class).isSkipDownloadLinkOnCaptchaTimeoutEnabled()) {
                        CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(getDownloadLink()));
                        if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                            HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                        }
                    }
                case REFRESH:
                    // we should forward the refresh request to a new pluginstructure soon. For now. the plugin will just retry
                    return "";
                case STOP_CURRENT_ACTION:
                    if (Thread.currentThread() instanceof SingleDownloadController) {
                        DownloadWatchDog.getInstance().stopDownloads();
                    }
                    break;
                }
            }
            throw new CaptchaException(e.getSkipRequest());
        } finally {
            link.removePluginProgress(progress);
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

    @Override
    public String getHost() {
        return lazyP.getDisplayName();
    }

    @Override
    public LogSource getLogger() {
        return (LogSource) super.getLogger();
    }

    @Override
    public SubConfiguration getPluginConfig() {
        return SubConfiguration.getConfig(lazyP.getHost());
    }

    @Override
    public void clean() {
        lastSolverJob = null;
        try {
            try {
                final DownloadInterface dl = getDownloadInterface();
                if (dl != null && dl.getConnection() != null) {
                    dl.getConnection().disconnect();
                }
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
        } finally {
            super.clean();
        }
    }

    public boolean gotDownloadInterface() {
        return dlSet;
    }

    public synchronized void setDownloadInterface(DownloadInterface dl) {
        final DownloadInterface oldDl = this.dl;
        this.dl = dl;
        if (dlSet == false && dl != null) {
            dlSet = true;
        }
        if (oldDl != null && oldDl != dl) {
            try {
                oldDl.close();
            } catch (final Throwable e) {
                getLogger().log(e);
            }
        }
    }

    protected void setBrowserExclusive() {
        if (br != null) {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
        }
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
            try {
                PluginForHost plugin = getLazyP().getPrototype(null);
                for (String url : hits) {
                    /* remove newlines... */
                    url = url.trim();
                    /*
                     * this removes the " from HTMLParser.ArrayToString
                     */
                    /* only 1 " at start */
                    while (url.charAt(0) == '"') {
                        url = url.substring(1);
                    }
                    /* can have several " at the end */
                    while (url.charAt(url.length() - 1) == '"') {
                        url = url.substring(0, url.length() - 1);
                    }

                    /*
                     * use this REGEX to cut of following http links, (?=https?:|$|\r|\n|)
                     */
                    /* we use null as ClassLoader to make sure all share the same ProtoTypeClassLoader */
                    if (isValidURL(url)) {
                        final DownloadLink link = new DownloadLink(plugin, null, getHost(), url, true);
                        links.add(link);
                    }
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

    public boolean isValidURL(String URL) {
        return true;
    }

    /**
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

    /**
     * this method returns absolut numbers of max allowed downloads for given plugin/link/account combination
     * 
     * @param link
     * @param account
     * @return
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
            if (max == -1) {
                /*-1 = unlimited*/
                return Integer.MAX_VALUE;
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
            /* no downloads */
            return 0;
        }
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
        if (ai == null) {
            return null;
        }
        props = ai.getProperties();
        if (props == null || props.size() == 0) {
            return null;
        }
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

    /**
     * return if we can download given downloadLink via given account with this pluginForHost
     * 
     * @param downloadLink
     * @param account
     * @return
     */
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return true;
    }

    /**
     * return if the given downloadLink can be downloaded via given pluginForHost
     * 
     * @param downloadLink
     * @param plugin
     * @return
     */
    public boolean allowHandle(DownloadLink downloadLink, PluginForHost plugin) {
        /**
         * example: only allow original host plugin
         * 
         * return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
         */
        return true;
    }

    public boolean enoughTrafficFor(DownloadLink downloadLink, Account account) {
        AccountInfo ai = null;
        if (account != null && (ai = account.getAccountInfo()) != null) {
            if (ai.isUnlimitedTraffic()) {
                return true;
            }
            final long left = ai.getTrafficLeft();
            if (left == 0) {
                return false;
            } else {
                final long size = downloadLink.getView().getBytesTotalEstimated();
                if (size >= 0) {
                    final long need = Math.max(0, size - downloadLink.getView().getBytesLoaded());
                    return left - need > 0;
                }
            }
        }
        return true;
    }

    public void checkFFmpeg(DownloadLink downloadLink, String reason) throws SkipReasonException, InterruptedException {
        FFmpeg ffmpeg = new FFmpeg();
        synchronized (DownloadWatchDog.getInstance()) {

            if (!ffmpeg.isAvailable()) {
                if (UpdateController.getInstance().getHandler() == null) {
                    getLogger().warning("Please set FFMPEG: BinaryPath in advanced options");
                    throw new SkipReasonException(SkipReason.FFMPEG_MISSING);
                }
                final FFMpegInstallProgress progress = new FFMpegInstallProgress();
                progress.setProgressSource(this);
                try {
                    downloadLink.addPluginProgress(progress);
                    FFmpegProvider.getInstance().install(progress, reason);
                } finally {
                    downloadLink.removePluginProgress(progress);
                }
                ffmpeg.setPath(JsonConfig.create(FFmpegSetup.class).getBinaryPath());
                if (!ffmpeg.isAvailable()) {
                    //

                    List<String> requestedInstalls = UpdateController.getInstance().getHandler().getRequestedInstalls();
                    if (requestedInstalls != null && requestedInstalls.contains(org.jdownloader.controlling.ffmpeg.FFMpegInstallThread.getFFmpegExtensionName())) {
                        throw new SkipReasonException(SkipReason.UPDATE_RESTART_REQUIRED);

                    } else {
                        throw new SkipReasonException(SkipReason.FFMPEG_MISSING);
                    }

                    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
                    // _GUI._.YoutubeDash_handleFree_ffmpegmissing());
                }
            }
        }
    }

    public void checkFFProbe(DownloadLink downloadLink, String reason) throws SkipReasonException, InterruptedException {
        FFprobe ffprobe = new FFprobe();
        synchronized (DownloadWatchDog.getInstance()) {

            if (!ffprobe.isAvailable()) {
                String filename = new File(JsonConfig.create(FFmpegSetup.class).getBinaryPath()).getName().replace("ffmpeg", "ffprobe");
                File ffprobeFile = new File(new File(JsonConfig.create(FFmpegSetup.class).getBinaryPath()).getParentFile(), filename);
                if (ffprobeFile.exists()) {
                    JsonConfig.create(FFmpegSetup.class).setBinaryPathProbe(ffprobeFile.getPath());
                    ffprobe = new FFprobe();
                    if (ffprobe.isAvailable()) {
                        return;
                    }
                }
                if (UpdateController.getInstance().getHandler() == null) {
                    getLogger().warning("Please set FFProbe: BinaryPath in advanced options");
                    throw new SkipReasonException(SkipReason.FFPROBE_MISSING);
                }
                final FFMpegInstallProgress progress = new FFMpegInstallProgress();
                progress.setProgressSource(this);
                try {
                    downloadLink.addPluginProgress(progress);
                    FFmpegProvider.getInstance().install(progress, reason);
                } finally {
                    downloadLink.removePluginProgress(progress);
                }
                ffprobe.setPath(JsonConfig.create(FFmpegSetup.class).getBinaryPathProbe());
                if (!ffprobe.isAvailable()) {
                    //

                    List<String> requestedInstalls = UpdateController.getInstance().getHandler().getRequestedInstalls();
                    if (requestedInstalls != null && requestedInstalls.contains(org.jdownloader.controlling.ffmpeg.FFMpegInstallThread.getFFmpegExtensionName())) {
                        throw new SkipReasonException(SkipReason.UPDATE_RESTART_REQUIRED);

                    } else {
                        throw new SkipReasonException(SkipReason.FFPROBE_MISSING);
                    }

                    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE,
                    // _GUI._.YoutubeDash_handleFree_ffmpegmissing());
                }
            }
        }
    }

    public void handle(final DownloadLink downloadLink, final Account account) throws Exception {
        try {
            preHandle(downloadLink, account);
            waitForNextStartAllowed(downloadLink, account);
            if (account != null) {
                /* with account */
                if (StringUtils.equalsIgnoreCase(account.getHoster(), downloadLink.getHost())) {
                    handlePremium(downloadLink, account);
                } else {
                    handleMultiHost(downloadLink, account);
                }
            } else {
                /* without account */
                handleFree(downloadLink);
            }
            handlePost(downloadLink, account);
        } finally {
            try {
                if (dl != null) {
                    downloadLink.getDownloadLinkController().getConnectionHandler().removeConnectionHandler(dl.getManagedConnetionHandler());
                }
            } catch (final Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public void update(final DownloadLink downloadLink, final Account account, long bytesTransfered) throws PluginException {
        if (account != null && bytesTransfered > 0) {
            final AccountInfo ai = account.getAccountInfo();
            if (ai != null && !ai.isUnlimitedTraffic()) {
                final long left = Math.max(0, ai.getTrafficLeft() - bytesTransfered);
                ai.setTrafficLeft(left);
                if (left == 0) {
                    if (!ai.isSpecialTraffic()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                    }
                }
            }
        }
    }

    private void handlePost(DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getBooleanProperty("GENERIC_VARIANTS", false) && downloadLink.hasVariantSupport()) {
            final GenericVariants var = downloadLink.getVariant(GenericVariants.class);
            if (var != null) {
                var.runPostDownload(this, downloadLink, account);
            }
        }
    }

    public void preHandle(final DownloadLink downloadLink, Account account) throws Exception {
        if (downloadLink.getBooleanProperty("GENERIC_VARIANTS", false) && downloadLink.hasVariantSupport()) {
            final GenericVariants var = downloadLink.getVariant(GenericVariants.class);
            if (var != null) {
                var.runPreDownload(this, downloadLink, account);
            }
        }
    }

    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        /*
         * fetchAccountInfo must fill ai.setMultiHostSupport to signal all supported multiHosts
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
        logger.severe("invalid call to handleMultiHost: " + downloadLink.getName() + ":" + downloadLink.getHost() + " to " + getHost() + ":" + this.getVersion() + " with " + account);
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    /**
     * Stellt das Plugin in den Ausgangszustand zurueck (variablen intialisieren etc)
     */
    public abstract void reset();

    public abstract void resetDownloadlink(DownloadLink link);

    public List<File> listProcessFiles(DownloadLink link) {
        final HashSet<File> ret = new HashSet<File>();
        ret.add(new File(link.getFileOutputForPlugin(false, false) + ".part"));
        ret.add(new File(link.getFileOutputForPlugin(false, false)));
        ret.add(new File(link.getFileOutputForPlugin(false, true)));
        return new ArrayList<File>(ret);
    }

    public int getTimegapBetweenConnections() {
        return 50;
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = Math.max(0, interval);
    }

    protected long getStartIntervall(final DownloadLink downloadLink, final Account account) {
        return WAIT_BETWEEN_STARTS;
    }

    protected void waitForNextStartAllowed(final DownloadLink downloadLink, final Account account) throws PluginException, InterruptedException {
        final WaitingQueueItem queueItem = downloadLink.getDownloadLinkController().getQueueItem();
        final long wait = Math.max(0, getStartIntervall(downloadLink, account));
        if (wait == 0) {
            queueItem.lastStartTimestamp.set(System.currentTimeMillis());
            return;
        }
        final PluginProgress progress = new PluginProgress(0, 0, null) {
            private String pluginMessage = null;

            @Override
            public String getMessage(Object requestor) {
                return pluginMessage;
            }

            @Override
            public PluginTaskID getID() {
                return PluginTaskID.WAIT;
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
        progress.setDisplayInProgressColumnEnabled(false);
        try {
            long lastQueuePosition = -1;
            long waitQueuePosition = -1;
            long waitMax = 0;
            long waitCur = 0;
            synchronized (queueItem) {
                if (!queueItem.lastStartTimestamp.compareAndSet(0, System.currentTimeMillis())) {
                    downloadLink.addPluginProgress(progress);
                    while ((waitQueuePosition = queueItem.indexOf(downloadLink)) >= 0 && !downloadLink.getDownloadLinkController().isAborting()) {
                        if (waitQueuePosition != lastQueuePosition) {
                            waitMax = (queueItem.lastStartTimestamp.get() - System.currentTimeMillis()) + ((waitQueuePosition + 1) * wait);
                            waitCur = waitMax;
                            lastQueuePosition = waitQueuePosition;
                        }
                        if (waitCur <= 0) {
                            break;
                        }
                        progress.updateValues(waitCur, waitMax);
                        long wTimeout = Math.min(1000, Math.max(0, waitCur));
                        queueItem.wait(wTimeout);
                        waitCur -= wTimeout;
                    }
                    if (downloadLink.getDownloadLinkController().isAborting()) {
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
                queueItem.lastStartTimestamp.set(System.currentTimeMillis());
            }
        } catch (final InterruptedException e) {
            if (downloadLink.getDownloadLinkController().isAborting()) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        } finally {
            downloadLink.removePluginProgress(progress);
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
            if (downloadLink.getDownloadLinkController().isAborting()) {
                throw new InterruptedException("Controller aborted");
            }
            Thread.sleep(waitCur);
            if (queueItem.lastConnectionTimestamp.compareAndSet(lastConnectionTimestamp, System.currentTimeMillis())) {
                break;
            }
        }
        if (downloadLink.getDownloadLinkController().isAborting()) {
            throw new InterruptedException("Controller aborted");
        }
    }

    protected void sleep(final long i, final DownloadLink downloadLink) throws PluginException {
        sleep(i, downloadLink, "");
    }

    @Deprecated
    public void resetPluginGlobals() {
    }

    /**
     * JD2 only
     * 
     * @return
     */
    public boolean isAbort() {
        final DownloadLink link = getDownloadLink();
        if (link != null) {
            final SingleDownloadController con = link.getDownloadLinkController();
            if (con != null) {
                return con.isAborting() || Thread.currentThread().isInterrupted();
            }
        }
        return super.isAbort();
    }

    protected void sleep(long i, DownloadLink downloadLink, final String message) throws PluginException {
        if (downloadLink.getDownloadLinkController().isAborting()) {
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        final PluginProgress progress = new SleepPluginProgress(i, message);
        progress.setProgressSource(this);
        progress.setDisplayInProgressColumnEnabled(false);
        try {
            downloadLink.addPluginProgress(progress);
            while (i > 0 && !downloadLink.getDownloadLinkController().isAborting()) {
                progress.setCurrent(i);
                synchronized (this) {
                    wait(Math.min(1000, Math.max(0, i)));
                }
                i -= 1000;
            }
        } catch (final InterruptedException e) {
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } finally {
            downloadLink.removePluginProgress(progress);
        }
        if (downloadLink.getDownloadLinkController().isAborting()) {
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
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
        if (premiumurl != null) {
            return premiumurl;
        }
        return premiumurl;
    }

    public boolean isPremiumEnabled() {
        return enablePremium;
    }

    public void setDownloadLink(DownloadLink link) {
        this.link = link;
    }

    public long getAvailableStatusTimeout(DownloadLink link, AvailableStatus availableStatus) {
        if (availableStatus != null) {
            switch (availableStatus) {
            case TRUE:
            case FALSE:
                return 5 * 60 * 1000l;
            default:
                return 2 * 60 * 1000l;
            }
        } else {
            return 1 * 60 * 1000l;
        }
    }

    public DownloadLink getDownloadLink() {
        return link;
    }

    public String rewriteHost(String host) {
        if (host != null && host.equals(getHost())) {
            return getHost();
        }
        return null;
    }

    public boolean assignPlugin(final DownloadLink link) {
        if (link != null) {
            link.setHost(getHost());
            link.setDefaultPlugin(this);
            return true;
        }
        return false;
    }

    public boolean assignPlugin(final Account account) {
        if (account != null) {
            account.setHoster(getHost());
            account.setPlugin(this);
            return true;
        }
        return false;
    }

    public static boolean implementsRewriteHost(PluginForHost plugin) {
        try {
            if (plugin != null) {
                final Method method = plugin.getClass().getMethod("rewriteHost", new Class[] { String.class });
                final boolean implementsHandlePremium = method.getDeclaringClass() != PluginForHost.class;
                return implementsHandlePremium && plugin.rewriteHost((String) null) != null;
            }
        } catch (NoSuchMethodException e) {
        } catch (Throwable e) {
            LogController.CL().log(e);
        }
        return false;
    }

    public static boolean implementsAllowHandle(PluginForHost plugin) {
        try {
            if (plugin != null) {
                final Method method = plugin.getClass().getMethod("allowHandle", new Class[] { DownloadLink.class, PluginForHost.class });
                final boolean implementsHandlePremium = method.getDeclaringClass() != PluginForHost.class;
                return implementsHandlePremium;
            }
        } catch (NoSuchMethodException e) {
        } catch (Throwable e) {
            LogController.CL().log(e);
        }
        return false;
    }

    public static boolean implementsCheckLinks(PluginForHost plugin) {
        try {
            if (plugin != null) {
                final Method method = plugin.getClass().getMethod("checkLinks", new DownloadLink[0].getClass());
                final boolean hasMassCheck = method.getDeclaringClass() != PluginForHost.class;
                return hasMassCheck;
            }
        } catch (NoSuchMethodException e) {
        } catch (Throwable e) {
            LogController.CL().log(e);
        }
        return false;
    }

    public static boolean implementsHandlePremium(PluginForHost plugin) {
        try {
            if (plugin != null && plugin.isPremiumEnabled()) {
                final Method method = plugin.getClass().getMethod("handlePremium", new Class[] { DownloadLink.class, Account.class });
                final boolean implementsHandlePremium = method.getDeclaringClass() != PluginForHost.class;
                return implementsHandlePremium;
            }
        } catch (NoSuchMethodException e) {
        } catch (Throwable e) {
            LogController.CL().log(e);
        }
        return false;
    }

    public static boolean implementsSortDownloadLink(PluginForHost plugin) {
        try {
            if (plugin != null) {
                final Method method = plugin.getClass().getMethod("sortDownloadLinks", new Class[] { Account.class, List.class });
                final boolean implementsSortDownloadLink = method.getDeclaringClass() != PluginForHost.class;
                return implementsSortDownloadLink;
            }
        } catch (NoSuchMethodException e) {
        } catch (Throwable e) {
            LogController.CL().log(e);
        }
        return false;
    }

    public String getCustomFavIconURL(DownloadLink link) {
        return getHost();
    }

    public DomainInfo getDomainInfo(DownloadLink link) {
        String host = getCustomFavIconURL(link);
        if (host == null) {
            host = getHost();
        }
        return DomainInfo.getInstance(host);
    }

    public static void main(String[] args) throws Exception {
        try {
            File home = new File(Application.getRessourceURL(PluginForHost.class.getName().replace(".", "/") + ".class").toURI()).getParentFile().getParentFile().getParentFile().getParentFile();
            File hostPluginsDir = new File(home, "src/jd/plugins/hoster/");
            for (File f : hostPluginsDir.listFiles()) {
                if (f.getName().endsWith(".java")) {
                    // StringBuilder method = new StringBuilder();
                    // String src = IO.readFileToString(f);
                    // if (src.toLowerCase().contains("captcha")) {
                    // if (new Regex(src, "(boolean\\s+hasCaptcha\\(\\s*DownloadLink .*?\\,\\s*Account .*?\\))").matches()) {
                    // continue;
                    // }
                    // if (src.contains("enablePremium")) {
                    // method.append("\r\n/* NO OVERRIDE!! We need to stay 0.9*compatible */");
                    // method.append("\r\npublic boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {");
                    // method.append("\r\nif (acc == null) {");
                    // method.append("\r\n/* no account, yes we can expect captcha */");
                    // method.append("\r\nreturn true;");
                    // method.append("\r\n}");
                    //
                    // method.append("\r\n if (Boolean.TRUE.equals(acc.getBooleanProperty(\"free\"))) {");
                    // method.append("\r\n/* free accounts also have captchas */");
                    // method.append("\r\nreturn true;");
                    // method.append("\r\n}");
                    // method.append("\r\nreturn false;");
                    // method.append("\r\n}");
                    //
                    // } else {
                    // method.append("\r\n/* NO OVERRIDE!! We need to stay 0.9*compatible */");
                    // method.append("\r\npublic boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {");
                    // method.append("\r\nreturn true;");
                    // method.append("\r\n}");
                    // }
                    //
                    // } else {
                    //
                    // }
                    //
                    // if (method.length() > 0) {
                    //
                    // src = src.substring(0, src.lastIndexOf("}")) + method.toString() + "\r\n}";
                    // FileCreationManager.getInstance().delete(f, null);
                    // IO.writeStringToFile(f, src);
                    // }

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
     * Some hosters have bad filenames. Rapidshare for example replaces all special chars and spaces with _. Plugins can try to autocorrect
     * this based on other downloadlinks
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
                    if (newName != null) {
                        return newName;
                    }
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
                if (fileNameSplit[0].equals(prototypesplit)) {
                    continue;
                }
                if (isHosterManipulatesFilenames() && fileNameSplit[0].length() == prototypesplit.length() && filteredName.equalsIgnoreCase(filterPackageID(prototypesplit))) {
                    newName = getFixedFileName(cache, originalFilename, originalReplaces, prototypesplit, next.getDefaultPlugin().getFilenameReplaceMap());
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
                    return next.getView().getDisplayName();
                }
            }

        } catch (Throwable e) {
            LogController.CL().log(e);
        }

        return null;
    }

    protected String getFixedFileName(HashMap<Object, Object> cache, String originalFilename, char[] originalReplaces, String prototypeName, char[] prototypeReplaces) {
        if (originalReplaces.length == 0 && prototypeReplaces.length == 0) {
            /* no replacements available */
            return null;
        }
        final Boolean original = (Boolean) cache.get(originalFilename + new String(originalReplaces));
        final Boolean prototype = (Boolean) cache.get(prototypeName + new String(prototypeReplaces));
        if (Boolean.FALSE.equals(original) && Boolean.FALSE.equals(prototype)) {
            return null;
        }
        final ArrayList<Character> foundOriginalReplaces = new ArrayList<Character>(originalReplaces.length);
        final ArrayList<Character> foundPrototypeReplaces = new ArrayList<Character>(prototypeReplaces.length);
        if (original == null) {
            for (int index = 0; index < originalReplaces.length; index++) {
                if (originalFilename.indexOf(originalReplaces[index]) >= 0) {
                    foundOriginalReplaces.add(originalReplaces[index]);
                }
            }
        }
        if (prototype == null) {
            for (int index = 0; index < prototypeReplaces.length; index++) {
                if (prototypeName.indexOf(prototypeReplaces[index]) >= 0) {
                    foundPrototypeReplaces.add(prototypeReplaces[index]);
                }
            }
        }
        if (original == null && foundOriginalReplaces.size() == 0) {
            cache.put(originalFilename + new String(originalReplaces), Boolean.FALSE);
        }
        if (prototype == null && foundPrototypeReplaces.size() == 0) {
            cache.put(prototypeName + new String(prototypeReplaces), Boolean.FALSE);
        }
        if (foundOriginalReplaces.size() == 0 && foundOriginalReplaces.size() == 0) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        mainLoop: for (int i = 0; i < prototypeName.length(); i++) {
            char oC = originalFilename.charAt(i);
            char pC = prototypeName.charAt(i);
            if (Character.toLowerCase(oC) != Character.toLowerCase(pC)) {
                for (Character oCC : foundOriginalReplaces) {
                    /*
                     * first we check if char from Original is on replacement List, if so, we use char from prototype
                     */
                    if (oC == oCC.charValue()) {
                        sb.append(pC);
                        continue mainLoop;
                    }
                }
                for (Character pCC : foundPrototypeReplaces) {
                    /*
                     * then we check if char from prototype is on replacement List, if so, we use char from original
                     */
                    if (pC == pCC.charValue()) {
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
            if (ret != null) {
                return (String) ret;
            }
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
        if (cache != null) {
            cache.put(originalFilename + "_" + prototypeName, sb.toString());
        }
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
        if (getHost().equalsIgnoreCase("letitbit.net") || getHost().equalsIgnoreCase("shareflare.net") || getHost().equalsIgnoreCase("vip-file.com") || getHost().equalsIgnoreCase("multivip.net")) {
            return new LetitBitAccountFactory();
        }
        return new DefaultAccountFactory();
    }

    public void resumeDownloadlink(DownloadLink downloadLink) {
    }

    public boolean checkLinks(final DownloadLink[] urls) {
        return false;
    }

    public AvailableStatus checkLink(DownloadLink downloadLink) throws Exception {
        return requestFileInformation(downloadLink);
    }

    public void setActiveVariantByLink(DownloadLink downloadLink, LinkVariant variant) {
        downloadLink.setVariant(variant);
        if (variant instanceof GenericVariants) {
            GenericVariants v = (GenericVariants) variant;

            switch (v) {
            case ORIGINAL:

                downloadLink.setCustomExtension(null);
                break;
            default:

                downloadLink.setCustomExtension(v.getExtension());
                break;

            }
        }
        // modifiyNameByVariant(downloadLink);

    }

    public LinkVariant getActiveVariantByLink(DownloadLink downloadLink) {
        return downloadLink.getVariant(GenericVariants.class);

    }

    public List<? extends LinkVariant> getVariantsByLink(DownloadLink downloadLink) {

        return downloadLink.getVariants(GenericVariants.class);
    }

    public JComponent getVariantPopupComponent(DownloadLink downloadLink) {
        return null;
    }

    public boolean hasVariantToChooseFrom(DownloadLink downloadLink) {
        List<? extends LinkVariant> variants = getVariantsByLink(downloadLink);
        return variants != null && variants.size() > 0;
    }

    public void extendLinkgrabberContextMenu(JComponent parent, final PluginView<CrawledLink> pv, List<PluginView<CrawledLink>> allPvs) {
        if (allPvs.size() == 1) {
            final JMenu setVariants = new JMenu(_GUI._.PluginForHost_extendLinkgrabberContextMenu_generic_convert());
            setVariants.setIcon(DomainInfo.getInstance(getHost()).getFavIcon());
            setVariants.setEnabled(false);

            final JMenu addVariants = new JMenu("Add converted variant...");

            addVariants.setIcon(new BadgeIcon(DomainInfo.getInstance(getHost()).getFavIcon(), new AbstractIcon(IconKey.ICON_ADD, 16), 4, 4));
            addVariants.setEnabled(false);

            // setVariants.setVisible(false);
            // addVariants.setVisible(false);
            new Thread("Collect Variants") {
                public void run() {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    HashSet<GenericVariants> map = new HashSet<GenericVariants>();
                    final ArrayList<GenericVariants> list = new ArrayList<GenericVariants>();
                    for (CrawledLink cl : pv.getChildren()) {

                        if (cl.getDownloadLink() == null || !cl.getDownloadLink().getBooleanProperty("GENERIC_VARIANTS", false) || !cl.getDownloadLink().hasVariantSupport()) {
                            continue;
                        }

                        List<GenericVariants> v = cl.getDownloadLink().getVariants(GenericVariants.class);
                        if (v != null) {
                            for (LinkVariant lv : v) {
                                if (lv instanceof GenericVariants) {
                                    if (map.add((GenericVariants) lv)) {
                                        list.add((GenericVariants) lv);
                                    }
                                }
                            }
                        }
                    }
                    if (list.size() == 0) {
                        return;
                    }
                    Collections.sort(list, new Comparator<GenericVariants>() {

                        @Override
                        public int compare(GenericVariants o1, GenericVariants o2) {
                            return o1.name().compareTo(o2.name());
                        }
                    });

                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            setVariants.setEnabled(true);
                            addVariants.setEnabled(true);
                            setVariants.setVisible(true);
                            addVariants.setVisible(true);

                            for (final GenericVariants gv : list) {

                                setVariants.add(new JMenuItem(new BasicAction() {
                                    {
                                        setName(CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled() ? gv._getExtendedName() : gv._getName());
                                    }

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                                        for (CrawledLink cl : pv.getChildren()) {
                                            // List<GenericVariants> variants = new ArrayList<GenericVariants>();
                                            for (LinkVariant v : getVariantsByLink(cl.getDownloadLink())) {
                                                if (v.equals(gv)) {
                                                    LinkCollector.getInstance().setActiveVariantForLink(cl, gv);
                                                    checkableLinks.add(cl);
                                                    break;
                                                }
                                            }

                                        }

                                        LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                        linkChecker.check(checkableLinks);
                                    }

                                }));

                                addVariants.add(new JMenuItem(new BasicAction() {
                                    {
                                        setName(CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled() ? gv._getExtendedName() : gv._getName());
                                    }

                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        java.util.List<CheckableLink> checkableLinks = new ArrayList<CheckableLink>(1);

                                        for (CrawledLink cl : pv.getChildren()) {
                                            List<GenericVariants> variants = new ArrayList<GenericVariants>();
                                            for (LinkVariant v : getVariantsByLink(cl.getDownloadLink())) {
                                                if (v.equals(gv)) {
                                                    CrawledLink newLink = LinkCollector.getInstance().addAdditional(cl, gv);

                                                    if (newLink != null) {
                                                        checkableLinks.add(newLink);
                                                    } else {
                                                        Toolkit.getDefaultToolkit().beep();
                                                    }
                                                    break;
                                                }
                                            }

                                        }

                                        LinkChecker<CheckableLink> linkChecker = new LinkChecker<CheckableLink>(true);
                                        linkChecker.check(checkableLinks);
                                    }

                                }));
                            }
                        }

                    };

                };
            }.start();

            parent.add(setVariants);
            parent.add(addVariants);
        }
    }

    public void extendDownloadsTableContextMenu(JComponent parent, PluginView<DownloadLink> pv) {
    }

    public Downloadable newDownloadable(DownloadLink downloadLink, final Browser br) {
        if (br != null) {
            return new DownloadLinkDownloadable(downloadLink) {
                @Override
                public Browser getContextBrowser() {
                    return br.cloneBrowser();
                }

            };
        }
        return new DownloadLinkDownloadable(downloadLink);
    }

    /**
     * JD2 ONLY
     * 
     * sort accounts for best order to download downloadLink
     * 
     * @param accounts
     * @param downloadLink
     * @return
     */
    public List<Account> sortAccounts(DownloadLink downloadLink, List<Account> accounts) {
        return accounts;
    }

    /**
     * JD2 ONLY
     * 
     * sort downloadLinks for best order to download via account
     * 
     * @param accounts
     * @param downloadLink
     * @return
     */
    public List<DownloadLink> sortDownloadLinks(Account account, List<DownloadLink> downloadLinks) {
        return downloadLinks;
    }

    /**
     * THIS IS JDOWNLOADER 2 ONLY!
     * 
     * @param domain
     * @throws DialogCanceledException
     * @throws DialogClosedException
     */
    protected void showFreeDialog(final String domain) {
        AskToUsePremiumDialog d = new AskToUsePremiumDialog(domain, this) {
            @Override
            public String getDontShowAgainKey() {
                return "adsPremium_" + domain;
            }
        };
        try {
            UIOManager.I().show(AskToUsePremiumDialogInterface.class, d).throwCloseExceptions();
            CrossSystem.openURL(new URL(d.getPremiumUrl()));
        } catch (DialogNoAnswerException e) {
            LogSource.exception(logger, e);
        } catch (IOException e) {
            LogSource.exception(logger, e);
        }

    }

    public static class FilePair {
        public FilePair(File oldFile, File newFile) {
            this.oldFile = oldFile;
            this.newFile = newFile;
        }

        private final File oldFile;

        public File getOldFile() {
            return oldFile;
        }

        public File getNewFile() {
            return newFile;
        }

        private final File newFile;
    }

    /**
     * Do not call directly. This method is called from the DownloadWatchdog.rename method only. The DownloadWatchdog assures, that the
     * method is not called during a processing download, but afterwards. Avoid to override this method. if possible, try to override
     * #listFilePairsToMove instead
     * 
     * @param link
     * @param string2
     * @param string
     * @param value
     */
    public void move(DownloadLink link, String currentDirectory, String currentName, String newDirectory, String newName) throws Exception {
        if (link.getView().getBytesLoaded() <= 0) {
            // nothing to rename or move. there should not be any file, and if there is, it does not belong to the link
            return;
        }
        final ArrayList<ExceptionRunnable> revertList = new ArrayList<ExceptionRunnable>();
        if (StringUtils.isEmpty(newName)) {
            newName = currentName;
        }
        if (StringUtils.isEmpty(newDirectory)) {
            newDirectory = currentDirectory;
        }
        if (CrossSystem.isWindows()) {
            if (StringUtils.equalsIgnoreCase(currentDirectory, newDirectory) && StringUtils.equalsIgnoreCase(currentName, newName)) {
                return;
            }
        } else {
            if (StringUtils.equals(currentDirectory, newDirectory) && StringUtils.equals(currentName, newName)) {
                return;
            }
        }
        final MovePluginProgress progress = new MovePluginProgress();
        try {
            link.addPluginProgress(progress);
            progress.setProgressSource(this);
            for (FilePair filesToHandle : listFilePairsToMove(link, currentDirectory, currentName, newDirectory, newName)) {
                handle(revertList, link, progress, filesToHandle.getOldFile(), filesToHandle.getNewFile());
            }
            revertList.clear();
        } catch (Exception e) {
            getLogger().log(e);
            throw e;
        } finally {
            try {
                // revert
                for (final ExceptionRunnable r : revertList) {
                    try {
                        if (r != null) {
                            r.run();
                        }
                    } catch (Throwable e1) {
                        getLogger().log(e1);
                    }
                }
            } finally {
                link.removePluginProgress(progress);
            }
        }
    }

    protected FilePair[] listFilePairsToMove(DownloadLink link, String currentDirectory, String currentName, String newDirectory, String newName) {
        FilePair[] ret = new FilePair[2];
        ret[0] = new FilePair(new File(new File(currentDirectory), currentName + ".part"), new File(new File(newDirectory), newName + ".part"));
        ret[1] = new FilePair(new File(new File(currentDirectory), currentName), new File(new File(newDirectory), newName));
        return ret;
    }

    private void handle(ArrayList<ExceptionRunnable> revertList, final DownloadLink downloadLink, final MovePluginProgress progress, final File currentFile, final File newFile) throws FileExistsException, CouldNotRenameException, IOException {
        if (!currentFile.exists() || currentFile.equals(newFile)) {
            return;
        }
        progress.setFile(newFile);
        revertList.add(new ExceptionRunnable() {

            @Override
            public void run() throws Exception {
                renameOrMove(progress, downloadLink, newFile, currentFile);
            }
        });
        renameOrMove(progress, downloadLink, currentFile, newFile);
    }

    private void renameOrMove(MovePluginProgress progress, final DownloadLink downloadLink, File old, File newFile) throws FileExistsException, CouldNotRenameException, IOException {

        // TODO: what if newFile exists?
        if (newFile.exists()) {
            throw new FileExistsException(old, newFile);
        }
        if (!newFile.getParentFile().exists() && !newFile.getParentFile().mkdirs()) {
            throw new IOException("Could not create " + newFile.getParent());
        }
        try {
            getLogger().info("Move " + old + " to " + newFile);
            if (CrossSystem.isWindows() && Application.getJavaVersion() >= Application.JAVA17) {
                java.nio.file.Files.move(java.nio.file.Paths.get(old.toURI()), java.nio.file.Paths.get(newFile.toURI()), java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } else if (!old.renameTo(newFile)) {
                throw new CouldNotRenameException(old, newFile);
            }
        } catch (CouldNotRenameException e) {
            getLogger().log(e);
            copyMove(progress, downloadLink, old, newFile);
        } catch (IOException e) {
            getLogger().log(e);
            copyMove(progress, downloadLink, old, newFile);
        }
        // TODO copy optimiz
        if (!newFile.getParentFile().equals(old.getParentFile())) {
            // check if we have to delete the old path;
            if (!old.getParentFile().equals(new File(CFG_GENERAL.DEFAULT_DOWNLOAD_FOLDER.getValue()))) {
                // we ignore the dynamic tags here. if the default downloaddirectory contains dynamic tags, we can delete the folders
                // anyaway if empty.
                old.getParentFile().delete();
            }
        }
    }

    private void copyMove(final MovePluginProgress progress, final DownloadLink downloadLink, final File old, final File newFile) throws IOException {
        if (!old.exists() && newFile.exists()) {
            return;
        }
        if (old.exists()) {
            // we did an file exists check earlier. so if the file exists here, the only reason is a failed rename/move;
            newFile.delete();
        } else {
            throw new IOException("Cannot move " + old + " to " + newFile + ". The File does not exist!");
        }
        Thread thread = null;
        if (JSonStorage.getPlainStorage("Dialogs").get(COPY_MOVE_FILE, -1) < 0) {
            // System.out.println("Thread start");
            thread = new Thread() {
                public void run() {
                    try {
                        Thread.sleep(3000);
                        // System.out.println("Dialog go");
                        ProgressDialog dialog = new ProgressDialog(new ProgressGetter() {

                            @Override
                            public void run() throws Exception {
                                while (true) {
                                    Thread.sleep(1000);
                                }
                            }

                            @Override
                            public String getString() {
                                return _JDT._.lit_please_wait();
                            }

                            @Override
                            public int getProgress() {
                                double perc = progress.getPercent();

                                return Math.min(99, (int) (perc));
                            }

                            @Override
                            public String getLabelString() {
                                return null;
                            }
                        }, Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.PluginForHost_copyMove_progressdialog_title(), null, new AbstractIcon(IconKey.ICON_SAVETO, 32), null, _JDT._.lit_hide()) {
                            @Override
                            public String getDontShowAgainKey() {
                                return COPY_MOVE_FILE;
                            }

                            private Component leftLabel(String name) {
                                JLabel ret = new JLabel(name);
                                ret.setHorizontalAlignment(SwingConstants.LEFT);
                                return ret;
                            }

                            protected void extendLayout(JPanel p) {

                                if (p.getComponentCount() == 0) {
                                    final JPanel subp = new MigPanel("ins 0,wrap 1", "[]", "[][]");
                                    p.add(subp, "wrap");
                                    p = subp;

                                    String packagename = downloadLink.getParentNode().getName();

                                    p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_hoster())), "split 2,sizegroup left,alignx left");
                                    DomainInfo di = downloadLink.getDomainInfo();
                                    JLabel ret = new JLabel(di.getTld());
                                    ret.setHorizontalAlignment(SwingConstants.LEFT);
                                    ret.setIcon(di.getFavIcon());
                                    p.add(ret);

                                    if (downloadLink.getParentNode() != FilePackage.getDefaultFilePackage()) {
                                        p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_package())), "split 2,sizegroup left,alignx left");
                                        p.add(leftLabel(packagename));
                                    }
                                    p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_filesize())), "split 2,sizegroup left,alignx left");
                                    p.add(leftLabel(SizeFormatter.formatBytes(old.length())));

                                    if (newFile.getName().equals(old.getName())) {
                                        p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_filename())), "split 2,sizegroup left,alignx left");
                                        p.add(leftLabel(newFile.getName()));
                                    } else {
                                        p.add(SwingUtils.toBold(new JLabel(_GUI._.PLUGINFORHOST_MOVECOPY_DIALOG_OLDFILENAME())), "split 2,sizegroup left,alignx left");
                                        p.add(leftLabel(old.getName()));
                                        p.add(SwingUtils.toBold(new JLabel(_GUI._.PLUGINFORHOST_MOVECOPY_DIALOG_NEWFILENAME())), "split 2,sizegroup left,alignx left");
                                        p.add(leftLabel(newFile.getName()));
                                    }

                                    p.add(SwingUtils.toBold(new JLabel(_GUI._.PLUGINFORHOST_MOVECOPY_DIALOG_OLD())), "split 2,sizegroup left,alignx left");
                                    p.add(leftLabel(old.getParent()));

                                    p.add(SwingUtils.toBold(new JLabel(_GUI._.PLUGINFORHOST_MOVECOPY_DIALOG_NEW())), "split 2,sizegroup left,alignx left");
                                    p.add(leftLabel(newFile.getParent()));
                                }
                            }

                        };
                        UIOManager.I().show(null, dialog);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            };
            thread.start();
        } else {
            // System.out.println("Do not show again " + JSonStorage.getPlainStorage("Dialogs").get(COPY_MOVE_FILE, -1));
        }
        try {
            IO.copyFile(new ProgressFeedback() {

                @Override
                public void setBytesTotal(long length) {
                    progress.setTotal(length);
                }

                @Override
                public void setBytesProcessed(long position) {
                    progress.setCurrent(position);
                }
            }, old, newFile, SYNC.META_AND_DATA);
            old.delete();
        } catch (IOException io) {
            newFile.delete();
            throw io;
        } finally {
            if (thread != null) {
                thread.interrupt();
            }
        }

    }

    public boolean isProxyRotationEnabledForLinkChecker() {
        return true;
    }

    /**
     * plugins may set a mirrorid to help the mirror detector. You have to ensure, that two mirrors either get the same mirror id, or no
     * mirrorid(null)
     * 
     * @return
     */

    public String getMirrorID(DownloadLink link) {
        return null;
    }

    public void resetLink(DownloadLink downloadLink) {
        resetDownloadlink(downloadLink);

    }

    public List<GenericVariants> getGenericVariants(DownloadLink downloadLink) {
        List<String> converts = getConvertToList(downloadLink);
        if (converts != null && converts.size() > 0) {
            List<GenericVariants> variants = new ArrayList<GenericVariants>();
            variants.add(GenericVariants.ORIGINAL);
            for (String v : converts) {
                try {
                    variants.add(GenericVariants.valueOf(v));
                } catch (Throwable e) {
                    e.printStackTrace();
                }

            }
            if (variants.size() > 1) {
                return variants;
            }
        } else {
            String name = downloadLink.getName();
            String exten = Files.getExtension(name);
            if (exten != null) {
                boolean isVideo = false;
                for (final ExtensionsFilterInterface extension : VideoExtensions.values()) {
                    if (extension.getPattern().matcher(exten).matches()) {
                        isVideo = true;
                        break;
                    }
                }
                if (isVideo) {
                    List<GenericVariants> variants = new ArrayList<GenericVariants>();

                    variants.add(GenericVariants.DEMUX_GENERIC_AUDIO);

                    return variants;
                }

            }
        }
        return null;
    }

    public List<String> getConvertToList(DownloadLink downloadLink) {

        return null;
    }

    public UrlProtection getUrlProtection(List<DownloadLink> hosterLinks) {
        return null;
    }

}