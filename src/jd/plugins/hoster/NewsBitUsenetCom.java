package jd.plugins.hoster;

import java.util.Arrays;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 3, names = { "bitusenet.com" }, urls = { "" }, flags = { 0 })
public class NewsBitUsenetCom extends UseNet {

    public NewsBitUsenetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.bitusenet.com");
    }

    @Override
    public String getAGBLink() {
        return "https://www.bitusenet.com/tos";
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage("https://www.bitusenet.com/login");
        final Form login = br.getForm(0);
        login.put("uname", Encoding.urlEncode(account.getUser()));
        login.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(login);
        if (br.getCookie(getHost(), "bitusenet") == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final String accountStatus = br.getRegex("Account Status</h4>\\s*?</li>\\s*?<li>\\s*?<p>(.*?)</").getMatch(0);
        if (!"Active".equalsIgnoreCase(accountStatus)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, accountStatus, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        final String started = br.getRegex("Started</h4>\\s*?</li>\\s*?<li>\\s*?<p>(.*? \\d+,\\s*? \\d+)</").getMatch(0);
        if (started != null) {
            final long date = TimeFormatter.getMilliSeconds(started, "MMM dd, yyyy", null);
            if (date > 0) {
                account.setRegisterTimeStamp(date);
            }
        }
        final AccountInfo ai = new AccountInfo();
        final String expires = br.getRegex("Expires</h4>\\s*?</li>\\s*?<li>\\s*?<p.*?>(.*? \\d+,\\s*? \\d+)</").getMatch(0);
        if (expires != null) {
            final long date = TimeFormatter.getMilliSeconds(expires, "MMM dd, yyyy", null);
            if (date > 0) {
                ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
            }
        }
        final String connections = br.getRegex("Connections</h4>\\s*?</li>\\s*?<li>(\\d+)</").getMatch(0);
        if (connections != null) {
            account.setMaxSimultanDownloads(Integer.parseInt(connections));
        } else {
            account.setMaxSimultanDownloads(50);
        }
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    protected String getServerAdress() {
        return "news.bitusenet.com";
    }

    @Override
    protected int[] getAvailablePorts() {
        return new int[] { 119, 53, 80, 443, 8080, 9000 };
    }

    @Override
    protected int[] getAvailableSSLPorts() {
        return new int[] { 563 };
    }
}
