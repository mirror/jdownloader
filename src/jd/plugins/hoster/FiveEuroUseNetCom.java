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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "5eurousenet.com" }, urls = { "" })
public class FiveEuroUseNetCom extends UseNet {
    public FiveEuroUseNetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.5eurousenet.com/en/cart/checkout");
    }

    @Override
    public String getAGBLink() {
        return "https://www.5eurousenet.com/en/general-terms";
    }

    public static interface FiveEuroUseNetComConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        try {
            boolean freshLogin = true;
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                getPage("https://www.5eurousenet.com/en/user");
                final Form login = br.getFormbyActionRegex("/login");
                if (login != null && login.containsHTML("name") && login.containsHTML("pass")) {
                    freshLogin = true;
                } else if (!br.containsHTML("/user/logout")) {
                    freshLogin = true;
                } else {
                    freshLogin = false;
                }
            }
            if (freshLogin) {
                account.clearCookies("");
                final String userName = account.getUser();
                getPage("https://www.5eurousenet.com/en/user/login");
                Form login = br.getFormbyActionRegex(".*?user/login\\?");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("name", Encoding.urlEncode(userName));
                login.put("pass", Encoding.urlEncode(account.getPass()));
                if (login.containsHTML("g-recaptcha")) {
                    final DownloadLink before = getDownloadLink();
                    try {
                        final DownloadLink dummyLink = new DownloadLink(this, "Account", getHost(), null, true);
                        setDownloadLink(dummyLink);
                        final CaptchaHelperHostPluginRecaptchaV2 rc2 = new CaptchaHelperHostPluginRecaptchaV2(this, br);
                        final String code = rc2.getToken();
                        if (StringUtils.isEmpty(code)) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        } else {
                            login.put("g-recaptcha-response", Encoding.urlEncode(code));
                        }
                    } finally {
                        setDownloadLink(before);
                    }
                }
                submitForm(login);
                login = br.getFormbyActionRegex(".*?user/login");
                if (login != null && login.containsHTML("name") && login.containsHTML("pass")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (!br.containsHTML("/user/logout")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String accountType = br.getRegex("Account type\\s*:\\s*</div>\\s*<div[^<]*>\\s*(.*?)</").getMatch(0);
            ai.setStatus(accountType);
            final String endDate = br.getRegex("<div[^<]*>\\s*End date\\s*:\\s*</div>\\s*<div[^<]*>\\s*(.*?)\\s*</").getMatch(0);
            if (endDate != null) {
                final long validUnitl = TimeFormatter.getMilliSeconds(endDate, "MMM' 'dd' 'yyyy' - 'HH':'mm", Locale.ENGLISH);
                ai.setValidUntil(validUnitl);
            }
            final boolean isExpired = br.containsHTML(">\\s*Your account has expired\\.?\\s*<");
            if (ai.isExpired() == false && isExpired) {
                ai.setExpired(true);
            }
            account.setMaxSimultanDownloads(20);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 5 * 60 * 60 * 1000l);
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("reader.5eurousenet.com", false, 119));
        ret.addAll(UsenetServer.createServerList("reader.5eurousenet.com", true, 563, 443, 89));
        return ret;
    }
}
