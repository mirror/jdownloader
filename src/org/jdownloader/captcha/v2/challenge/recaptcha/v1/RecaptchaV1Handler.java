package org.jdownloader.captcha.v2.challenge.recaptcha.v1;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

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
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
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
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.captcha.v2.solverjob.SolverJob;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.plugins.CaptchaStepProgress;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public abstract class RecaptchaV1Handler {

    private String  siteKey;
    private Browser browser;
    private Plugin  plugin;
    private Thread  thread;

    public class ResponsePair {
        public ResponsePair(String[] responsePair) {
            this.challenge = responsePair[0];
            this.response = responsePair[1];
        }

        private String challenge;
        private String response;
    }

    public RecaptchaV1Handler(String rcID, Plugin plg, Browser br) {
        this.siteKey = rcID;
        this.browser = br;
        this.plugin = plg;
    }

    public void run() throws PluginException, InterruptedException, IOException {

        for (int i = 0; i < getMaxRetries(); i++) {
            ResponsePair response = null;
            if (plugin instanceof PluginForHost) {
                response = runPluginForHost();
            }

            if (response == null) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            } else {
                boolean success = sendResponse(i, browser, response.challenge, response.response);
                if (success) {
                    if (plugin instanceof PluginForHost) {
                        ((PluginForHost) plugin).validateLastChallengeResponse();
                    }
                    return;

                } else {
                    if (plugin instanceof PluginForHost) {
                        ((PluginForHost) plugin).invalidateLastChallengeResponse();
                    }

                }
            }
        }
        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
    }

    protected abstract boolean sendResponse(int retry, Browser br, String challenge, String response) throws IOException, PluginException, InterruptedException;

    public int getMaxRetries() {
        return 5;
    }

    private ResponsePair runPluginForHost() throws PluginException, InterruptedException {
        PluginForHost plugin = (PluginForHost) this.plugin;

        LogSource logger = plugin.getLogger();
        if (Thread.currentThread() instanceof LinkCrawlerThread) {
            logger.severe("PluginForHost.getCaptchaCode inside LinkCrawlerThread!?");
        }
        final DownloadLink link = plugin.getDownloadLink();
        final CaptchaStepProgress progress = new CaptchaStepProgress(0, 1, null);
        progress.setProgressSource(plugin);
        progress.setDisplayInProgressColumnEnabled(false);
        try {
            link.addPluginProgress(progress);

            final boolean insideAccountChecker = Thread.currentThread() instanceof AccountCheckerThread;
            final RecaptchaV1Challenge c = new RecaptchaV1Challenge(siteKey, plugin) {

                @Override
                public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                    if (insideAccountChecker) {
                        /* we don't want to skip login captcha inside fetchAccountInfo(Thread is AccountCheckerThread) */
                        return false;
                    }
                    final Plugin challengePlugin = Challenge.getPlugin(challenge);
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
                        final DownloadLink lLink = Challenge.getDownloadLink(challenge);
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
            return new ResponsePair(JSonStorage.restoreFromString(result, TypeRef.STRING_ARRAY));
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

    public static String load(Browser rcBr, final String siteKey) throws IOException, InterruptedException {

        final AtomicReference<String> url = new AtomicReference<String>();

        AbstractBrowserChallenge dummyChallenge = new AbstractBrowserChallenge("recaptcha", null) {

            @Override
            public boolean canBeSkippedBy(SkipRequest skipRequest, ChallengeSolver<?> solver, Challenge<?> challenge) {
                return false;
            }

            @Override
            public String getHTML() {
                String html;
                try {
                    URL url = RecaptchaV1Challenge.class.getResource("recaptchaGetChallenge.html");
                    html = IO.readURLToString(url);

                    html = html.replace("%%%sitekey%%%", siteKey);
                    return html;
                } catch (IOException e) {
                    throw new WTFException(e);
                }
            }

            @Override
            public boolean onGetRequest(BrowserReference browserReference, GetRequest request, HttpResponse response) throws IOException {
                String pDo = request.getParameterbyKey("do");
                if (pDo.equals("setChallenge")) {
                    url.set(request.getParameterbyKey("url"));

                    response.getOutputStream(true).write("true".getBytes("UTF-8"));
                    synchronized (url) {
                        url.notifyAll();
                    }
                    return true;
                }
                return super.onGetRequest(browserReference, request, response);
            }

            @Override
            public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
                return null;
            }
        };
        BrowserReference ref = new BrowserReference(dummyChallenge) {

            @Override
            public void onResponse(String request) {
            }

        };
        ref.open();
        try {
            synchronized (url) {
                url.wait(15000);
            }
        } finally {
            ref.dispose();
        }

        String urlString = url.get();
        if (StringUtils.isEmpty(urlString)) {
            return null;
        }

        return urlString.substring(urlString.indexOf("c=") + 2);
    }

}
