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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "newsdemon.com" }, urls = { "" })
public class NewsDemonCom extends UseNet {
    public NewsDemonCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://www.newsdemon.com/usenet-access.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.newsdemon.com/terms.php";
    }

    public static interface NewsDemonComConfigInterface extends UsenetAccountConfigInterface {
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
                br.getPage("https://www.newsdemon.com/members/index.php");
                login = br.getFormbyActionRegex("/login.php");
                if (login != null) {
                    br.getCookies(getHost()).clear();
                } else if (!br.containsHTML("/logout.php")) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (!br.containsHTML("/logout.php")) {
                account.clearCookies("");
                final String userName = account.getUser();
                br.getPage("https://www.newsdemon.com/members/memlogin.php");
                login = br.getFormbyActionRegex("/login.php");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("memberid", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                if (login.hasInputFieldByName("captcha_code")) {
                    final String captchaCalculation[] = br.getRegex("(\\d+)\\s*(\\+|-|/|\\*)\\s*(\\d+)\\s*=").getRow(0);
                    if (captchaCalculation == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final int result;
                    if ("+".equals(captchaCalculation[1])) {
                        result = Integer.parseInt(captchaCalculation[0]) + Integer.parseInt(captchaCalculation[2]);
                    } else if ("-".equals(captchaCalculation[1])) {
                        result = Integer.parseInt(captchaCalculation[0]) - Integer.parseInt(captchaCalculation[2]);
                    } else if ("/".equals(captchaCalculation[1])) {
                        result = Integer.parseInt(captchaCalculation[0]) / Integer.parseInt(captchaCalculation[2]);
                    } else if ("*".equals(captchaCalculation[1])) {
                        result = Integer.parseInt(captchaCalculation[0]) * Integer.parseInt(captchaCalculation[2]);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    login.put("captcha_code", String.valueOf(result));
                }
                br.submitForm(login);
                login = br.getFormbyActionRegex("/login.php");
                if (login != null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("/members/index.php");
                login = br.getFormbyActionRegex("/login.php");
                if (login != null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (!br.containsHTML("members/logout.php")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String trafficUsed = br.getRegex("Used:.*?>([0-9\\.]+)</span>\\s*GB").getMatch(0);
            final String trafficAvailable = br.getRegex("Remaining:.*?>([0-9\\.]+)</span>\\s*GB").getMatch(0);
            if (trafficUsed != null && trafficAvailable != null) {
                final long max = SizeFormatter.getSize(trafficAvailable) + SizeFormatter.getSize(trafficUsed);
                final long left = SizeFormatter.getSize(trafficAvailable);
                ai.setTrafficMax(max);
                ai.setTrafficLeft(left);
            }
            final String status = br.getRegex("Status:</li>.*?span class=.*?>(.*?)</span").getMatch(0);
            if (!"Active".equals(status)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Status:" + status, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.setMaxSimultanDownloads(50);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 2 * 60 * 60 * 1000l);
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        // geoDNS
        ret.addAll(UsenetServer.createServerList("news.newsdemon.com", false, 119, 23, 443, 3128, 7000, 8000, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("news.newsdemon.com", true, 563, 80, 81, 9119));
        // force north american server
        ret.addAll(UsenetServer.createServerList("us.newsdemon.com", false, 119, 23, 443, 3128, 7000, 8000, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("us.newsdemon.com", true, 563, 80, 81, 9119));
        // force euorpean server
        ret.addAll(UsenetServer.createServerList("eu.newsdemon.com", false, 119, 23, 443, 3128, 7000, 8000, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("eu.newsdemon.com", true, 563, 80, 81, 9119));
        return ret;
    }
}
