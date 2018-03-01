package org.jdownloader.captcha.v2.challenge.geetest;

import jd.controlling.captcha.SkipException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.plugins.CaptchaException;
import jd.plugins.DecrypterException;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllCrawlerCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;

public class CaptchaHelperCrawlerPluginGeeTest extends AbstractCaptchaHelperGeeTest<PluginForDecrypt> {
    public CaptchaHelperCrawlerPluginGeeTest(PluginForDecrypt plugin, Browser br, String siteKey) {
        super(plugin, br, siteKey);
    }

    public CaptchaHelperCrawlerPluginGeeTest(PluginForDecrypt plugin, Browser br) {
        this(plugin, br, null);
    }

    public String getToken() throws PluginException, InterruptedException, DecrypterException {
        if (Thread.currentThread() instanceof SingleDownloadController) {
            logger.severe("PluginForDecrypt.getCaptchaCode inside SingleDownloadController!?");
        }
        String apiKey = siteKey;
        if (apiKey == null) {
            apiKey = getGeeTestApiKey();
            if (apiKey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "AreYouAHuman API Key can not be found");
            }
        }
        final LinkCrawler currentCrawler = getPlugin().getCrawler();
        final CrawledLink currentOrigin = getPlugin().getCurrentLink().getOriginLink();
        GeeTestChallenge c = new GeeTestChallenge(apiKey, getPlugin()) {
            @Override
            public BrowserViewport getBrowserViewport(BrowserWindow screenResource, java.awt.Rectangle elementBounds) {
                return null;
            }
        };
        c.setTimeout(getPlugin().getCaptchaTimeout(c));
        getPlugin().invalidateLastChallengeResponse();
        final BlacklistEntry<?> blackListEntry = CaptchaBlackList.getInstance().matches(c);
        if (blackListEntry != null) {
            logger.warning("Cancel. Blacklist Matching");
            throw new CaptchaException(blackListEntry);
        }
        try {
            ChallengeResponseController.getInstance().handle(c);
            if (!c.isSolved()) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            if (!c.isCaptchaResponseValid()) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            return c.getResult().getValue();
        } catch (InterruptedException e) {
            LogSource.exception(logger, e);
            throw e;
        } catch (SkipException e) {
            LogSource.exception(logger, e);
            switch (e.getSkipRequest()) {
            case BLOCK_ALL_CAPTCHAS:
                CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(getPlugin().getCrawler()));
                break;
            case BLOCK_HOSTER:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByHost(getPlugin().getCrawler(), getPlugin().getHost()));
                break;
            case BLOCK_PACKAGE:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByPackage(getPlugin().getCrawler(), getPlugin().getCurrentLink()));
                break;
            case REFRESH:
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
                    CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(getPlugin().getCrawler()));
                }
                break;
            default:
                break;
            }
            throw new CaptchaException(e.getSkipRequest());
        }
    }
}
