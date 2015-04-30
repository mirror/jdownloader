package org.jdownloader.captcha.v2.challenge.confidentcaptcha;

import java.awt.Rectangle;

import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.plugins.CaptchaException;
import jd.plugins.DecrypterException;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllCrawlerCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;

public class CaptchaHelperCrawlerPluginConfidentCaptcha extends AbstractCaptchaHelperConfidentCaptcha<PluginForDecrypt> {

    public CaptchaHelperCrawlerPluginConfidentCaptcha(final PluginForDecrypt plugin, final Browser br, final String siteKey) {
        super(plugin, br, siteKey);
    }

    public CaptchaHelperCrawlerPluginConfidentCaptcha(final PluginForDecrypt plugin, final Browser br) {
        this(plugin, br, null);
    }

    public String getToken() throws PluginException, InterruptedException, DecrypterException {
        if (Thread.currentThread() instanceof SingleDownloadController) {
            logger.severe("PluginForDecrypt.getCaptchaCode inside SingleDownloadController!?");
        }
        String sitekey = siteKey;
        if (sitekey == null) {
            sitekey = getConfidentCaptchaApiKey();
            if (sitekey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "ConfidentCaptcha info can not be found");
            }
        }
        final PluginForDecrypt plugin = getPlugin();
        final LinkCrawler currentCrawler = plugin.getCrawler();
        final CrawledLink currentOrigin = plugin.getCurrentLink().getOriginLink();
        ConfidentCaptchaChallenge c = new ConfidentCaptchaChallenge(plugin, sitekey) {
            @Override
            public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                Plugin challengePlugin = Challenge.getPlugin(challenge);
                if (challengePlugin != null && !(challengePlugin instanceof PluginForDecrypt)) {
                    /* we only want block PluginForDecrypt captcha here */
                    return false;
                }
                PluginForDecrypt decrypt = (PluginForDecrypt) challengePlugin;
                if (currentCrawler != decrypt.getCrawler()) {
                    /* we have a different crawler source */
                    return false;
                }
                switch (skipRequest) {
                case STOP_CURRENT_ACTION:
                    /* user wants to stop current action (eg crawling) */
                    return true;
                case BLOCK_ALL_CAPTCHAS:
                    /* user wants to block all captchas (current session) */
                    return true;
                case BLOCK_HOSTER:
                    /* user wants to block captchas from specific hoster */
                    return StringUtils.equals(plugin.getHost(), Challenge.getHost(challenge));
                case BLOCK_PACKAGE:
                    CrawledLink crawledLink = decrypt.getCurrentLink();
                    return crawledLink != null && crawledLink.getOriginLink() == currentOrigin;
                default:
                    return false;
                }
            }

            @Override
            public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
                return null;
            }
        };
        int ct = plugin.getCaptchaTimeout();
        c.setTimeout(ct);
        plugin.invalidateLastChallengeResponse();
        final BlacklistEntry blackListEntry = CaptchaBlackList.getInstance().matches(c);
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
                CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(plugin.getCrawler()));
                break;
            case BLOCK_HOSTER:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByHost(plugin.getCrawler(), plugin.getHost()));
                break;
            case BLOCK_PACKAGE:
                CaptchaBlackList.getInstance().add(new BlockCrawlerCaptchasByPackage(plugin.getCrawler(), plugin.getCurrentLink()));
                break;
            case REFRESH:
                // refresh is not supported from the pluginsystem right now.
                return "";
            case STOP_CURRENT_ACTION:
                if (Thread.currentThread() instanceof LinkCrawlerThread) {
                    LinkCollector.getInstance().abort();
                    // Just to be sure
                    CaptchaBlackList.getInstance().add(new BlockAllCrawlerCaptchasEntry(plugin.getCrawler()));
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

}
