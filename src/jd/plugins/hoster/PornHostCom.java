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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornhost.com" }, urls = { "http://[\\w\\.]*?GhtjGEuzrjTU\\.com/([0-9]+/[0-9]+\\.html|[0-9]+)" }, flags = { 0 })
public class PornHostCom extends PluginForHost {

    public PornHostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.pornhost.com/tos.html";
    }

    public void correctDownloadLink(DownloadLink link) {
        // This is neded because we also got a pornhost decrypter, the decrypter
        // gives the links out like "GhtjGEuzrjTU.com" instead of "pornhost.com"
        // because otherwise there would be a conflict (2 plugins having the
        // same regexes)
        link.setUrlDownload(link.getDownloadURL().replace("GhtjGEuzrjTU", "pornhost"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("gallery not found") || br.containsHTML("You will be redirected to")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>pornhost\\.com - free file hosting with a twist - gallery(.*?)</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"url\" value=\"http://www\\.pornhost\\.com/(.*?)/\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>pornhost\\.com - free file hosting with a twist -(.*?)</title>").getMatch(0);
                if (filename == null) filename = br.getRegex("\"http://file[0-9]+\\.pornhost\\.com/.*?/(.*?)\"").getMatch(0);
            }
        }
        String ending = br.getRegex("<label>download this file</label>.*?<a href=\".*?\">.*?(\\..*?)</a>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (ending != null) {
            downloadLink.setName(filename.trim() + ending);
        } else {
            downloadLink.setName(filename.trim());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String dllink = null;
        if (!downloadLink.getDownloadURL().contains(".html")) {
            dllink = br.getRegex("\"http://dl[0-9]+\\.pornhost\\.com/files/.*?/.*?/.*?/.*?/.*?/.*?\\..*?\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("reateRNPlayer.*?\"(http://.*?)\"").getMatch(0);
            }
            if (dllink == null) {
                dllink = br.getRegex("download this file</label>.*?<a href=\"(.*?)\"").getMatch(0);
            }
        } else {
            dllink = br.getRegex("style=\"width: 499px; height: 372px\">[\t\n\r ]+<img src=\"(http.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://file[0-9]+\\.pornhost\\.com/[0-9]+/.*?)\"").getMatch(0);
        }
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        dl.setAllowFilenameFromURL(true);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
