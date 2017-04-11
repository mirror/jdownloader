package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "giganews.com" }, urls = { "" })
public class GiganewsCom extends UseNet {
    public GiganewsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.giganews.com/signup/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.giganews.com/legal/tos_personal.html";
    }

    public static interface GiganewsComConfigInterface extends UsenetAccountConfigInterface {
    };

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
                br.getPage("https://www.giganews.com/controlpanel/");
                login = br.getFormbyActionRegex("/auth");
                if (login != null && login.containsHTML("credential_0") && login.containsHTML("credential_1")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "sid") == null || br.getCookie(getHost(), "GN::Web::ControlPanel_ControlPanel") == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "sid") == null || br.getCookie(getHost(), "GN::Web::ControlPanel_ControlPanel") == null) {
                account.clearCookies("");
                final String userName = account.getUser();
                br.getPage("https://www.giganews.com/");
                login = br.getFormbyActionRegex("/auth");
                login.put("credential_0", Encoding.urlEncode(userName));
                login.put("credential_1", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex("/auth");
                if (login != null && login.containsHTML("credential_0") && login.containsHTML("credential_1")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "sid") == null || br.getCookie(getHost(), "GN::Web::ControlPanel_ControlPanel") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String accountLevel = br.getRegex("<li>Account Level:\\s*<strong>(.*?)</").getMatch(0);
            final String transferLimit = br.getRegex("<li>Transfer Limit:\\s*<b>(.*?)</").getMatch(0);
            if (StringUtils.containsIgnoreCase(accountLevel, "Diamond")) {
                account.setMaxSimultanDownloads(50);
            } else if (StringUtils.containsIgnoreCase(accountLevel, "Platinum")) {
                account.setMaxSimultanDownloads(20);
            } else if (StringUtils.containsIgnoreCase(accountLevel, "Silver")) {
                account.setMaxSimultanDownloads(20);
            } else if (StringUtils.containsIgnoreCase(accountLevel, "Bronze")) {
                account.setMaxSimultanDownloads(20);
            } else if (StringUtils.containsIgnoreCase(accountLevel, "Pearl")) {
                account.setMaxSimultanDownloads(20);
            } else {
                account.setMaxSimultanDownloads(1);
            }
            ai.setStatus("Account Level: " + accountLevel + " Transfer Limit: " + transferLimit);
            if (!br.containsHTML("/controlpanel/usenet.html\">Manage</")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "No Usenet Feature available", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String status = br.getRegex("class=\"field\">Status</span>\\s*<span.*?\">\\s*(.*?)\\s*</").getMatch(0);
            if (status == null || !"Active".equalsIgnoreCase(status.trim())) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Status: " + status, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String validUntil = br.getRegex("Next Scheduled Rotation:\\s*<b>\\s*(\\d+/\\d+/\\d+)").getMatch(0);
            if (validUntil != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "MM'/'dd'/'yyyy", Locale.ENGLISH) + (24 * 60 * 60 * 1000l));
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
        ret.addAll(UsenetServer.createServerList("news.giganews.com", false, 119, 23, 80));
        ret.addAll(UsenetServer.createServerList("news.giganews.com", true, 563, 443));
        return ret;
    }
}
