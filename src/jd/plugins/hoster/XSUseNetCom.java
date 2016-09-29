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

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision: 31032 $", interfaceVersion = 3, names = { "xsusenet.com" }, urls = { "" })
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

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    @Override
    protected UsenetServer getUsenetServer(Account account) throws Exception {
        final UsenetServer ret = super.getUsenetServer(account);
        final AccountInfo ai = account.getAccountInfo();
        if (account.getMaxSimultanDownloads() == 5 || (ai != null && StringUtils.contains(ai.getStatus(), "free"))) {
            // ssl is not supported for free accounts
            return getAvailableUsenetServer().get(0);
        } else {
            return ret;
        }
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
                br.getPage("https://my.xsusenet.com/index.php");
                login = br.getFormbyActionRegex("checklogin");
                if (login != null && login.containsHTML("name=\"username\"") && login.containsHTML("name=\"password\"")) {
                    br.getCookies(getHost()).clear();
                } else if (br.getCookie(getHost(), "PHPSESSID") == null) {
                    br.getCookies(getHost()).clear();
                } else {
                    br.getPage("https://my.xsusenet.com/packages.php");
                }
            }
            if (br.getCookie(getHost(), "PHPSESSID") == null) {
                account.clearCookies("");
                final String userName = account.getUser();
                if (userName == null || !userName.matches("^.+?@.+?\\.[^\\.]+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please enter your e-mail/password for xsusenet.com website!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.getPage("https://my.xsusenet.com/index.php");
                login = br.getFormbyActionRegex("checklogin");
                login.put("username", Encoding.urlEncode(userName));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                login = br.getFormbyActionRegex("checklogin");
                if (login != null && login.containsHTML("name=\"username\"") && login.containsHTML("name=\"password\"")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getCookie(getHost(), "PHPSESSID") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            if (!StringUtils.containsIgnoreCase(br.getURL(), "https://my.xsusenet.com/packages.php")) {
                br.getPage("https://my.xsusenet.com/packages.php");
            }
            account.saveCookies(br.getCookies(getHost()), "");
            final String[] ids = br.getRegex("<tr>(.*?)</tr>").getColumn(0);
            for (final String id : ids) {
                if (StringUtils.containsIgnoreCase(id, "<td>active</td>") && StringUtils.contains(id, "Usenet")) {
                    final String detailsID = new Regex(id, "details\\.php\\?id=(\\d+)").getMatch(0);
                    if (detailsID != null) {
                        br.getPage("https://my.xsusenet.com/details.php?id=" + detailsID);
                        final String userName = br.getRegex("<li><strong>Usenet username</strong></li>.*?<li><strong>(.*?)</strong>").getMatch(0);
                        if (userName == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        } else {
                            account.setProperty(USENET_USERNAME, userName.trim());
                        }
                        final String packageType = br.getRegex("<li>Package</li>.*?<li>(.*?)</li>").getMatch(0);
                        if (packageType != null) {
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
                            ai.setStatus("Unknown packageType! Please contact JDownloader support at support@jdownloader.org");
                            // Free account: 5 connections
                            account.setMaxSimultanDownloads(5);
                        }
                        final String endDate = br.getRegex("<li>End date</li>.*?<li>(\\d+-\\d+-\\d+)</li>").getMatch(0);
                        if (endDate != null) {
                            final long date = TimeFormatter.getMilliSeconds(endDate, "yyyy'-'MM'-'dd", null);
                            if (date > 0) {
                                ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                            }
                        }
                        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
                        return ai;
                    }
                }
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        ret.addAll(UsenetServer.createServerList("reader.xsusenet.com", false, 80, 119));
        ret.addAll(UsenetServer.createServerList("reader.xsusenet.com", true, 563, 443));
        // ret.addAll(UsenetServer.createServerList("free.xsusenet.com", false, 119));//free not supported
        return ret;
    }
}
