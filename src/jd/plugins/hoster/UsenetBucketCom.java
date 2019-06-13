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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "usenetbucket.com" }, urls = { "" })
public class UsenetBucketCom extends UseNet {
    public UsenetBucketCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.usenetbucket.com/de/order/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.usenetbucket.com/de/legal/terms-and-conditions/";
    }

    private final String USENET_USERNAME = "USENET_USERNAME";
    private final String USENET_PASSWORD = "USENET_PASSWORD";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    @Override
    protected String getPassword(Account account) {
        return account.getStringProperty(USENET_PASSWORD, account.getUser());
    }

    public static interface UsenetBucketConfigInterface extends UsenetAccountConfigInterface {
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
                br.getPage("https://member.usenetbucket.com/");
                login = br.getForm(0);
                if (login != null && login.containsHTML("name=\"email\"") && login.containsHTML("name=\"password\"")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getHostCookie("laravel_session", Cookies.NOTDELETEDPATTERN) == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "laravel_session", Cookies.NOTDELETEDPATTERN) == null) {
                account.clearCookies("");
                final String userName = account.getUser();
                if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail/password for usenetbucket.com website!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("https://member.usenetbucket.com/login");
                login = br.getForm(0);
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("email", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getForm(0);
                if (login != null && login.containsHTML("name=\"email\"") && login.containsHTML("name=\"password\"")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getHostCookie("laravel_session", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (!StringUtils.equalsIgnoreCase(br.getURL(), "https://member.usenetbucket.com/")) {
                br.getPage("https://member.usenetbucket.com/");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String userName = br.getRegex("<div>\\s*(?:Username|Gebruikersnaam)\\s*:\\s*(.*?)\\s*<").getMatch(0);
            final String passWord = br.getRegex("<div>\\s*(?:Password|Wachtwoord)\\s*:\\s*(.*?)\\s*<").getMatch(0);
            if (userName == null || passWord == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(USENET_USERNAME, userName);
                account.setProperty(USENET_PASSWORD, passWord);
            }
            final String connections = br.getRegex("<div>\\s*Connections\\s*:\\s*(.*?)</div>").getMatch(0);
            if (connections != null) {
                account.setMaxSimultanDownloads(Integer.parseInt(connections));
            } else {
                account.setMaxSimultanDownloads(25);
            }
            final String validUntil = br.getRegex("<div>\\s*(?:Valid until|Geldig tot)\\s*:\\s*(.*?)</div>").getMatch(0);
            final String daysRemaining = br.getRegex("(\\d+) (?:days remaining|dagen resterend)").getMatch(0);
            final String bucketType = br.getRegex("\\s*((Basic|Comfort|Ultimate|Free) Bucket)").getMatch(0);
            if (bucketType != null) {
                ai.setStatus(bucketType);
            } else {
                ai.setStatus("Unknown Bucket");
            }
            if (daysRemaining != null) {
                ai.setValidUntil(System.currentTimeMillis() + (Integer.parseInt(daysRemaining) * (24 * 60 * 60 * 1000l)));
            }
            if (validUntil != null) {
                final long until = TimeFormatter.getMilliSeconds(validUntil, "dd' 'MMM' 'yyyy', at'HH:mm:ss", null);
                if (until > 0) {
                    ai.setValidUntil(until);
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
        ret.addAll(UsenetServer.createServerList("reader.usenetbucket.com", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("reader.usenetbucket.com", true, 563, 443));
        return ret;
    }
}
