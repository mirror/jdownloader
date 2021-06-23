package org.jdownloader.captcha.v2.challenge.hcaptcha;

import java.awt.Rectangle;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.exceptions.WTFException;
import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.net.protocol.http.HTTPConstants.ResponseCode;
import org.appwork.remoteapi.exceptions.RemoteAPIException;
import org.appwork.utils.IO;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.HTTPHeader;
import org.appwork.utils.net.httpserver.requests.GetRequest;
import org.appwork.utils.net.httpserver.requests.HttpRequest;
import org.appwork.utils.net.httpserver.responses.HttpResponse;
import org.jdownloader.captcha.v2.AbstractResponse;
import org.jdownloader.captcha.v2.ChallengeSolver;
import org.jdownloader.captcha.v2.challenge.stringcaptcha.BasicCaptchaChallenge;
import org.jdownloader.captcha.v2.solver.browser.AbstractBrowserChallenge;
import org.jdownloader.captcha.v2.solver.browser.BrowserReference;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.gui.translate._GUI;

public class HCaptchaChallenge extends AbstractBrowserChallenge {
    public static final String             RAWTOKEN = "rawtoken";
    public static final String             HCAPTCHA = "hcaptcha";
    private final String                   siteKey;
    private volatile BasicCaptchaChallenge basicChallenge;
    private final String                   siteDomain;

    public String getSiteKey() {
        return siteKey;
    }

    public String getType() {
        return AbstractHCaptcha.TYPE.NORMAL.name();
    };

    @Override
    public AbstractResponse<String> parseAPIAnswer(String result, String resultFormat, ChallengeSolver<?> solver) {
        if (hasBasicCaptchaChallenge()) {
            final BasicCaptchaChallenge basic = createBasicCaptchaChallenge(false);
            if (basic != null) {
                return basic.parseAPIAnswer(result, resultFormat, solver);
            }
        }
        return super.parseAPIAnswer(result, resultFormat, solver);
    }

    @Override
    public Object getAPIStorable(String format) throws Exception {
        final BasicCaptchaChallenge basic = createBasicCaptchaChallenge(true);
        if (basic != null) {
            return basic.getAPIStorable(format);
        } else {
            return super.getAPIStorable(format);
        }
    }

    @Override
    public void cleanup() {
        super.cleanup();
        final BasicCaptchaChallenge basicChallenge = this.basicChallenge;
        if (basicChallenge != null) {
            basicChallenge.cleanup();
        }
    }

    public HCaptchaChallenge(final String siteKey, Plugin pluginForHost, Browser br, String siteDomain) {
        super(HCAPTCHA, pluginForHost, br);
        this.siteKey = siteKey;
        this.siteDomain = siteDomain;
        if (siteKey == null || !siteKey.matches("^[\\w-]+$")) {
            throw new WTFException("Bad SiteKey");
        }
    }

    public AbstractHCaptcha<?> getAbstractCaptchaHelperHCaptcha() {
        return null;
    }

    public String getSiteUrl() {
        return null;
    }

    public String getSiteDomain() {
        return siteDomain;
    }

    @Override
    public boolean onGetRequest(final BrowserReference brRef, final GetRequest request, final HttpResponse response) throws IOException, RemoteAPIException {
        synchronized (this) {
            String pDo = request.getParameterbyKey("do");
            if ("solve".equals(pDo)) {
                String responsetoken = request.getParameterbyKey("response");
                brRef.onResponse(responsetoken);
                response.setResponseCode(ResponseCode.SUCCESS_OK);
                response.getResponseHeaders().add(new HTTPHeader(HTTPConstants.HEADER_RESPONSE_CONTENT_TYPE, "text/html; charset=utf-8"));
                response.getOutputStream(true).write("Please Close the Browser now".getBytes("UTF-8"));
                return true;
            }
            return false;
        }
    }

