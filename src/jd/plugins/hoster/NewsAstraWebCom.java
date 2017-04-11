package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.accountchecker.AccountCheckerThread;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "news.astraweb.com" }, urls = { "" }) public class NewsAstraWebCom extends UseNet {
    public NewsAstraWebCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.news.astraweb.com/signup.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.news.astraweb.com/aup.html";
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface NewsAstraWebComConfigInterface extends UsenetAccountConfigInterface {
    };

    private final String EXPIREDCOOKIE = "expired";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        try {
            Form login = null;
            if (cookies != null) {
                final Cookie expiredCookie = cookies.get(EXPIREDCOOKIE);
                if (expiredCookie != null && "true".equalsIgnoreCase(expiredCookie.getValue())) {
                    if (AccountCheckerThread.isForced() == false && account.getError() == null) {
                        cookies.add(new Cookie(getHost(), EXPIREDCOOKIE, "true"));
                        account.saveCookies(cookies, "");
                        return account.getAccountInfo();
                    } else {
                        cookies.remove(expiredCookie);
                    }
                }
                br.setCookies(getHost(), cookies);
                br.getPage("http://www.news.astraweb.com/members_v2/viewdetails.cgi");
                login = br.getFormbyActionRegex("viewdetails.cgi");
                if (login != null && login.containsHTML("user") && login.containsHTML("pass")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "astralogin") == null) {
                    br.getCookies(getHost()).clear();
                } else if (br.containsHTML("Session expired")) {
                    if (AccountCheckerThread.isForced() == false && account.getError() == null) {
                        cookies.add(new Cookie(getHost(), EXPIREDCOOKIE, "true"));
                        account.saveCookies(cookies, "");
                        return account.getAccountInfo();
                    }
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "astralogin") == null) {
                account.clearCookies("");
                final String userName = account.getUser();
                br.getPage("http://www.news.astraweb.com/members_v2.cgi");
                login = br.getFormbyActionRegex("viewdetails.cgi");
                login.put("user", Encoding.urlEncode(userName));
                login.put("pass", Encoding.urlEncode(account.getPass()));
                if (login.containsHTML("encrypted")) {
                    final String captcha = br.getRegex("img src=\"(https?://customers\\.astraweb\\.com/captcha/index\\.cgi.*?)\"").getMatch(0);
                    if (captcha != null) {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", getHost(), null, true);
                        final String code = getCaptchaCode(captcha, dummyLink);
                        if (StringUtils.isEmpty(code)) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        } else {
                            login.put("reply", Encoding.urlEncode(code));
                        }
                    }
                }
                br.submitForm(login);
                login = br.getFormbyActionRegex("viewdetails.cgi");
                if (login != null && login.containsHTML("user") && login.containsHTML("pass")) {
                    if (br.containsHTML("The verification words you entered did not match")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "The verification words you entered did not match", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "astralogin") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String userName = br.getRegex("Username:</font>.*?<font.*?>(.*?)</font").getMatch(0);
            if (userName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(USENET_USERNAME, userName.trim());
            }
            final String accountStatus = br.getRegex("Account Status:</font>.*?<font.*?>(.*?)</font").getMatch(0);
            // final String bytesDownloaded = br.getRegex("Bytes Downloaded:</font>.*?<font.*?>(.*?)</font").getMatch(0);
            final String downloadsLeft = br.getRegex("Downloads Left:</font>.*?<font.*?>(.*?)</font").getMatch(0);
            final String packageType = br.getRegex("Your Account:</font>.*?<font.*?>(.*?)</font").getMatch(0);
            final String maxConnections = br.getRegex("Maximum Connections:.*?\\s*(\\d+)\\s*connections").getMatch(0);
            if (maxConnections != null) {
                account.setMaxSimultanDownloads(Integer.parseInt(maxConnections));
            } else {
                account.setMaxSimultanDownloads(1);
            }
            if (packageType != null) {
                ai.setStatus(packageType);
            } else {
                ai.setStatus("Unknown Type");
            }
            if (!"Active".equals(accountStatus)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account Status: " + accountStatus, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (StringUtils.containsIgnoreCase(downloadsLeft, "unlimited")) {
                ai.setUnlimitedTraffic();
            } else {
                final String left = new Regex(downloadsLeft, "\\((.*?)\\)").getMatch(0);
                ai.setTrafficLeft(left);
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.astraweb.com", false, 119, 23, 1818, 8080));
        ret.addAll(UsenetServer.createServerList("eu.news.astraweb.com", false, 119, 23, 1818, 8080));
        ret.addAll(UsenetServer.createServerList("us.news.astraweb.com", false, 119, 23, 1818, 8080));
        ret.addAll(UsenetServer.createServerList("ssl.astraweb.com", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("ssl-eu.astraweb.com", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("ssl-us.astraweb.com", true, 563, 443));
        return ret;
    }
}
