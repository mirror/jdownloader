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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jamendo.com" }, urls = { "https?://(?:[\\w\\-]*\\.)?jamendo\\.com/.?.?/?(?:track/|download/album/|download/a|download/track/)\\d+" })
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

    /** TODO: Consider implementing their API in the future: https://developer.jamendo.com/v3.0 */
    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.br = prepBR(this.br);
        String trackDownloadID = new Regex(parameter.getDownloadURL(), "/download/track/(\\d+)").getMatch(0);
        if (trackDownloadID != null) {
            br.setFollowRedirects(true);
            br.getPage("https://www." + this.getHost() + "/en/track/" + trackDownloadID);
            String Track = br.getRegex("og:title\" content=\"(.*?)\"").getMatch(0);
            String Artist = br.getRegex("og:description\" content=\"Track by (.*?) - \\d").getMatch(0);
            if (Track == null || Artist == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            parameter.setName(Artist + " - " + Track + ".mp3");
            parameter.setProperty("linktype", "downloadTrack");
            return AvailableStatus.TRUE;
        }
        String trackID = new Regex(parameter.getDownloadURL(), "/track/(\\d+)").getMatch(0);
        if (trackID != null) {
            br.setFollowRedirects(true);
            br.getPage("https://www.jamendo.com/en/track/" + trackID + "/");
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String fullname = br.getRegex("<title>([^<>\"]+) \\| Jamendo Music \\| Free music downloads</title>").getMatch(0);
            if (fullname == null) {
                final String track = br.getRegex("itemprop=\"name\" content=\"([^<>\"]*?)\"").getMatch(0);
                final String artist = br.getRegex("itemprop=\"author\">([^<>\"]*?)</a>").getMatch(0);
                if (track == null || artist == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                fullname = Encoding.htmlDecode(artist).trim() + Encoding.htmlDecode(track).trim();
            }
            parameter.setName(fullname + ".mp3");
            if (getPluginConfig().getBooleanProperty(PREFER_HIGHQUALITY, true)) {
                parameter.setProperty("linktype", "downloadTrack");
            } else {
                parameter.setProperty("linktype", "webtrack");
            }
            return AvailableStatus.TRUE;
        }
        String albumDownloadID = new Regex(parameter.getDownloadURL(), "/download/album/(\\d+)").getMatch(0);
        if (albumDownloadID == null) {
            albumDownloadID = new Regex(parameter.getDownloadURL(), "/download/a(\\d+)").getMatch(0);
        }
        if (albumDownloadID != null) {
            br.setFollowRedirects(true);
            br.getPage("https://www.jamendo.com/en/list/a" + albumDownloadID);
            String album = br.getRegex("og:title\" content=\"(.*?)\"").getMatch(0);
            String artist = br.getRegex("og:description\" content=\"Album by (.*?)\"").getMatch(0);
            if (album == null || artist == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String packageName = "";
            if (album != null) {
                packageName = packageName + album;
            }
            if (artist != null) {
                if (packageName.length() > 0) {
                    packageName = " - " + packageName;
                }
                packageName = artist + packageName;
            }
            parameter.setName(packageName + ".zip");
            parameter.setProperty("linktype", "downloadAlbum");
            return AvailableStatus.TRUE;
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String dlurl = null;
        if (link.getStringProperty("linktype", "webtrack").equalsIgnoreCase("webtrack")) {
            dlurl = br.getRegex("og:audio:url\" content=\"(http.*?)\"").getMatch(0);
        } else if (link.getStringProperty("linktype", "webtrack").equalsIgnoreCase("downloadTrack")) {
            String trackID = new Regex(link.getDownloadURL(), "track/(\\d+)").getMatch(0);
            /* Alternative: "https://storage.jamendo.com/download/track/" + trackID + "/mp32/" */
            dlurl = "http://storage-new.newjamendo.com/download/track/" + trackID + "/";
        } else if (link.getStringProperty("linktype", "webtrack").equalsIgnoreCase("downloadAlbum")) {
            String albumID = new Regex(link.getDownloadURL(), "/download/album/(\\d+)").getMatch(0);
            if (albumID == null) {
                albumID = new Regex(link.getDownloadURL(), "/download/a(\\d+)").getMatch(0);
            }
            dlurl = "http://storage-new.newjamendo.com/download/a" + albumID + "/";
        }
        if (dlurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlurl, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private Browser prepBR(final Browser br) {
        br.setCookie(this.getHost(), "jammusiclang", "en");
        return br;
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