package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "snelnl.com" }, urls = { "" })
public class SnelNLUsenet extends UseNet {
    public SnelNLUsenet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.snelnl.com/en/products");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        if (allowOnlyUserCookieLogin) {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USENET, LazyPlugin.FEATURE.COOKIE_LOGIN_ONLY };
        } else {
            return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USENET, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.snelnl.com/en/general-terms";
    }

    @Override
    protected String getUseNetUsername(final Account account) {
        return account.getStringProperty(PROPERTY_ACCOUNT_USENET_USERNAME, account.getUser());
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

    private final String         PROPERTY_ACCOUNT_USENET_USERNAME = "USENET_USERNAME";
    /* 2023-02-13: Required due to Cloudflare blocking login page. */
    private static final boolean allowOnlyUserCookieLogin         = true;

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        final Cookies cookies = account.loadCookies("");
        final Cookies userCookies = account.loadUserCookies();
        br.setFollowRedirects(true);
        try {
            Form loginform = null;
            if (cookies != null || userCookies != null) {
                logger.info("Checking login cookies");
                if (userCookies != null) {
                    br.setCookies(userCookies);
                } else {
                    br.setCookies(cookies);
                }
                br.getPage("https://www." + this.getHost() + "/en/user/login");
                loginform = br.getFormbyActionRegex("/en/user/login\\?.*");
                if ((loginform != null && loginform.containsHTML("name") && loginform.containsHTML("pass")) || !containsSessionCookie(br.getCookies(getHost()))) {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    if (userCookies != null) {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                } else {
                    logger.info("Cookie login successful");
                    if (!StringUtils.endsWithCaseInsensitive(br.getURL(), "en/user")) {
                        br.getPage("https://www." + this.getHost() + "/en/user");
                    }
                }
            }
            if (allowOnlyUserCookieLogin && userCookies == null) {
                showCookieLoginInfo();
                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_required());
            }
            if (!containsSessionCookie(br.getCookies(getHost()))) {
                logger.info("Performing full login");
                account.clearCookies("");
                final String username = account.getUser();
                if (username == null || !username.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please use your email address to login", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.setCookie(getHost(), "language", "en");
                br.getPage("https://www." + this.getHost() + "/en/user/login");
                loginform = br.getFormbyActionRegex("/en/user/login\\?.*");
                loginform.put("name", Encoding.urlEncode(account.getUser()));
                loginform.put("pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                loginform = br.getFormbyActionRegex("/en/user/login\\?.*");
                if (loginform != null && loginform.containsHTML("name") && loginform.containsHTML("pass")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (!containsSessionCookie(br.getCookies(getHost()))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (userCookies == null) {
                account.saveCookies(br.getCookies(getHost()), "");
            }
            String usenetUsername = br.getRegex("(?i)item-title\">\\s*Username\\s*:\\s*</div>.*?item-text\">(.*?)<").getMatch(0);
            if (StringUtils.isEmpty(usenetUsername)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            usenetUsername = Encoding.htmlDecode(usenetUsername).trim();
            account.setProperty(PROPERTY_ACCOUNT_USENET_USERNAME, usenetUsername);
            if (userCookies != null) {
                /* When using cookie login, user can enter whatever he wants into username field but we want to have unique usernames. */
                account.setUser(usenetUsername);
            }
            final String packageType = br.getRegex("(?i)item-title\">\\s*Account type\\s*:\\s*</div>.*?item-text\">(.*?)<").getMatch(0);
            if (packageType == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ai.setStatus(packageType);
            ai.setUnlimitedTraffic();
            final String endDate = br.getRegex("(?i)End date:</div>.*?item-text\">(.*?)<").getMatch(0);
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
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        ai.setMultiHostSupport(this, Arrays.asList(new String[] { "usenet" }));
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
