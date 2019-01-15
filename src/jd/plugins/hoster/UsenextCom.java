package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import jd.PluginWrapper;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.components.usenet.UsenetAccountConfigInterface;
import org.jdownloader.plugins.components.usenet.UsenetServer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "usenext.com" }, urls = { "" })
public class UsenextCom extends UseNet {
    public UsenextCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.usenext.com/signup");
    }

    @Override
    public String getAGBLink() {
        return "https://www.usenext.com/terms";
    }

    private final String USENET_USERNAME = "USENET_USERNAME";

    @Override
    protected String getUsername(Account account) {
        return account.getStringProperty(USENET_USERNAME, account.getUser());
    }

    public static interface UsenextConfigInterface extends UsenetAccountConfigInterface {
    };

    @Override
    public void update(final DownloadLink downloadLink, final Account account, long bytesTransfered) throws PluginException {
        final UsenetServer server = getLastUsedUsenetServer();
        if (server == null || !StringUtils.equalsIgnoreCase("flat.usenext.de", server.getHost())) {
            super.update(downloadLink, account, bytesTransfered);
        }
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account, AbstractProxySelectorImpl proxy) {
        if (account != null) {
            final UsenetAccountConfigInterface config = getAccountJsonConfig(account);
            if (config != null && StringUtils.equalsIgnoreCase("flat.usenext.de", config.getHost())) {
                return 4;
            }
        }
        return super.getMaxSimultanDownload(link, account, proxy);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        setBrowserExclusive();
        final AccountInfo ai = new AccountInfo();
        br.setFollowRedirects(true);
        final Cookies cookies = account.loadCookies("com");
        try {
            boolean freshLogin = true;
            if (cookies != null) {
                br.setCookies("usenext.com", cookies);
                final Cookies de = account.loadCookies("de");
                if (de != null) {
                    br.setCookies("usenext.de", de);
                }
                getPage(br, "https://www.usenext.com/en-US/ma/dashboard");
                if (br.getHostCookie("UseNeXT.MemberArea.WebSite.ServerApp_Auth", Cookies.NOTDELETEDPATTERN) == null) {
                    br.getCookies("usenext.com").clear();
                    br.getCookies("usenext.de").clear();
                } else if (br.getHostCookie("UseNeXT.MemberArea.WebSite.ServerApp_Session", Cookies.NOTDELETEDPATTERN) == null) {
                    br.getCookies("usenext.com").clear();
                    br.getCookies("usenext.de").clear();
                } else {
                    freshLogin = false;
                }
            }
            if (freshLogin) {
                account.clearCookies("");
                getPage(br, "https://www.usenext.com/");
                getPage(br, "https://auth.usenext.de/login");
                final Form login = br.getFormbyKey("username");
                if (login == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                submitForm(br, login);
                if (br.getHostCookie("UseNeXT.AuthorizationServerApp_Auth", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getHostCookie("UseNeXT.AuthorizationServerApp_Session", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                getPage(br, "https://www.usenext.com/en-US/ma/dashboard");
                if (br.getHostCookie("UseNeXT.MemberArea.WebSite.ServerApp_Auth", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.getHostCookie("UseNeXT.MemberArea.WebSite.ServerApp_Session", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            account.saveCookies(br.getCookies("usenext.com"), "com");
            account.saveCookies(br.getCookies("usenext.de"), "de");
            final String currentPlan = br.getRegex("<label>\\s*Current Plan\\s*</label>\\s*<h6>\\s*(.*?)\\s*</h").getMatch(0);
            if (currentPlan != null) {
                ai.setStatus(Encoding.htmlDecode(currentPlan));
            } else {
                ai.setStatus("Unknown UseNeXT plan");
            }
            final String availableTraffic[][] = br.getRegex("<span class=\"donut-label-inner\\s*\"\\s*>\\s*([0-9,\\.]+)\\s*<br/>\\s*<small>\\s*([KTMG]B)\\s*<").getMatches();
            if (availableTraffic == null || availableTraffic.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                final long trafficNormal = SizeFormatter.getSize(availableTraffic[0][0] + availableTraffic[0][1]);
                final long trafficBoost;
                if (availableTraffic.length == 2) {
                    trafficBoost = SizeFormatter.getSize(availableTraffic[1][0] + availableTraffic[1][1]);
                } else {
                    trafficBoost = 0;
                }
                final String dataVolume[] = br.getRegex("<label>\\s*Data Volume\\s*</label>\\s*</div>\\s*<div.*?>\\s*([0-9,\\.]+)\\s*<small>\\s*([KTMG]B)\\s*<").getRow(0);
                if (dataVolume != null && dataVolume.length == 1) {
                    ai.setTrafficMax(dataVolume[0] + dataVolume[1]);
                }
                ai.setTrafficLeft(trafficNormal + trafficBoost);
            }
            final String duration = br.getRegex("<label>\\s*Duration\\s*</label>\\s*</div>\\s*<div.*?>\\s*(\\d+ (months|days|weeks|years))\\s*<").getMatch(0);
            final String activationDate = br.getRegex("<label>\\s*Activation Date\\s*</label>\\s*</div>\\s*<div.*?>.*?<span>\\s*(\\d+/\\d+/\\d+)\\s*<").getMatch(0);
            if (duration != null && activationDate != null) {
                if (StringUtils.containsIgnoreCase(duration, "months")) {
                    final String months = new Regex(duration, "(\\d+)").getMatch(0);
                    final long activationTimeStamp = TimeFormatter.getMilliSeconds(activationDate, "MM/dd/yyyy", Locale.ENGLISH);
                    if (activationTimeStamp > 0) {
                        final Calendar cl = Calendar.getInstance(Locale.ENGLISH);
                        cl.setTimeInMillis(activationTimeStamp);
                        cl.add(Calendar.MONTH, Integer.parseInt(months));
                        ai.setValidUntil(cl.getTimeInMillis() + (24 * 60 * 60 * 1000l));
                    }
                }
            }
            // TODO: check this
            account.setMaxSimultanDownloads(30);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("com");
                account.clearCookies("de");
            }
            throw e;
        }
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "usenet" }));
        return ai;
    }

    @Override
    public List<UsenetServer> getAvailableUsenetServer() {
        final List<UsenetServer> ret = new ArrayList<UsenetServer>();
        ret.addAll(UsenetServer.createServerList("news.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("news.usenext.de", true, 563));
        ret.addAll(UsenetServer.createServerList("flat.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("flat.usenext.de", true, 563));
        ret.addAll(UsenetServer.createServerList("high.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("high.usenext.de", true, 563));
        return ret;
    }
}
