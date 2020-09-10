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

    private Map<String, Object> createPostRequest(Browser br, final String url, final String jwtToken) throws IOException {
        final Map<String, Object> json = new HashMap<String, Object>();
        json.put("brandName", "astraweb");
        json.put("userId", jwtToken);
        final PostRequest postRequest = br.createJSonPostRequest(url, JSonStorage.toString(json));
        postRequest.getHeaders().put("Origin", "https://www.astraweb.com");
        postRequest.getHeaders().put("Authorization", "Bearer " + jwtToken);
        final Browser brc = br.cloneBrowser();
        final String response = brc.getPage(postRequest);
        return JSonStorage.restoreFromString(response, TypeRef.HASHMAP);
    }

    private final String jwtTokenProperty = "jwt_token";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (account) {
            final AccountInfo ai = new AccountInfo();
            br.setFollowRedirects(true);
            String jwtToken = account.getStringProperty(jwtTokenProperty, null);
            Map<String, Object> response = null;
            try {
                if (jwtToken != null) {
                    response = createPostRequest(br, "https://middleware.astraweb.com/subscription/getSubscriptionsForUser?XDEBUG_SESSION_START=PHPSTORM", jwtToken);
                    if (!StringUtils.equalsIgnoreCase("running", (String) response.get("status"))) {
                        account.removeProperty(jwtTokenProperty);
                        jwtToken = null;
                    }
                }
                if (jwtToken == null) {
                    final String userName = account.getUser();
                    br.getPage("https://www.astraweb.com/login?redirect=%2Fmember");
                    final Map<String, Object> loginJSON = new HashMap<String, Object>();
                    loginJSON.put("brandName", "astraweb");
                    loginJSON.put("username", userName);
                    loginJSON.put("password", account.getPass());
                    final Browser brc = br.cloneBrowser();
                    final PostRequest loginRequest = brc.createJSonPostRequest("https://middleware.astraweb.com/user/authenticate?XDEBUG_SESSION_START=PHPSTORM", JSonStorage.toString(loginJSON));
                    final String responseString = brc.getPage(loginRequest);
                    response = JSonStorage.restoreFromString(responseString, TypeRef.HASHMAP);
                    if (response.containsKey("errorCode")) {
                        switch (((Number) response.get("errorCode")).intValue()) {
                        case 1002:
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        default:
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
                        if (username == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else {
                            account.setProperty(USENET_USERNAME, username.trim());
                        }
                        response = createPostRequest(br, "https://middleware.astraweb.com/subscription/getSubscriptionsForUser?XDEBUG_SESSION_START=PHPSTORM", jwtToken);
                        if (!StringUtils.equalsIgnoreCase("running", (String) response.get("status"))) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                final int threads = (int) JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(response, "current_package/meta_data/threads"), 1);
                account.setMaxSimultanDownloads(threads);
                if (StringUtils.equalsIgnoreCase("Unlimited", (String) JavaScriptEngineFactory.walkJson(response, "current_package/meta_data/bandwidth"))) {
                    ai.setUnlimitedTraffic();
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // TODO: expire date
                // https://middleware.astraweb.com/billing/getBillingEventsForUser?XDEBUG_SESSION_START=PHPSTORM
                // [{"billing_event_id":xyz,"billing_method_name":"Creditcard","currency_code":"USD","amount":35.88,"created_at":"2020-09-09T10:01:31+00:00","status":"success","description":"12 Months"}
                account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 5 * 60 * 60 * 1000l);
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
        ret.addAll(UsenetServer.createServerList("news.astraweb.com", false, 119, 23, 1818, 8080));
        ret.addAll(UsenetServer.createServerList("us.news.astraweb.com", false, 119, 23, 1818, 8080));
        ret.addAll(UsenetServer.createServerList("eu.news.astraweb.com", false, 119, 23, 1818, 8080));
        ret.addAll(UsenetServer.createServerList("ssl.astraweb.com", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("ssl-eu.astraweb.com", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("ssl-us.astraweb.com", true, 563, 443));
        return ret;
    }
}
