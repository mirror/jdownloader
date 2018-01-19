package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
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
        final Cookies cookies = account.loadCookies("");
        try {
            if (cookies != null) {
                br.setCookies(getHost(), cookies);
                br.getPage("https://www.usenext.com/UsenextDE/MemberAreaInt/misc/tutorial/tuIndex.cfm?sLangToken=ENG");
                final String accountStatus = br.getRegex("Account status:.*?<span class=\".*?\">(.*?)</span>").getMatch(0);
                if (!StringUtils.equalsIgnoreCase(accountStatus, "OK")) {
                    br.getCookies(getHost()).clear();
                }
            }
            if (br.getCookie(getHost(), "SNUUID") == null) {
                account.clearCookies("");
                br.getPage("https://www.usenext.com/");
                final Form login = br.getFormbyKey("__RequestVerificationToken");
                login.setMethod(MethodType.POST);
                login.put("Username", Encoding.urlEncode(account.getUser()));
                login.put("Password", Encoding.urlEncode(account.getPass()));
                login.setAction("https://www.usenext.com/en-US/Account/LogInAjax");
                br.submitForm(login);
                final String url = br.getRegex("\"url\"\\s*:\\s*\"(https?.*?)\"").getMatch(0);
                if (url != null) {
                    br.getPage(Encoding.unicodeDecode(url));
                } else {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (br.getCookie(getHost(), "SNUUID") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            br.getPage("/UseNeXTDE/MemberAreaInt/obj/user/usEdit.cfm?sLangToken=ENG");
            account.saveCookies(br.getCookies(getHost()), "");
            final String userName = br.getRegex("Username</label>.*?value=\"(avi-.*?)\"").getMatch(0);
            if (userName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                account.setProperty(USENET_USERNAME, userName.trim());
            }
            final String accountStatus = br.getRegex("Account status:.*?<span class=\".*?\">(.*?)</span>").getMatch(0);
            if (!StringUtils.equalsIgnoreCase(accountStatus, "OK")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account Status: " + accountStatus, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String downloadVolume = br.getRegex("Download Volume:.*?<span>(.*?)</").getMatch(0);
            if (downloadVolume == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                final String trafficLeft = downloadVolume.replaceAll("\r\n", "").replaceAll("\t+", " ").trim();
                ai.setTrafficLeft(trafficLeft);
            }
            br.getPage("/UseNeXTDE/MemberAreaInt/obj/user/uscontract.cfm?sLangToken=ENG");
            String validUntil = br.getRegex("Subscription through:</td>.*?<td>(\\d+/\\d+/\\d+)</").getMatch(0);
            if (validUntil == null) {
                validUntil = br.getRegex("Minimum term until:\\s*<.*?\"paket\"\\s*>\\s*(\\d+/\\d+/\\d+)\\s*</").getMatch(0);
            }
            final String bucketType = br.getRegex("My UseNeXT plan:\\s*</.*?\"paket\"\\s*>\\s*(.*?)\\s*</").getMatch(0);
            if (bucketType != null) {
                ai.setStatus(bucketType);
            } else {
                ai.setStatus("Unknown UseNeXT plan");
            }
            if (validUntil != null) {
                final long date = TimeFormatter.getMilliSeconds(validUntil, "MM/dd/yyyy", null);
                if (date > 0) {
                    ai.setValidUntil(date + (24 * 60 * 60 * 1000l));
                }
            }
            // TODO: check this
            account.setMaxSimultanDownloads(30);
        } catch (final PluginException e) {
            if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                account.clearCookies("");
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
        ret.addAll(UsenetServer.createServerList("high.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("high.usenext.de", true, 563));
        ret.addAll(UsenetServer.createServerList("flat.usenext.de", false, 119, 443));
        ret.addAll(UsenetServer.createServerList("flat.usenext.de", true, 563));
        return ret;
    }
}
