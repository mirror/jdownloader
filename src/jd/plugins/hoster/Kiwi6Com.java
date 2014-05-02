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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kiwi6.com" }, urls = { "http://((www\\.)?kiwi6\\.com/file/[a-z0-9]+|(www\\.)?[a-z0-9]+\\.kiwi6\\.com/hotlink/[a-z0-9]+)" }, flags = { 0 })
public class Kiwi6Com extends PluginForHost {

    public Kiwi6Com(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(3 * 1000l);
    }

    @Override
    public String getAGBLink() {
        return "http://kiwi6.com/pages/tos";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://kiwi6.com/file/" + new Regex(link.getDownloadURL(), "([a-z0-9]+)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Upload not found<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String artist = br.getRegex("=\"/artists/([^<>\"]*?)\"").getMatch(0);
        String filename = br.getRegex("<h2>About <i>([^<>\"]*?)</i>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>\"]*?)\\- Listen and download mp3").getMatch(0);
        // For MP3s
        String filesize = br.getRegex(">Download MP3</a></h1>([^<>\"]*?)</div>").getMatch(0);
        // For all the other links
        if (filesize == null) filesize = br.getRegex(">Download File</a></h1>([^<>\"]*?)</div>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (artist != null) {
            link.setFinalFileName(encodeUnicode(Encoding.htmlDecode(artist.trim()) + " - " + Encoding.htmlDecode(filename.trim()) + ".mp3"));
        } else {
            link.setFinalFileName(encodeUnicode(Encoding.htmlDecode(filename.trim()) + ".mp3"));
        }
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* Slow server */
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        final String fid = new Regex(downloadLink.getDownloadURL(), "([a-z0-9]+)$").getMatch(0);
        final String host = br.getRegex("data\\-host=\"([^<>\"]*?)\"").getMatch(0);
        final String dllink = "http://" + host + "/download/" + fid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("You are downloading too quickly")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many downloads started in a short time!", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
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