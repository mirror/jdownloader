package jd.plugins.hoster;

import java.util.Arrays;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 3, names = { "tweaknews.eu" }, urls = { "" }, flags = { 0 })
public class NewsTweaknewsEu extends UseNet {
    public NewsTweaknewsEu(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.tweaknews.eu/en/usenet-plans");
    }

    @Override
    public String getAGBLink() {
        return "http://www.tweaknews.eu/en/conditions";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(getHost(), "language", "en");
        br.getPage("https://members.tweaknews.eu/en");
        Form login = br.getForm(0);
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
        final AccountInfo ai = new AccountInfo();
        final String packageType = br.getRegex("name\">Package:(.*?)</div>").getMatch(0);
        if (!StringUtils.contains(packageType, "Block Package")) {
            // time limit
            ai.setStatus(packageType.trim());
            ai.setUnlimitedTraffic();
            final String expires = br.getRegex("Valid until: (\\d+-\\d+-\\d+)").getMatch(0);
            if (expires != null) {
                final long date = TimeFormatter.getMilliSeconds(expires, "YYYY'-MMM'-dd", null);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                }
            }
        } else if (StringUtils.contains(packageType, "Free Trial")) {
            // free trial, no traffic left?
            ai.setStatus(packageType.trim());
            final String expires = br.getRegex("Valid until: (\\d+-\\d+-\\d+)").getMatch(0);
            if (expires != null) {
                final long date = TimeFormatter.getMilliSeconds(expires, "YYYY'-MMM'-dd", null);
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
    protected String getServerAdress() {
        return "news.tweaknews.eu";
    }

    @Override
    protected int[] getAvailablePorts() {
        return new int[] { 119 };
    }

    @Override
    protected int[] getAvailableSSLPorts() {
        return new int[] { 563 };
    }
}
