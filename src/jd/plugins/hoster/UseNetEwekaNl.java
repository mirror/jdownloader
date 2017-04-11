package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eweka.nl" }, urls = { "" }) public class UseNetEwekaNl extends UseNet {
    public UseNetEwekaNl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.eweka.nl/en/usenet_toegang/specificaties/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.eweka.nl/en/av/";
    }

    public static interface EwekaNlConfigInterface extends UsenetAccountConfigInterface {
    };

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        try {
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                br.getPage("https://www.eweka.nl/myeweka/?lang=en");
                if (br.getCookie(getHost(), "PHPSESSID") == null || br.containsHTML("\\$\\('#login-form'\\);")) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "PHPSESSID") == null) {
                account.clearCookies("");
                br.getPage("https://www.eweka.nl/myeweka/?lang=en");
                final String response = br.getPage("https://www.eweka.nl/myeweka/auth.php?u=" + Encoding.urlEncode(account.getUser()) + "&p=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(getHost(), "PHPSESSID") == null || br.containsHTML("\\$\\('#login-form'\\);") || !"1".equals(response)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (!StringUtils.containsIgnoreCase(br.getURL(), "https://www.eweka.nl/myeweka/?lang=en")) {
                // switch to english
                br.getPage("https://www.eweka.nl/myeweka/?lang=en");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String server = br.getRegex("<td><b>Server</b></td>.*?<td.*?>(.*?)</td>").getMatch(0);
            final String port = br.getRegex("<td><b>Port</b></td>.*?<td.*?>(\\d+)</td>").getMatch(0);
            // TODO: use these infos for available servers
            final String connections = br.getRegex("<td><b>Connections</b></td>.*?<td.*?>(\\d+)</td>").getMatch(0);
            if (connections != null) {
                account.setMaxSimultanDownloads(Integer.parseInt(connections));
            } else {
                account.setMaxSimultanDownloads(8);
            }
            final String userName = br.getRegex("<td><b>Username</b></td>.*?<td.*?>(.*?)</td>").getMatch(0);
            if (userName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(USENET_USERNAME, userName);
            }
            final String validUntil = br.getRegex("<td><b>Valid until</b></td>.*?<td.*?>\\s*?(\\d+-\\d+-\\d+\\s+\\d+:\\d+)").getMatch(0);
            if (validUntil != null) {
                final long date = TimeFormatter.getMilliSeconds(validUntil, "dd'-'MM'-'yyyy' 'HH:mm", null);
                if (date > 0) {
                    ai.setValidUntil(date);
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
        ret.addAll(UsenetServer.createServerList("newsreader.eweka.nl", false, 119));// resolves to 3 IP
        // ret.addAll(UsenetServer.createServerList("newsreader124.eweka.nl", false, 119));//resolves to 1 IP
        ret.addAll(UsenetServer.createServerList("sslreader.eweka.nl", true, 563, 443));// resolves to 3 IP
        return ret;
    }
}
