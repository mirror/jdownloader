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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sunnyusenet.com" }, urls = { "" })
public class SunnyUsenetCom extends UseNet {

    public SunnyUsenetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.sunnyusenet.com/en/packages");
    }

    @Override
    public String getAGBLink() {
        return "https://www.sunnyusenet.com/en/terms-and-conditions";
    }

    public static interface SunnyUsenetComConfigInterface extends UsenetAccountConfigInterface {
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
                br.getPage("https://www.sunnyusenet.com/en");
                br.getPage("https://www.sunnyusenet.com/en/login");
                login = br.getFormbyActionRegex(".*/login");
                if (login != null && (login.containsHTML("email") || login.containsHTML("password"))) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "auth-token") == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "auth-token") == null) {
                account.clearCookies("");
                final String userName = account.getUser();
                if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail/password for sunnyusenet.com website!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("https://www.sunnyusenet.com/en");
                br.getPage("https://www.sunnyusenet.com/en/login");
                login = br.getFormbyActionRegex(".*/login");
                login.put("email", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex(".*/login");
                if (login != null && (login.containsHTML("email") || login.containsHTML("password"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "auth-token") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String yourPackage = br.getRegex("Your package:\\s*<span.*?>\\s*(Sunny.*?)\\s*<").getMatch(0);
            final String expireDate = br.getRegex("Expiration date:\\s*</b>\\s*(\\d+-\\d+-\\d+)\\s*<").getMatch(0);
            final String connections = br.getRegex("Connections:\\s*</b>\\s*(\\d+)\\s*<").getMatch(0);
            final int packageConnections;
            if (StringUtils.containsIgnoreCase(yourPackage, "UNL")) {
                packageConnections = 20;
            } else if (StringUtils.containsIgnoreCase(yourPackage, "90")) {
                packageConnections = 15;
            } else if (StringUtils.containsIgnoreCase(yourPackage, "60")) {
                packageConnections = 15;
            } else if (StringUtils.containsIgnoreCase(yourPackage, "30")) {
                packageConnections = 10;
            } else if (StringUtils.containsIgnoreCase(yourPackage, "20")) {
                packageConnections = 10;
            } else if (StringUtils.containsIgnoreCase(yourPackage, "10")) {
                packageConnections = 5;
            } else if (StringUtils.containsIgnoreCase(yourPackage, "5")) {
                packageConnections = 5;
            } else {
                packageConnections = 1;
            }
            if (connections != null) {
                account.setMaxSimultanDownloads(Math.min(Integer.parseInt(connections), packageConnections));
            } else {
                account.setMaxSimultanDownloads(packageConnections);
            }
            ai.setStatus("Your package: " + yourPackage);
            if (expireDate != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy'-'MM'-'dd", Locale.ENGLISH) + (24 * 60 * 60 * 1000l));
            } else {
                ai.setExpired(true);
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 5 * 60 * 60 * 1000l);
        return ai;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.sunnyusenet.com", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("news.sunnyusenet.com", true, 563, 443));
        return ret;
    }
}
