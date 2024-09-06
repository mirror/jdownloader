package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xsusenet.com" }, urls = { "" })
public class XSUseNetCom extends UseNet {
    public XSUseNetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.xsusenet.com/sign-up/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.xsusenet.com/terms-of-service/";
    }

    public static interface XSUseNetComConfigInterface extends UsenetAccountConfigInterface {
    };

    private final String USENET_USERNAME = "USENET_USERNAME";
    private final String USENET_PASSWORD = "USENET_PASSWORD";

    @Override
    protected String getUseNetUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    @Override
    protected String getUseNetPassword(Account account) {
        return account.getStringProperty(USENET_PASSWORD, account.getUser());
    }

    @Override
    protected UsenetServer getUseNetServer(Account account) throws Exception {
        final UsenetServer ret = super.getUseNetServer(account);
        if (AccountType.FREE.equals(account.getType())) {
            if (ret.getHost().startsWith("free")) {
                return ret;
            } else {
                return new UsenetServer("free.xsusenet.com", 119);
            }
        } else {
            return ret;
        }
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("\\?action=logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        try {
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                getPage("https://my.xsusenet.com");
                if (!isLoggedIN(br)) {
                    logger.info("Cookie login failed");
                    br.getCookies(getHost()).clear();
                } else {
                    logger.info("Cookie login successful");
                }
            }
            if (!isLoggedIN(br)) {
                account.clearCookies("");
                final String userName = account.getUser();
                if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail/password for xsusenet.com website!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                getPage("https://my.xsusenet.com/index.php?/clientarea/");
                final Form login = br.getFormByInputFieldKeyValue("action", "login");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("username", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                submitForm(login);
                if (!isLoggedIN(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (br.getRequest() == null || !StringUtils.endsWithCaseInsensitive(br.getURL(), "/index.php?/clientarea/")) {
                this.getPage("https://my.xsusenet.com/index.php?/clientarea/");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            /* Detect account type. Very important because different Usenet servers are used for free/premium accounts! */
            final boolean isFree;
            if (br.containsHTML("(?i)aria-selected=\"true\">\\s*Free Usenet\\s*<")) {
                isFree = true;
            } else {
                isFree = false;
            }
            final String table = br.getRegex("<tbody>(.*?)</tbody>").getMatch(0);
            final String[] tableRows = new Regex(table, "<tr(.*?)</tr>").getColumn(0);
            long highestExpireTimestamp = 0;
            String subscriptionName = null;
            String subscriptionInfoURL = null;
            int skippedSubscriptions = 0;
            for (final String subscriptionHTML : tableRows) {
                if (!subscriptionHTML.contains("class=\"badge badge-Active\"")) {
                    /* Skip inactive/expired subscriptions */
                    skippedSubscriptions++;
                    continue;
                }
                final String nextDueDate = new Regex(subscriptionHTML, "Next Due Date</small><br />\\s*<span>(\\d{2}/\\d{2}/\\d{4})</span>").getMatch(0);
                if (nextDueDate != null) {
                    final long subscriptionExpireTimestamp = TimeFormatter.getMilliSeconds(nextDueDate, "dd/MM/yyyy", Locale.ENGLISH);
                    if (subscriptionExpireTimestamp > highestExpireTimestamp) {
                        highestExpireTimestamp = subscriptionExpireTimestamp;
                        subscriptionName = new Regex(subscriptionHTML, "class=\"text-dark font-weight-bold\">([^<]+)</span>").getMatch(0);
                        subscriptionInfoURL = new Regex(subscriptionHTML, "<a href=\"([^<>\"]+)\" class=\"text-small\">").getMatch(0);
                    }
                }
            }
            if (highestExpireTimestamp == 0) {
                if (skippedSubscriptions > 0) {
                    ai.setExpired(true);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find any subscriptions");
                }
            }
            if (subscriptionName == null || subscriptionInfoURL == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(subscriptionInfoURL + "&widget=logindetails");
            final String username = br.getRegex(">Username</td>\\s*<td>([^<]*)<").getMatch(0);
            final String password = br.getRegex("id=\"showpassword\">([^<]+)</span>").getMatch(0);
            if (StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find UseNet login credentials");
            }
            ai.setValidUntil(highestExpireTimestamp, br);
            account.setProperty(USENET_USERNAME, username.trim());
            account.setProperty(USENET_PASSWORD, password);
            ai.setStatus(subscriptionName);
            if (isFree) {
                account.setType(Account.AccountType.FREE);
                account.setMaxSimultanDownloads(5);
                ai.setStatus(subscriptionName);
            } else {
                account.setType(Account.AccountType.PREMIUM);
                ai.setStatus(subscriptionName);
                /* TODO: Check if this is still working (2022-01-12: Only checked free account handling) */
                if (subscriptionName.contains("200")) {
                    // 200 Mbit package: 50 connection
                    account.setMaxSimultanDownloads(50);
                } else if (subscriptionName.contains("150")) {
                    // 150 Mbit package: 50 connection
                    account.setMaxSimultanDownloads(50);
                } else if (subscriptionName.contains("100")) {
                    // 100 Mbit package: 50 connections
                    account.setMaxSimultanDownloads(50);
                } else if (subscriptionName.contains("50")) {
                    // 50 Mbit package: 40 connection
                    account.setMaxSimultanDownloads(40);
                } else if (subscriptionName.contains("25")) {
                    // 25 Mbit package: 30 connections
                    account.setMaxSimultanDownloads(30);
                } else if (subscriptionName.contains("10")) {
                    // 10 Mbit package: 20 connections
                    account.setMaxSimultanDownloads(20);
                } else {
                    // Free account: 5 connections(fallback)
                    account.setMaxSimultanDownloads(5);
                }
            }
            ai.setMultiHostSupport(this, Arrays.asList(new String[] { "usenet" }));
            account.setRefreshTimeout(5 * 60 * 60 * 1000l);
            try {
                verifyUseNetLogins(account);
                return ai;
            } catch (final InvalidAuthException e) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, null, PluginException.VALUE_ID_PREMIUM_DISABLE, e);
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
                account.removeProperty(USENET_PASSWORD);
                account.removeProperty(USENET_PASSWORD);
            }
            throw e;
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.xsusenet.com", false, 119, 443, 23, 80, 81, 8080, 2323, 8181));
        ret.addAll(UsenetServer.createServerList("reader.xsusenet.com", true, 443, 563, 564, 600, 663, 664));
        ret.addAll(UsenetServer.createServerList("free.xsusenet.com", false, 119, 443, 23, 80, 81, 8080, 2323, 8181));
        return ret;
    }
}
