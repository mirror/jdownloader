package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.io.IOException;

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

import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllDownloadCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByLink;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.RecaptchaV2Challenge.Recaptcha2FallbackChallenge;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.CaptchaStepProgress;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class CaptchaHelperHostPluginRecaptchaV2 extends AbstractCaptchaHelperRecaptchaV2<PluginForHost> {

    /* Most likely used for login captchas. */
    public CaptchaHelperHostPluginRecaptchaV2(final PluginForHost plugin, final Browser br, final String siteKey) {
        super(plugin, br, siteKey);
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
        String apiKey = siteKey;
        if (apiKey == null) {
            apiKey = getRecaptchaV2ApiKey();
            if (apiKey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "RecaptchaV2 API Key can not be found");
            }
        }
        final CaptchaStepProgress progress = new CaptchaStepProgress(0, 1, null);
        progress.setProgressSource(this);
        progress.setDisplayInProgressColumnEnabled(false);
        try {
            link.addPluginProgress(progress);
            final boolean insideAccountChecker = Thread.currentThread() instanceof AccountCheckerThread;
            final RecaptchaV2Challenge c = new RecaptchaV2Challenge(apiKey, plugin);
            c.setTimeout(plugin.getCaptchaTimeout());
            if (insideAccountChecker || FilePackage.isDefaultFilePackage(link.getFilePackage())) {
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
            ChallengeResponseController.getInstance().handle(c);
            if (c.getResult().size() == 1 && c.getResult().get(0).getChallenge() instanceof Recaptcha2FallbackChallenge) {
                final Recaptcha2FallbackChallenge challenge = ((Recaptcha2FallbackChallenge) c.getResult().get(0).getChallenge());
                try {
                    challenge.reload(2, c.getResult().get(0).getValue());
                    ChallengeResponseController.getInstance().handle(challenge);
                    if (challenge.getToken() != null) {
                        return challenge.getToken();
                        // challenge.evaluate()
                    } else {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    }
                } catch (IOException e) {
                    LogSource.exception(logger, e);
                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                }
            }
            if (!c.isSolved()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            if (!c.isCaptchaResponseValid()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA, "Captcha reponse value did not validate!");
            }
            return c.getResult().getValue();
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
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case BLOCK_HOSTER:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByHost(link.getHost()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;

                case BLOCK_PACKAGE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByPackage(link.getParentNode()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case SINGLE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(link));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case TIMEOUT:
                    /*
                     * TODO: come up with better solution... <br /> For now not using
                     * JsonConfig.create(CaptchaSettings.class).isSkipDownloadLinkOnCaptchaTimeoutEnabled() because in previous
                     * implementation we wouldn't break and would return "" (from refresh) response to plugins!! this is BADDDDDDDDDDDDDD
                     * idea! we should never return empty response to plugins, specially a response that is shared for different error
                     * types. Plugins have zero idea what happen to cause issue.
                     */
                    // if (!JsonConfig.create(CaptchaSettings.class).isSkipDownloadLinkOnCaptchaTimeoutEnabled()) {
                    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "CaptchaDialog timed out!", 5 * 60 * 1000l);
                    // }
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(link));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
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
