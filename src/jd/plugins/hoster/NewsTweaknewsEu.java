package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tweaknews.eu" }, urls = { "" })
public class NewsTweaknewsEu extends UseNet {
    public NewsTweaknewsEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.tweaknews.eu/en/usenet-plans");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USENET, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    @Override
    public String getAGBLink() {
        return "http://www.tweaknews.eu/en/conditions";
    }

    public static interface NewsTweaknewsEuConfig extends UsenetAccountConfigInterface {
    };

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        final Cookies userCookies = account.loadUserCookies();
        Form login = null;
        if (cookies != null || userCookies != null) {
            logger.info("Attempting cookie login");
            if (userCookies != null) {
                br.setCookies(userCookies);
            } else {
                br.setCookies(cookies);
            }
            br.getPage("https://www." + this.getHost() + "/en/login?backurl=%2Fen%2Fmember");
            login = br.getForm(0);
            if (isLoggedIn(br)) {
                logger.info("Cookie login successful");
            } else {
                logger.info("Cookie login failed");
                if (userCookies != null) {
                    /* Dead-end */
                    if (account.hasEverBeenValid()) {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                    } else {
                        throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                    }
                }
            }
        }
        if (!isLoggedIn(br)) {
            account.clearCookies("");
            br.setCookie(getHost(), "language", "en");
            br.getPage("https://www." + this.getHost() + "/en/login?backurl=%2Fen%2Fmember");
            login = br.getForm(0);
            login.put("username", Encoding.urlEncode(account.getUser()));
            login.put("password", Encoding.urlEncode(account.getPass()));
            br.submitForm(login);
            login = br.getForm(0);
            if (login != null && login.containsHTML("username") && login.containsHTML("password")) {
                /* 2023-12-28: TODO: Check if this is still needed */
                /* Account valid but not usable */
                final String alertDanger = br.getRegex("class=\"alert alert-danger\">(.*?)</").getMatch(0);
                if (alertDanger != null) {
                    throw new AccountInvalidException(alertDanger);
                } else {
                    final String inputInfo = br.getRegex("class=\"input-info\">(.*?)</").getMatch(0);
                    throw new AccountInvalidException(inputInfo);
                }
            }
            if (!this.isLoggedIn(br)) {
                throw new AccountInvalidException();
            }
        }
        if (userCookies == null) {
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
        final String userName = br.getRegex("Username:</td>.*?<td>(.*?)</td>").getMatch(0);
        if (StringUtils.isEmpty(userName)) {
            if (br.containsHTML("ADD NEW SUBSRIPTION") || br.containsHTML("No Active Packages")) {
                throw new AccountInvalidException("No active/valid subscription");
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        account.setProperty(USENET_USERNAME, Encoding.htmlDecode(userName).trim());
        final String packageType = br.getRegex("name\">Package:(.*?)</div>").getMatch(0);
        if (!StringUtils.contains(packageType, "Block Package")) {
            // time limit
            ai.setStatus(packageType.trim());
            ai.setUnlimitedTraffic();
            final String expires = br.getRegex("Valid until: (\\d+-\\d+-\\d+)").getMatch(0);
            if (expires != null) {
                final long date = TimeFormatter.getMilliSeconds(expires, "yyyy'-'MM'-'dd", null);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                }
            }
        } else if (StringUtils.contains(packageType, "Free Trial")) {
            // free trial, no traffic left?
            ai.setStatus(packageType.trim());
            final String expires = br.getRegex("Valid until: (\\d+-\\d+-\\d+)").getMatch(0);
            if (expires != null) {
                final long date = TimeFormatter.getMilliSeconds(expires, "yyyy'-'MM'-'dd", null);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                }
            }
            final String blockPackageSize = new Regex(packageType, "(\\d+\\s*?GB)").getMatch(0);
            final long max = SizeFormatter.getSize(blockPackageSize);
            ai.setTrafficMax(max);
        } else {
            // traffic limit
            ai.setStatus(packageType.trim());
            final String blockPackageSize = new Regex(packageType, "(\\d+\\s*?GB)").getMatch(0);
            final long max = SizeFormatter.getSize(blockPackageSize);
            final String dataRemaining = br.getRegex("Data Remaining:</td>.*?<td>(.*?\\s*?GB)").getMatch(0);
            final long left = SizeFormatter.getSize(dataRemaining);
            ai.setValidUntil(-1);
            ai.setTrafficMax(max);
            ai.setTrafficLeft(left);
            if (left <= 0) {
                throw new AccountUnavailableException("No more traffic left", 5 * 60 * 1000l);
            }
        }
        final String connections = br.getRegex("Threads:</td>.*?<td>(\\d+)").getMatch(0);
        if (connections != null) {
            account.setMaxSimultanDownloads(Integer.parseInt(connections));
        } else {
            account.setMaxSimultanDownloads(40);
        }
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    private boolean isLoggedIn(final Browser br) {
        return br.containsHTML("/member/logout");
    }

    @Override
    protected String getUseNetUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.tweaknews.eu", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("news.tweaknews.eu", true, 563, 443));
        return ret;
    }
}
