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

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharehoster.de" }, urls = { "http://[\\w\\.]*?sharehoster\\.(de|com|net)/(dl|wait|vid)/[a-z0-9]+" }, flags = { 2 })
public class ShareHosterDe extends PluginForHost {

    public ShareHosterDe(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.sharehoster.com/index.php?open=premium");
    }

    private static final String MAINPAGE = "http://www.sharehoster.com/";

    @Override
    public String getAGBLink() {
        return "http://www.sharehoster.de/index.php?content=agb";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("sharehoster\\.(com|net|de)", "sharehoster.com").replace("/dl/", "/wait/"));
    }

    // Note: This hoster is EXTREMELY BUGGY...
    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL().replace("/vid/", "/wait/"));
        // No filename or size is on the page so just check if there is an
        // error, if not, the file should be online!
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("download_failed") || br.getRedirectLocation().contains("downloadfailed") || br.getRedirectLocation().contains("premium&vid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.getPage(br.getRedirectLocation());
        }
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("download_failed") || br.getRedirectLocation().contains("downloadfailed") || br.getRedirectLocation().contains("premium&vid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String fileID = new Regex(downloadLink.getDownloadURL(), "/(wait|vid)/(.+)").getMatch(1);
        String dllink = null;
        br.setFollowRedirects(true);
        String waitCode = br.getRegex("name=\"wait\" value=\"(.*?)\"").getMatch(0);
        if (waitCode == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String postData = "continue=Fortfahren&file=" + fileID + "&wait=" + waitCode;
        if (downloadLink.getDownloadURL().contains("/vid/")) postData += "&open=show_wait";
        String postPage = br.getRegex("<form id=\"prepare\" name=\"prepare\" method=\"post\" action=\"(http://.*?)\"").getMatch(0);
        if (postPage == null) {
            postPage = downloadLink.getDownloadURL().replace("/wait/", "/dl/");
            if (downloadLink.getDownloadURL().contains("/vid/")) postPage = downloadLink.getDownloadURL();
        }
        if (postPage.contains("/vid/")) downloadLink.setUrlDownload(postPage);
        br.setFollowRedirects(false);
        br.postPage(postPage, postData);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (br.getRedirectLocation().contains("/vid/")) {
            br.getPage(br.getRedirectLocation());
        } else {
            br.getPage("http://www.sharehoster.com/?open=download_prepare&file=" + fileID);
        }
        if (downloadLink.getDownloadURL().contains("/vid/")) {
            dllink = br.getRegex("name=\"stream\" value=\"(http://.*?)\"").getMatch(0);
            if (dllink == null && br.containsHTML("/v/")) {
                br.getPage("http://www.sharehoster.com/flowplayer/config.php?movie=" + fileID);
                dllink = br.getRegex("\\'url\\': \\'(?!http://www\\.sharehoster\\.com/design)(http://.*?\\.mp4)\\'").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\\'(http://(upload|media)\\d+\\.sharehoster\\.com/video/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+\\.mp4)\\'").getMatch(0);
                    if (dllink == null) dllink = br.getRegex("Stream: (http://(media|upload)\\d+\\.sharehoster\\.com/video/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+\\.mp4)").getMatch(0);
                }
            }
        }
        if (dllink == null) {
            for (int i = 0; i <= 5; i++) {
                br.setFollowRedirects(false);
                Form dlform = br.getFormbyProperty("name", "downloadprepare");
                if (dlform == null || !br.containsHTML("captcha.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String captchaUrl = "http://www.sharehoster.com/content/captcha.php";
                String code = getCaptchaCode(captchaUrl, downloadLink);
                dlform.put("code", code);
                br.submitForm(dlform);
                dllink = br.getRedirectLocation();
                if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (dllink.contains("&err")) {
                    br.setFollowRedirects(true);
                    br.getPage(br.getRedirectLocation());
                    continue;
                }
                break;
            }
            if (dllink.contains("&error")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -8);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.clearCookies(MAINPAGE);
        br.getHeaders().put("Referer", "");
        br.postPage("http://www.sharehoster.com/index.php", "login=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&login_submit=login");
        if (br.getCookie(MAINPAGE, "sharehoster-data") == null || !br.containsHTML("- Premium for")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("- Premium for (\\d+) day\\(s\\)").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(System.currentTimeMillis() + Integer.parseInt(expire) * 24 * 60 * 60 * 1001);
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<param name=\"src\" value=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<embed type=\"video/divx\" src=\"(http://.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(http://(upload|media)\\d+\\.sharehoster\\.com/video/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+\\.(avi|mp4))\"").getMatch(0);
            }
        }
        if (dllink == null) {
            logger.info("Couldn't find dllink, trying workaround...");
            String theID = new Regex(link.getDownloadURL(), "sharehoster\\.com/(dl|wait|vid)/(.+)").getMatch(1);
            if (br.containsHTML("(value=\"AVI\"|value=\"FLASH\")"))
                br.postPage("http://www.sharehoster.com/dl/" + theID, "");
            else
                br.getPage("http://www.sharehoster.com/dl/" + theID);
            if (br.getRedirectLocation() != null) {
                br.getPage(br.getRedirectLocation());
                br.postPage("http://www.sharehoster.com/dl/" + theID, "submit=Download&open=show_wait_premium&file=" + theID + "&wait=");
                dllink = br.getRedirectLocation();
            }
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}