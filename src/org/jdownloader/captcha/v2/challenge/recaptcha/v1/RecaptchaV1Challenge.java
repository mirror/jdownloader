package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;

import jd.controlling.accountchecker.AccountCheckerThread;
import jd.controlling.captcha.CaptchaSettings;
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

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.httpserver.requests.PostRequest;
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
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.CaptchaStepProgress;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public abstract class RecaptchaV1Challenge extends AbstractBrowserChallenge {

    private String siteKey;

    public String getSiteKey() {
        return siteKey;
    }

    @Override
    public BrowserViewport getBrowserViewport(BrowserWindow screenResource) {

        Rectangle rect = screenResource.getRectangleByColor(0xff9900, 0, 0, 1d, 0, 0);

        return new Recaptcha1BrowserViewport(screenResource, rect);
    }

    @Override
    public String handleRequest(PostRequest request) throws IOException {
        String challenge = request.getParameterbyKey("recaptcha_challenge_field");
        String responseString = request.getParameterbyKey("recaptcha_challenge_field");

        return JSonStorage.serializeToJson(new String[] { challenge, responseString });
    }

    public RecaptchaV1Challenge(String siteKey, Plugin pluginForHost) {
        super("recaptchav1", pluginForHost);

        if (siteKey == null || !siteKey.trim().matches("^[\\w]+$")) {
            throw new WTFException("Bad SiteKey");
        }
        this.siteKey = siteKey.trim();
    }

    @Override
    public String getHTML() {
        String html;
        try {
            URL url = RecaptchaV1Challenge.class.getResource("recaptcha.html");
            html = IO.readURLToString(url);

            html = html.replace("%%%sitekey%%%", siteKey);
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    public static String[] solve(String siteKey, PluginForHost plugin, Browser br) throws PluginException, InterruptedException {
        LogSource logger = plugin.getLogger();
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            logger.severe("PluginForHost.getCaptchaCode inside LinkCrawlerThread!?");
        }
        final DownloadLink link = plugin.getDownloadLink();
        String apiKey = siteKey;

        final CaptchaStepProgress progress = new CaptchaStepProgress(0, 1, null);
        progress.setProgressSource(plugin);
        progress.setDisplayInProgressColumnEnabled(false);
        plugin.setHasCaptchas(true);

        try {
            link.addPluginProgress(progress);

            final boolean insideAccountChecker = Thread.currentThread() instanceof AccountCheckerThread;
            RecaptchaV1Challenge c = new RecaptchaV1Challenge(apiKey, plugin) {

                @Override
                public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                    if (insideAccountChecker) {
                        /* we don't want to skip login captcha inside fetchAccountInfo(Thread is AccountCheckerThread) */
                        return false;
                    }
                    Plugin challengePlugin = Challenge.getPlugin(challenge);
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
                        return StringUtils.equals(link.getHost(), Challenge.getHost(challenge));
                    case BLOCK_PACKAGE:
                        /* user wants to block captchas from current FilePackage */
                        DownloadLink lLink = Challenge.getDownloadLink(challenge);
                        if (lLink == null || lLink.getDefaultPlugin() == null) {
                            return false;
                        }
                        return link.getFilePackage() == lLink.getFilePackage();
                    default:
                        return false;
                    }
                }

            };
            c.setTimeout(plugin.getCaptchaTimeout());
            if (Thread.currentThread() instanceof AccountCheckerThread || FilePackage.isDefaultFilePackage(link.getFilePackage())) {
                /**
                 * account login -> do not use anticaptcha services
                 */
                c.setAccountLogin(true);
            }
            plugin.invalidateLastChallengeResponse();
            final BlacklistEntry blackListEntry = CaptchaBlackList.getInstance().matches(c);
            if (blackListEntry != null) {
                logger.warning("Cancel. Blacklist Matching");
                throw new CaptchaException(blackListEntry);
            }
            final SolverJob<String> job = ChallengeResponseController.getInstance().handle(c);
            if (!c.isSolved()) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            String result = c.getResult().getValue();
            if (StringUtils.isEmpty(result)) {
                return null;
            }
            return JSonStorage.restoreFromString(result, TypeRef.STRING_ARRAY);
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
                    if (JsonConfig.create(CaptchaSettings.class).isSkipDownloadLinkOnCaptchaTimeoutEnabled()) {
                        CaptchaBlackList.getInstance().add(new BlockDownloadCaptchasByLink(link));
                        if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                            HelpDialog.show(false, true, HelpDialog.getMouseLocation(), "SKIPPEDHOSTER", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_title(), _GUI._.ChallengeDialogHandler_viaGUI_skipped_help_msg(), NewTheme.I().getIcon("skipped", 32));
                        }
                    }
                case REFRESH:
                    // we should forward the refresh request to a new pluginstructure soon. For now. the plugin will just retry
                    return null;
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
