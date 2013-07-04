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
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.captcha.easy.load.LoadImage;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerAbort;
import jd.controlling.linkcrawler.LinkCrawlerDistributer;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;

import org.appwork.utils.Exceptions;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.blacklist.CrawlerBlackListEntry;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeResponseValidation;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solverjob.ResponseList;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.crawler.LazyCrawlerPlugin;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin {

    private LinkCrawlerDistributer    distributer           = null;

    private LazyCrawlerPlugin         lazyC                 = null;
    private CrawledLink               currentLink           = null;
    private LinkCrawlerAbort          linkCrawlerAbort;

    private LinkCrawler               crawler;
    private transient ResponseList<?> lastChallengeResponse = null;

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

    public Browser getBrowser() {
        return br;
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
        return new DownloadLink(null, null, getHost(), Encoding.urlDecode(link, true), true);
    }

    /**
     * Die Methode entschlüsselt einen einzelnen Link.
     */
    public abstract ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception;

    /**
     * Die Methode entschlüsselt einen einzelnen Link. Alle steps werden durchlaufen. Der letzte step muss als parameter einen
     * Vector<String> mit den decoded Links setzen
     * 
     * @param cryptedLink
     *            Ein einzelner verschlüsselter Link
     * 
     * @return Ein Vector mit Klartext-links
     */
    public ArrayList<DownloadLink> decryptLink(CrawledLink source) {
        CryptedLink cryptLink = source.getCryptedLink();
        if (cryptLink == null) return null;
        ProgressController progress = new ProgressController();
        cryptLink.setProgressController(progress);
        ArrayList<DownloadLink> tmpLinks = null;
        boolean showException = true;
        Throwable exception = null;
        try {
            lastChallengeResponse = null;
            this.currentLink = source;
            /*
             * we now lets log into plugin specific loggers with all verbose/debug on
             */
            br.setLogger(logger);
            br.setVerbose(true);
            br.setDebug(true);
            /* now we let the decrypter do its magic */
            tmpLinks = decryptIt(cryptLink, progress);
            validateLastChallengeResponse();
        } catch (RuntimeDecrypterException e) {
            if (DecrypterException.CAPTCHA.equals(e.getMessage())) {
                invalidateLastChallengeResponse();
                showException = false;
            } else if (DecrypterException.PASSWORD.equals(e.getMessage())) {
                showException = false;
            } else if (DecrypterException.ACCOUNT.equals(e.getMessage())) {
                showException = false;
            }
            /*
             * we got a decrypter exception, clear log and note that something went wrong
             */
            if (logger instanceof LogSource) {
                /* make sure we use the right logger */
                ((LogSource) logger).clear();
            }
            LogSource.exception(logger, e);
        } catch (DecrypterException e) {
            if (DecrypterException.CAPTCHA.equals(e.getMessage())) {
                invalidateLastChallengeResponse();
                showException = false;
            } else if (DecrypterException.PASSWORD.equals(e.getMessage())) {
                showException = false;
            } else if (DecrypterException.ACCOUNT.equals(e.getMessage())) {
                showException = false;
            }
            /*
             * we got a decrypter exception, clear log and note that something went wrong
             */
            if (logger instanceof LogSource) {
                /* make sure we use the right logger */
                ((LogSource) logger).clear();
            }
            LogSource.exception(logger, e);
        } catch (InterruptedException e) {
            /* plugin got interrupted, clear log and note what happened */
            if (logger instanceof LogSource) {
                /* make sure we use the right logger */
                ((LogSource) logger).clear();
                ((LogSource) logger).log(e);
            } else {
                LogSource.exception(logger, e);
            }
        } catch (Throwable e) {
            /*
             * damn, something must have gone really really bad, lets keep the log
             */
            exception = e;
            LogSource.exception(logger, e);
        } finally {
            lastChallengeResponse = null;
            this.currentLink = null;
        }
        if (tmpLinks == null && showException) {
            /*
             * null as return value? something must have happened, do not clear log
             */
            errLog(exception, br, source);
            logger.severe("CrawlerPlugin out of date: " + this + " :" + getVersion());
            logger.severe("URL was: " + source.getURL());

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

    public void errLog(Throwable e, Browser br, CrawledLink link) {
        LogSource errlogger = LogController.getInstance().getLogger("PluginErrors");
        try {
            errlogger.severe("CrawlerPlugin out of date: " + this + " :" + getVersion());
            errlogger.severe("URL was: " + link.getURL());
            if (e != null) errlogger.log(e);
        } finally {
            errlogger.close();
        }
    }

    public CrawledLink getCurrentLink() {
        return currentLink;
    }

    /**
     * use this to process decrypted links while the decrypter itself is still running
     * 
     * NOTE: if you use this, please put it in try{}catch(Throwable) as this function is ONLY available in>09581
     * 
     * @param links
     */
    protected void distribute(DownloadLink... links) {
        LinkCrawlerDistributer dist = distributer;
        if (dist == null || links == null || links.length == 0) return;
        dist.distribute(links);
    }

    public int getDistributeDelayerMinimum() {
        return 1000;
    }

    public int getDistributeDelayerMaximum() {
        return 5000;
    }

    protected String getCaptchaCode(String captchaAddress, CryptedLink param) throws IOException, DecrypterException {
        return getCaptchaCode(getHost(), captchaAddress, param);
    }

    protected String getCaptchaCode(LoadImage li, CryptedLink param) throws IOException, DecrypterException {
        return getCaptchaCode(getHost(), li.file, param);
    }

    protected String getCaptchaCode(String method, String captchaAddress, CryptedLink param) throws IOException, DecrypterException {
        if (captchaAddress == null) {
            logger.severe("Captcha Adresse nicht definiert");
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        File captchaFile = this.getLocalCaptchaFile();
        try {
            Browser brc = br.cloneBrowser();
            try {
                brc.getDownload(captchaFile, captchaAddress);

            } catch (Exception e) {

                logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            // erst im Nachhinein das der Bilddownload nicht gestört wird
            String captchaCode = getCaptchaCode(method, captchaFile, param);
            return captchaCode;
        } finally {
            if (captchaFile != null) captchaFile.delete();
        }
    }

    protected String getCaptchaCode(File captchaFile, CryptedLink param) throws DecrypterException {
        return getCaptchaCode(getHost(), captchaFile, param);
    }

    protected String getCaptchaCode(String methodname, File captchaFile, CryptedLink param) throws DecrypterException {
        return getCaptchaCode(methodname, captchaFile, 0, param, null, null);
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

    public void setLastChallengeResponse(ResponseList<?> lastChallengeResponse) {
        this.lastChallengeResponse = lastChallengeResponse;
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

    /**
     * 
     * @param method
     *            Method name (name of the captcha method)
     * @param file
     *            (imagefile)
     * @param flag
     *            (Flag of UserIO.FLAGS
     * @param link
     *            (CryptedlinkO)
     * @param defaultValue
     *            (suggest this code)
     * @param explain
     *            (Special captcha? needs explaination? then use this parameter)
     * @return
     * @throws DecrypterException
     */
    protected String getCaptchaCode(String method, File file, int flag, final CryptedLink link, String defaultValue, String explain) throws DecrypterException {

        String orgCaptchaImage = link.getStringProperty("orgCaptchaFile", null);

        if (orgCaptchaImage != null && new File(orgCaptchaImage).exists()) {
            file = new File(orgCaptchaImage);
        }

        BasicCaptchaChallenge c = new BasicCaptchaChallenge(method, file, defaultValue, explain, this, flag) {
            @Override
            public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                switch (skipRequest) {
                case STOP_CURRENT_ACTION:
                    /* user wants to stop current action (eg crawling) */
                    return true;
                case BLOCK_ALL_CAPTCHAS:
                    /* user wants to block all captchas (current session) */
                    return true;
                case BLOCK_HOSTER:
                    /* user wants to block captchas from specific hoster */
                    return PluginForDecrypt.this.getHost().equals(Challenge.getHost(challenge));
                case REFRESH:
                case SINGLE:
                default:
                    return false;
                }
            }
        };
        int ct = getCaptchaTimeout();
        c.setTimeout(ct);
        invalidateLastChallengeResponse();
        if (CaptchaBlackList.getInstance().matches(c)) {
            logger.warning("Cancel. Blacklist Matching");
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        try {
            ChallengeResponseController.getInstance().handle(c);
        } catch (InterruptedException e) {
            logger.warning(Exceptions.getStackTrace(e));
            throw new DecrypterException(DecrypterException.CAPTCHA);
        } catch (SkipException e) {
            switch (e.getSkipRequest()) {
            case BLOCK_ALL_CAPTCHAS:
                CaptchaBlackList.getInstance().add(new CrawlerBlackListEntry(crawler));
                break;
            case BLOCK_HOSTER:
            case BLOCK_PACKAGE:
            case SINGLE:
            case TIMEOUT:
                break;
            case REFRESH:
                // refresh is not supported from the pluginsystem right now.
                return "GiveMeANewCaptcha!";
            case STOP_CURRENT_ACTION:
                LinkCollector.getInstance().abort();
                // Just to be sure
                CaptchaBlackList.getInstance().add(new CrawlerBlackListEntry(crawler));
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        if (!c.isSolved()) throw new DecrypterException(DecrypterException.CAPTCHA);
        setLastChallengeResponse(c.getResult());
        return c.getResult().getValue();

    }

    protected void setBrowserExclusive() {
        if (br == null) return;
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

    public void setLinkCrawlerAbort(LinkCrawlerAbort linkCrawlerAbort) {
        this.linkCrawlerAbort = linkCrawlerAbort;
    }

    /**
     * DO not use in Plugins for old 09581 Stable or try/catch
     * 
     * @return
     */
    public boolean isAbort() {
        LinkCrawlerAbort llinkCrawlerAbort = linkCrawlerAbort;
        if (llinkCrawlerAbort != null) return llinkCrawlerAbort.isAbort();
        return Thread.currentThread().isInterrupted();
    }

    public void setCrawler(LinkCrawler linkCrawler) {
        crawler = linkCrawler;
    }

    public LinkCrawler getCrawler() {
        return crawler;
    }

}