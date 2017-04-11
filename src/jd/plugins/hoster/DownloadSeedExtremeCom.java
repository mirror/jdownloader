package jd.plugins.hoster;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "download.seedextreme.com" }, urls = { "" }) public class DownloadSeedExtremeCom extends PluginForHost {

    public DownloadSeedExtremeCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://download.seedextreme.com");
    }

    @Override
    public String getAGBLink() {
        return "http://download.seedextreme.com";
    }

    private void login(Account account) throws Exception {
        synchronized (account) {
            final Cookies cookies = account.loadCookies("");
            try {
                Form login = null;
                if (cookies != null) {
                    br.setCookies(getHost(), cookies);
                    br.getPage("http://download.seedextreme.com/app/page/dashboard/");
                    login = br.getFormbyActionRegex("#signin");
                    if (login != null && login.containsHTML("username_l") && login.containsHTML("password_l")) {
                        br.getCookies(getHost()).clear();
                    }
                    if (br.getCookie(getHost(), "_session") == null) {
                        br.getCookies(getHost()).clear();
                    }
                }
                if (br.getCookie(getHost(), "_session") == null) {
                    account.clearCookies("");
                    br.setCookie(getHost(), "language", "en");
                    br.getPage("http://download.seedextreme.com/app/page/login/");
                    login = br.getFormbyActionRegex("#signin");
                    login.put("username_l", Encoding.urlEncode(account.getUser()));
                    login.put("password_l", Encoding.urlEncode(account.getPass()));
                    br.submitForm(login);
                    login = br.getFormbyActionRegex("#signin");
                    if (login != null && login.containsHTML("username_l") && login.containsHTML("password_l")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    if (br.getCookie(getHost(), "_session") == null) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                if (!StringUtils.containsIgnoreCase(br.getURL(), "app/page/dashboard")) {
                    br.getPage("http://download.seedextreme.com/app/page/dashboard/");
                }
            } catch (final PluginException e) {
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
        br.setFollowRedirects(true);
        login(account);
        account.saveCookies(br.getCookies(getHost()), "");
        final String expireDate = br.getRegex("<div class=\"number\">\\s*<span>\\s*(\\d+-\\d+-\\d+)\\s*</span>\\s*</div>\\s*<div class=\"desc\">\\s*Expiration Date\\s*</div>").getMatch(0);
        final String status = br.getRegex("<div class=\"number\">\\s*<span>\\s*([^<]*?)\\s*</span>\\s*</div>\\s*<div class=\"desc\">\\s*Status\\s*</div>").getMatch(0);
        final String trafficAvailable = br.getRegex("<div class=\"number\">\\s*<span>\\s*([^<]*?)\\s*</span>\\s*</div>\\s*<div class=\"desc\">\\s*Bandwidth Package Available\\s*</div>").getMatch(0);
        if ("premium".equalsIgnoreCase(status)) {
            ai.setStatus("premium");
            account.setType(AccountType.PREMIUM);
            if ("-".equals(trafficAvailable)) {
                ai.setUnlimitedTraffic();
            } else {
                ai.setTrafficLeft(SizeFormatter.getSize(trafficAvailable));
            }
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy'-'MM'-'dd", Locale.ENGLISH) + (24 * 60 * 60 * 1000l));
        } else if ("free".equalsIgnoreCase(status)) {
            account.setType(AccountType.FREE);
            ai.setStatus("free");
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        account.setMaxSimultanDownloads(20);
        ai.setProperty("multiHostSupport", Arrays.asList(new String[] { "uploaded.to" }));
        return ai;
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
    }

    private final String GENERATED_LINK = "GLINK";

    @Override
    public void handleMultiHost(DownloadLink downloadLink, Account account) throws Exception {
        String link = downloadLink.getStringProperty(GENERATED_LINK);
        if (link != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(link);
                if (!con.isContentDisposition() || con.getContentType().contains("html")) {
                    downloadLink.removeProperty(GENERATED_LINK);
                    link = null;
                }
            } catch (Exception e) {
                downloadLink.removeProperty(GENERATED_LINK);
                link = null;
            } finally {
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable e) {
                }
            }
        }
        if (link == null) {
            login(account);
            final String responseString = br.getPage("http://download.seedextreme.com/?page=api&action=generateLink&link=" + Encoding.urlEncode(downloadLink.getDefaultPlugin().buildExternalDownloadURL(downloadLink, this)));
            final Map<String, Object> response = JSonStorage.restoreFromString(responseString, TypeRef.HASHMAP, null);
            if (response == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (response.get("error") != null && ((Number) response.get("error")).intValue() == 10) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE);
            }
            final String message = (String) response.get("message");
            if (!"OK".equalsIgnoreCase(message)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link = (String) response.get("link");
            if (link == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String fileName = (String) response.get("filename");
            if (fileName != null && downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName(fileName);
            }
            final String fileSize = (String) response.get("size");
            if (fileSize != null && downloadLink.getKnownDownloadSize() == -1) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
            }
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 1);
        if (!dl.getConnection().isContentDisposition() || dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(GENERATED_LINK, link);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        if (link != null) {
            link.removeProperty(GENERATED_LINK);
        }
    }

}
