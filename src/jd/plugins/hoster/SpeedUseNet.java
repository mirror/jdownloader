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

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "speeduse.net" }, urls = { "" })
public class SpeedUseNet extends UseNet {

    public SpeedUseNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.speeduse.net/overview/");
    }

    public static interface SpeedUseNetAccountConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public String getAGBLink() {
        return "https://www.speeduse.net/agb/";
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
                br.getPage("https://www.speeduse.net/frontend/code/index.php");
                final String username = br.getRegex("Username:</td><td class='rc'>(.*?)<").getMatch(0);
                login = br.getFormbyAction("index.php");
                if ((login != null && login.containsHTML("form\\[element")) || username == null) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "speeduse-session") == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "speeduse-session") == null) {
                account.clearCookies("");
                br.getPage("https://www.speeduse.net/frontend/");
                login = br.getFormbyAction("index.php");
                login.remove("form%5Belement1%5D");
                login.remove("form%5Belement0%5D");
                login.put("form[element0]", Encoding.urlEncode(account.getUser()));
                login.put("form[element1]", Encoding.urlEncode(account.getPass()));
                login.remove("resetButton");
                br.submitForm(login);
                final String username = br.getRegex("Username:</td><td class='rc'>(.*?)<").getMatch(0);
                login = br.getFormbyAction("index.php");
                if ((login != null && login.containsHTML("form\\[element")) || username == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "speeduse-session") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            br.getPage("https://www.speeduse.net/frontend/code/getAccountStatus.php");
            if (br.containsHTML("Blockaccount")) {
                final boolean isActive = br.containsHTML("<b>active</b>");
                if (!isActive) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                ai.setStatus("BlockAccount");
                final String trafficLeft = br.getRegex("Restguhaben:(.*?)<").getMatch(0);
                if (trafficLeft != null) {
                    ai.setTrafficLeft(trafficLeft);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else if (br.containsHTML("Flatrate")) {
                final String until = br.getRegex("(\\d{2}\\.\\d{2}.\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
                if (until != null) {
                    ai.setStatus("Flatrate");
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(until, "dd'.'MM'.'yyyy' 'HH':'mm':'ss", null));
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setMaxSimultanDownloads(40);
            account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 4 * 60 * 60 * 1000l);
            ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
            return ai;
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
        ret.addAll(UsenetServer.createServerList("ams.speeduse.net", true, 563, 5563));
        ret.addAll(UsenetServer.createServerList("us.speeduse.net", true, 563, 5563));
        return ret;
    }
}
