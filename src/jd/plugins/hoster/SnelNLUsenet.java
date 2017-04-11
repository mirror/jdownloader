package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "snelnl.com" }, urls = { "" }) public class SnelNLUsenet extends UseNet {
    public SnelNLUsenet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.snelnl.com/en/products");
    }

    @Override
    public String getAGBLink() {
        return "https://www.snelnl.com/en/general-terms";
    }

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface SnelNLUsenetConfigInterface extends UsenetAccountConfigInterface {
    };

    private boolean containsSessionCookie(final Cookies cookies) {
        for (final Cookie cookie : cookies.getCookies()) {
            if (StringUtils.startsWithCaseInsensitive(cookie.getKey(), "SSESS") && !StringUtils.equalsIgnoreCase(cookie.getValue(), "deleted")) {
                return true;
            }
        }
        return false;
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        final Cookies cookies = account.loadCookies("");
        br.setFollowRedirects(true);
        try {
            Form login = null;
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                br.getPage("https://www.snelnl.com/en/user/login");
                login = br.getFormbyActionRegex("/en/user/login\\?.*");
                if (login != null && login.containsHTML("name") && login.containsHTML("pass")) {
                    br.getCookies(getHost()).clear();
                } else if (!containsSessionCookie(br.getCookies(getHost()))) {
                    br.getCookies(getHost()).clear();
                } else {
                    if (!StringUtils.endsWithCaseInsensitive(br.getURL(), "en/user")) {
                        br.getPage("https://www.snelnl.com/en/user");
                    }
                }
            }
            if (!containsSessionCookie(br.getCookies(getHost()))) {
                account.clearCookies("");
                final String username = account.getUser();
                if (username == null || !username.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please use your email address to login", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.setCookie(getHost(), "language", "en");
                br.getPage("https://www.snelnl.com/en/user/login");
                login = br.getFormbyActionRegex("/en/user/login\\?.*");
                login.put("name", Encoding.urlEncode(username));
                login.put("pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex("/en/user/login\\?.*");
                if (login != null && login.containsHTML("name") && login.containsHTML("pass")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (!containsSessionCookie(br.getCookies(getHost()))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String userName = br.getRegex("item-title\">Username:</div>.*?item-text\">(.*?)<").getMatch(0);
            if (userName != null) {
                account.setProperty(USENET_USERNAME, userName);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String packageType = br.getRegex("item-title\">Account type:</div>.*?item-text\">(.*?)<").getMatch(0);
            if (packageType != null) {
                ai.setStatus(packageType);
                ai.setUnlimitedTraffic();
                final String endDate = br.getRegex("End date:</div>.*?item-text\">(.*?)<").getMatch(0);
                if (endDate == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                ai.setValidUntil(TimeFormatter.getMilliSeconds(endDate, "MMM dd yyyy' - 'HH:mm", Locale.ENGLISH));
                if (StringUtils.containsIgnoreCase(packageType, "slow")) {
                    account.setMaxSimultanDownloads(4);
                } else if (StringUtils.containsIgnoreCase(packageType, "basic")) {
                    account.setMaxSimultanDownloads(8);
                } else if (StringUtils.containsIgnoreCase(packageType, "fast")) {
                    account.setMaxSimultanDownloads(12);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Please contact JDownloader support");
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Please contact JDownloader support");
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
        ret.addAll(UsenetServer.createServerList("reader.snelnl.com", false, 119, 443, 8080));
        ret.addAll(UsenetServer.createServerList("reader.snelnl.com", true, 563, 80, 81));
        return ret;
    }
}
