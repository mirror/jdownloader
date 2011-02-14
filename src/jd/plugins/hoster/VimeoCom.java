//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "http://(www\\.)?vimeo\\.com/[0-9]+" }, flags = { 2 })
public class VimeoCom extends PluginForHost {
    private static final String MAINPAGE = "http://www.vimeo.com";
    static private final String AGB      = "http://www.vimeo.com/terms";
    private String              clipData;
    private String              finalURL;

    public VimeoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://vimeo.com/join");
    }

    public String getAGBLink() {
        return AGB;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL() + "?hd=1");
        String clipID = br.getRegex("targ_clip_id:   (\\d+)").getMatch(0);
        this.clipData = br.getPage("/moogaloop/load/clip:" + clipID + "/local?param_force_embed=0&param_clip_id=" + clipID + "&param_show_portrait=0&param_multimoog=&param_server=vimeo.com&param_show_title=0&param_autoplay=0&param_show_byline=0&param_color=00ADEF&param_fullscreen=1&param_md5=0&param_context_id=&context_id=null");
        String title = getClipData("caption");
        String dlURL = "/moogaloop/play/clip:" + getClipData("clip_id") + "/" + getClipData("request_signature") + "/" + getClipData("request_signature_expires") + "/?q=" + (getClipData("isHD").equals("1") ? "hd" : "sd");
        br.setFollowRedirects(false);
        br.getPage(dlURL);
        this.finalURL = br.getRedirectLocation();
        if (finalURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(finalURL);
            if (con.getContentType() != null && con.getContentType().contains("mp4")) {
                downloadLink.setFinalFileName(title + ".mp4");
            } else {
                downloadLink.setFinalFileName(title + ".flv");
            }
            downloadLink.setDownloadSize(br.getRequest().getContentLength());
            if (title == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(title);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    private String getClipData(String tag) {
        return new Regex(this.clipData, "<" + tag + ">(.*?)</" + tag + ">").getMatch(0);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception {
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setDebug(true);
        // HEADER_BEGIN
        br.setCookie(MAINPAGE, "cached_email", account.getUser());
        br.getHeaders().put("Accept", "application/x-ms-application, image/jpeg, application/xaml+xml, image/gif, image/pjpeg, application/x-ms-xbap, */*");
        br.getHeaders().put("Accept-Encoding", "gzip, deflate");
        br.getHeaders().put("Accept-Language", "de-DE");
        br.getHeaders().put("User-Agent", "Mozilla/4.0 (compatible; MSIE 8.0; Windows NT 6.1; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C)");
        br.getHeaders().put("Referer", "http://vimeo.com/");
        br.getHeaders().put("Accept-Charset", null);
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
        br.getHeaders().put("connection", "Keep-Alive");
        // HEADER_END
        br.getPage(MAINPAGE + "/log_in");
        final String token = br.getRegex("name=\"token\" value=\"(.*?)\"").getMatch(0);
        if (token == null) {
            logger.warning("Login is broken!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.getHeaders().put("Pragma", "no-cache");
        br.getHeaders().put("Referer", "http://vimeo.com/log_in");
        br.setCookie(MAINPAGE, "xsrftv", token);
        // br.setCookie(MAINPAGE, "home_active_tab", "inbox");
        br.postPage(MAINPAGE + "/log_in", "sign_in%5Bemail%5D=" + Encoding.htmlDecode(account.getUser()) + "&sign_in%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&token=" + Encoding.urlEncode(token));
        if (br.getCookie(MAINPAGE, "vimeo") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
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
        account.setValid(true);
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("\">Sorry, not available for download")) {
            logger.info("No download available for link: " + link.getDownloadURL() + " , downloading as unregistered user...");
            doFree(link);
        }
        String dllink = br.getRegex("class=\"download\">[\t\n\r ]+<a href=\"(/.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(/download/video:\\d+\\?v=\\d+\\&e=\\d+\\&h=[a-z0-9]+\\&uh=[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = MAINPAGE + dllink;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

}
