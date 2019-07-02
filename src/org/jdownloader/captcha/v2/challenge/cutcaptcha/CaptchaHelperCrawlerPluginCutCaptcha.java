package org.jdownloader.captcha.v2.challenge.cutcaptcha;

import jd.controlling.captcha.SkipException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
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

public class CaptchaHelperCrawlerPluginCutCaptcha extends AbstractCaptchaHelperCutCaptcha<PluginForDecrypt> {
    public CaptchaHelperCrawlerPluginCutCaptcha(PluginForDecrypt plugin, Browser br, String siteKey) {
        super(plugin, br, siteKey);
    }

    public String getToken() throws PluginException, InterruptedException, DecrypterException {
        logger.info("SiteKey:" + getSiteKey());
        if (Thread.currentThread() instanceof SingleDownloadController) {
            logger.severe("PluginForDecrypt.getCaptchaCode inside SingleDownloadController!?");
        }
        final CutCaptchaChallenge challenge = createChallenge();
        final PluginForDecrypt plugin = getPlugin();
        challenge.setTimeout(plugin.getChallengeTimeout(challenge));
        getPlugin().invalidateLastChallengeResponse();
        final BlacklistEntry<?> blackListEntry = CaptchaBlackList.getInstance().matches(challenge);
        if (blackListEntry != null) {
            logger.warning("Cancel. Blacklist Matching");
            throw new CaptchaException(blackListEntry);
        }
        try {
            ChallengeResponseController.getInstance().handle(challenge);
            if (!challenge.isSolved()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else if (!challenge.isCaptchaResponseValid()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                return challenge.getResult().getValue();
            }
        } catch (InterruptedException e) {
            LogSource.exception(logger, e);
            throw e;
        } catch (SkipException e) {
            LogSource.exception(logger, e);
            switch (e.getSkipRequest()) {
            case BLOCK_ALL_CAPTCHAS:
                CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(plugin.getCrawler()));
                break;
            case BLOCK_HOSTER:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByHost(plugin.getCrawler(), plugin.getHost()));
                break;
            case BLOCK_PACKAGE:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByPackage(plugin.getCrawler(), plugin.getCurrentLink()));
                break;
            case REFRESH:
                break;
            case TIMEOUT:
                plugin.onCaptchaTimeout(plugin.getCurrentLink(), challenge);
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
                    CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(plugin.getCrawler()));
                }
                break;
            default:
                break;
            }
            throw new CaptchaException(e.getSkipRequest());
        }
    }
}
