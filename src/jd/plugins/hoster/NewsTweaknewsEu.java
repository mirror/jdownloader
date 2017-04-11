package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tweaknews.eu" }, urls = { "" }) public class NewsTweaknewsEu extends UseNet {
    public NewsTweaknewsEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.tweaknews.eu/en/usenet-plans");
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
        try {
            Form login = null;
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                br.getPage("https://members.tweaknews.eu/en");
                login = br.getForm(0);
                if (login != null && login.containsHTML("username") && login.containsHTML("password")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "twn-SESS") == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "twn-SESS") == null) {
                account.clearCookies("");
                br.setCookie(getHost(), "language", "en");
                br.getPage("https://members.tweaknews.eu/en");
                login = br.getForm(0);
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getForm(0);
                if (login != null && login.containsHTML("username") && login.containsHTML("password")) {
                    final String alertDanger = br.getRegex("class=\"alert alert-danger\">(.*?)</").getMatch(0);
                    if (alertDanger != null) {
                        if (StringUtils.contains(alertDanger, "IP is blocked")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, alertDanger, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        }
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, alertDanger, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    final String inputInfo = br.getRegex("class=\"input-info\">(.*?)</").getMatch(0);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, inputInfo, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie(getHost(), "twn-SESS") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String userName = br.getRegex("Username:</td>.*?<td>(.*?)</td>").getMatch(0);
            if (userName != null) {
                account.setProperty(USENET_USERNAME, userName);
            } else {
                if (br.containsHTML("ADD NEW SUBSRIPTION")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "No active/valid subscription", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "No more traffic left", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
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

    @Override
    protected String getUsername(Account account) {
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
