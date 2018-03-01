package org.jdownloader.captcha.v2.challenge.geetest;

import java.awt.Rectangle;

import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.captcha.SkipException;
import jd.controlling.captcha.SkipRequest;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.linkcrawler.LinkCrawlerThread;
import jd.http.Browser;
import jd.plugins.CaptchaException;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.captcha.blacklist.BlacklistEntry;
import org.jdownloader.captcha.blacklist.BlockAllDownloadCaptchasEntry;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByHost;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByLink;
import org.jdownloader.captcha.blacklist.BlockDownloadCaptchasByPackage;
import org.jdownloader.captcha.blacklist.CaptchaBlackList;
import org.jdownloader.captcha.v2.Challenge;
import org.jdownloader.captcha.v2.ChallengeResponseController;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.CaptchaStepProgress;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class CaptchaHelperHostPluginGeeTest extends AbstractCaptchaHelperGeeTest<PluginForHost> {
    public CaptchaHelperHostPluginGeeTest(PluginForHost plugin, Browser br, String siteKey) {
        super(plugin, br, siteKey);
    }

    public CaptchaHelperHostPluginGeeTest(PluginForHost plugin, Browser br) {
        this(plugin, br, null);
    }

    public String getToken() throws PluginException, InterruptedException {
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            logger.severe("PluginForHost.getCaptchaCode inside LinkCrawlerThread!?");
        }
        String apiKey = siteKey;
        if (apiKey == null) {
            apiKey = getGeeTestApiKey();
            if (apiKey == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "GeeTest API Key can not be found");
            }
        }
        final DownloadLink link = getPlugin().getDownloadLink();
        final CaptchaStepProgress progress = new CaptchaStepProgress(0, 1, null);
        progress.setProgressSource(this);
        progress.setDisplayInProgressColumnEnabled(false);
        try {
            link.addPluginProgress(progress);
            final boolean insideAccountChecker = Thread.currentThread() instanceof AccountCheckerThread;
            final GeeTestChallenge c = new GeeTestChallenge(apiKey, getPlugin()) {
                @Override
                public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                    if (insideAccountChecker) {
                        /* we don't want to skip login captcha inside fetchAccountInfo(Thread is AccountCheckerThread) */
                        return false;
                    }
                    final Plugin challengePlugin = challenge.getPlugin();
                    if (challengePlugin != null && !(challengePlugin instanceof PluginForHost)) {
                        /* we only want block PluginForHost captcha here */
                        return false;
                    }
                    switch (skipRequest) {
                    case BLOCK_ALL_CAPTCHAS:
                        /* user wants to block all captchas (current session) */
                        return true;
                    case BLOCK_HOSTER:
                        /* user wants to block captchas from specific hoster */
                        return StringUtils.equals(link.getHost(), challenge.getHost());
                    case BLOCK_PACKAGE:
                        /* user wants to block captchas from current FilePackage */
                        final DownloadLink lLink = challenge.getDownloadLink();
                        if (lLink == null || lLink.getDefaultPlugin() == null) {
                            return false;
                        }
                        return link.getFilePackage() == lLink.getFilePackage();
                    default:
                        return false;
                    }
                }

                @Override
                public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
                    return null;
                }
            };
            c.setTimeout(getPlugin().getCaptchaTimeout(c));
            if (insideAccountChecker || FilePackage.isDefaultFilePackage(link.getFilePackage())) {
                /**
                 * account login -> do not use antiCaptcha services
                 */
                c.setAccountLogin(true);
            } else {
                final SingleDownloadController controller = link.getDownloadLinkController();
                if (controller != null) {
                    getPlugin().setHasCaptcha(link, controller.getAccount(), true);
                }
            }
            getPlugin().invalidateLastChallengeResponse();
            final BlacklistEntry<?> blackListEntry = CaptchaBlackList.getInstance().matches(c);
            if (blackListEntry != null) {
                logger.warning("Cancel. Blacklist Matching");
                throw new CaptchaException(blackListEntry);
            }
            ChallengeResponseController.getInstance().handle(c);
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
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case BLOCK_HOSTER:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByHost(link.getHost()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case BLOCK_PACKAGE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByPackage(link.getParentNode()));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case TIMEOUT:
                    getPlugin().onCaptchaTimeout(link, e.getChallenge());
                    // TIMEOUT may fallthrough to SINGLE
                case SINGLE:
                    CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(link));
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI.T.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                    }
                    break;
                case REFRESH:
                    break;
                case STOP_CURRENT_ACTION:
                    if (Thread.currentThread() instanceof SingleDownloadController) {
                        DownloadWatchDog.getInstance().stopDownloads();
                    }
                    break;
                }
            }
            throw new CaptchaException(e.getSkipRequest());
        } finally {
            link.removePluginProgress(progress);
        }
    }
}
