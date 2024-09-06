package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hitnews.com" }, urls = { "" })
public class HitNewsCom extends UseNet {
    public HitNewsCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://member.hitnews.com/signup.php");
    }

    public static interface HitNewsConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public String getAGBLink() {
        return "http://www.hitnews.com/index.php?id=41";
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
                br.getPage("https://member.hitnews.com/member.php");
                login = br.getFormbyActionRegex("/member\\.php");
                if (login != null && login.containsHTML("amember_login") && login.containsHTML("amember_pass")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "PHPSESSID") == null) {
                    br.getCookies(getHost()).clear();
                } else if (br.getRegex(">Your payment history<.*?<tr>.*?</tr>\\s*<tr.*?>\\s*<td>\\s*(.*?)\\s*</td>\\s*<td.*?>\\s*(\\d+/\\d+/\\d+)\\s*</td>\\s*<td.*?>\\s*(\\d+/\\d+/\\d+)\\s*</td>").getRow(0) == null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "PHPSESSID") == null) {
                account.clearCookies("");
                br.getPage("https://member.hitnews.com/login");
                final String pleaseWait = br.getRegex("Please wait\\s*(\\d+)\\s*seconds before next login attempt").getMatch(0);
                if (pleaseWait != null) {
                    throw new AccountUnavailableException("Please wait before next login attempt", Integer.parseInt(pleaseWait) * 1000l);
                }
                login = br.getFormbyKey("login_attempt_id");
                login.put("amember_login", Encoding.urlEncode(account.getUser()));
                login.put("amember_pass", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyKey("login_attempt_id");
                if (login != null && login.containsHTML("amember_login") && login.containsHTML("amember_pass")) {
                    final String errmsg = br.getRegex("class\\s*=\\s*\"am-errors\">\\s*<li>\\s*(.*?)\\s*</li>").getMatch(0);
                    if (errmsg != null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, errmsg, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else if (br.getCookie(getHost(), "PHPSESSID", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String packageInfos[];
            if (false) {
                // does not contain expire date but billing date
                if (br.getRequest() == null || !StringUtils.endsWithCaseInsensitive(br.getURL(), "/member/payment-history")) {
                    br.getPage("/member/payment-history");
                }
                final String paymentHistory = br.getRegex("(?i)>\\s*(?:Your)?\\s*Payments? history\\s*<.*<table.*?>(.*?)</table>").getMatch(0);
                packageInfos = new Regex(paymentHistory, "<tr[^>]*>(.*?)</tr>").getColumn(0);
            } else {
                if (br.getRequest() == null || !StringUtils.endsWithCaseInsensitive(br.getURL(), "/member")) {
                    br.getPage("/member");
                }
                final String paymentHistory = br.getRegex("(?i)<ul[^>]*id\\s*=\\s*\"member-subscriptions\"[^>]*>(.*?)</ul>").getMatch(0);
                packageInfos = new Regex(paymentHistory, "<li[^>]*>(.*?)</li>").getColumn(0);
            }
            if (packageInfos == null || packageInfos.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            boolean subscriptionFound = false;
            for (String packageInfo : packageInfos) {
                String product = new Regex(packageInfo, "history-items\"[^>]*>\\s*(.*?)\\s*</td>").getMatch(0);
                if (product == null) {
                    product = new Regex(packageInfo, "subscriptions-title\"[^>]*>\\s*<strong>\\s*(.*?)\\s*</strong>").getMatch(0);
                }
                if (product == null || !product.matches("(?i).*(day|power|family)\\s*plan.*")) {
                    continue;
                } else {
                    ai.setStatus(product);
                    long validUntil = -1;
                    String expireDate = new Regex(packageInfo, "date\"[^>]*>\\s*(\\d{2}/\\d{2}/\\d{4})\\s*<").getMatch(0);
                    if (expireDate != null) {
                        validUntil = TimeFormatter.getMilliSeconds(expireDate, "dd'/'MM'/'yyyy", Locale.ENGLISH) + (24 * 60 * 60 * 1000l);
                    } else {
                        expireDate = new Regex(packageInfo, "expires_date\"[^>]*data-date\\s*=\\s*\"(\\d{4}-\\d{2}-\\d{2})\"").getMatch(0);
                        validUntil = TimeFormatter.getMilliSeconds(expireDate, "yyyy'-'MM'-'dd", Locale.ENGLISH) + (24 * 60 * 60 * 1000l);
                    }
                    if (validUntil != -1) {
                        account.setType(AccountType.PREMIUM);
                        account.setMaxSimultanDownloads(100);
                        ai.setStatus(product);
                        ai.setValidUntil(validUntil);
                        if (!ai.isExpired()) {
                            subscriptionFound = true;
                            break;
                        }
                    }
                }
            }
            if (!subscriptionFound) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "No active usenet subscription found!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
            }
            throw e;
        }
        ai.setMultiHostSupport(this, Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    protected UsenetServer getUseNetServer(final Account account) throws Exception {
        final UsenetServer ret = super.getUseNetServer(account);
        if (AccountType.FREE.equals(account.getType())) {
            if (ret.isSSL()) {
                return new UsenetServer("free.hitnews.com", 563, true);
            } else {
                return new UsenetServer("free.hitnews.com", 119, false);
            }
        }
        return ret;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.hitnews.com", false, 119, 80));
        ret.addAll(UsenetServer.createServerList("ssl.hitnews.com", true, 563, 995));
        return ret;
    }
}
