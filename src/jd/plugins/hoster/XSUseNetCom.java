package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "xsusenet.com" }, urls = { "" })
public class XSUseNetCom extends UseNet {
    public XSUseNetCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.xsusenet.com/sign-up/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.xsusenet.com/terms-of-service/";
    }

    public static interface XSUseNetComConfigInterface extends UsenetAccountConfigInterface {
    };

    private final String USENET_USERNAME = "USENET_USERNAME";

    private final String USENET_PASSWORD = "USENET_PASSWORD";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    @Override
    protected String getPassword(Account account) {
        return account.getStringProperty(USENET_PASSWORD, account.getUser());
    }

    @Override
    protected UsenetServer getUsenetServer(Account account) throws Exception {
        final UsenetServer ret = super.getUsenetServer(account);
        if (AccountType.FREE.equals(account.getType())) {
            if (ret.getHost().startsWith("free")) {
                return ret;
            } else {
                return new UsenetServer("free.xsusenet.com", 119);
            }
        } else {
            return ret;
        }
    }

    private boolean containsSessionCookie(Browser br) {
        final Cookies cookies = br.getCookies(getHost());
        for (final Cookie cookie : cookies.getCookies()) {
            if (cookie.getKey().startsWith("WHMCS") && !"deleted".equals(cookie.getValue())) {
                return true;
            }
        }
        return false;
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
                br.getPage("https://portal.xsusenet.com/clientarea.php");
                login = br.getFormbyActionRegex("dologin");
                if (login != null && login.containsHTML("name=\"username\"") && login.containsHTML("name=\"password\"")) {
                    br.getCookies(getHost()).clear();
                } else if (!containsSessionCookie(br)) {
                    br.getCookies(getHost()).clear();
                } else {
                    br.getPage("https://portal.xsusenet.com/clientarea.php?action=services");
                }
            }
            if (!containsSessionCookie(br)) {
                account.clearCookies("");
                final String userName = account.getUser();
                if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail/password for xsusenet.com website!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("https://portal.xsusenet.com/clientarea.php");
                login = br.getFormbyActionRegex("dologin");
                login.put("username", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex("dologin");
                if (login != null && login.containsHTML("name=\"username\"") && login.containsHTML("name=\"password\"")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (!containsSessionCookie(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (!StringUtils.containsIgnoreCase(br.getURL(), "https://portal.xsusenet.com/clientarea.php?action=services")) {
                br.getPage("https://portal.xsusenet.com/clientarea.php?action=services");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final HashSet<String> idMap = new HashSet<String>();
            final String[] ids = br.getRegex("clientarea\\.php\\?action=productdetails&amp;id=(\\d+)").getColumn(0);
            for (final String id : ids) {
                if (idMap.add(id)) {
                    br.getPage("https://portal.xsusenet.com/clientarea.php?action=productdetails&id=" + id);
                    final boolean isActive = br.containsHTML("product-status-text\">\\s*Active");
                    if (isActive) {
                        final boolean isFree = br.containsHTML("<h4>FREE</h4>") || br.containsHTML("free\\.xsusenet\\.com");
                        final String userName = br.getRegex("<strong>Your Username</strong>.*?>\\s*(\\d+)\\s*<").getMatch(0);
                        if (userName == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else {
                            account.setProperty(USENET_USERNAME, userName.trim());
                        }
                        final String packageType = br.getRegex("<li>Package</li>.*?<li>(.*?)</li>").getMatch(0);
                        if (packageType != null && !isFree) {
                            account.setType(Account.AccountType.PREMIUM);
                            ai.setStatus(packageType);
                            if (packageType.contains("200")) {
                                // 200 Mbit package: 50 connection
                                account.setMaxSimultanDownloads(50);
                            } else if (packageType.contains("150")) {
                                // 150 Mbit package: 50 connection
                                account.setMaxSimultanDownloads(50);
                            } else if (packageType.contains("100")) {
                                // 100 Mbit package: 50 connections
                                account.setMaxSimultanDownloads(50);
                            } else if (packageType.contains("50")) {
                                // 50 Mbit package: 40 connection
                                account.setMaxSimultanDownloads(40);
                            } else if (packageType.contains("25")) {
                                // 25 Mbit package: 30 connections
                                account.setMaxSimultanDownloads(30);
                            } else if (packageType.contains("10")) {
                                // 10 Mbit package: 20 connections
                                account.setMaxSimultanDownloads(20);
                            } else {
                                // Free account: 5 connections(fallback)
                                account.setMaxSimultanDownloads(5);
                            }
                        } else {
                            // Free account: 5 connections
                            account.setType(Account.AccountType.FREE);
                            account.setMaxSimultanDownloads(5);
                        }
                        final String endDate = br.getRegex("<li>End date</li>.*?<li>(\\d+-\\d+-\\d+)</li>").getMatch(0);
                        if (endDate != null && !isFree) {
                            final long date = TimeFormatter.getMilliSeconds(endDate, "yyyy'-'MM'-'dd", null);
                            if (date > 0) {
                                ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                            }
                        }
                        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                        account.setProperty(Account.PROPERTY_REFRESH_TIMEOUT, 5 * 60 * 60 * 1000l);
                        try {
                            verifyUseNetLogins(account);
                            return ai;
                        } catch (InvalidAuthException e) {
                            logger.log(e);
                            final DownloadLink dummyLink = new DownloadLink(this, "Account:" + getUsername(account), getHost(), "https://www.xsusenet.com/", true);
                            final AskDownloadPasswordDialogInterface handle = UIOManager.I().show(AskDownloadPasswordDialogInterface.class, new AskForPasswordDialog("Please enter your XSUsenet Usenet Password", dummyLink));
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
        ret.addAll(UsenetServer.createServerList("reader.xsusenet.com", false, 80, 119));
        ret.addAll(UsenetServer.createServerList("reader.xsusenet.com", true, 563, 443));
        ret.addAll(UsenetServer.createServerList("free.xsusenet.com ", false, 119, 443, 23, 80, 81, 8080, 2323, 8181));
        return ret;
    }
}
