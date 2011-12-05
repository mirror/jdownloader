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
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "u.115.com" }, urls = { "http://(www\\.)?(u\\.)?115\\.com/file/[a-z0-9]+" }, flags = { 0 })
public class U115Com extends PluginForHost {

    private final String        ua                    = RandomUserAgent.generate();

    private static final String UNDERMAINTENANCEURL   = "http://u.115.com/weihu.html";

    private static final String UNDERMAINTENANCETEXT  = "The servers are under maintenance";

    private static final String NOFREESLOTS           = "网络繁忙时段，非登陆用户其它下载地址暂时关闭。推荐您使用优蛋下载";

    private static final String ACCOUNTNEEDED         = "(为加强知识产权的保护力度，营造健康有益的网络环境，115网盘暂时停止影视资源外链服务。|is_no_check=\"1\")";

    private static final String ACCOUNTNEEDEDUSERTEXT = "Account is needed to download this link";
    private static final String EXACTLINKREGEX        = "\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/down_group\\d+/[^<>\"\\']+)\"";

    public U115Com(PluginWrapper wrapper) {
        super(wrapper);
        // 10 seconds waittime between the downloadstart of simultan DLs of this
        // host
        this.setStartIntervall(10000l);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("u.115.com/file/", "115.com/file/"));
    }

    public String findLink(DownloadLink link) throws Exception {
        String linkToDownload = br.getRegex(EXACTLINKREGEX).getMatch(0);
        if (linkToDownload == null) {
            linkToDownload = br.getRegex("<div class=\"btn\\-wrap\">[\t\n\r ]+<a href=\"(http://\\d+\\.\\d+\\.\\d+\\.\\d+/[^\"\\'<>]+)\"").getMatch(0);
            if (linkToDownload == null) {
                final String pickLink = br.getRegex("\"(/\\?ct=pickcode\\&.*?)\"").getMatch(0);
                if (pickLink != null) {
                    int wait = 30;
                    String waittime = br.getRegex("id=\"js_get_download_second\">(\\d+)</b>").getMatch(0);
                    if (waittime == null) waittime = br.getRegex("var second = (\\d+);").getMatch(0);
                    if (waittime != null) wait = Integer.parseInt(waittime);
                    sleep(wait * 1001l, link);
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getPage("http://115.com" + pickLink);
                    String correctedBR = br.toString().replace("\\", "");
                    linkToDownload = new Regex(correctedBR, "\"url\":\"(http:.*?)\"").getMatch(0);
                    if (linkToDownload == null) linkToDownload = new Regex(correctedBR, EXACTLINKREGEX).getMatch(0);
                }
            }
        }
        return linkToDownload;
    }

    @Override
    public String getAGBLink() {
        return "http://u.115.com/tos.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (UNDERMAINTENANCEURL.equals(br.getRedirectLocation())) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.U115Com.undermaintenance", UNDERMAINTENANCETEXT));
        if (br.containsHTML(NOFREESLOTS)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available at the moment");
        /**
         * I don't know what this text means (google couldn't help) so i handle
         * it like that
         */
        if (br.containsHTML(ACCOUNTNEEDED)) {
            logger.warning("Only downloadable via account: " + link.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_FATAL, ACCOUNTNEEDEDUSERTEXT);
        }
        if (!br.containsHTML("(<div class=\"download\\-box dl\\-hint\" id=\"|<div class=\"download\\-box\" style=\"display:none\")")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.U115Com.dlnotpossible", "Download not possible at the moment"), 5 * 60 * 1000l);
        String dllink = findLink(link);
        if (dllink == null) {
            logger.warning("dllink is null, seems like the regexes are defect!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /** Don't do html decode, it can make the dllink invalid */
        // dllink = Encoding.htmlDecode(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("(help_down.html|onclick=\"WaittingManager|action=submit_feedback|\"report_box\"|UploadErrorMsg)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepareBrowser(final Browser br) {
        try {
            if (br == null) { return; }
            br.setReadTimeout(2 * 60 * 1000);
            br.setCookie("http://u.115.com/", "lang", "en");
            br.getHeaders().put("User-Agent", ua);
            br.setCustomCharset("utf-8");
            br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            br.getHeaders().put("Accept-Language", "en-us,de;q=0.7,en;q=0.3");
            br.getHeaders().put("Pragma", null);
            br.getHeaders().put("Cache-Control", null);
        } catch (Throwable e) {
            /* setCookie throws exception in 09580 */
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().equals(UNDERMAINTENANCEURL)) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.U115Com.undermaintenance", UNDERMAINTENANCETEXT));
                return AvailableStatus.UNCHECKABLE;
            }
            br.getPage(br.getRedirectLocation());
        }
        if (br.containsHTML("(id=\"pickcode_error\">很抱歉，文件不存在。</div>|很抱歉，文件不存在。|>很抱歉，该文件提取码不存在。<|<title>115网盘\\|网盘\\|115,我的网盘\\|免费网络硬盘 \\- 爱分享，云生活</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)网盘下载\\|115网盘|网盘|115网络U盘-我的网盘|免费网络硬盘</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"Download\"></a><a id=\"Download(.*?)\"></a>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("file_name: \\'(.*?)\\',").getMatch(0);
            }
        }
        String filesize = br.getRegex("文件大小：(.*?)<div class=\"share-url\"").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("u6587\\\\u4ef6\\\\u5927\\\\u5c0f\\\\uff1a(.*?)\\\\r\\\\n\\\\").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("file_size: \\'(.*?)\\'").getMatch(0);
                if (filesize == null) filesize = br.getRegex("<li>文件大小：(.*?)</li>").getMatch(0);
            }
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = filesize.replace(",", "");
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        String sh1 = br.getRegex("<li>SHA1：(.*?) <a href=\"").getMatch(0);
        if (sh1 == null) sh1 = br.getRegex("sha1: \"(.*?)\",").getMatch(0);
        if (sh1 != null) link.setSha1Hash(sh1.trim());
        if (br.containsHTML(ACCOUNTNEEDED)) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.u115com.only4registered", ACCOUNTNEEDEDUSERTEXT));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

}