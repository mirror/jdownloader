package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;

@HostPlugin(revision = "$Revision: 36468 $", interfaceVersion = 3, names = { "bentbox.co" }, urls = { "" })
public class BentBoxCo extends PluginForHost {
    public BentBoxCo(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("https://bentbox.co/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://bentbox.co/terms";
    }

    private static boolean isCookieSet(Browser br, String key) {
        final String value = br.getCookie("bentbox.co", key);
        return StringUtils.isNotEmpty(value) && !StringUtils.equalsIgnoreCase(value, "deleted");
    }

    public static void login(Browser br, Account account) throws Exception {
        synchronized (account) {
            try {
                br.getHeaders().put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/61.0.3163.100 Safari/537.36");
                final String host = "bentbox.co";
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(host, cookies);
                    br.getPage("https://bentbox.co/");
                }
                if (!isCookieSet(br, "userId") || !isCookieSet(br, "accessToken")) {
                    final String userName = account.getUser();
                    if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail/password for bentbox.co website!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    account.clearCookies("");
                    br.getPage("https://bentbox.co/signin");
                    final Form login = new Form();
                    login.setMethod(MethodType.POST);
                    login.setAction("https://bentbox.co/signin_check");
                    login.put("email_address", Encoding.urlEncode(userName));
                    login.put("password", Encoding.urlEncode(account.getPass()));
                    login.put("robot_check", "notarobot");
                    login.put("redirectURL", "?");
                    br.submitForm(login);
                    if (!isCookieSet(br, "userId") || !isCookieSet(br, "accessToken")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.setCookie(host, "adultFilter", "off");
                account.saveCookies(br.getCookies(host), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        login(br, account);
        ai.setStatus("Valid Account");
        return ai;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        throw new AccountRequiredException();
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
