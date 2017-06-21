package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.util.ArrayList;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllDownloadCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByLink;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.CaptchaStepProgress;
import org.jdownloader.settings.staticreferences.CFG_GUI;

import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.captcha.SkipException;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.plugins.CaptchaException;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class CaptchaHelperHostPluginRecaptchaV2 extends AbstractCaptchaHelperRecaptchaV2<PluginForHost> {
    public CaptchaHelperHostPluginRecaptchaV2(final PluginForHost plugin, final Browser br, final String siteKey, final String secureToken, boolean boundToDomain) {
        super(plugin, br, siteKey, secureToken, boundToDomain);
    }

    /* Most likely used for login captchas. */
    public CaptchaHelperHostPluginRecaptchaV2(final PluginForHost plugin, final Browser br, final String siteKey) {
        this(plugin, br, siteKey, null, false);
    }

    public CaptchaHelperHostPluginRecaptchaV2(final PluginForHost plugin, final Browser br) {
        this(plugin, br, null);
    }

    public String getToken() throws PluginException, InterruptedException {
        runDdosPrevention();
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            logger.severe("PluginForHost.getCaptchaCode inside LinkCrawlerThread!?");
        }
        final PluginForHost plugin = this.plugin;
        final DownloadLink link = getPlugin().getDownloadLink();
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
        final CaptchaStepProgress progress = new CaptchaStepProgress(0, 1, null);
        progress.setProgressSource(this);
        progress.setDisplayInProgressColumnEnabled(false);
        ArrayList<SolverJob<String>> jobs = new ArrayList<SolverJob<String>>();
        try {
            if (link != null) {
                link.addPluginProgress(progress);
            }
            final boolean insideAccountChecker = Thread.currentThread() instanceof AccountCheckerThread;
            final RecaptchaV2Challenge c = new RecaptchaV2Challenge(siteKey, secureToken, boundToDomain, plugin, br, getSiteDomain());
            try {
                c.setTimeout(plugin.getCaptchaTimeout());
                if (insideAccountChecker || FilePackage.isDefaultFilePackage(link.getFilePackage())) {
                    // coalado: discuss why. FilePackage.isDefaultFilePackage(link.getFilePackage()) is triggered for captchas during online
                    // check es well
                    /**
                     * account login -> do not use anticaptcha services
                     */
                    c.setAccountLogin(true);
                } else {
                    final SingleDownloadController controller = link.getDownloadLinkController();
                    if (controller != null) {
                        plugin.setHasCaptcha(link, controller.getAccount(), true);
                    }
                }
                plugin.invalidateLastChallengeResponse();
                final BlacklistEntry<?> blackListEntry = CaptchaBlackList.getInstance().matches(c);
                if (blackListEntry != null) {
                    logger.warning("Cancel. Blacklist Matching");
                    throw new CaptchaException(blackListEntry);
                }
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
                    if (rcFallback != null && rcFallback.getToken() == null) {
                        // retry
                        try {
                            rcFallback.reload(jobs.size() + 1);
                        } catch (Throwable e) {
                            LogSource.exception(logger, e);
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
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
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
                if (c.getResult() != null) {
                    for (AbstractResponse<String> r : c.getResult()) {
                        if (r.getChallenge() instanceof AbstractRecaptcha2FallbackChallenge) {
                            String token = ((AbstractRecaptcha2FallbackChallenge) r.getChallenge()).getToken();
                            if (!RecaptchaV2Challenge.isValidToken(token)) {
                                for (int i = 0; i < jobs.size(); i++) {
                                    jobs.get(i).invalidate();
                                }
                                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                            } else {
                                for (int i = 0; i < jobs.size(); i++) {
                                    jobs.get(i).validate();
                                }
                            }
                            return token;
                        }
                    }
                }
                // check main challenge
                if (!c.isCaptchaResponseValid()) {
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA, "Captcha reponse value did not validate!");
                }
                return c.getResult().getValue();
            } finally {
                c.cleanup();
            }
            // } catch (PluginException e) {
            // for (int i = 0; i < jobs.size(); i++) {
            // jobs.get(i).invalidate();
            // }
            // throw e;
        } catch (InterruptedException e) {
            LogSource.exception(logger, e);
            throw e;
        } catch (SkipException e) {
            LogSource.exception(logger, e);
            if (link != null) {
                switch (e.getSkipRequest()) {
                case BLOCK_ALL_CAPTCHAS:
                    CaptchaBlackList.getInstance().add(new BlockAllDownloadCaptchasEntry());
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), new AbstractIcon(IconKey.ICON_SKIPPED, 32));
                    }
                    break;
                case BLOCK_HOSTER:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByHost(link.getHost()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), new AbstractIcon(IconKey.ICON_SKIPPED, 32));
                    }
                    break;
                case BLOCK_PACKAGE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByPackage(link.getParentNode()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), new AbstractIcon(IconKey.ICON_SKIPPED, 32));
                    }
                    break;
                case TIMEOUT:
                    getPlugin().onCaptchaTimeout(link, e.getChallenge());
                    // TIMEOUT may fallthrough to SINGLE
                case SINGLE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(link));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), new AbstractIcon(IconKey.ICON_SKIPPED, 32));
                    }
                    break;
                case REFRESH:
                    break;
                case STOP_CURRENT_ACTION:
                    if (Thread.currentThread() instanceof SingleDownloadController) {
                        DownloadWatchDog.getInstance().stopDownloads();
                    }
                    break;
                default:
                    break;
                }
            }
            throw new CaptchaException(e.getSkipRequest());
        } finally {
            link.removePluginProgress(progress);
        }
    }
}
