package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "usenetnow.net" }, urls = { "" }) public class UsenetNow extends UseNet {
    public UsenetNow(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://billing.usenetnow.net/signup");
    }

    @Override
    public String getAGBLink() {
        return "http://usenetnow.net/dmca.html";
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface UsenetNowConfigInterface extends UsenetAccountConfigInterface {
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
                br.getPage("https://billing.usenetnow.net/login");
                login = br.getFormbyActionRegex("/login/form/");
                if (login != null && login.containsHTML("amember_login") && login.containsHTML("amember_pass")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "PHPSESSID") == null || br.getCookie(getHost(), "amember_nr") == null) {
                    br.getCookies(getHost()).clear();
                } else if (br.containsHTML("<li>The user name or password is incorrect</li>")) {
                    br.getCookies(getHost()).clear();
                } else {
                    if (!StringUtils.containsIgnoreCase(br.getURL(), "/member")) {
                        // switch to english
                        br.getPage("https://billing.usenetnow.net/member");
                    }
                }
            }
            if (br.getCookie(getHost(), "PHPSESSID") == null || br.getCookie(getHost(), "amember_nr") == null) {
                account.clearCookies("");
                final String userName = account.getUser();
                br.getPage("https://billing.usenetnow.net/login");
                login = br.getFormbyActionRegex("/login");
                login.put("amember_login", Encoding.urlEncode(userName));
                login.put("amember_pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex("/login/form/");
                if (login != null && login.containsHTML("amember_login") && login.containsHTML("amember_pass")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.containsHTML("<li>The user name or password is incorrect</li>")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "PHPSESSID") == null || br.getCookie(getHost(), "amember_nr") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (!StringUtils.containsIgnoreCase(br.getURL(), "/member")) {
                // switch to english
                br.getPage("https://billing.usenetnow.net/member");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String userName = br.getRegex("<div class=\"am-user-identity-block\">(.*?)<a").getMatch(0);
            if (userName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(USENET_USERNAME, userName.trim());
            }
            account.setMaxSimultanDownloads(50);
            final String validUntil = br.getRegex("member-subscriptions\">.*?<li><strong>.*?</strong>.*?expires (\\d+/\\d+/\\d+)").getMatch(0);
            final String bucketType = br.getRegex("member-subscriptions\">.*?<li><strong>(.*?)</").getMatch(0);
            if (bucketType != null) {
                ai.setStatus(bucketType);
            } else {
                ai.setStatus("Unknown Bucket");
            }
            if (validUntil != null) {
                final long date = TimeFormatter.getMilliSeconds(validUntil, "MM/dd/yy", null);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                }
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
        ret.addAll(UsenetServer.createServerList("usnews.usenetnow.net", false, 23, 119, 2000, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("usnews.usenetnow.net", true, 443, 563, 5563));
        ret.addAll(UsenetServer.createServerList("eunews.usenetnow.net", false, 23, 119, 2000, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("eunews.usenetnow.net", true, 443, 563, 5563));
        ret.addAll(UsenetServer.createServerList("eunews2.usenetnow.net", false, 23, 119, 2000, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("eunews2.usenetnow.net", true, 443, 563, 5563));
        return ret;
    }
}
