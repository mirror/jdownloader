package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision: 38215 $", interfaceVersion = 3, names = { "xsnews.nl" }, urls = { "" })
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
            if (cookie.getKey().startsWith("PHPSESSID") && !"deleted".equals(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    public static interface XsNewsNlConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        try {
            Form login = null;
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                br.getPage("https://www.xsnews.nl/myxsnews/member.php?lang=en");
                login = br.getFormbyActionRegex(".*myxsnews/profile.php.*");
                if (login != null) {
                    br.getCookies(getHost()).clear();
                } else if (!containsSessionCookie(br)) {
                    br.getCookies(getHost()).clear();
                } else if (!br.containsHTML("Your Subscriptions")) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (!containsSessionCookie(br)) {
                account.clearCookies("");
                br.setCookie(getHost(), "lang", "en");
                br.getPage("https://www.xsnews.nl/myxsnews/profile.php?lang=enl");
                login = br.getFormbyActionRegex(".*myxsnews/profile.php.*");
                login.put("amember_login", Encoding.urlEncode(account.getUser()));
                login.put("amember_pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex(".*myxsnews/member.php");
                if (login != null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (!containsSessionCookie(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            if (!StringUtils.endsWithCaseInsensitive(br.getURL(), "/myxsnews/member.php?lang=en")) {
                br.getPage("https://www.xsnews.nl/myxsnews/member.php?lang=en");
            }
            final String currentSubscription = br.getRegex("<b>Product:</b>\\s*(.*?)\\s*<").getMatch(0);
            final String threads = br.getRegex("<b>Threads:</b>\\s*(\\d+)\\d*<").getMatch(0);
            if (currentSubscription != null && threads != null) {
                account.setType(Account.AccountType.PREMIUM);
                account.setMaxSimultanDownloads(Integer.parseInt(threads));
                ai.setStatus(currentSubscription);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            br.getPage("https://www.xsnews.nl/myxsnews/member.php?tab=payment_history&lang=en");
            final String validUntil = br.getRegex("<td nowrap=\"nowrap\">\\s*\\d+-\\d+-\\d+\\s*</td>\\s*<td nowrap=\"nowrap\">\\s*(\\d+-\\d+-\\d+)\\s*<").getMatch(0);
            if (validUntil != null) {
                final long date = TimeFormatter.getMilliSeconds(validUntil, "dd'-'MM'-'yyyy", Locale.ENGLISH);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                }
            }
            ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
            account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 5 * 60 * 60 * 1000l);
            return ai;
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.xsnews.nl", false, 80, 119));
        ret.addAll(UsenetServer.createServerList("reader.xsnews.nl", true, 563, 443));
        return ret;
    }
}