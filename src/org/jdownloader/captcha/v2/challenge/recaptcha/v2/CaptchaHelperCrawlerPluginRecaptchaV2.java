package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.util.ArrayList;

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

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllCrawlerCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockCrawlerCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.solverjob.SolverJob;

public class CaptchaHelperCrawlerPluginRecaptchaV2 extends AbstractCaptchaHelperRecaptchaV2<PluginForDecrypt> {
    public CaptchaHelperCrawlerPluginRecaptchaV2(final PluginForDecrypt plugin, final Browser br, final String siteKey, final String secureToken, boolean boundToDomain) {
        super(plugin, br, siteKey, secureToken, boundToDomain);
    }

    public CaptchaHelperCrawlerPluginRecaptchaV2(final PluginForDecrypt plugin, final Browser br, final String siteKey) {
        this(plugin, br, siteKey, null, false);
    }

    public CaptchaHelperCrawlerPluginRecaptchaV2(final PluginForDecrypt plugin, final Browser br) {
        this(plugin, br, null);
    }

    public String getToken() throws PluginException, InterruptedException, DecrypterException {
        runDdosPrevention();
        if (Thread.currentThread() instanceof SingleDownloadController) {
            logger.severe("PluginForDecrypt.getCaptchaCode inside SingleDownloadController!?");
        }
        if (siteKey == null) {
            siteKey = getSiteKey();
            if (siteKey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "RecaptchaV2 API Key can not be found");
            }
        }
        if (secureToken == null) {
            secureToken = getSecureToken();
            // non fatal if secureToken is null.
        }
        final PluginForDecrypt plugin = getPlugin();
        final RecaptchaV2Challenge c = createChallenge();
        c.setTimeout(plugin == null ? 60000 : plugin.getCaptchaTimeout());
        if (plugin != null) {
            plugin.invalidateLastChallengeResponse();
        }
        final BlacklistEntry<?> blackListEntry = CaptchaBlackList.getInstance().matches(c);
        if (blackListEntry != null) {
            logger.warning("Cancel. Blacklist Matching");
            throw new CaptchaException(blackListEntry);
        }
        ArrayList<SolverJob<String>> jobs = new ArrayList<SolverJob<String>>();
        try {
            jobs.add(ChallengeResponseController.getInstance().handle(c));
            AbstractRecaptcha2FallbackChallenge rcFallback = null;
            while (jobs.size() <= 20) {
                if (rcFallback == null && c.getResult() != null) {
                    for (AbstractResponse<String> r : c.getResult()) {
                        if (r.getChallenge() != null && r.getChallenge() instanceof AbstractRecaptcha2FallbackChallenge) {
                            rcFallback = (AbstractRecaptcha2FallbackChallenge) r.getChallenge();
                            break;
                        }
                    }
                }
                if (rcFallback != null && StringUtils.isEmpty(rcFallback.getToken())) {
                    // retry
                    try {
                        rcFallback.reload(jobs.size() + 1);
                    } catch (Throwable e) {
                        LogSource.exception(logger, e);
                        throw new DecrypterException(DecrypterException.CAPTCHA);
                    }
                    if (rcFallback.doRunAntiDDosProtection()) {
                        runDdosPrevention();
                    }
                    jobs.add(ChallengeResponseController.getInstance().handle(rcFallback));
                    if (StringUtils.isNotEmpty(rcFallback.getToken())) {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (!c.isSolved()) {
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            if (c.getResult() != null) {
                for (AbstractResponse<String> r : c.getResult()) {
                    if (r.getChallenge() instanceof AbstractRecaptcha2FallbackChallenge) {
                        String token = ((AbstractRecaptcha2FallbackChallenge) r.getChallenge()).getToken();
                        if (!RecaptchaV2Challenge.isValidToken(token)) {
                            for (int i = 0; i < jobs.size(); i++) {
                                jobs.get(i).invalidate();
                            }
                            throw new DecrypterException(DecrypterException.CAPTCHA);
                        } else {
                            for (int i = 0; i < jobs.size(); i++) {
                                jobs.get(i).validate();
                            }
                        }
                        return token;
                    }
                }
            }
            if (!c.isCaptchaResponseValid()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Captcha reponse value did not validate!");
            }
            return c.getResult().getValue();
        } catch (PluginException e) {
            for (int i = 0; i < jobs.size(); i++) {
                jobs.get(i).invalidate();
            }
            throw e;
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
        } finally {
            c.cleanup();
        }
    }

    protected RecaptchaV2Challenge createChallenge() {
        return new RecaptchaV2Challenge(getSiteKey(), getSecureToken(), getPlugin(), br, getSiteDomain()) {
            @Override
            public String getSiteUrl() {
                return CaptchaHelperCrawlerPluginRecaptchaV2.this.getSiteUrl();
            }
        };
    }
}
