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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "edisk.cz" }, urls = { "http://[\\w\\.]*?edisk\\.(cz|sk)/stahni/[0-9]+/.+\\.html" }, flags = { 0 })
public class EdiskCz extends PluginForHost {

    public EdiskCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.edisk.cz/kontakt";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("edisk.sk", "edisk.cz"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("UTF-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Tento soubor již neexistuje z následujích důvodů:|<li>soubor byl smazán majitelem</li>|<li>vypršela doba, po kterou může být soubor nahrán</li>|<li>odkaz je uvedený v nesprávném tvaru</li>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<span class=\"fl\" title=\"(.*?)\">").getMatch(0));
        if (filename == null) filename = br.getRegex("<title> \\&nbsp;\\&quot;(.*?)\\&quot; \\(").getMatch(0);
        String filesize = br.getRegex("<p>Velikost souboru: <strong>(.*?)</strong></p>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<title> \\&nbsp;\\&quot;.*?\\&quot; \\((.*?)\\) - stáhnout soubor\\&nbsp; </title>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("Stáhnout soubor:\\&nbsp;<span class=\"bold\">.*? \\((.*?)\\)</span>").getMatch(0);
            }
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Set the final filename here because server gives us filename +
        // ".html" which is bad
        link.setFinalFileName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace("/stahni/", "/stahni-pomalu/"));
        br.setFollowRedirects(true);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        String postUrl = downloadLink.getDownloadURL().replace("/stahni/", "/x-download/");
        String postData = "action=" + new Regex(downloadLink.getDownloadURL(), "/stahni/(\\d+.*?\\.html)").getMatch(0);
        br.postPage(postUrl, postData);
        String dllink = br.toString().trim();
        if (!dllink.startsWith("http://") || !dllink.endsWith(".html") || dllink.length() > 500) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.toString().trim(), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("/error/503") || br.containsHTML("<h3>Z této IP adresy již probíhá stahování</h3>")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
            String unknownErrormessage = br.getRegex("<h3>(.*?)</h3>").getMatch(0);
            if (unknownErrormessage != null) throw new PluginException(LinkStatus.ERROR_FATAL, unknownErrormessage);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
