package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eweka.nl" }, urls = { "" })
public class UseNetEwekaNl extends UseNet {
    public UseNetEwekaNl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.eweka.nl/en/usenet_toegang/specificaties/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.USENET, LazyPlugin.FEATURE.COOKIE_LOGIN_OPTIONAL };
    }

    @Override
    public String getAGBLink() {
        return "https://www.eweka.nl/en/av/";
    }

    public static interface EwekaNlConfigInterface extends UsenetAccountConfigInterface {
    };

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUseNetUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        // final String server = br.getRegex("<td><b>Server</b></td>.*?<td.*?>(.*?)</td>").getMatch(0);
        // final String port = br.getRegex("<td><b>Port</b></td>.*?<td.*?>(\\d+)</td>").getMatch(0);
        // TODO: use these infos for available servers
        getPage("/myeweka?p=acd");
        final String connections = br.getRegex("(?i)<td><b>Connections</b></td>.*?<td.*?>(\\d+)</td>").getMatch(0);
        if (connections != null) {
            account.setMaxSimultanDownloads(Integer.parseInt(connections));
        } else {
            /* Fallback */
            account.setMaxSimultanDownloads(8);
        }
        final String userNameHTML = br.getRegex("name=\"username\" value=\"([^<>\"]+)\"").getMatch(0);
        final String username;
        if (userNameHTML != null) {
            username = userNameHTML;
        } else {
            /* Fallback: Use user entered username as Usenet username */
            username = account.getUser();
        }
        /*
         * When using cookie login user can enter whatever he wants into username field but we try to have unique usernames so user cannot
         * add same account twice.
         */
        if (account.loadUserCookies() != null && userNameHTML != null) {
            account.setUser(userNameHTML);
        }
        account.setProperty(USENET_USERNAME, username);
        String validUntil = br.getRegex("(?i)<td><b>\\s*Valid until\\s*</b></td>.*?<td.*?>\\s*?(\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2})").getMatch(0);
        if (validUntil == null) {
            /* 2020-01-21 */
            validUntil = br.getRegex("(?i)>\\s*Next billing at</b></td>\\s*<td>(\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2})").getMatch(0);
        }
        if (validUntil == null) {
            /* 2020-01-21 - wide open RegEx as fallback */
            validUntil = br.getRegex("(\\d{2}-\\d{2}-\\d{4} \\d{2}:\\d{2})").getMatch(0);
        }
        if (validUntil != null) {
            final long date = TimeFormatter.getMilliSeconds(validUntil, "dd'-'MM'-'yyyy' 'HH:mm", null);
            if (date > 0) {
                ai.setValidUntil(date, br);
            }
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        }
        ai.setMultiHostSupport(this, Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    private void login(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                /* 2021-09-03: Added cookie login as possible workaround for them using Cloudflare on login page. */
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null) {
                    logger.info("Checking login user cookies");
                    if (!verifyCookies) {
                        logger.info("Trust user login cookies without check");
                        br.setCookies(userCookies);
                        return;
                    }
                    if (checkLogin(br, userCookies)) {
                        logger.info("Successfully loggedin via user cookies");
                        return;
                    } else {
                        if (account.hasEverBeenValid()) {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                        } else {
                            throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                        }
                    }
                }
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    if (!verifyCookies) {
                        logger.info("Trust login cookies without check");
                        br.setCookies(cookies);
                        return;
                    }
                    logger.info("Checking login cookies");
                    if (checkLogin(br, cookies)) {
                        logger.info("Failed to login via cookies");
                        /* Delete old cookies */
                        account.clearCookies("");
                        br.getCookies(getHost()).clear();
                    } else {
                        logger.info("Successfully loggedin via cookies");
                        account.saveCookies(br.getCookies(getHost()), "");
                        return;
                    }
                }
                logger.info("Performing full login");
                getPage("https://www." + this.getHost() + "/myeweka/?lang=en");
                final Form loginform = br.getFormbyProperty("id", "login-form");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.setMethod(MethodType.POST);
                loginform.put("identifier", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                submitForm(loginform);
                if (!isLoggedIN(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(getHost()), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean checkLogin(final Browser br, final Cookies cookies) throws Exception {
        br.setCookies(cookies);
        getPage("https://www." + this.getHost() + "/en/myeweka?p=pro");
        if (isLoggedIN(br)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isLoggedIN(final Browser br) {
        String logintoken = br.getCookie(getHost(), "auth-token", Cookies.NOTDELETEDPATTERN);
        return !StringUtils.isEmpty(logintoken) && !logintoken.equals("\"\"");
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("newsreader.eweka.nl", false, 119));// resolves to 3 IP
        // ret.addAll(UsenetServer.createServerList("newsreader124.eweka.nl", false, 119));//resolves to 1 IP
        ret.addAll(UsenetServer.createServerList("sslreader.eweka.nl", true, 563, 443));// resolves to 3 IP
        return ret;
    }
}
