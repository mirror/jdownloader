//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filemonkey.in" }, urls = { "https?://(www\\.)?filemonkey\\.in/file/[a-z0-9]+" }, flags = { 2 })
public class FileMonkeyIn extends PluginForHost {

    public static class StringContainer {
        public String string = null;
    }

    private static final String    mainURL       = "https://www.filemonkey.in";
    private final String           apiURL        = "https://www.filemonkey.in/api/v1";
    private static Object          LOCK          = new Object();
    private static StringContainer agent         = new StringContainer();
    private static AtomicBoolean   useAPI        = new AtomicBoolean(true);
    private final boolean          supportsHTTPS = true;
    private final boolean          enforcesHTTPS = true;

    public FileMonkeyIn(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mainURL + "/premium");
    }

    @Override
    public String getAGBLink() {
        return mainURL + "/tos";
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public static Browser prepareBrowser(Browser prepBr) {
        if (agent.string == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent.string);
        return prepBr;
    }

    /**
     * Corrects downloadLink.urlDownload().<br/>
     * <br/>
     * The following code respect the hoster supported protocols via plugin boolean settings and users config preference
     * 
     * @author raztoki
     * */
    @SuppressWarnings("unused")
    @Override
    public void correctDownloadLink(final DownloadLink downloadLink) {
        if (supportsHTTPS && enforcesHTTPS) {
            // does the site enforce the use of https?
            downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceFirst("http://", "https://"));
        } else if (!supportsHTTPS) {
            // link cleanup, but respect users protocol choosing.
            downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceFirst("https://", "http://"));
        }
        // output the hostmask as we wish based on COOKIE_HOST url!
        String desiredHost = new Regex(mainURL, "https?://([^/]+)").getMatch(0);
        String importedHost = new Regex(downloadLink.getDownloadURL(), "https?://([^/]+)").getMatch(0);
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll(importedHost, desiredHost));
    }

    private String getFUID(final DownloadLink downloadLink) {
        if (downloadLink == null) return null;
        final String fuid = new Regex(downloadLink.getDownloadURL(), "/file/([^/]+)").getMatch(0);
        return fuid;
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        final Browser br = new Browser();
        prepareBrowser(br);
        br.setCookiesExclusive(true);
        final String ul = "&uploads[]=";
        try {
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append(ul.replace("&", "?"));
                for (final DownloadLink dl : links) {
                    sb.append(getFUID(dl));
                    sb.append(ul);
                }
                // lets remove last "&uploads[]="
                sb.replace(sb.length() - ul.length(), sb.length(), "");
                br.getPage(apiURL + "/checkuploads" + sb.toString());
                for (final DownloadLink dl : links) {
                    final String filter = br.getRegex("\\{(\"upload\":\"" + getFUID(dl) + "\"[^\\}]+)").getMatch(0);
                    if (filter == null) dl.setAvailable(false);
                    final String status = new Regex(filter, "status\":\"((not)found|online)\"").getMatch(0);
                    if (status != null && ("found".equals(status) || "online".equals(status)))
                        dl.setAvailable(true);
                    else {
                        dl.setAvailable(false);
                        continue;
                    }
                    final String name = new Regex(filter, "\"name\":\"?([^\"]+)").getMatch(0);
                    if (name != null) dl.setName(name.trim());
                    final String size = new Regex(filter, "\"size\":\"?(\\d+)").getMatch(0);
                    if (size != null) {
                        dl.setDownloadSize(Long.parseLong(size));
                        try {
                            dl.setVerifiedFileSize(Long.parseLong(size));
                        } catch (final Throwable e) {
                        }
                    }
                    final String md5 = new Regex(filter, "md5\":\"([a=f0-9]{32})").getMatch(0);
                    if (md5 != null) dl.setMD5Hash(md5);
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return getAvailableStatus(link);
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) return (AvailableStatus) ret;
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        br = new Browser();
        prepareBrowser(br);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            br.getPage(downloadLink.getDownloadURL());
            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            final String reconnectWait = br.getRegex("Please wait (\\d{2}(\\.\\d{2})?) minutes before downloading another file").getMatch(0);
            if (reconnectWait != null) {
                final double reconWait = Double.parseDouble(reconnectWait) * 60;
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (long) reconWait * 1001 + 10000);
            }
            if (dllink == null) {
                final PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                final jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((jd.plugins.decrypter.LnkCrptWs) solveplug).getSolveMedia(br);
                File cf = null;
                try {
                    cf = sm.downloadCaptcha(getLocalCaptchaFile());
                } catch (final Exception e) {
                    if (br.containsHTML(">error: invalid ckey<")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 1 * 60 * 1000l);
                    throw e;
                }
                final String code = getCaptchaCode(cf, downloadLink);
                final String chid = sm.getChallenge(code);
                br.postPage(br.getURL(), "adcopy_challenge=" + Encoding.urlEncode(sm.getChallenge()) + "&adcopy_response=" + Encoding.urlEncode(chid));
                if (br.containsHTML("solvemedia\\.com/")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                dllink = br.getRegex("(\\'|\")(https?://[a-z0-9\\.\\-]+\\.filemonkey\\.in/download/[^<>\"]*?)(\\'|\")").getMatch(1);
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private static AtomicInteger maxPrem = new AtomicInteger(1);

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        synchronized (LOCK) {
            if (useAPI.get()) {
                ai = fetchAccountInfo_API(account, ai);
            }
        }
        return ai;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    public AccountInfo fetchAccountInfo_API(final Account account, final AccountInfo ai) throws Exception {
        synchronized (LOCK) {
            try {
                maxPrem.set(1);
                String apikey = login_API(account);
                if (apikey != null) {
                    br.getPage(apiURL + "/getaccountinfo?apikey=" + apikey);
                    account.setValid(true);
                    /* premium account */
                    final String traffic_left = getJson("trafficleft_bytes");
                    if (traffic_left != null) ai.setTrafficLeft(Long.parseLong(traffic_left));
                    // us ms to expire, works best with time zone difference and incorrect systemtime.
                    final String ms_expire = getJson("premium_mstoexpire");
                    if (ms_expire != null) ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(ms_expire));
                    final boolean is_premium = Boolean.parseBoolean(getJson("ispremium"));
                    if (is_premium && !ai.isExpired()) {
                        account.setProperty("free", false);
                        ai.setStatus("Premium account");
                        try {
                            maxPrem.set(-1);
                            account.setMaxSimultanDownloads(-1);
                            account.setConcurrentUsePossible(true);
                        } catch (final Throwable e) {
                            // not available in old Stable 0.9.581
                        }
                    } else {
                        account.setProperty("free", true);
                        ai.setStatus("Free account");
                        try {
                            maxPrem.set(-1);
                            account.setMaxSimultanDownloads(-1);
                            account.setConcurrentUsePossible(true);
                        } catch (final Throwable e) {
                            // not available in old Stable 0.9.581
                        }
                    }
                }
            } catch (PluginException e) {
                account.setValid(false);
                throw e;
            }
        }
        return ai;
    }

    private String login_API(final Account account) throws Exception {
        synchronized (LOCK) {
            // this never changes (apparently)
            String api_key = account.getStringProperty("apikey", null);
            if (api_key == null) {
                try {
                    prepareBrowser(br);
                    br.getPage(apiURL + "/getapikey?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (!isSuccess()) {
                        if (br.containsHTML("email\\\\/password combination wrong")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        final String captcha_required = getJson("error_code");
                        if (captcha_required != null && "err_captcha_required".equals(captcha_required)) {
                            br.getPage(apiURL + "/getcaptcha");
                            final String captcha_img = getJson("captchaurl");
                            final String captcha_id = getJson("captchaid");
                            if (captcha_img != null && captcha_id != null) {
                                DownloadLink dummyLink = new DownloadLink(this, "Account", this.getHost(), mainURL, true);
                                br.setFollowRedirects(true);
                                final String captchaValue = getCaptchaCode(captcha_img.replaceAll("\\\\/", "/").replace("http://", "https://"), dummyLink);
                                // getCaptchaCode(methodname, captchaFile, downloadLink)
                                br.getPage(apiURL + "/getapikey?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&captchaid=" + captcha_id + "&captchavalue=" + captchaValue);
                                if (!isSuccess()) {
                                    // wrong captcha?
                                    throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                                }
                            } else {
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                        api_key = getJson("apikey");
                    }
                } finally {
                    if (api_key != null)
                        account.setProperty("apikey", api_key);
                    else
                        account.setProperty("apikey", Property.NULL);
                }
            }
            return api_key;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (useAPI.get()) {
            handlePremium_API(link, account);
        } else {
            handlePremium_web(link, account);
        }
    }

    public void handlePremium_API(final DownloadLink downloadLink, final Account account) throws Exception {
        prepareBrowser(br);
        final String fuid = getFUID(downloadLink);
        synchronized (LOCK) {
            if (account.getBooleanProperty("free", false)) {
                handleFree(downloadLink);
                return;
            }
            String apikey = login_API(account);
            if (apikey == null) {
                // useAPI.set(false);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(apiURL + "/getpremiumdownloadlink?apikey=" + apikey + "&upload=" + fuid);
            if (isSuccess()) {
                String url = getJson("url");
                if (url == null) {
                    // useAPI.set(false);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url.replaceAll("\\\\/", "/"), true, 0);
                if (dl.getConnection().getContentType().contains("html")) {
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    if (br.containsHTML("<h2>Error 500</h2>[\r\n ]+<div class=\"error\">"))
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster is issues", 60 * 60 * 1000l);
                    else {
                        // useAPI.set(false);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                dl.startDownload();
            }
        }
    }

    public void handlePremium_web(final DownloadLink link, final Account account) throws Exception {
        if (true) return;
    }

    private String getJson(final String source, final String search) {
        String result = null;
        if (source == null || search == null) return result;
        result = new Regex(source, "\"" + search + "\":\"([^\"]+)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + search + "\":(true|false|\\d+)").getMatch(0);
        }
        return result;
    }

    private String getJson(final String search) {
        return getJson(br.toString(), search);
    }

    private boolean isSuccess(final String source) {
        return new Regex(source, "\"status\":\"(success)\"").matches();
    }

    private boolean isSuccess() {
        return isSuccess(br.toString());
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
