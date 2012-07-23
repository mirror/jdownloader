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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "indowebster.com" }, urls = { "http://(www\\.)?indowebster\\.com/(download/(files|audio|video)/.+|[^\\s]+\\.html)" }, flags = { 0 })
public class Indowebster extends PluginForHost {

    private static final String PASSWORDTEXT = "(>THIS FILE IS PASSWORD PROTECTED<|>INSERT PASSWORD<|class=\"redbtn\" value=\"Unlock\"|method=\"post\" id=\"form_pass\")";

    public Indowebster(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.indowebster.com/policy-tos.php";
    }

    private String getDLLink() throws Exception {
        final Regex importantStuff = br.getRegex("\\$\\.post\\(\\'(http://[^<>\"]+)\\',\\{(.*?)\\}");
        final String action = importantStuff.getMatch(0);
        final String pagePiece = importantStuff.getMatch(1);
        if (action == null || pagePiece == null) { return null; }
        final String[] list = pagePiece.split(",");
        if (list == null || list.length == 0) { return null; }
        String post = "";
        for (final String str : list) {
            final Regex strregex = new Regex(str, "(.*?):\\'(.*?)\\'");
            if (strregex.getMatch(0) == null || strregex.getMatch(1) == null) { return null; }
            post += post.equals("") ? post : "&";
            post += strregex.getMatch(0) + "=" + strregex.getMatch(1);
        }
        br.postPage(action, post);
        final String dllink = br.toString();
        if (dllink == null || !dllink.startsWith("http") || dllink.length() > 500) { return null; }
        return dllink.replace("[", "%5B").replace("]", "%5D");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (br.containsHTML("Storage Maintenance, Back Later")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Storage maintenance", 60 * 60 * 1000l); }
        if (br.containsHTML(">404 Page Not Found<")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Unknown server error (404)"); }
        String passCode = link.getStringProperty("pass", null);
        if (br.containsHTML(PASSWORDTEXT)) {
            final String valueName = br.getRegex("type=\"password\" name=\"(.*?)\"").getMatch(0);
            if (valueName == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", link);
            }
            br.postPage(link.getDownloadURL(), valueName + "=" + Encoding.urlEncode(passCode));
            if (br.containsHTML(PASSWORDTEXT)) { throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password"); }
        }
        String ad_url = br.getRegex("<a id=\"download\" href=\"(http://.*?)\"").getMatch(0);
        if (ad_url == null) {
            ad_url = br.getRegex("\"(http://v\\d+\\.indowebster\\.com/downloads/jgjbcf/[a-z0-9]+)\"").getMatch(0);
        }
        if (ad_url == null) {
            logger.warning("ad_url is null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!ad_url.startsWith("http://")) {
            ad_url = "http://www.indowebster.com/" + ad_url;
        }
        br.getPage(ad_url);
        final String realName = br.getRegex("<strong id=\"filename\">(\\[www\\.indowebster\\.com\\])?(.*?)</strong>").getMatch(1);
        if (realName != null) {
            link.setFinalFileName(Encoding.htmlDecode(realName));
        }
        /**
         * If we reach this line the password should be correct even if the
         * download fails
         */
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        final String waittime = br.getRegex("var s = (\\d+);").getMatch(0);
        int wait = 25;
        if (waittime != null) {
            wait = Integer.parseInt(waittime);
        }
        sleep(wait * 1001l, link);
        String dllink = getDLLink();
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dllink = dllink.trim();
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">404 Not Found<")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l); }
            if (br.containsHTML(">Indowebster\\.com under maintenance")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.indowebster.undermaintenance", "Under maintenance"), 30 * 60 * 1000l); }
            if (br.containsHTML("But Our Download Server Can be Accessed from Indonesia Only")) { throw new PluginException(LinkStatus.ERROR_FATAL, "Download Server Can be Accessed from Indonesia Only"); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setReadTimeout(3 * 60 * 1000);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(Requested file is deleted|image/default/404\\.png\")") || br.getURL().contains("/error") || br.getURL().contains("/files_not_found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.containsHTML(">404 Page Not Found<")) return AvailableStatus.UNCHECKABLE;
        // Convert old links to new links
        String newlink = br.getRegex("<meta http\\-equiv=\"refresh\" content=\"\\d+;URL=(http://v\\d+\\.indowebster\\.com/.*?)\"").getMatch(0);
        if (newlink != null) {
            newlink = newlink.trim();
            downloadLink.setUrlDownload(newlink);
            logger.info("New link set...");
            br.getPage(newlink);
        }
        String filename = br.getRegex("<h1 class=\"title\">(.*?)</h1>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\- Indowebster\\.com</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex(">Size : <span style=\"float:none;\">(.*?)</span><").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("Date upload: .{1,20} Size: (.*?)\"").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("(?i)<strong>Size:</strong> ([\\d+\\.]+ ?(MB|GB))").getMatch(0);
            }
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        if (downloadLink.getDownloadURL().contains("/audio/")) {
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        } else {
            downloadLink.setName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (br.containsHTML(PASSWORDTEXT)) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.indowebstercom.passwordprotected", "This link is password protected"));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}