    @Override
    public String getHTML(HttpRequest request, String id) {
        try {
            final String userAgent = request.getRequestHeaders().getValue(HTTPConstants.HEADER_REQUEST_USER_AGENT);
            final boolean isSafari = userAgent != null && userAgent.toLowerCase(Locale.ENGLISH).matches("(?s).*^((?!chrome|android|crios|fxios).)*safari.*");
            final boolean isEdge = userAgent != null && userAgent.toLowerCase(Locale.ENGLISH).matches(".*edge\\/.*");
            final URL url = HCaptchaChallenge.class.getResource("hcaptcha.html");
            String html = IO.readURLToString(url);
            html = html.replace("%%%provider%%%", getCaptchaNameSpace());
            html = html.replace("%%%headTitle%%%", _GUI.T.recaptchav2_head_title());
            html = html.replace("%%%headDescription%%%", _GUI.T.recaptchav2_head_description());
            html = html.replace("%%%captchaHeader%%%", _GUI.T.recaptchav2_header());
            html = html.replace("%%%unsupportedBrowserHeader%%%", _GUI.T.extension_unsupported_browser_header());
            html = html.replace("%%%unsupportedBrowserDescription%%%", _GUI.T.extension_unsupported_browser_description());
            html = html.replace("%%%helpHeader%%%", _GUI.T.extension_help_header());
            html = html.replace("%%%helpDescription%%%", _GUI.T.extension_help_description());
            html = html.replace("%%%helpDescriptionLinkTitle%%%", _GUI.T.extension_help_description_link_title());
            html = html.replace("%%%extensionSupportHeader%%%", _GUI.T.extension_support_header());
            html = html.replace("%%%extensionSupportDescription%%%", _GUI.T.extension_support_description());
            html = html.replace("%%%extensionSupportLinkTitle%%%", _GUI.T.extension_support_link_title());
            html = html.replace("%%%siteUrl%%%", StringUtils.valueOrEmpty(getSiteUrl()));
            html = html.replace("%%%siteDomain%%%", getSiteDomain());
            html = html.replace("%%%sitekey%%%", getSiteKey());
            html = html.replace("%%%sitekeyType%%%", getType());
            html = html.replace("%%%unsupportedBrowser%%%", (isSafari || isEdge) ? "block" : "none");
            html = html.replace("%%%display%%%", "block");
            html = html.replace("%%%noExtensionHeader%%%", _GUI.T.extension_required_header());
            html = html.replace("%%%noExtensionDescription%%%", _GUI.T.extension_required_description());
            html = html.replace("%%%noExtensionLinkTitle%%%", _GUI.T.extension_required_link_title());
            html = html.replace("%%%installExtensionHeader%%%", _GUI.T.install_extension_header());
            html = html.replace("%%%installExtensionDone%%%", _GUI.T.install_extension_done());
            html = html.replace("%%%installExtensionUsingOtherBrowser%%%", _GUI.T.install_extension_using_other_browser());
            html = html.replace("%%%installExtensionShowAllBrowsers%%%", _GUI.T.install_extension_show_all_browsers_link());
            html = html.replace("%%%chromeExtensionHeader%%%", _GUI.T.install_chrome_extension_header());
            html = html.replace("%%%chromeExtensionDescription%%%", _GUI.T.install_chrome_extension_description());
            html = html.replace("%%%chromeExtensionLinkTitle%%%", _GUI.T.install_chrome_extension_link_title());
            html = html.replace("%%%firefoxExtensionHeader%%%", _GUI.T.install_firefox_extension_header());
            html = html.replace("%%%firefoxExtensionDescription%%%", _GUI.T.install_firefox_extension_description());
            html = html.replace("%%%firefoxExtensionLinkTitle%%%", _GUI.T.install_firefox_extension_link_title());
            html = html.replace("%%%operaExtensionHeader%%%", _GUI.T.install_opera_extension_header());
            html = html.replace("%%%operaExtensionDescription%%%", _GUI.T.install_opera_extension_description());
            html = html.replace("%%%operaExtensionLinkTitle%%%", _GUI.T.install_opera_extension_link_title());
            html = html.replace("%%%findMoreAppsLinkTitle%%%", _GUI.T.find_more_apps_link_title());
            html = html.replace("%%%findMoreAppsDescription%%%", _GUI.T.find_more_apps_description());
            html = html.replace("%%%session%%%", id);
            html = html.replace("%%%challengeId%%%", Long.toString(getId().getID()));
            html = html.replace("%%%namespace%%%", getHttpPath());
            return html;
        } catch (IOException e) {
            throw new WTFException(e);
        }
    }

    @Override
    protected String getHttpPath() {
        if (plugin != null) {
            return "captcha/" + getCaptchaNameSpace() + "/" + getSiteDomain();
        } else {
            return super.getHttpPath();
        }
    }

    @Override
    protected String getCaptchaNameSpace() {
        return "hcaptcha";
    }

    /**
     * Used to validate result against expected pattern. <br />
     * This is different to AbstractBrowserChallenge.isSolved, as we don't want to throw the same error exception.
     *
     * @param result
     * @return
     * @author raztoki
     */
    protected final boolean isCaptchaResponseValid() {
        final String v = getResult().getValue();
        if (isSolved() && isValidResponseToken(v)) {
            return true;
        } else {
            return false;
        }
    }

    protected boolean isValidResponseToken(String v) {
        return v != null && v.matches("^[\\w-_\\.]{150,}");
    }

    @Override
    public boolean validateResponse(AbstractResponse<String> response) {
        return super.validateResponse(response) && isValidResponseToken(response.getValue());
    }

    @Override
    public void onHandled() {
        super.onHandled();
        final BasicCaptchaChallenge basicChallenge = this.basicChallenge;
        if (basicChallenge != null) {
            basicChallenge.onHandled();
        }
    }

    public synchronized boolean hasBasicCaptchaChallenge() {
        return basicChallenge != null;
    }

    public synchronized BasicCaptchaChallenge createBasicCaptchaChallenge(final boolean showInstallDialog) {
        return basicChallenge;
    }

    @Override
    public BrowserViewport getBrowserViewport(BrowserWindow screenResource, Rectangle elementBounds) {
        return null;
    }
}
