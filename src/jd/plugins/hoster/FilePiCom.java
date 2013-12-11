//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filepi.com" }, urls = { "http://(www\\.)?filepi\\.com/i/[A-Za-z0-9]+" }, flags = { 0 })
public class FilePiCom extends PluginForHost {

    public FilePiCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filepi.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:25.0) Gecko/20100101 Firefox/25.0");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex(">File Name:</p>[\t\n\r ]+<p class=\"text\\-lower\">([^<>\"]*?)</p>").getMatch(0);
        final String filesize = br.getRegex(">File Size:</p>[\t\n\r ]+<p class=\"text\\-lower\">([^<>\"]*?)</p>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        final String md5 = br.getRegex(">File MD5:</p>[\t\n\r ]+<p class=\"text\\-lower\">([a-z0-9]{32})</p>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String waittime = br.getRegex(">wait</small> <span id=\"wait\">(\\d+)</span>").getMatch(0);
        int wait = 5;
        if (waittime != null) wait = Integer.parseInt(waittime);
        final String fid = new Regex(downloadLink.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        final String fhash = br.getRegex("var hash = \\'([a-z0-9]+)\\';").getMatch(0);
        if (fhash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final Browser br2 = br.cloneBrowser();
        br2.getPage("http://static.filepi.com/js/file.js?15");
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br2);
        rc.findID();
        rc.load();
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        for (int i = 1; i <= 5; i++) {
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            br.getHeaders().put("Referer", downloadLink.getDownloadURL());
            br.postPage("http://filepi.com/ajax-re", "cap1=" + Encoding.urlEncode(rc.getChallenge()) + "&cap2=" + Encoding.urlEncode(c) + "&tag=" + fid + "&pass=" + fhash + "&hash=" + fhash);
            if (br.containsHTML("\"error\":\"captcha\"")) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br.containsHTML("\"error\":\"captcha\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        final String time = getJson("re_time"), hash = getJson("re_hash");
        if (time == null || hash == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        sleep(wait * 1001l, downloadLink);
        br.getHeaders().put("Referer", downloadLink.getDownloadURL());
        br.postPage("http://filepi.com/ajax-down", "tag=" + fid + "&pass=" + fhash + "&re_time=" + time + "&re_hash=" + hash);
        String dllink = getJson("url");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}