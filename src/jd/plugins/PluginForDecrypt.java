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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.storage.config.JsonConfig;
import org.appwork.timetracker.TimeTracker;
import org.appwork.timetracker.TrackerJob;
import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.DomainInfo;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllCrawlerCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.clickcaptcha.ClickedPoint;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.multiclickcaptcha.MultiClickedPoint;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.RecaptchaV1CaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.solvemedia.SolveMediaCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.ImageCaptchaChallenge;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.dialog.AskCrawlerPasswordDialogInterface;
import org.jdownloader.gui.dialog.AskForCryptedLinkDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.LazyPlugin.FEATURE;
import org.jdownloader.plugins.controller.UpdateRequiredClassNotFoundException;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.controlling.captcha.CaptchaSettings;
import jd.controlling.captcha.SkipException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawler.LinkCrawlerGeneration;
import jd.controlling.linkcrawler.LinkCrawlerDistributer;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.http.Browser.BlockedByAntiDDosException;
import jd.http.Browser.BrowserException;
import jd.nutils.encoding.Encoding;
import jd.plugins.DecrypterRetryException.RetryReason;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 *
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin {
    private volatile LinkCrawlerDistributer distributer             = null;
    private volatile LazyCrawlerPlugin      lazyC                   = null;
    private volatile LinkCrawlerGeneration  generation;
    private volatile LinkCrawler            crawler;
    private final static ProgressController dummyProgressController = new ProgressController();

    /**
     * @return the distributer
     */
    public LinkCrawlerDistributer getDistributer() {
        return distributer;
    }

    @Override
    public SubConfiguration getPluginConfig() {
        return SubConfiguration.getConfig(lazyC.getDisplayName());
    }

    public FEATURE[] getFeatures() {
        return new FEATURE[0];
    }

    @Override
    public String getCrawlerLoggerID(CrawledLink link) {
        return getHost() + "_" + getLazyC().getClassName();
    }

    protected final List<String> getPreSetPasswords() {
        final List<String> ret = new ArrayList<String>();
        CrawledLink crawledLink = getCurrentLink();
        if (crawledLink != null && crawledLink.getCryptedLink() != null) {
            final String password = crawledLink.getCryptedLink().getDecrypterPassword();
            if (StringUtils.isNotEmpty(password)) {
                ret.add(password);
            }
        }
        while (crawledLink != null) {
            final String password = new Regex(crawledLink.getURL(), "#password=(.*?)($|&)").getMatch(0);
            if (StringUtils.isNotEmpty(password) && !ret.contains(password)) {
                ret.add(password);
            }
            crawledLink = crawledLink.getSourceLink();
        }
        return ret;
    }

    /**
     * Use this when e.g. crawling folders & subfolders from cloud-services. </br>
     * Use this to find the last path in order to continue to build the path until all subfolders are crawled.
     */
    protected final String getAdoptedCloudFolderStructure() {
        return getAdoptedCloudFolderStructure(null);
    }

    /**
     * Use this when e.g. crawling folders & subfolders from cloud-services. </br>
     * Use this to find the last path in order to continue to build the path until all subfolders are crawled.
     */
    protected final String getAdoptedCloudFolderStructure(final String fallback) {
        CrawledLink current = getCurrentLink();
        while (current != null) {
            if (current.getDownloadLink() != null && getSupportedLinks().matcher(current.getURL()).matches()) {
                final String path = current.getDownloadLink().getRelativeDownloadFolderPath();
                if (path != null) {
                    return path;
                } else {
                    break;
                }
            }
            current = current.getSourceLink();
        }
        return fallback;
    }

    @Override
    public LogInterface getLogger() {
        return super.getLogger();
    }

    @Override
    public Matcher getMatcher() {
        return lazyC.getMatcher();
    }

    /**
     * @param distributer
     *            the distributer to set
     */
    public void setDistributer(LinkCrawlerDistributer distributer) {
        this.distributer = distributer;
    }

    public PluginForDecrypt() {
    }

    public Pattern getSupportedLinks() {
        return lazyC.getPattern();
    }

    public String getHost() {
        return lazyC.getDisplayName();
    }

    @Deprecated
    public PluginForDecrypt(PluginWrapper wrapper) {
        super(wrapper);
        this.lazyC = (LazyCrawlerPlugin) wrapper.getLazy();
    }

    @Override
    public long getVersion() {
        return lazyC.getVersion();
    }

    public void sleep(long i, CryptedLink link) throws InterruptedException {
        while (i > 0) {
            i -= 1000;
            synchronized (this) {
                this.wait(1000);
            }
            if (isAbort()) {
                throw new InterruptedException();
            }
        }
    }

    /**
     * return how many Instances of this PluginForDecrypt may crawl concurrently
     *
     * @return
     */
    public int getMaxConcurrentProcessingInstances() {
        return Integer.MAX_VALUE;
    }

    /**
     * Diese Methode entschlüsselt Links.
     *
     * @param cryptedLinks
     *            Ein Vector, mit jeweils einem verschlüsseltem Link. Die einzelnen verschlüsselten Links werden aufgrund des Patterns
     *            {@link jd.plugins.Plugin#getSupportedLinks() getSupportedLinks()} herausgefiltert
     * @return Ein Vector mit Klartext-links
     */
    protected DownloadLink createDownloadlink(String link) {
        return createDownloadlink(link, true);
    }

    protected DownloadLink createDownloadlink(String link, boolean urlDecode) {
        return new DownloadLink(null, null, getHost(), urlDecode ? Encoding.urlDecode(link, true) : link, true);
    }

    /**
     * creates a offline link.
     *
     * @param link
     * @return
     * @since JD2
     * @author raztoki
     */
    @Deprecated
    protected DownloadLink createOfflinelink(final String link) {
        return createOfflinelink(link, null, null);
    }

    /**
     * creates a offline link, with logger and comment message.
     *
     * @param link
     * @param message
     * @return
     * @since JD2
     * @author raztoki
     */
    @Deprecated
    protected DownloadLink createOfflinelink(final String link, final String message) {
        return createOfflinelink(link, null, message);
    }

    /**
     * creates a offline link, with filename, with logger and comment message.
     *
     * @param link
     * @param filename
     * @param message
     * @since JD2
     * @author raztoki
     */
    @Deprecated
    protected DownloadLink createOfflinelink(final String link, final String filename, final String message) {
        if (logger != null) {
            logger.log(new Exception("createOfflinelink:" + link + "|name:" + filename + "|message:" + message));
        }
        final DownloadLink dl = createLinkCrawlerRetry(getCurrentLink(), new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, filename, message));
        dl.setProperty("deprecatedOffline", link);
        return dl;
    }

    /**
     * Die Methode entschlüsselt einen einzelnen Link.
     */
    public abstract ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception;

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
    public String getUserInput(final String title, final String message, CryptedLink link) throws DecrypterException {
        if (link == null) {
            link = getCurrentLink().getCryptedLink();
        }
        final AskCrawlerPasswordDialogInterface handle = UIOManager.I().show(AskCrawlerPasswordDialogInterface.class, new AskForCryptedLinkDialog(title, message, link, getCurrentActivePlugin()));
        if (handle.getCloseReason() == CloseReason.OK) {
            final String password = handle.getText();
            if (StringUtils.isEmpty(password)) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            } else {
                return password;
            }
        } else {
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
    }

    public String getUserInput(final String message, final CryptedLink link) throws DecrypterException {
        return getUserInput(_GUI.T.AskForPasswordDialog_AskForPasswordDialog_title_(), message, link);
    }

    private boolean processCaptchaException(Throwable e) {
        if (e instanceof CaptchaException) {
            invalidateLastChallengeResponse();
            return true;
        } else if (e instanceof PluginException && ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_CAPTCHA) {
            invalidateLastChallengeResponse();
            return true;
        } else {
            return false;
        }
    }

    protected void handleAccountException(Account account, Throwable throwable) throws Exception {
        handleAccountException(null, account, throwable);
    }

    protected void handleAccountException(PluginForHost plugin, Account account, Throwable throwable) throws Exception {
        if (plugin == null || !StringUtils.equals(account.getHosterByPlugin(), plugin.getHost())) {
            plugin = getNewPluginInstance(account.getPlugin().getLazyP());
        }
        final LogInterface logger = getLogger();
        plugin.handleAccountException(account, logger, throwable);
    }

    public ArrayList<DownloadLink> decryptIt(CrawledLink link) throws Exception {
        return decryptIt(link.getCryptedLink(), dummyProgressController);
    }

    protected DownloadLink createLinkCrawlerRetry(final CrawledLink link, final DecrypterRetryException retryException) {
        final LazyHostPlugin plugin = HostPluginController.getInstance().get("LinkCrawlerRetry");
        if (plugin != null && retryException != null) {
            try {
                String name = retryException.getCustomName();
                if (StringUtils.isEmpty(name)) {
                    try {
                        name = getFileNameFromURL(new URL(link.getURL()));
                        if (StringUtils.isEmpty(name)) {
                            name = getHost();
                        }
                    } catch (final MalformedURLException e) {
                        name = getHost();
                    }
                }
                final DownloadLink ret = new DownloadLink(plugin.getPrototype(null), retryException.getReason().getExplanation(this) + "!" + name, plugin.getHost(), link.getURL(), true);
                if (StringUtils.isNotEmpty(retryException.getComment())) {
                    ret.setComment(retryException.getComment());
                }
                if (DecrypterRetryException.RetryReason.FILE_NOT_FOUND.equals(retryException.getReason())) {
                    ret.setAvailable(false);
                }
                ret.setProperty("reason", retryException.getReason());
                ret.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.TXT);
                return ret;
            } catch (UpdateRequiredClassNotFoundException e) {
                logger.log(e);
            }
        }
        return null;
    }

    protected List<LazyCrawlerPlugin> findNextLazyCrawlerPlugins(final String url, final LazyCrawlerPlugin.FEATURE... features) {
        final List<LazyCrawlerPlugin> ret = new ArrayList<LazyCrawlerPlugin>();
        final LinkCrawler crawler = getCrawler();
        final List<LazyCrawlerPlugin> sortedLazyCrawlerPlugins = crawler.getSortedLazyCrawlerPlugins();
        for (final LazyCrawlerPlugin lazyCrawlerPlugin : sortedLazyCrawlerPlugins) {
            if ((features == null || features.length == 0 || lazyCrawlerPlugin.hasFeature(features)) && crawler.canHandle(lazyCrawlerPlugin, url, getCurrentLink()) && !getLazyC().equals(lazyCrawlerPlugin) && !crawler.breakPluginForDecryptLoop(lazyCrawlerPlugin, getCurrentLink())) {
                ret.add(lazyCrawlerPlugin);
            }
        }
        return ret;
    }

    protected List<LazyHostPlugin> findNextLazyHostPlugins(final String url, final LazyPlugin.FEATURE... features) {
        final List<LazyHostPlugin> ret = new ArrayList<LazyHostPlugin>();
        final LinkCrawler crawler = getCrawler();
        final List<LazyHostPlugin> sortedLazyHostPlugins = crawler.getSortedLazyHostPlugins();
        for (final LazyHostPlugin lazyHostPlugin : sortedLazyHostPlugins) {
            if ((features == null || features.length == 0 || lazyHostPlugin.hasFeature(features)) && crawler.canHandle(lazyHostPlugin, url, getCurrentLink())) {
                ret.add(lazyHostPlugin);
            }
        }
        return ret;
    }

    protected ArrayList<DownloadLink> addLinkCrawlerRetryTask(ArrayList<DownloadLink> results, final CrawledLink link, final DecrypterRetryException retryException) {
        final String[] retryTasks = LinkCrawler.getConfig().getAddRetryCrawlerTasks();
        if (retryTasks != null && retryException != null && Arrays.asList(retryTasks).contains(retryException.getReason().name())) {
            final DownloadLink retry = createLinkCrawlerRetry(link, retryException);
            if (retry != null) {
                if (results == null) {
                    results = new ArrayList<DownloadLink>();
                }
                results.add(retry);
            }
        }
        return results;
    }

    /**
     * Die Methode entschlüsselt einen einzelnen Link. Alle steps werden durchlaufen. Der letzte step muss als parameter einen Vector
     * <String> mit den decoded Links setzen
     *
     * @param cryptedLink
     *            Ein einzelner verschlüsselter Link
     *
     * @return Ein Vector mit Klartext-links
     */
    public ArrayList<DownloadLink> decryptLink(CrawledLink link) {
        if (link.getCryptedLink() == null) {
            return null;
        }
        retry: while (true) {
            ArrayList<DownloadLink> results = null;
            try {
                challenges = null;
                setCurrentLink(link);
                /*
                 * we now lets log into plugin specific loggers with all verbose/debug on
                 */
                // prevent NPE when breakpointing
                if (br == null) {
                    br = new Browser();
                }
                br.setLogger(logger);
                br.setVerbose(true);
                br.setDebug(true);
                /* now we let the decrypter do its magic */
                results = decryptIt(link);
                validateLastChallengeResponse();
                if (results == null && !isAbort()) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    return results;
                }
            } catch (final Throwable e) {
                if (logger instanceof LogSource) {
                    if (logger instanceof LogSource) {
                        /* make sure we use the right logger */
                        ((LogSource) logger).clear();
                        ((LogSource) logger).log(e);
                    } else {
                        LogSource.exception(logger, e);
                    }
                }
                try {
                    if (isAbort()) {
                        return results;
                    } else if (e instanceof DecrypterRetryException) {
                        throw (DecrypterRetryException) e;
                    } else if (e instanceof BlockedByAntiDDosException) {
                        final BlockedByAntiDDosException ba = (BlockedByAntiDDosException) e;
                        throw new DecrypterRetryException(RetryReason.BLOCKED_BY_ANTI_DDOS, ba.getBlockedBy().getLabel(), null, e);
                    } else if (e instanceof BrowserException || e instanceof UnknownHostException) {
                        throw new DecrypterRetryException(RetryReason.HOST, null, null, e);
                    } else if (processCaptchaException(e)) {
                        if (e instanceof CaptchaException) {
                            throw new DecrypterRetryException(RetryReason.CAPTCHA, ((CaptchaException) e).getSkipRequest().name(), e.getMessage(), e);
                        } else {
                            throw new DecrypterRetryException(RetryReason.CAPTCHA, null, e.getMessage(), e);
                        }
                    } else if (DecrypterException.PASSWORD.equals(e.getMessage())) {
                        throw new DecrypterRetryException(RetryReason.PASSWORD, null, null, e);
                    } else if (e instanceof AccountRequiredException) {
                        throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, e.getMessage(), null, e);
                    } else if (e instanceof DecrypterException || e.getCause() instanceof DecrypterException) {
                        return results;
                    } else if (e instanceof PluginException) {
                        final PluginException pe = (PluginException) e;
                        switch (pe.getLinkStatus()) {
                        case LinkStatus.ERROR_PREMIUM:
                            throw new DecrypterRetryException(RetryReason.NO_ACCOUNT, null, null, e);
                        case LinkStatus.ERROR_FILE_NOT_FOUND:
                            throw new DecrypterRetryException(RetryReason.FILE_NOT_FOUND, null, null, e);
                        case LinkStatus.ERROR_PLUGIN_DEFECT:
                            throw new DecrypterRetryException(RetryReason.PLUGIN_DEFECT, null, null, e);
                        case LinkStatus.ERROR_RETRY:
                            continue retry;
                        case LinkStatus.ERROR_FATAL:
                            if (StringUtils.equals(_JDT.T.plugins_errors_wrongpassword(), e.getMessage())) {
                                throw new DecrypterRetryException(RetryReason.PASSWORD, null, null, e);
                            }
                            break;
                        default:
                            break;
                        }
                    }
                } catch (DecrypterRetryException retryException) {
                    if (RetryReason.PLUGIN_DEFECT.equals(retryException.getReason())) {
                        onPluginDefect(retryException, link);
                    }
                    results = addLinkCrawlerRetryTask(results, link, retryException);
                }
                return results;
            } finally {
                clean();
                challenges = null;
                if (logger instanceof LogSource) {
                    ((LogSource) logger).clear();
                }
            }
        }
    }

    protected void onPluginDefect(Throwable throwable, CrawledLink link) {
        if (logger instanceof LogSource) {
            ((LogSource) logger).flush();
        }
        final LogSource errlogger = LogController.getInstance().getLogger("PluginErrors");
        try {
            errlogger.severe("CrawlerPlugin out of date: " + this + " :" + getVersion());
            errlogger.severe("URL was: " + link.getURL());
            if (throwable != null) {
                errlogger.log(throwable);
            }
        } finally {
            errlogger.close();
        }
    }

    /**
     * use this to process decrypted links while the decrypter itself is still running
     *
     * NOTE: if you use this, please put it in try{}catch(Throwable) as this function is ONLY available in>09581
     *
     * @param links
     */
    public void distribute(DownloadLink... links) {
        final LinkCrawlerDistributer dist = getDistributer();
        if (dist == null || links == null || links.length == 0) {
            return;
        } else {
            try {
                dist.distribute(links);
            } finally {
                validateLastChallengeResponse();
            }
        }
    }

    public void distribute(final List<DownloadLink> links) {
        final LinkCrawlerDistributer dist = getDistributer();
        if (dist == null || links == null || links.size() == 0) {
            return;
        } else {
            distribute(links.toArray(new DownloadLink[0]));
        }
    }

    public int getDistributeDelayerMinimum() {
        return 1000;
    }

    public int getDistributeDelayerMaximum() {
        return 5000;
    }

    public String getCaptchaCode(String captchaAddress, CryptedLink param) throws Exception {
        return getCaptchaCode(getHost(), captchaAddress, param);
    }

    protected String getCaptchaCode(String method, String captchaAddress, CryptedLink param) throws Exception {
        return getCaptchaCode(br, method, captchaAddress, param);
    }

    public void onCaptchaTimeout(final CrawledLink link, Challenge<?> challenge) throws CaptchaException, PluginException {
        switch (JsonConfig.create(CaptchaSettings.class).getCrawlerCaptchaTimeoutAction()) {
        case RETRY:
            throw new PluginException(LinkStatus.ERROR_RETRY);
        case ASK:
            DomainInfo domainInfo = link.getDomainInfo();
            if (domainInfo == null) {
                domainInfo = DomainInfo.getInstance(Browser.getHost(link.getURL()));
            }
            if (UIOManager.I().showConfirmDialog(0, _GUI.T.gui_captchaWindow_askForInput(domainInfo.getTld()), _GUI.T.StatusBarImpl_skippedCrawlersMarker_desc(1), new AbstractIcon(IconKey.ICON_QUESTION, 32), _GUI.T.CaptchaDialog_layoutDialogContent_refresh(), _GUI.T.AbstractCaptchaDialog_AbstractCaptchaDialog_cancel())) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            break;
        case SKIP:
        default:
            break;
        }
    }

    protected String getCaptchaCode(final Browser br, final String method, final String captchaAddress, final CryptedLink param) throws Exception {
        if (captchaAddress == null) {
            logger.severe("Captcha address is not defined!");
            new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final File captchaFile = this.getLocalCaptchaFile();
        try {
            final Browser brc = getCaptchaBrowser(br);
            brc.getDownload(captchaFile, captchaAddress);
            // erst im Nachhinein das der Bilddownload nicht gestört wird
            final String captchaCode = getCaptchaCode(method, captchaFile, param);
            return captchaCode;
        } finally {
            if (captchaFile != null) {
                FileCreationManager.getInstance().delete(captchaFile, null);
            }
        }
    }

    protected Browser getCaptchaBrowser(Browser br) {
        final Browser ret = br.cloneBrowser();
        ret.getHeaders().put("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
        ret.getHeaders().put("Cache-Control", null);
        return ret;
    }

    protected String getCaptchaCode(File captchaFile, CryptedLink param) throws Exception {
        return getCaptchaCode(getHost(), captchaFile, param);
    }

    protected ClickedPoint getCaptchaClickedPoint(File captchaFile, CryptedLink param) throws Exception {
        return getCaptchaClickedPoint(getHost(), captchaFile, param, null);
    }

    protected ClickedPoint getCaptchaClickedPoint(final String captchaAddress, final CryptedLink param) throws Exception {
        return getCaptchaClickedPoint(captchaAddress, param, null);
    }

    protected String getCaptchaCode(String method, File file, CryptedLink param) throws Exception {
        final File copy = copyCaptcha(method, file);
        final BasicCaptchaChallenge c = createChallenge(method, copy, 0, null, null);
        return handleCaptchaChallenge(c);
    }

    private File copyCaptcha(String method, File file) throws Exception {
        if (file != null) {
            final File copy = Application.getResource("captchas/" + method + "/" + Hash.getMD5(file) + "." + Files.getExtension(file.getName()));
            copy.delete();
            copy.getParentFile().mkdirs();
            IO.copyFile(file, copy);
            return copy;
        } else {
            return null;
        }
    }

    protected ClickedPoint getCaptchaClickedPoint(final String captchaAddress, CryptedLink param, final String explain) throws Exception {
        final File captchaFile = this.getLocalCaptchaFile();
        final Browser brc = getCaptchaBrowser(br);
        brc.getDownload(captchaFile, captchaAddress);
        return getCaptchaClickedPoint(getHost(), captchaFile, param, explain);
    }

    protected ClickedPoint getCaptchaClickedPoint(final File captchaFile, CryptedLink param, final String explain) throws Exception {
        return getCaptchaClickedPoint(getHost(), captchaFile, param, explain);
    }

    protected ClickedPoint getCaptchaClickedPoint(String method, File file, final CryptedLink link, String explain) throws Exception {
        /* TODO: 2021-10-21: What's the idea behind parameter 'defaultValue'? A fallback value? */
        final File copy = copyCaptcha(method, file);
        final ClickCaptchaChallenge c = new ClickCaptchaChallenge(copy, explain, this);
        return handleCaptchaChallenge(c);
    }

    protected MultiClickedPoint getMultiCaptchaClickedPoint(String method, File file, final CryptedLink link, String defaultValue, String explain) throws Exception {
        /* TODO: 2021-10-21: What's the idea behind parameter 'defaultValue'? A fallback value? */
        final File copy = copyCaptcha(method, file);
        final MultiClickCaptchaChallenge c = new MultiClickCaptchaChallenge(copy, explain, this);
        return handleCaptchaChallenge(c);
    }

    protected <ReturnType> ReturnType handleCaptchaChallenge(Challenge<ReturnType> c) throws PluginException, CaptchaException, InterruptedException {
        if (c instanceof ImageCaptchaChallenge) {
            final File captchaFile = ((ImageCaptchaChallenge) c).getImageFile();
            cleanUpCaptchaFiles.addIfAbsent(captchaFile);
        }
        if (Thread.currentThread() instanceof SingleDownloadController) {
            logger.severe("PluginForDecrypt.getCaptchaCode inside SingleDownloadController!?");
        }
        c.setTimeout(getChallengeTimeout(c));
        invalidateLastChallengeResponse();
        final BlacklistEntry<?> blackListEntry = CaptchaBlackList.getInstance().matches(c);
        if (blackListEntry != null) {
            logger.warning("Cancel. Blacklist Matching");
            throw new CaptchaException(blackListEntry);
        }
        try {
            ChallengeResponseController.getInstance().handle(c);
        } catch (InterruptedException e) {
            LogSource.exception(logger, e);
            throw e;
        } catch (SkipException e) {
            LogSource.exception(logger, e);
            switch (e.getSkipRequest()) {
            case BLOCK_ALL_CAPTCHAS:
                CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(getCrawler()));
                break;
            case BLOCK_HOSTER:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByHost(getCrawler(), getHost()));
                break;
            case BLOCK_PACKAGE:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByPackage(getCrawler(), getCurrentLink()));
                break;
            case REFRESH:
                // refresh is not supported from the pluginsystem right now.
                return c.getRefreshTrigger();
            case TIMEOUT:
                onCaptchaTimeout(getCurrentLink(), c);
                // TIMEOUT may fallthrough to SINGLE
            case SINGLE:
                break;
            case STOP_CURRENT_ACTION:
                if (Thread.currentThread() instanceof LinkCrawlerThread) {
                    final LinkCrawler linkCrawler = ((LinkCrawlerThread) Thread.currentThread()).getCurrentLinkCrawler();
                    if (linkCrawler instanceof JobLinkCrawler) {
                        final JobLinkCrawler jobLinkCrawler = ((JobLinkCrawler) linkCrawler);
                        logger.info("Abort JobLinkCrawler:" + jobLinkCrawler.getUniqueAlltimeID().toString());
                        jobLinkCrawler.abort();
                    } else {
                        logger.info("Abort global LinkCollector");
                        LinkCollector.getInstance().abort();
                    }
                    CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(getCrawler()));
                }
                break;
            default:
                break;
            }
            throw new CaptchaException(e.getSkipRequest());
        }
        if (!c.isSolved()) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        return c.getResult().getValue();
    }

    protected BasicCaptchaChallenge createChallenge(String method, File file, int flag, String defaultValue, String explain) {
        if ("recaptcha".equalsIgnoreCase(method)) {
            return new RecaptchaV1CaptchaChallenge(file, defaultValue, explain, this, flag);
        } else if ("solvemedia".equalsIgnoreCase(method)) {
            return new SolveMediaCaptchaChallenge(file, defaultValue, explain, this, flag);
        } else {
            return new BasicCaptchaChallenge(method, file, defaultValue, explain, this, flag);
        }
    }

    protected void setBrowserExclusive() {
        if (br != null) {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
        }
    }

    /**
     * @param lazyC
     *            the lazyC to set
     */
    public void setLazyC(LazyCrawlerPlugin lazyC) {
        this.lazyC = lazyC;
    }

    /**
     * @return the lazyC
     */
    public LazyCrawlerPlugin getLazyC() {
        return lazyC;
    }

    /**
     * Can be overridden to show the current status for example in captcha dialog
     *
     * @return
     */
    public String getCrawlerStatusString() {
        return null;
    }

    public void setLinkCrawlerGeneration(LinkCrawlerGeneration generation) {
        this.generation = generation;
    }

    /**
     *
     * Can we expect a captcha if we try to load link with/without account?
     *
     *
     * Use within plugin only.
     *
     * @param link
     * @param acc
     * @return
     */
    public boolean hasCaptcha(CryptedLink link, Account acc) {
        return false;
    }

    /**
     * DO not use in Plugins for old 09581 Stable or try/catch
     *
     * @return
     */
    protected boolean isAbort() {
        final LinkCrawlerGeneration generation = this.generation;
        if (generation != null) {
            return !generation.isValid() || Thread.currentThread().isInterrupted();
        } else {
            return super.isAbort();
        }
    }

    public void setCrawler(LinkCrawler linkCrawler) {
        crawler = linkCrawler;
    }

    public LinkCrawler getCrawler() {
        if (crawler != null) {
            return crawler;
        } else {
            return super.getCrawler();
        }
    }

    public LinkCrawler getCustomNextCrawler() {
        return null;
    }

    protected List<DownloadLink> loadContainerFile(File file) {
        final LinkCrawler lc = LinkCrawler.newInstance();
        lc.crawl(file.toURI().toString());
        lc.waitForCrawling();
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>(lc.getCrawledLinks().size());
        for (final CrawledLink link : lc.getCrawledLinks()) {
            final DownloadLink dl = link.getDownloadLink();
            if (dl == null) {
                final String url = link.getURL();
                if (url != null) {
                    ret.add(new DownloadLink(null, null, null, url, true));
                }
            } else {
                ret.add(dl);
            }
        }
        return ret;
    }

    @Override
    public void runCaptchaDDosProtection(String id) throws InterruptedException {
        final TimeTracker tracker = ChallengeResponseController.getInstance().getTracker(id);
        final TrackerJob trackerJob = new TrackerJob(1) {
            @Override
            public void waitForNextSlot(long waitFor) throws InterruptedException {
                while (waitFor > 0 && !isAbort()) {
                    synchronized (this) {
                        if (waitFor <= 0) {
                            return;
                        }
                        if (waitFor > 1000) {
                            wait(1000);
                        } else {
                            this.wait(waitFor);
                        }
                        waitFor -= 1000;
                    }
                }
                if (isAbort()) {
                    throw new InterruptedException("PluginForDecrypt is aborting");
                }
            };
        };
        tracker.wait(trackerJob);
    }
}