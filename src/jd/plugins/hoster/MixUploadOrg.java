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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mixupload.org" }, urls = { "http://(www\\.)?mixupload\\.org/track/[^<>\"/]+" }, flags = { 0 })
public class MixUploadOrg extends PluginForHost {

    public MixUploadOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mixupload.org/about/agreement";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String trackID = link.getStringProperty("trackid", null);
        if (trackID == null) {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML(">Page not found<|>Error<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            trackID = br.getRegex("id=\"pl_track(\\d+)\"").getMatch(0);
            if (trackID == null) {
                trackID = br.getRegex("p\\.playTrackId\\((\\d+)\\)").getMatch(0);
                if (trackID == null) {
                    trackID = br.getRegex("p\\.playTrackIdPart\\((\\d+)").getMatch(0);
                    if (trackID == null) {
                        trackID = br.getRegex("var page_id = \\'(\\d+)\\';").getMatch(0);
                    }
                }
            }
            if (trackID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setProperty("trackid", trackID);
        }
        br.getPage("http://mixupload.org/player/getTrackInfo/" + trackID);
        if ("[]".equals(br.toString().trim())) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = getJson("artist") + " - " + getJson("title");
        final String filesize = getJson("sizebyte");
        if (filename.contains("null") || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = "http://mixupload.org/player/play/" + downloadLink.getStringProperty("trackid", null) + "/0/track.mp3";
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
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