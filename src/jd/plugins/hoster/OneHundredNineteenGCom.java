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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "119g.com" }, urls = { "http://(www\\.)?d\\.119g\\.com/f/[A-Z0-9]+\\.html" }, flags = { 0 })
public class OneHundredNineteenGCom extends PluginForHost {

    public OneHundredNineteenGCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://119g.com/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">404 \\- 找不到文件或目录。")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        /* Abused */
        if (br.containsHTML("\\- 该文件涉及违法，已经屏蔽\\!</font>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("class=\"downfile\\-type kz\\-[a-z0-9]+\"></i>([^<>\"]*?)</div>").getMatch(0);
        final String filesize = br.getRegex("<strong>文件大小：</strong>([^<>\"]*?)</div>").getMatch(0);
        String extension = br.getRegex("<strong>文件类型：</strong>([^<>\"]*?)</div>").getMatch(0);
        if (filename == null || filesize == null || extension == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = Encoding.htmlDecode(filename.trim());
        extension = extension.trim();
        if (!filename.endsWith(extension)) filename += extension;
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        final String sha1 = br.getRegex("<strong>SHA1值：</strong>([^<>\"]*?)</div>").getMatch(0);
        if (sha1 != null) link.setSha1Hash(sha1.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL().replace(".html", "_bak.html"));
        String authURL = br.getRegex("src=\\\\'(http:\\\\/\\\\/\\d+\\.down\\.119g\\.com(:\\d+)?\\\\/AyangAuth\\\\)'").getMatch(0);
        final String linkPart = br.getRegex("var thunder_url = \"(http://[^<>\"]*?)\"").getMatch(0);
        if (authURL == null || linkPart == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        authURL = authURL.replace("\\", "");
        br.getPage(authURL);
        final String key = br.getRegex("key=\\'([a-z0-9]+)\\';").getMatch(0);
        if (key == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String dllink = linkPart + key;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

}