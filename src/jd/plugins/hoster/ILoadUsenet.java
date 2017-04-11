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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "iload-usenet.com" }, urls = { "" }) public class ILoadUsenet extends UseNet {
    public ILoadUsenet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.iload-usenet.com/prices");
    }

    public static interface ILoadUsenetConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public String getAGBLink() {
        return "https://www.iload-usenet.com/tos";
    }

    @Override
    protected String getUsername(Account account) {
        return account.getUser() + "@iload-usenet.com";
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
                br.getPage("https://www.iload-usenet.com/home?_lang=en_US");
                login = br.getFormbyActionRegex(".*dologin.*");
                if (login != null && login.containsHTML("j_username") && login.containsHTML("j_password")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "JSESSIONID") == null || (br.getCookie(getHost(), "SPRING_SECURITY_REMEMBER_ME_COOKIE") == null && br.getCookie(getHost(), "remember-me") == null)) {
                    br.getCookies(getHost()).clear();
                } else {
                    br.getPage("https://www.iload-usenet.com/account/home?_lang=en_US");
                }
            }
            if (br.getCookie(getHost(), "JSESSIONID") == null || (br.getCookie(getHost(), "SPRING_SECURITY_REMEMBER_ME_COOKIE") == null && br.getCookie(getHost(), "remember-me") == null)) {
                account.clearCookies("");
                br.setCookie(getHost(), "language", "en");
                br.getPage("https://www.iload-usenet.com/home?_lang=en_US");
                login = br.getFormbyActionRegex(".*dologin.*");
                login.put("j_username", Encoding.urlEncode(account.getUser()));
                login.put("j_password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex(".*dologin.*");
                if (login != null && login.containsHTML("j_username") && login.containsHTML("j_password")) {
                    final String alertDanger = br.getRegex("class=\"alert alert-danger\">(.*?)</div").getMatch(0);
                    if (alertDanger != null) {
                        if (StringUtils.contains(alertDanger, "IP is blocked")) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, alertDanger, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                        }
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, alertDanger, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie(getHost(), "JSESSIONID") == null || (br.getCookie(getHost(), "SPRING_SECURITY_REMEMBER_ME_COOKIE") == null && br.getCookie(getHost(), "remember-me") == null)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String regDate = br.getRegex("Registration Date:.*?form-control-static\">(.*?)<").getMatch(0);
            final long regTimeStamp;
            if (regDate != null) {
                regTimeStamp = TimeFormatter.getMilliSeconds(regDate, "MMM dd','yyyy", null);
                account.setRegisterTimeStamp(regTimeStamp);
            } else {
                regTimeStamp = -1;
            }
            final String packageType = br.getRegex("(?-s)(<strong>(.*?)</strong> of your <strong>.*?</strong>.+)").getMatch(0);
            if (StringUtils.contains(packageType, "trial volume")) {
                // free trial
                ai.setStatus("Trial");
                ai.setUnlimitedTraffic();
                final String maxTraffic = new Regex(packageType, "of your <strong>(.*?)</strong>").getMatch(0);
                final long max = SizeFormatter.getSize(maxTraffic);
                final String consumedTraffic = new Regex(packageType, "<strong>(.*?)</strong>").getMatch(0);
                final long consumed = SizeFormatter.getSize(consumedTraffic);
                ai.setTrafficMax(max);
                ai.setTrafficLeft(Math.max(0, max - consumed));
                if (ai.getTrafficLeft() < 0) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "No more trial traffic left", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (regTimeStamp > 0) {
                    ai.setValidUntil(regTimeStamp + (14 * 24 * 60 * 60 * 1000l));
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown account type, please contact JDownloader support");
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        account.setMaxSimultanDownloads(20);
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.iload-usenet.com", false, 119, 23, 443, 8080, 9000));
        ret.addAll(UsenetServer.createServerList("secure.news.iload-usenet.com", true, 563, 81, 81));
        return ret;
    }
}
