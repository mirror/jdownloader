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

import org.appwork.uio.CloseReason;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.usenet.InvalidAuthException;
import org.jdownloader.gui.dialog.AskDownloadPasswordDialogInterface;
import org.jdownloader.gui.dialog.AskForPasswordDialog;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "maximumusenet.com" }, urls = { "" })
public class MaximumUsenetCom extends UseNet {
    public MaximumUsenetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.maximumusenet.com/choose-your-plan.php");
    }

    @Override
    public String getAGBLink() {
        return "http://www.maximumusenet.com/terms.php";
    }

    public static interface MaximumUsenetComConfigInterface extends UsenetAccountConfigInterface {
    };

    private final String USENET_USERNAME = "USENET_USERNAME";
    private final String USENET_PASSWORD = "USENET_PASSWORD";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    @Override
    protected String getPassword(Account account) {
        return account.getStringProperty(USENET_PASSWORD, account.getPass());
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
                br.getPage("https://www.maximumusenet.com/members/");
                if (br.containsHTML(">Logging in...<")) {
                    Thread.sleep(2000);
                    br.getPage("https://www.maximumusenet.com/members/index.php");
                }
                login = br.getFormbyActionRegex(".*members/login.php");
                if (login != null && login.containsHTML("memberid") && login.containsHTML("password")) {
                    br.clearCookies(getHost());
                } else if (br.containsHTML("Invalid User ID/Password!")) {
                    br.clearCookies(getHost());
                } else if (!br.containsHTML("members/logout.php")) {
                    br.clearCookies(getHost());
                }
            }
            if (br.getCookie(getHost(), "PHPSESSID") == null) {
                account.clearCookies("");
                final String userName = account.getUser();
                br.getPage("https://www.maximumusenet.com/members/members_login.php");
                login = br.getFormbyActionRegex(".*members/login.php");
                login.put("memberid", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                final String equation[] = login.getRegex(">\\s*(\\d+)\\s*(\\+|-|\\*|/)\\s*(\\d+)\\s*=").getRow(0);
                if (equation == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Integer result;
                if ("+".equals(equation[1])) {
                    result = Integer.parseInt(equation[0]) + Integer.parseInt(equation[2]);
                } else if ("*".equals(equation[1])) {
                    result = Integer.parseInt(equation[0]) * Integer.parseInt(equation[2]);
                } else if ("/".equals(equation[1])) {
                    result = Integer.parseInt(equation[0]) / Integer.parseInt(equation[2]);
                } else {
                    result = Integer.parseInt(equation[0]) - Integer.parseInt(equation[2]);
                }
                login.put("captcha_code", result.toString());
                br.submitForm(login);
                if (br.containsHTML(">Logging in...<")) {
                    Thread.sleep(2000);
                    br.getPage("https://www.maximumusenet.com/members/index.php");
                }
                login = br.getFormbyActionRegex(".*members/login.php");
                if (login != null && login.containsHTML("memberid") && login.containsHTML("password")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.containsHTML("Invalid User ID/Password!") || !br.containsHTML("members/logout.php")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String memberID = br.getRegex("Member Id:\\s*</span>\\s*<span\\s*class=\".*?\">(.*?)<").getMatch(0);
            if (memberID == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(USENET_USERNAME, memberID.trim());
            }
            final String status = br.getRegex("Status:\\s*</span>\\s*<span\\s*class=\".*?\">(.*?)<").getMatch(0);
            if (!"active".equalsIgnoreCase(status)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account not active!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String nextBillingDate = br.getRegex("Next Billing Date:\\s*</span>\\s*<span\\s*class=\".*?\">(\\d+-\\d+-\\d+)").getMatch(0);
            if (nextBillingDate != null) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(nextBillingDate, "yyyy'-'MM'-'dd", Locale.ENGLISH) + (24 * 60 * 60 * 1000l));
            }
            account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 5 * 60 * 60 * 1000l);
            account.setMaxSimultanDownloads(50);
            ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
            try {
                verifyUseNetLogins(account);
                return ai;
            } catch (InvalidAuthException e) {
                logger.log(e);
                final DownloadLink dummyLink = new DownloadLink(this, "Account:" + getUsername(account), getHost(), "https://www.maximumusenet.com", true);
                final AskDownloadPasswordDialogInterface handle = UIOManager.I().show(AskDownloadPasswordDialogInterface.class, new AskForPasswordDialog("Please enter your MaximumUsenet Usenet Password", dummyLink));
                if (handle.getCloseReason() == CloseReason.OK) {
                    final String password = handle.getText();
                    if (StringUtils.isNotEmpty(password)) {
                        account.setProperty(USENET_PASSWORD, password);
                        try {
                            verifyUseNetLogins(account);
                            return ai;
                        } catch (InvalidAuthException e2) {
                            logger.log(e2);
                        }
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
                account.removeProperty(USENET_PASSWORD);
            }
            throw e;
        }
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("useast.maximumusenet.com", false, 119, 443, 8080));
        ret.addAll(UsenetServer.createServerList("us-secure.maximumusenet.com", true, 563, 80, 81));

        ret.addAll(UsenetServer.createServerList("europe.maximumusenet.com", false, 119, 443, 8080));
        ret.addAll(UsenetServer.createServerList("europe-ssl.maximumusenet.com", true, 563, 80, 81));
        return ret;
    }
}
