//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filecloud.io", "ifile.it" }, urls = { "http://(www\\.)?(ifile\\.it|filecloud\\.io)/[a-z0-9]+", "fhrfzjnerhfDELETEMEdhzrnfdgvfcas4378zhb" }, flags = { 2, 0 })
public class IFileIt extends PluginForHost {

    private final String        useragent               = "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:12.0) Gecko/20100101 Firefox/12.0";
    /* must be static so all plugins share same lock */
    private static final Object LOCK                    = new Object();
    private boolean             showDialog              = false;
    private int                 MAXFREECHUNKS           = 1;
    private static final String ONLY4REGISTERED         = "You need to be a registered user in order to download this file";
    private static final String ONLY4REGISTEREDUSERTEXT = JDL.LF("plugins.hoster.ifileit.only4registered", "Only downloadable for registered users");
    private static final String NOCHUNKS                = "NOCHUNKS";
    private static final String NORESUME                = "NORESUME";

    public IFileIt(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filecloud.io/user-register.html");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("ifile.it/", "filecloud.io/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        setBrowserExclusive();
        updateBrowser(br);
        br.setRequestIntervalLimit(getHost(), 250);
        simulateBrowser();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("var __ab1 = 0")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink downloadLink, boolean resume, boolean viaAccount) throws Exception, PluginException {
        final String ab1 = br.getRegex("var __ab1 = (\\d+);").getMatch(0);
        if (ab1 == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        br.setFollowRedirects(true);
        // Br2 is our xml browser now!
        final Browser br2 = br.cloneBrowser();
        br2.setReadTimeout(40 * 1000);
        final String ukey = new Regex(downloadLink.getDownloadURL(), "filecloud\\.io/(.+)").getMatch(0);
        xmlrequest(br2, "http://filecloud.io/download-request.json", "ukey=" + ukey + "&__ab1=" + ab1);
        if (br2.containsHTML("\"captcha\":1")) {
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br2);
            // Semi-automatic reCaptcha handling
            final String k = br.getRegex("recaptcha_public.*?=.*?\\'([^<>\"]*?)\\';").getMatch(0);
            if (k == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            rc.setId(k);
            rc.load();
            for (int i = 0; i <= 5; i++) {
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                xmlrequest(br2, "http://filecloud.io/download-request.json", "ukey=" + ukey + "&__ab1=" + ab1 + "&ctype=recaptcha&recaptcha_response=" + Encoding.urlEncode_light(c) + "&recaptcha_challenge=" + rc.getChallenge());
                if (br2.containsHTML("(\"retry\":1|\"captcha\":1)")) {
                    rc.reload();
                    continue;
                }
                break;
            }
        }
        if (br2.containsHTML("(\"retry\":1|\"captcha\":1)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.getPage("http://filecloud.io/download.html");
        String dllink = br.getRegex("id=\"requestBtnHolder\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br2.getRegex("\"(http://s\\d+\\.filecloud\\.io/[a-z0-9]+/\\d+/[^<>\"/]*?)\"").getMatch(0);
        if (dllink == null) {
            logger.info("last try getting dllink failed, plugin must be defect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.setFollowRedirects(false);
        int chunks = MAXFREECHUNKS;
        if (downloadLink.getBooleanProperty(IFileIt.NOCHUNKS, false) || resume == false) {
            chunks = 1;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, chunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 503) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!this.dl.startDownload()) {
            try {
                if (dl.externalDownloadStop()) return;
            } catch (final Throwable e) {
            }
            if (downloadLink.getLinkStatus().getErrorMessage() != null && downloadLink.getLinkStatus().getErrorMessage().startsWith(JDL.L("download.error.message.rangeheaders", "Server does not support chunkload"))) {
                if (downloadLink.getBooleanProperty(IFileIt.NORESUME, false) == false) {
                    downloadLink.setChunksProgress(null);
                    downloadLink.setProperty(IFileIt.NORESUME, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            } else {
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(IFileIt.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(IFileIt.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        showDialog = true;
        try {
            loginAPI(account, ai, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        /* account info */
        br.postPage("http://ifile.it/api-fetch_account_info.api", "akey=" + account.getProperty("akey", null));
        final String[] response = br.getRegex("\\{\"status\":\"(.*?)\",\"num_files\":\"?(\\d+)\"?,\"num_folders\":\"?(\\d+)\"?,\"storage_used\":\"?(\\d+)\"?,\"bw_used_24hrs\":\"?(\\d+)\"?,\"user_id\":\"(.*?)\",\"user_name\":\"(.*?)\",\"user_group\":\"(.*?)\"\\}").getRow(0);
        if (response == null || response.length != 8 || response[0].equals("error")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }

        account.setValid(true);
        account.setProperty("typ", "unknown");
        if (response[7].equals("vip")) {
            ai.setStatus("VIP User");
            account.setProperty("typ", "vip");
        } else if (response[7].equals("premium")) {
            ai.setStatus("Premium User");
            account.setProperty("typ", "premium");
        } else if (response[7].equals("unverified")) {
            if (showDialog) {
                UserIO.getInstance().requestMessageDialog(0, "ifile.it Premium Error", "Account '" + account.getUser() + "' is not verified.\r\nPlease activate your Account!");
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else if (response[7].equals("normal")) {
            ai.setStatus("Normal User");
            account.setProperty("typ", "free");
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://ifile.it/help-tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        doFree(downloadLink, true, false);
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        showDialog = false;
        loginAPI(account, null, false);
        if (account.getProperty("typ", null).equals("free")) {
            /* without this we always get a captcha! */
            br = new Browser();
            updateBrowser(br);
            loginAPI(account, null, false);
            br.getPage(downloadLink.getDownloadURL());
            doFree(downloadLink, true, true);
        } else {
            final String ukey = new Regex(downloadLink.getDownloadURL(), "http://.*?/(.*?)(/.*?)?$").getMatch(0);
            br.postPage("http://filecloud.io/api-fetch_download_url.api", "akey=" + account.getProperty("akey", null) + "&ukey=" + ukey);
            final String[] response = br.getRegex("\\{\"status\":\"(.*?)\",\"(.*?)\":\"(.*?)\"\\}").getRow(0);
            if (response == null || response.length != 3 || response[0].equals("error")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

            final String dllink = response[2].replace("\\", "");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -1);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 503) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l); }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void loginAPI(final Account account, AccountInfo ai, boolean refresh) throws Exception {
        synchronized (LOCK) {
            if (ai == null) {
                ai = account.getAccountInfo();
                if (ai == null) {
                    ai = new AccountInfo();
                    account.setAccountInfo(ai);
                }
            }
            updateBrowser(br);
            if (refresh || account.getProperty("akey", null) == null) {
                /* login get apikey */
                br.postPage("https://secure.filecloud.io/api-fetch_apikey.api", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final String[] response = br.getRegex("\\{\"status\":\"(.*?)\",\"(.*?)\":\"(.*?)\"\\}").getRow(0);
                if (response == null || response.length != 3 || response[0].equals("error")) {
                    if (response[1].equals("message")) {
                        if (response[2].equals("invalid username and\\/or password")) {
                            if (showDialog) {
                                UserIO.getInstance().requestMessageDialog(0, "ifile.it Account Error", "Invalid username '" + account.getUser() + "' and/or password.\r\nPlease check your Account!");
                            }
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("akey", response[2]);
            }
            br.setCookie("http://filecloud.io", "if_akey", account.getStringProperty("akey", null));
        }
    }

    private void updateBrowser(Browser br) {
        if (br == null) return;
        br.getHeaders().put("User-Agent", useragent);
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.setCookie("http://filecloud.io/", "lang", "en");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    private void simulateBrowser() throws IOException {
        br.cloneBrowser().getPage("http://ifile.it/ads/adframe.js");
    }

    private void xmlrequest(final Browser br, final String url, final String postData) throws IOException {
        br.getHeaders().put("User-Agent", useragent);
        br.getHeaders().put("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.postPage(url, postData);
        br.getHeaders().remove("X-Requested-With");
    }

}