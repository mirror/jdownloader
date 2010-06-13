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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jamendo.com" }, urls = { "http://[\\w\\.]*?jamendo\\.com/.*.*/?(track|download/album)/\\d+" }, flags = { 0 })
public class JamendoCom extends PluginForHost {

    private static String PREFER_HIGHQUALITY = "PREFER_HIGHQUALITY";

    public JamendoCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.jamendo.com/en/cgu_user";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        String TrackDownloadID = new Regex(parameter.getDownloadURL(), "/download/track/(\\d+)").getMatch(0);
        if (TrackDownloadID != null) {
            br.getPage("http://www.jamendo.com/en/track/" + TrackDownloadID);
            String Track = br.getRegex("<div class='page_title' style=''>(.*?)</div>").getMatch(0);
            String Artist = br.getRegex("<div class='page_title' style=''>.*?</div>.*?class=\"g_artist_name\" title=\"\" >(.*?)</a").getMatch(0);
            if (Track == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(Track);
            parameter.setProperty("linktyp", "downloadTrack");
            return AvailableStatus.TRUE;
        }
        String TrackID = new Regex(parameter.getDownloadURL(), "/track/(\\d+)").getMatch(0);
        if (TrackID != null) {
            br.getPage("http://www.jamendo.com/en/track/" + TrackID);
            String Track = br.getRegex("<div class='page_title' style=''>(.*?)</div>").getMatch(0);
            String Artist = br.getRegex("<div class='page_title' style=''>.*?</div>.*?class=\"g_artist_name\" title=\"\" >(.*?)</a").getMatch(0);
            if (Track == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(Track);
            if (getPluginConfig().getBooleanProperty(PREFER_HIGHQUALITY, true)) {
                parameter.setProperty("linktyp", "downloadTrack");
            } else
                parameter.setProperty("linktyp", "webtrack");
            return AvailableStatus.TRUE;
        }
        String AlbumDownloadID = new Regex(parameter.getDownloadURL(), "/download/album/(\\d+)").getMatch(0);
        if (AlbumDownloadID != null) {
            br.getPage("http://www.jamendo.com/en/album/" + AlbumDownloadID);
            String Album = br.getRegex("<div class='page_title' style=''>(.*?)</div>").getMatch(0);
            String Artist = br.getRegex("<div class='page_title' style=''>.*?</div>.*?class=\"g_artist_name\" title=\"\" >(.*?)</a").getMatch(0);
            if (Album == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(Album);
            parameter.setProperty("linktyp", "downloadalbum");
            return AvailableStatus.TRUE;
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String dlurl = null;
        br.setDebug(true);
        if (link.getStringProperty("linktyp", "webtrack").equalsIgnoreCase("webtrack")) {
            String TrackID = new Regex(link.getDownloadURL(), "track/(\\d+)").getMatch(0);
            String AlbumID = br.getRegex("Jamendo.page.play\\('(album/.*?)'\\)").getMatch(0);
            br.setFollowRedirects(true);
            br.getPage("http://www.jamendo.com/player/?url=" + AlbumID);
            br.setFollowRedirects(false);
            br.getPage("http://www.jamendo.com/en/get2/stream/track/redirect/?id=" + TrackID);
            br.setFollowRedirects(true);
            String FileName = Plugin.extractFileNameFromURL(br.getRedirectLocation());
            link.setFinalFileName(FileName);
            dlurl = br.getRedirectLocation();
        } else if (link.getStringProperty("linktyp", "webtrack").equalsIgnoreCase("downloadalbum")) {
            String AlbumID = br.getRegex("Jamendo.page.play\\('album/(.*?)'\\)").getMatch(0);
            dlurl = prepareDownload("album", AlbumID, link);
        } else if (link.getStringProperty("linktyp", "webtrack").equalsIgnoreCase("downloadTrack")) {
            String TrackID = new Regex(link.getDownloadURL(), "track/(\\d+)").getMatch(0);
            dlurl = prepareDownload("track", TrackID, link);
        }
        if (dlurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlurl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String prepareDownload(String typ, String ID, DownloadLink link) throws IOException, PluginException {
        String dlurl = null;
        String dlPage = "http://www.jamendo.com/en/download/" + typ + "/" + ID + "/do?output=contentonly";
        br.getPage(dlPage);
        String filename = br.getRegex("encodeURIComponent\\(\"(.*?)\"\\);").getMatch(0);
        String dl_unit = br.getRegex("var dl_unit = \"(.*?)\";").getMatch(0);
        String dl_serverno = br.getRegex("dl_serverno = (\\d+);").getMatch(0);
        String dl_encoding = br.getRegex("var dl_encoding = \"(.*?)\";").getMatch(0);
        if (filename == null || dl_unit == null || dl_serverno == null || dl_encoding == null) {
            logger.warning("Error in prepareDownload!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (int i = 0; i < 10; i++) {
            String dllpage = "http://download" + dl_serverno + ".jamendo.com/request/" + dl_unit + "/" + ID + "/" + dl_encoding + "/" + Math.random();
            br.getPage(dllpage);
            String status = br.getRegex("Jamendo_HttpDownloadCallback\\('(.*?)','.*?'\\);").getMatch(0);
            String data = br.getRegex("Jamendo_HttpDownloadCallback\\('.*?','(.*?)'\\);").getMatch(0);

            if (status != null) {
                /* HTTPDownloadCallback */
                if (status.equalsIgnoreCase("ready")) {
                    link.setFinalFileName(filename);
                    dlurl = "http://download" + dl_serverno + ".jamendo.com/download/" + dl_unit + "/" + ID + "/" + dl_encoding + "/" + data + "/" + Encoding.urlEncode(filename);
                    break;
                }
                if (status.equalsIgnoreCase("making")) {
                    sleep(5 * 1000l, link, "Making ");
                    continue;
                }
                if (status.equalsIgnoreCase("queued")) {
                    if (data.equalsIgnoreCase("0")) {
                        sleep(5 * 1000l, link);
                    } else {
                        sleep(5 * 1000l, link, "Queued " + data);
                    }
                    continue;
                }
                logger.info("unknown status,please inform support: " + status + " " + data);
            }
            sleep(5 * 1000l, link);
        }
        if (dlurl == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        return dlurl;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_HIGHQUALITY, JDL.L("plugins.hoster.jamendo", "Prefer High Quality Download")).setDefaultValue(true));
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
