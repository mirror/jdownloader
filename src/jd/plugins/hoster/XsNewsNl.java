package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xsnews.nl" }, urls = { "" })
public class XsNewsNl extends UseNet {
    public XsNewsNl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.xsnews.nl/en/products.html");
    }

    @Override
    public String getAGBLink() {
        return "https://www.xsnews.nl/en/terms-and-conditions.html";
    }

    private boolean containsSessionCookie(Browser br) {
        final Cookies cookies = br.getCookies(getHost());
        for (final Cookie cookie : cookies.getCookies()) {
            if (cookie.getKey().startsWith("sessid") && !"deleted".equals(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static interface XsNewsNlConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        synchronized (account) {
            setBrowserExclusive();
            final AccountInfo ai = new AccountInfo();
            br.setFollowRedirects(true);
            final Cookies cookies = account.loadCookies("");
            try {
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    br.setCurrentURL("https://www.xsnews.nl/en/myxsnews.html");
                    final GetRequest request = br.createGetRequest("https://www.xsnews.nl/action/auth/status?_=" + System.currentTimeMillis());
                    request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getPage(request);
                    if (!containsSessionCookie(br)) {
                        br.clearCookies(getHost());
                    } else if (!br.containsHTML("\"ok\"\\s*:\\s*true")) {
                        br.clearCookies(getHost());
                    }
                }
                if (!containsSessionCookie(br)) {
                    final AccountInfo oldai = account.getAccountInfo();
                    if (oldai != null && oldai.getLastValidUntil() != -1 && oldai.getLastValidUntil() > System.currentTimeMillis()) {
                        try {
                            logger.info("Usenet only login");
                            verifyUseNetLogins(account);
                            ai.setStatus(oldai.getStatus());
                            ai.setValidUntil(oldai.getLastValidUntil());
                            ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                            return ai;
                        } catch (InvalidAuthException e2) {
                        }
                    }
                    logger.info("Full login");
                    account.clearCookies("");
                    br.clearCookies(getHost());
                    br.setCookie(getHost(), "lang", "en");
                    br.getPage("https://www.xsnews.nl/en/myxsnews.html");
                    final CaptchaHelperHostPluginRecaptchaV2 captcha = new CaptchaHelperHostPluginRecaptchaV2(this, br, "6LcZur0UAAAAAKgw3J_fWyW6Dip32vp3QIQwtyGo") {
                        @Override
                        public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                            return TYPE.INVISIBLE;
                        }
                    };
                    final PostRequest login = br.createPostRequest("https://www.xsnews.nl/action/auth/login&lang=en", "g-recaptcha-response=" + Encoding.urlEncode(captcha.getToken()) + "&user=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                    login.getHeaders().put("Origin", "https://www.xsnews.nl");
                    login.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getPage(login);
                    if (br.containsHTML("No such user/pass")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if (!containsSessionCookie(br)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else if (!br.containsHTML("\"ok\"\\s*:\\s*true")) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                account.saveCookies(br.getCookies(getHost()), "");
                br.setCurrentURL("https://www.xsnews.nl/en/my-subscriptions.html");
                final GetRequest request = br.createGetRequest("https://www.xsnews.nl/action/acct/subr?_=" + System.currentTimeMillis());
                request.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.getPage(request);
                if (!br.containsHTML("\"ok\"\\s*:\\s*true")) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                    final Map<String, Object> active = (Map<String, Object>) response.get("active");
                    if (active == null || active.size() == 0) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "No active package", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        final long conns = JavaScriptEngineFactory.toLong(active.get("conns"), 1);
                        account.setMaxSimultanDownloads((int) conns);
                        final String title = (String) active.get("title");
                        ai.setStatus("Package: " + title);
                        final String expire_date = (String) active.get("expire_date");
                        if (expire_date != null) {
                            final long date = TimeFormatter.getMilliSeconds(expire_date, "yyyy'-'MM'-'dd", Locale.ENGLISH);
                            if (date > 0) {
                                ai.setValidUntil(date + (12 * 60 * 60 * 1000l));
                            }
                        }
                        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                        account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 5 * 60 * 60 * 1000l);
                        return ai;
                    }
                }
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.xsnews.nl", false, 80, 119));
        ret.addAll(UsenetServer.createServerList("reader.xsnews.nl", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("readeripv6.xsnews.nl", false, 80, 119));
        ret.addAll(UsenetServer.createServerList("readeripv6.xsnews.nl", true, 563, 443));
        return ret;
    }
}