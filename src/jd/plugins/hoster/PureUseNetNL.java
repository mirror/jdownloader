package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "pureusenet.nl" }, urls = { "" })
public class PureUseNetNL extends UseNet {
    public PureUseNetNL(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pureusenet.nl/en/packages");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USENET, LazyPlugin.FEATURE.USERNAME_IS_EMAIL };
    }

    private final String PROPERTY_USENET_USERNAME = "usenet_username";

    public static interface XPureUseNetNLAccountConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public String getAGBLink() {
        return "https://www.pureusenet.nl/en/terms-and-conditions";
    }

    private boolean containsSessionCookie(Browser br) {
        final Cookies cookies = br.getCookies(getHost());
        for (final Cookie cookie : cookies.getCookies()) {
            if (cookie.getKey().startsWith("auth-token") && !"deleted".equals(cookie.getValue())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        Form login = null;
        if (cookies != null) {
            logger.info("Checking login cookies...");
            br.setCookies(getHost(), cookies);
            br.getPage("https://www." + this.getHost() + "/en/member");
            login = br.getFormbyActionRegex(".*/login");
            if (login != null && login.containsHTML("name=\"password\"")) {
                logger.info("Cookie login successful");
                br.getCookies(getHost()).clear();
            } else if (!containsSessionCookie(br)) {
                logger.info("Cookie login failed");
                br.getCookies(getHost()).clear();
            }
        }
        if (!containsSessionCookie(br)) {
            logger.info("Performing full login");
            account.clearCookies("");
            final String userName = account.getUser();
            if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                throw new AccountInvalidException("Please enter your e-mail address into the username field!");
            }
            br.getPage("https://www." + this.getHost() + "/en/login");
            login = br.getFormbyActionRegex(".*/login");
            login.put("email", Encoding.urlEncode(userName));
            login.put("password", Encoding.urlEncode(account.getPass()));
            br.submitForm(login);
            login = br.getFormbyActionRegex(".*/login");
            if (login != null && login.containsHTML("name=\"password\"")) {
                throw new AccountInvalidException();
            } else if (!containsSessionCookie(br)) {
                throw new AccountInvalidException();
            }
        }
        if (!StringUtils.containsIgnoreCase(br.getURL(), "/en/member")) {
            br.getPage("/en/member");
        }
        account.saveCookies(br.getCookies(getHost()), "");
        final String dedicatedUsenetUsername = br.getRegex(">\\s*([^<]+)\\s*</div>\\s*<a href=\"/[a-z]+/member/settings\"").getMatch(0);
        if (dedicatedUsenetUsername == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.equalsIgnoreCase(dedicatedUsenetUsername, account.getUser())) {
            /* dedicatedUsenetUsername == username of account -> No need to store it as a property. */
            account.removeProperty(PROPERTY_USENET_USERNAME);
        } else {
            logger.info("Found dedicated Usenet username which differs from the username of this account: " + dedicatedUsenetUsername);
            account.setProperty(PROPERTY_USENET_USERNAME, dedicatedUsenetUsername);
        }
        String packageType = br.getRegex("(?i)<h3>Your package:\\s*<span class=\"font-primary.*?\">(.*?)<").getMatch(0);
        String connections = br.getRegex("<li><b>Connections:</b>\\s*(\\d+)\\s*<").getMatch(0);
        if (connections == null) {
            /* 2022-11-22 */
            connections = br.getRegex("(?i)>\\s*Connections\\s*</h5>\\s*<div class=\"server-settings__value\">\\s*(\\d+)").getMatch(0);
        }
        final boolean isPremium = br.containsHTML("(?i)>\\s*Auto Renew Status\\s*:\\s*</h5>\\s*<div class=\"server-settings__value\">\\s*<b class=\"server-settings__value-b\">\\s*Active");
        if (packageType == null && isPremium) {
            packageType = "Auto Renew: Active";
        }
        if (packageType != null) {
            if (connections != null) {
                account.setMaxSimultanDownloads(Integer.parseInt(connections));
            } else {
                if (StringUtils.containsIgnoreCase(packageType, "XS")) {
                    // Pure XS = 4 Connections
                    account.setMaxSimultanDownloads(4);
                } else if (StringUtils.containsIgnoreCase(packageType, "S")) {
                    // Pure S = 4 Connections
                    account.setMaxSimultanDownloads(4);
                } else if (StringUtils.containsIgnoreCase(packageType, "M")) {
                    // Pure M = 8 Connections
                    account.setMaxSimultanDownloads(8);
                } else if (StringUtils.containsIgnoreCase(packageType, "XXL")) {
                    // Pure XXL = 12 Connections
                    account.setMaxSimultanDownloads(12);
                } else if (StringUtils.containsIgnoreCase(packageType, "XL")) {
                    // Pure XL = 12 Connections
                    account.setMaxSimultanDownloads(12);
                } else if (StringUtils.containsIgnoreCase(packageType, "L")) {
                    // Pure L = 8 Connections
                    account.setMaxSimultanDownloads(8);
                } else {
                    // 5 connections(fallback)
                    account.setMaxSimultanDownloads(5);
                }
            }
            packageType += " | Connections: " + account.getMaxSimultanDownloads();
            ai.setStatus(packageType);
        } else {
            ai.setStatus("Unknown packageType! Please contact JDownloader support at support@jdownloader.org");
            if (connections != null) {
                account.setMaxSimultanDownloads(Integer.parseInt(connections));
            } else {
                // 5 connections(fallback)
                account.setMaxSimultanDownloads(5);
            }
        }
        String expireDate = br.getRegex("(?i)<li><b>Expiration date:</b>\\s*(\\d{4}-\\d{2}-\\d{2})\\s*<").getMatch(0);
        if (expireDate == null) {
            /* 2020-01-21 */
            expireDate = br.getRegex(">Next Billing:</b>\\s*(\\d{4}-\\d{2}-\\d{2})\\s*</li>").getMatch(0);
        }
        if (expireDate == null) {
            /* 2020-01-21 - wide open RegEx --> Last chance */
            expireDate = br.getRegex("(\\d{4}-\\d{2}-\\d{2})").getMatch(0);
        }
        account.setRefreshTimeout(2 * 60 * 60 * 1000l);
        ai.setMultiHostSupport(this, Arrays.asList(new String[] { "usenet" }));
        if (expireDate != null) {
            /* Premium account with expiredate */
            account.setType(AccountType.PREMIUM);
            final long date = TimeFormatter.getMilliSeconds(expireDate, "yyyy'-'MM'-'dd", null);
            if (date > 0) {
                /* Add one extra day */
                ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                return ai;
            }
        }
        if (isPremium) {
            /* Auto Renew premium without known expire-date. */
            account.setType(AccountType.PREMIUM);
            return ai;
        } else {
            /* Free account or plugin failure */
            throw new AccountInvalidException("Unsupported account type 'Free' or this plugin is broken");
        }
    }

    @Override
    protected String getUseNetUsername(final Account account) {
        final String dedicatedUsenetUsername = account.getStringProperty(PROPERTY_USENET_USERNAME);
        if (dedicatedUsenetUsername != null) {
            return dedicatedUsenetUsername;
        } else {
            return account.getUser();
        }
    }

    @Override
    protected String getUseNetPassword(Account account) {
        return account.getPass();
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.pureusenet.nl", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("news.pureusenet.nl", true, 563, 443));
        return ret;
    }
}
