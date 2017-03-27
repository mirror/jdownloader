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

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 3, names = { "pureusenet.nl" }, urls = { "" })
public class PureUseNetNL extends UseNet {

    public PureUseNetNL(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.pureusenet.nl/en/packages");
    }

    public static interface XPureUseNetNLAccountConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public String getAGBLink() {
        return "https://www.pureusenet.nl/en/terms-and-conditions";
    }

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
                br.getPage("https://members.pureusenet.nl/en/index");
                login = br.getFormbyKey("username");
                if (login != null && login.containsHTML("name=\"username\"") && login.containsHTML("name=\"password\"")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "PUR-SESS") == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "PUR-SESS") == null) {
                account.clearCookies("");
                final String userName = account.getUser();
                if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail/password for pureusenet.nl website!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("https://members.pureusenet.nl/en");
                login = br.getFormbyKey("username");
                login.put("username", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyKey("username");
                if (login != null && login.containsHTML("name=\"username\"") && login.containsHTML("name=\"password\"")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "PUR-SESS") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (!StringUtils.containsIgnoreCase(br.getURL(), "https://members.pureusenet.nl/en/index") && !StringUtils.containsIgnoreCase(br.getURL(), "https://members.pureusenet.nl/en")) {
                br.getPage("https://members.pureusenet.nl/en/index");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String packageType = br.getRegex("<h3>Your package:\\s*<span class=\"font-primary\">(.*?)<").getMatch(0);
            final String connections = br.getRegex("<li><b>Connections:</b>\\s*(\\d+)\\s*<").getMatch(0);
            if (packageType != null) {
                ai.setStatus(packageType);
                if (connections != null) {
                    account.setMaxSimultanDownloads(Integer.parseInt(connections));
                } else {
                    if (packageType.contains("XS")) {
                        // Pure XS = 4 Connections
                        account.setMaxSimultanDownloads(4);
                    } else if (packageType.contains("S")) {
                        // Pure S = 4 Connections
                        account.setMaxSimultanDownloads(4);
                    } else if (packageType.contains("M")) {
                        // Pure M = 8 Connections
                        account.setMaxSimultanDownloads(8);
                    } else if (packageType.contains("XXL")) {
                        // Pure XXL = 12 Connections
                        account.setMaxSimultanDownloads(12);
                    } else if (packageType.contains("XL")) {
                        // Pure XL = 12 Connections
                        account.setMaxSimultanDownloads(12);
                    } else if (packageType.contains("L")) {
                        // Pure L = 8 Connections
                        account.setMaxSimultanDownloads(8);
                    } else {
                        // 5 connections(fallback)
                        account.setMaxSimultanDownloads(5);
                    }
                }
            } else {
                ai.setStatus("Unknown packageType! Please contact JDownloader support at support@jdownloader.org");
                if (connections != null) {
                    account.setMaxSimultanDownloads(Integer.parseInt(connections));
                } else {
                    // 5 connections(fallback)
                    account.setMaxSimultanDownloads(5);
                }
            }
            final String expireDate = br.getRegex("<li><b>Expiration date:</b>\\s*(\\d+-\\d+-\\d+)\\s*<").getMatch(0);
            if (expireDate != null) {
                final long date = TimeFormatter.getMilliSeconds(expireDate, "yyyy'-'MM'-'dd", null);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                    ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                    account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 2 * 60 * 60 * 1000l);
                    return ai;
                }
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.pureusenet.nl", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("news.pureusenet.nl", true, 563, 443));
        return ret;
    }
}
