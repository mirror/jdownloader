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
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
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
import jd.nutils.encoding.Encoding;

import org.appwork.timetracker.TimeTracker;
import org.appwork.timetracker.TrackerJob;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Hash;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
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
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin.FEATURE;

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

    public Browser getBrowser() {
        return br;
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

    /**
     * @since JD2
     */
    public void setBrowser(Browser br) {
        this.br = br;
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
    protected DownloadLink createOfflinelink(final String link, final String filename, final String message) {
        if (logger != null) {
            logger.log(new Exception("createOfflinelink:" + link + "|name:" + filename + "|message:" + message));
        }
        final DownloadLink dl = new DownloadLink(null, null, getHost(), "directhttp://" + Encoding.urlDecode(link, true), true);
        dl.setProperty("OFFLINE", true);
        dl.setAvailable(false);
        if (filename != null) {
            dl.setName(filename.trim());
        }
        if (message != null) {
            dl.setComment(message);
            logger.info("Offline Link: " + link + " :: " + message);
        } else {
            logger.info("Offline Link: " + link);
        }
        return dl;
    }

    /**
     * Die Methode entschlüsselt einen einzelnen Link.
     */
    public abstract ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception;

    private boolean processCaptchaException(Throwable e) {
        if (e instanceof DecrypterException && DecrypterException.CAPTCHA.equals(e.getMessage())) {
            invalidateLastChallengeResponse();
            return true;
        } else if (e instanceof RuntimeDecrypterException && DecrypterException.CAPTCHA.equals(e.getMessage())) {
            invalidateLastChallengeResponse();
            return true;
        } else if (e instanceof CaptchaException) {
            invalidateLastChallengeResponse();
            return true;
        } else if (e instanceof PluginException && ((PluginException) e).getLinkStatus() == LinkStatus.ERROR_CAPTCHA) {
            invalidateLastChallengeResponse();
            return true;
        }
        return false;
    }

    public ArrayList<DownloadLink> decryptIt(CrawledLink link) throws Exception {
        return decryptIt(link.getCryptedLink(), dummyProgressController);
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
        ArrayList<DownloadLink> tmpLinks = null;
        Throwable throwable = null;
        boolean linkstatusOffline = false;
        boolean pwfailed = false;
        boolean captchafailed = false;
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
            tmpLinks = decryptIt(link);
            validateLastChallengeResponse();
        } catch (final Throwable e) {
            throwable = e;
            if (isAbort()) {
                throwable = null;
            } else if (processCaptchaException(e)) {
                /* User entered wrong captcha (too many times) */
                throwable = null;
                captchafailed = true;
            } else if (DecrypterException.PLUGIN_DEFECT.equals(e.getMessage())) {
                // leave alone.
            } else if (DecrypterException.PASSWORD.equals(e.getMessage())) {
                /* User entered password captcha (too many times) */
                throwable = null;
                pwfailed = true;
            } else if (DecrypterException.ACCOUNT.equals(e.getMessage()) || e instanceof AccountRequiredException) {
                throwable = null;
                final String reason = e.getMessage();
                tmpLinks = createOfflineLink(link, tmpLinks, reason);
            } else if (e instanceof DecrypterException || e.getCause() instanceof DecrypterException) {
                throwable = null;
            } else if (e instanceof PluginException) {
                // offline file linkstatus exception, this should not be treated as crawler error..
                if (((PluginException) e).getLinkStatus() == 32) {
                    throwable = null;
                    linkstatusOffline = true;
                    tmpLinks = createOfflineLink(link, tmpLinks, null);
                }
            }
            if (throwable == null && logger instanceof LogSource) {
                if (logger instanceof LogSource) {
                    /* make sure we use the right logger */
                    ((LogSource) logger).clear();
                    ((LogSource) logger).log(e);
                } else {
                    LogSource.exception(logger, e);
                }
            }
        } finally {
            clean();
            challenges = null;
        }
        if ((tmpLinks == null || throwable != null) && !isAbort() && !pwfailed && !captchafailed && !linkstatusOffline) {
            /*
             * null as return value? something must have happened, do not clear log
             */
            errLog(throwable, br, link);
            logger.severe("CrawlerPlugin out of date: " + this + " :" + getVersion());
            logger.severe("URL was: " + link.getURL());
            tmpLinks = createOfflineLink(link, tmpLinks, null);
            /* lets forward the log */
            if (logger instanceof LogSource) {
                /* make sure we use the right logger */
                ((LogSource) logger).flush();
            }
        }
        if (logger instanceof LogSource) {
            /* make sure we use the right logger */
            ((LogSource) logger).clear();
        }
        return tmpLinks;
    }

    /**
     * we can effectively create generic offline link here. For custom message/comments this must be done within the plugin, else we can use
     * the exception message
     *
     * @author raztoki
     * @return
     */
    private ArrayList<DownloadLink> createOfflineLink(final CrawledLink link, ArrayList<DownloadLink> tmpLinks, final String message) {
        if (tmpLinks == null && LinkCrawler.getConfig().isAddDefectiveCrawlerTasksAsOfflineInLinkgrabber()) {
            tmpLinks = new ArrayList<DownloadLink>();
            tmpLinks.add(createOfflinelink(link.getURL(), message));
        }
        return tmpLinks;
    }

    public void errLog(Throwable e, Browser br, CrawledLink link) {
        LogSource errlogger = LogController.getInstance().getLogger("PluginErrors");
        try {
            errlogger.severe("CrawlerPlugin out of date: " + this + " :" + getVersion());
            errlogger.severe("URL was: " + link.getURL());
            if (e != null) {
                errlogger.log(e);
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
        LinkCrawlerDistributer dist = distributer;
        if (dist == null || links == null || links.length == 0) {
            return;
        }
        try {
            dist.distribute(links);
        } finally {
            validateLastChallengeResponse();
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

    protected String getCaptchaCode(final Browser br, final String method, final String captchaAddress, final CryptedLink param) throws Exception {
        if (captchaAddress == null) {
            logger.severe("Captcha address is not defined!");
            new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final File captchaFile = this.getLocalCaptchaFile();
        try {
            final Browser brc = br.cloneBrowser();
            // most captcha are images?
            brc.getHeaders().put("Accept", "image/webp,image/*,*/*;q=0.8");
            brc.getHeaders().put("Cache-Control", null);
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

    protected String getCaptchaCode(File captchaFile, CryptedLink param) throws Exception {
        return getCaptchaCode(getHost(), captchaFile, param);
    }

    protected ClickedPoint getCaptchaClickedPoint(File captchaFile, CryptedLink param) throws Exception {
        return getCaptchaClickedPoint(getHost(), captchaFile, param, null, null);
    }

    protected String getCaptchaCode(String method, File file, CryptedLink param) throws Exception {
        final File copy = copyCaptcha(method, file);
        final BasicCaptchaChallenge c = createChallenge(method, copy, 0, null, null);
        return handleCaptchaChallenge(c);
    }

    private File copyCaptcha(String method, File file) throws Exception {
        final File copy = Application.getResource("captchas/" + method + "/" + Hash.getMD5(file) + "." + Files.getExtension(file.getName()));
        copy.delete();
        copy.getParentFile().mkdirs();
        IO.copyFile(file, copy);
        return copy;
    }

    protected ClickedPoint getCaptchaClickedPoint(String method, File file, final CryptedLink link, String defaultValue, String explain) throws Exception {
        final File copy = copyCaptcha(method, file);
        final ClickCaptchaChallenge c = new ClickCaptchaChallenge(copy, explain, this);
        return handleCaptchaChallenge(c);
    }

    protected MultiClickedPoint getMultiCaptchaClickedPoint(String method, File file, final CryptedLink link, String defaultValue, String explain) throws Exception {
        final File copy = copyCaptcha(method, file);
        final MultiClickCaptchaChallenge c = new MultiClickCaptchaChallenge(copy, explain, this);
        return handleCaptchaChallenge(c);
    }

    protected <ReturnType> ReturnType handleCaptchaChallenge(Challenge<ReturnType> c) throws CaptchaException, InterruptedException, DecrypterException {
        if (c instanceof ImageCaptchaChallenge) {
            final File captchaFile = ((ImageCaptchaChallenge) c).getImageFile();
            cleanUpCaptchaFiles.add(captchaFile);
        }
        if (Thread.currentThread() instanceof SingleDownloadController) {
            logger.severe("PluginForDecrypt.getCaptchaCode inside SingleDownloadController!?");
        }
        c.setTimeout(getCaptchaTimeout(c));
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
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        return c.getResult().getValue();
    }

    protected BasicCaptchaChallenge createChallenge(String method, File file, int flag, String defaultValue, String explain) {
        if ("recaptcha".equalsIgnoreCase(method)) {
            return new RecaptchaV1CaptchaChallenge(file, defaultValue, explain, this, flag);
        } else if ("solvemedia".equalsIgnoreCase(method)) {
            return new SolveMediaCaptchaChallenge(file, defaultValue, explain, this, flag);
        }
        return new BasicCaptchaChallenge(method, file, defaultValue, explain, this, flag);
    }

    protected void setBrowserExclusive() {
        if (br == null) {
            return;
        }
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
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
     * DO not use in Plugins for old 09581 Stable or try/catch
     *
     * @return
     */
    protected boolean isAbort() {
        final LinkCrawlerGeneration generation = this.generation;
        if (generation != null) {
            return !generation.isValid() || Thread.currentThread().isInterrupted();
        }
        return super.isAbort();
    }

    public void setCrawler(LinkCrawler linkCrawler) {
        crawler = linkCrawler;
    }

    public LinkCrawler getCrawler() {
        if (crawler != null) {
            return crawler;
        }
        return super.getCrawler();
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