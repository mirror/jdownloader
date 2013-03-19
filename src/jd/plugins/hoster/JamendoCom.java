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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jamendo.com" }, urls = { "http://[\\w\\.\\-]*?jamendo\\.com/.?.?/?(track/|download/album/|download/a|download/track/)\\d+" }, flags = { 0 })
public class JamendoCom extends PluginForHost {

    private String             PREFER_HIGHQUALITY = "PREFER_HIGHQUALITY";
    public static final String PREFER_WHOLEALBUM  = "PREFER_WHOLEALBUM";

    public JamendoCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.jamendo.com/en/cgu_user";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String dlurl = null;
        if (link.getStringProperty("linktype", "webtrack").equalsIgnoreCase("webtrack")) {
            dlurl = br.getRegex("og:audio:url\" content=\"(http.*?)\"").getMatch(0);
        } else if (link.getStringProperty("linktype", "webtrack").equalsIgnoreCase("downloadTrack")) {
            String TrackID = new Regex(link.getDownloadURL(), "track/(\\d+)").getMatch(0);
            dlurl = "http://storage-new.newjamendo.com/download/track/" + TrackID + "/";
        } else if (link.getStringProperty("linktype", "webtrack").equalsIgnoreCase("downloadAlbum")) {
            String AlbumID = new Regex(link.getDownloadURL(), "/download/album/(\\d+)").getMatch(0);
            if (AlbumID == null) AlbumID = new Regex(link.getDownloadURL(), "/download/a(\\d+)").getMatch(0);
            dlurl = "http://storage-new.newjamendo.com/download/a" + AlbumID + "/";
        }
        if (dlurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlurl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        String TrackDownloadID = new Regex(parameter.getDownloadURL(), "/download/track/(\\d+)").getMatch(0);
        if (TrackDownloadID != null) {
            br.setFollowRedirects(true);
            br.getPage("http://www.jamendo.com/en/track/" + TrackDownloadID);
            String Track = br.getRegex("og:title\" content=\"(.*?)\"").getMatch(0);
            String Artist = br.getRegex("og:description\" content=\"Track by (.*?) - \\d").getMatch(0);
            if (Track == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(Artist + " - " + Track + ".mp3");
            parameter.setProperty("linktype", "downloadTrack");
            return AvailableStatus.TRUE;
        }
        String TrackID = new Regex(parameter.getDownloadURL(), "/track/(\\d+)").getMatch(0);
        if (TrackID != null) {
            br.setFollowRedirects(true);
            br.getPage("http://www.jamendo.com/en/track/" + TrackID);
            String Track = br.getRegex("og:title\" content=\"(.*?)\"").getMatch(0);
            String Artist = br.getRegex("og:description\" content=\"Track by (.*?) - \\d").getMatch(0);
            if (Track == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(Artist + " - " + Track + ".mp3");
            if (getPluginConfig().getBooleanProperty(PREFER_HIGHQUALITY, true)) {
                parameter.setProperty("linktype", "downloadTrack");
            } else {
                parameter.setProperty("linktype", "webtrack");
            }
            return AvailableStatus.TRUE;
        }
        String AlbumDownloadID = new Regex(parameter.getDownloadURL(), "/download/album/(\\d+)").getMatch(0);
        if (AlbumDownloadID == null) AlbumDownloadID = new Regex(parameter.getDownloadURL(), "/download/a(\\d+)").getMatch(0);
        if (AlbumDownloadID != null) {
            br.setFollowRedirects(true);
            br.getPage("http://www.jamendo.com/en/list/a" + AlbumDownloadID);
            String Album = br.getRegex("og:title\" content=\"(.*?)\"").getMatch(0);
            String Artist = br.getRegex("og:description\" content=\"Album by (.*?)\"").getMatch(0);
            if (Album == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String packageName = "";
            if (Album != null) packageName = packageName + Album;
            if (Artist != null) {
                if (packageName.length() > 0) {
                    packageName = " - " + packageName;
                }
                packageName = Artist + packageName;
            }
            parameter.setName(packageName + ".zip");
            parameter.setProperty("linktype", "downloadAlbum");
            return AvailableStatus.TRUE;
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_HIGHQUALITY, JDL.L("plugins.hoster.jamendo", "Prefer High Quality Download")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_WHOLEALBUM, JDL.L("plugins.decrypt.jamendoalbum", "Prefer whole Album as Zip")).setDefaultValue(true));
    }

}