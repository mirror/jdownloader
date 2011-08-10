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

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uppit.com" }, urls = { "http://(www\\.)?uppit\\.com/([a-z0-9]+/.+|[A-Z0-9]{6})" }, flags = { 0 })
public class UppItCom extends PluginForHost {

    public UppItCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://uppit.com/tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Invalid download link|>File Not Found<|The file you were looking for could not be found,|<li>The file expired|<li>The file was deleted by its owner)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Regexes for 3 kinds of links
        Regex finfo = br.getRegex(">You have requested the file<br /><b>(.*?)</b> \\((.*?)\\)<br /><br");
        String filename = br.getRegex("<font size=\"20\"><b>(.*?)</b></font>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>UppIT \\- Free File Sharing \\-(.*?)</title>").getMatch(0);
            if (filename == null) {
                filename = finfo.getMatch(0);
                if (filename == null) filename = br.getRegex(">File download:\\&nbsp;<strong>(.*?)</strong>").getMatch(0);
            }
        }
        String filesize = br.getRegex("<h3>File Size:</h3></td><td align=\"right\"><h3>(.*?)</h3>").getMatch(0);
        if (filesize == null) {
            filesize = finfo.getMatch(1);
            if (filesize == null) filesize = br.getRegex("Size:\\&nbsp;\\&nbsp;<strong>(.*?)</strong><br").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        if (downloadLink.getDownloadURL().matches(".*?uppit\\.com/[a-z0-9]{2,}/.+")) {
            Form dlform = br.getFormbyProperty("name", "F1");
            if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            int wait = 6;
            String waitRegexed = br.getRegex("id=\"countdown_str\">Wait <span id=\"[a-z0-9]+\">(\\d+)</span>").getMatch(0);
            if (waitRegexed != null) wait = Integer.parseInt(waitRegexed);
            sleep(wait * 1001l, downloadLink);
            br.submitForm(dlform);
        }
        String dllink = br.getRegex("<span style=\"background:#f9f9f9;border:1px dotted #bbb;padding:7px;\">[\t\n\r ]+<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("<meta HTTP-EQUIV=\"REFRESH\" content=\"\\d+; url=(http://.*?)\">").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(http://server\\d+\\.uppit\\.com/files/\\d+/[a-z0-9]+/.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("getElementById\\(\"dl\"\\)\\.innerHTML = \\'<a href=\\\\'(http://.*?)\\\\'\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("\\'(http://uppit\\.com/d2/[A-Z0-9]{6}/[a-z0-9]+/.*?)\\\\'").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("var downloadlink = unescape\\(\\'(http://.*?)\\'\\)").getMatch(0);
                            if (dllink == null) {
                                dllink = br.getRegex("\\'(http://wickers\\.uppit\\.com/save/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/[a-z0-9]+/.*?)\\'").getMatch(0);
                            }
                        }
                    }
                }
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Chunkload is working for some links but not for all!
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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