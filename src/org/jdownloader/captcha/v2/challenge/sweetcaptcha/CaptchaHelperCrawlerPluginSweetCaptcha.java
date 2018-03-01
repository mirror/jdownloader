package org.jdownloader.captcha.v2.challenge.sweetcaptcha;

import java.awt.Rectangle;

import jd.controlling.captcha.SkipException;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkCollector.JobLinkCrawler;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.parser.html.Form;
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

public class CaptchaHelperCrawlerPluginSweetCaptcha extends AbstractCaptchaHelperSweetCaptcha<PluginForDecrypt> {
    public CaptchaHelperCrawlerPluginSweetCaptcha(final PluginForDecrypt plugin, final Browser br, final String siteKey, final String apiKey) {
        super(plugin, br, siteKey, apiKey);
    }

    public CaptchaHelperCrawlerPluginSweetCaptcha(final PluginForDecrypt plugin, final Browser br) {
        this(plugin, br, null, null);
    }

    public String getToken() throws PluginException, InterruptedException, DecrypterException {
        if (Thread.currentThread() instanceof SingleDownloadController) {
            logger.severe("PluginForDecrypt.getCaptchaCode inside SingleDownloadController!?");
        }
        String appkey = appKey;
        if (appkey == null) {
            appkey = getSweetCaptchaAppKey();
            if (appkey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "SweetCaptcha AppKey can not be found");
            }
        }
        String sitekey = siteKey;
        if (sitekey == null) {
            sitekey = getSweetCaptchaApiKey();
            if (sitekey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "SweetCaptcha SiteKey can not be found");
            }
        }
        final PluginForDecrypt plugin = getPlugin();
        SweetCaptchaChallenge c = new SweetCaptchaChallenge(sitekey, appkey, plugin) {
            @Override
            public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
                return null;
            }
        };
        c.setTimeout(plugin.getCaptchaTimeout(c));
        plugin.invalidateLastChallengeResponse();
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
        if (!c.isSolved()) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        return c.getResult().getValue();
    }

    public Form setFormValues(final Form form) throws PluginException, InterruptedException, DecrypterException {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Form not provided");
        }
        return setFormValues(form, this.getToken());
    }
}
