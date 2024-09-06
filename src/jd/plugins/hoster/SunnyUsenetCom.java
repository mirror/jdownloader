package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "sunnyusenet.com" }, urls = { "" })
public class SunnyUsenetCom extends UseNet {
    public SunnyUsenetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.sunnyusenet.com/en/packages");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USENET, LazyPlugin.FEATURE.USERNAME_IS_EMAIL };
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
                br.getPage("https://www." + this.getHost() + "/en");
                br.getPage("/en/login");
                login = br.getFormbyActionRegex(".*/login");
                if (login != null && (login.containsHTML("email") || login.containsHTML("password"))) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "auth-token", Cookies.NOTDELETEDPATTERN) == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "auth-token", Cookies.NOTDELETEDPATTERN) == null) {
                logger.info("Performing full login");
                account.clearCookies("");
                final String userName = account.getUser();
                if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail address into the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("https://www." + this.getHost() + "/en");
                br.getPage("/en/login");
                login = br.getFormbyActionRegex(".*/login");
                login.put("email", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex(".*/login");
                if (login != null && (login.containsHTML("email") || login.containsHTML("password"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "auth-token", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                logger.info("Full login successful");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String yourPackage = br.getRegex("(?i)>\\s*Plan Type\\s*:\\s*</h5>\\s*<div class=\"server-settings__value\"><b\\s*class=\"server-settings__value-b\">([^<>\"]+)</b>").getMatch(0);
            final String expireDate = br.getRegex("(?i)Expiration date\\s*:\\s*</b>\\s*(\\d+-\\d+-\\d+)\\s*<").getMatch(0);
            final String connections = br.getRegex("(?i)>\\s*Connections\\s*</h5>\\s*<div class=\"server-settings__value\">\\s*(\\d+)").getMatch(0);
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
            /* 2020-02-25: They have packages without expire date e.g. "Sunny UNL" */
            if (expireDate != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy'-'MM'-'dd", Locale.ENGLISH) + (24 * 60 * 60 * 1000l));
            } else if (yourPackage == null || !StringUtils.containsIgnoreCase(yourPackage, "UNL")) {
                ai.setExpired(true);
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        ai.setMultiHostSupport(this, Arrays.asList(new String[] { "usenet" }));
        account.setRefreshTimeout(5 * 60 * 60 * 1000l);
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
