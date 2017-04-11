package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "thundernews.com" }, urls = { "" }) public class ThunderNewsCom extends UseNet {
    public ThunderNewsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.thundernews.com/signup.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.thundernews.com/terms.php";
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface ThunderNewsConfigInterface extends UsenetAccountConfigInterface {
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
                br.getPage("https://www.thundernews.com/members.php");
                login = br.getFormbyActionRegex("thundernews\\.com/memlogin\\.php");
                if (login != null && login.containsHTML("memberid") && login.containsHTML("password")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "PHPSESSID") == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "PHPSESSID") == null) {
                account.clearCookies("");
                br.getPage("https://www.thundernews.com/login.php");
                login = br.getFormbyActionRegex("thundernews\\.com/memlogin\\.php");
                login.put("memberid", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                final String equation[] = login.getRegex(">\\s*(\\d+)\\s*<span.*?>(\\+|-|\\*|/)</span>\\s*(\\d+)\\s*=\\s*<").getRow(0);
                if (equation == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Integer result;
                if ("+".equals(equation[1])) {
                    result = Integer.parseInt(equation[0]) + Integer.parseInt(equation[2]);
                } else if ("*".equals(equation[1])) {
                    result = Integer.parseInt(equation[0]) * Integer.parseInt(equation[2]);
                } else if ("/".equals(equation[1])) {
                    result = Integer.parseInt(equation[0]) / Integer.parseInt(equation[2]);
                } else {
                    result = Integer.parseInt(equation[0]) - Integer.parseInt(equation[2]);
                }
                login.put("captcha_code", result.toString());
                br.submitForm(login);
                login = br.getFormbyActionRegex("thundernews\\.com/memlogin\\.php");
                if (login != null && login.containsHTML("memberid") && login.containsHTML("password")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie(getHost(), "PHPSESSID") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("https://www.thundernews.com/members.php");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String accountStatus = br.getRegex(">Account status:.*?<span>(.*?)</span").getMatch(0);
            if (!StringUtils.equalsIgnoreCase(accountStatus, "Active")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account status: " + accountStatus, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String userName = br.getRegex("<p>User id:\\s*<span>(.*?)</span></p>").getMatch(0);
            if (userName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(USENET_USERNAME, userName);
            }
            final String plan = br.getRegex("<td>Plan:</td>.*?<td class=\"info\">(.*?)</td>").getMatch(0);
            if (plan != null) {
                ai.setStatus("Plan: " + plan);
            } else {
                ai.setStatus("Unknown plan");
            }
            final String connections = br.getRegex("<td>Connections:</td>.*?<td class=\"info\">(\\d+)&nbsp;Connections</td>").getMatch(0);
            if (connections != null) {
                account.setMaxSimultanDownloads(Integer.parseInt(connections));
            } else {
                account.setMaxSimultanDownloads(25);
            }
            final String nextInvoice = br.getRegex("<td>Next Invoice:</td>.*?<td class=\"info\">(\\d+/\\d+/\\d+)</td>").getMatch(0);// month/day/year
            if (nextInvoice != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(nextInvoice, "MM/dd/yyyy", Locale.ENGLISH) + (24 * 60 * 60 * 1000l));
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
        ret.addAll(UsenetServer.createServerList("eu.thundernews.com", false, 119, 23, 443, 3128, 7000, 8000, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("us.thundernews.com", false, 119, 23, 443, 3128, 7000, 8000, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("secure.eu.thundernews.com", true, 563, 80, 81));
        ret.addAll(UsenetServer.createServerList("secure.us.thundernews.com", true, 563, 80, 81));
        ret.addAll(UsenetServer.createServerList("secure.us.thundernews.com", true, 563, 80, 81));
        return ret;
    }
}
