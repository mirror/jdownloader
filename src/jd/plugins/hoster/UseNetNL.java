package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "usenet.nl" }, urls = { "" })
public class UseNetNL extends UseNet {
    public UseNetNL(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://en.usenet.nl/unf/shop/misc/miscShowFullPackages.cfm/");
    }

    @Override
    public String getAGBLink() {
        return "https://en.usenet.nl/gtc/";
    }

    public static interface UseNetNLConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("");
        try {
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                br.getPage("https://en.usenet.nl/unf/memberarea/obj/user/usShowUpgrade.cfm");
                final Form login = br.getFormbyProperty("id", "login-modal-form");
                if (br.getCookie(getHost(), "SNUUID") == null || login != null) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "SNUUID") == null) {
                account.clearCookies("");
                br.getPage("https://en.usenet.nl/");
                final Form login = new Form();
                login.setAction("/unf/shop/obj/user/userLogin.cfm");
                login.setMethod(MethodType.POST);
                login.put("sUsername", Encoding.urlEncode(account.getUser()));
                login.put("sPassword", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                final Map<String, Object> response = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
                if (Boolean.TRUE.equals(response.get("SUCCESS"))) {
                    br.getPage((String) response.get("URL"));
                } else {
                    final Map<String, Object> errors = ((Map<String, Object>) response.get("ERRORS"));
                    final String error;
                    if (errors.containsKey("PASSWORD")) {
                        error = (String) errors.get("PASSWORD");
                    } else if (errors.containsKey("LOGIN")) {
                        error = (String) errors.get("LOGIN");
                    } else {
                        error = null;
                    }
                    if (error != null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, error, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (!StringUtils.endsWithCaseInsensitive(br.getURL(), "miscShowHomePage.cfm")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "SNUUID") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("https://en.usenet.nl/unf/memberarea/obj/user/usShowUpgrade.cfm");
            }
            br.getPage("?sLangToken=ENG");
            account.saveCookies(br.getCookies(getHost()), "");
            final String currentDownloadVolume = br.getRegex("Current download volume\\s*<br />\\s*([0-9\\. GBMT]+)").getMatch(0);
            final String currentPlan = br.getRegex("My usenet.nl plan:</td>\\s*<td>\\s*(.*?)\\s*</td>").getMatch(0);
            final String validUntil = br.getRegex("Contract valid until:</td>\\s*<td>\\s*(\\d+\\s*/\\s*\\d+\\s*/\\s*\\d+)\\s*</td>").getMatch(0);
            final String accountStatus = br.getRegex("Account status:\\s*</strong>\\s*\\<br />\\s*<span.*?>\\s*(.*?)\\s*</span>").getMatch(0);
            if ("OK".equals(accountStatus) && validUntil != null) {
                ai.setStatus("Your usenet.nl plan: " + currentPlan);
                final long date = TimeFormatter.getMilliSeconds(validUntil.replace(" ", ""), "MM'/'dd'/'yyyy", null);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                }
                if (currentDownloadVolume != null) {
                    ai.setTrafficLeft(currentDownloadVolume);
                }
                account.setMaxSimultanDownloads(16);
                ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 2 * 60 * 60 * 1000l);
                return ai;
            }
            if (accountStatus != null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Accountstatus: " + accountStatus, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
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
        ret.addAll(UsenetServer.createServerList("power.usenet.nl", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("power.usenet.nl", true, 563));
        ret.addAll(UsenetServer.createServerList("eco.usenet.nl", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("eco.usenet.nl", true, 563));
        return ret;
    }
}
