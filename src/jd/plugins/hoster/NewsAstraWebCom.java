package jd.plugins.hoster;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "astraweb.com" }, urls = { "" })
public class NewsAstraWebCom extends UseNet {
    public NewsAstraWebCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.astraweb.com/signup.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.astraweb.com/aup.html";
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "news.astraweb.com".equals(host)) {
            return "astraweb.com";
        } else {
            return super.rewriteHost(host);
        }
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUseNetUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface NewsAstraWebComConfigInterface extends UsenetAccountConfigInterface {
    };

    private Map<String, Object> createPostRequest(Browser br, final String url, final String jwtToken, final Map<String, Object> customJson) throws IOException {
        final Map<String, Object> json = new HashMap<String, Object>();
        if (customJson != null) {
            json.putAll(customJson);
        } else {
            json.put("brandName", "astraweb");
            json.put("userId", jwtToken);
        }
        final PostRequest postRequest = br.createJSonPostRequest(url, JSonStorage.toString(json));
        postRequest.getHeaders().put("Origin", "https://www.astraweb.com");
        postRequest.getHeaders().put("Authorization", "Bearer " + jwtToken);
        final Browser brc = br.cloneBrowser();
        brc.setAllowedResponseCodes(403);
        final String response = brc.getPage(postRequest);
        if (brc.getRequest().getHttpConnection().getResponseCode() == 403) {
            return new HashMap<String, Object>();
        } else {
            return restoreFromString(response, TypeRef.MAP);
        }
    }

    private final String jwtTokenProperty = "jwt_token";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (account) {
            final AccountInfo ai = new AccountInfo();
            final AccountInfo previousai = account.getAccountInfo();
            br.setFollowRedirects(true);
            String jwtToken = account.getStringProperty(jwtTokenProperty, null);
            Map<String, Object> response = null;
            try {
                if (jwtToken != null) {
                    if (true) {
                        try {
                            verifyUseNetLogins(account);
                            if (previousai != null) {
                                ai.setStatus(previousai.getStatus());
                            }
                            ai.setUnlimitedTraffic();
                            ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                            // https://www.astraweb.com/, 50
                            account.setMaxSimultanDownloads(50);
                            account.setRefreshTimeout(5 * 60 * 60 * 1000l);
                            return ai;
                        } catch (InvalidAuthException e) {
                            logger.log(e);
                            account.removeProperty(jwtTokenProperty);
                            jwtToken = null;
                        }
                    } else {
                        // session expire very fast
                        response = createPostRequest(br, "https://middleware.astraweb.com/subscription/getSubscriptionsForUser?XDEBUG_SESSION_START=PHPSTORM", jwtToken, null);
                        if (response.containsKey("errorCode")) {
                            switch (((Number) response.get("errorCode")).intValue()) {
                            case 1002:
                                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                            case 1014:
                                // {"function":"..........","errorType":"ExpiredException","errorCode":1014}
                                account.removeProperty(jwtTokenProperty);
                                jwtToken = null;
                                break;
                            default:
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Response:" + response.toString());
                            }
                        } else if (response.isEmpty()) {
                            account.removeProperty(jwtTokenProperty);
                            jwtToken = null;
                        }
                    }
                }
                if (jwtToken == null) {
                    final String userName = account.getUser();
                    br.getPage("https://www.astraweb.com/login?redirect=%2Fmember");
                    final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LcXsloaAAAAAHEnpDmiuFfOw61pHmms9Wt_aH0x") {
                        @Override
                        public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                            return TYPE.INVISIBLE;
                        }
                    };
                    final Map<String, Object> loginJSON = new HashMap<String, Object>();
                    loginJSON.put("brandName", "astraweb");
                    loginJSON.put("username", userName);
                    loginJSON.put("password", account.getPass());
                    loginJSON.put("reCaptchaToken", rc2.getToken());
                    final Browser brc = br.cloneBrowser();
                    final PostRequest loginRequest = brc.createJSonPostRequest("https://middleware.astraweb.com/user/authenticate?XDEBUG_SESSION_START=PHPSTORM", JSonStorage.toString(loginJSON));
                    loginRequest.getHeaders().put("Origin", "https://www.astraweb.com");
                    loginRequest.getHeaders().put("Referer", "https://www.astraweb.com");
                    final String responseString = brc.getPage(loginRequest);
                    response = restoreFromString(responseString, TypeRef.MAP);
                    if (response.containsKey("errorCode")) {
                        switch (((Number) response.get("errorCode")).intValue()) {
                        case 1002:
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        default:
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Response:" + response.toString());
                        }
                    } else if (!response.containsKey("user") || !response.containsKey(jwtTokenProperty)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        jwtToken = (String) response.get(jwtTokenProperty);
                        if (jwtToken == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else {
                            account.setProperty(jwtTokenProperty, jwtToken);
                        }
                        final String username = (String) JavaScriptEngineFactory.walkJson(response, "user/meta_data/username");
                        if (StringUtils.isEmpty(username)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else {
                            account.setProperty(USENET_USERNAME, username.trim());
                        }
                        response = createPostRequest(br, "https://middleware.astraweb.com/subscription/getSubscriptionsForUser?XDEBUG_SESSION_START=PHPSTORM", jwtToken, null);
                    }
                }
                final Number subscription_id = (Number) response.get("subscription_id");
                if (subscription_id == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String status = (String) response.get("status");
                final Number threads = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(response, "current_package/meta_data/threads"), 1);
                // https://www.astraweb.com/, 50
                account.setMaxSimultanDownloads(threads.intValue());
                if (StringUtils.equalsIgnoreCase("Unlimited", (String) JavaScriptEngineFactory.walkJson(response, "current_package/meta_data/bandwidth"))) {
                    ai.setUnlimitedTraffic();
                } else {
                    // not yet supported
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String package_identifier = (String) JavaScriptEngineFactory.walkJson(response, "current_package/package_identifier");
                ai.setStatus(package_identifier);
                if (StringUtils.containsIgnoreCase(package_identifier, "BLOCK")) {
                    // TODO: traffic left
                    // https://middleware.astraweb.com/subscription/subscriptionCallSpi?XDEBUG_SESSION_START=PHPSTORM
                    // {brandName":"astraweb","subscriptionId":number,"reCaptchaToken":...
                    // {"used":"203417977305","allowed":"798199167657","remaining":594781190352,"success":true}
                    // unlimited, prepaid traffic
                } else if (!StringUtils.equalsIgnoreCase(status, "running")) {
                    // TODO check expire date
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    // TODO: expire date
                    // https://middleware.astraweb.com/billing/getBillingEventsForUser?XDEBUG_SESSION_START=PHPSTORM
                    // [{"billing_event_id":xyz,"billing_method_name":"Creditcard","currency_code":"USD","amount":35.88,"created_at":"2020-09-09T10:01:31+00:00","status":"success","description":"12 Months"}
                }
                account.setRefreshTimeout(5 * 60 * 60 * 1000l);
                ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                return ai;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.removeProperty(jwtTokenProperty);
                }
                throw e;
            }
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.astraweb.com", new int[] { 119, 23, 1818, 8080 }, new int[] { 563, 443 }));
        ret.addAll(UsenetServer.createServerList("us.astraweb.com", new int[] { 119, 23, 1818, 8080 }, new int[] { 563, 443 }));
        ret.addAll(UsenetServer.createServerList("eu.astraweb.com", new int[] { 119, 23, 1818, 8080 }, new int[] { 563, 443 }));
        ret.addAll(UsenetServer.createServerList("news6.astraweb.com", new int[] { 119, 23, 1818, 8080 }, new int[] { 563, 443 }));
        ret.addAll(UsenetServer.createServerList("us6.astraweb.com", new int[] { 119, 23, 1818, 8080 }, new int[] { 563, 443 }));
        ret.addAll(UsenetServer.createServerList("eu6.astraweb.com", new int[] { 119, 23, 1818, 8080 }, new int[] { 563, 443 }));
        return ret;
    }
}
