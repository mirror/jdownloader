package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

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
    public boolean getFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        String TrackDownloadID = new Regex(parameter.getDownloadURL(), "/download/track/(\\d+)").getMatch(0);
        if (TrackDownloadID != null) {
            br.getPage("http://www.jamendo.com/en/track/" + TrackDownloadID);
            String Track = br.getRegex("<div class='page_title' style=''>(.*?)</div>").getMatch(0);
            String Artist = br.getRegex("<div class='page_title' style=''>.*?</div>.*?class=\"g_artist_name\" title=\"\" >(.*?)</a").getMatch(0);
            if (Track == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(Track);
            parameter.setProperty("linktyp", "downloadTrack");
            return true;
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
            return true;
        }
        String AlbumDownloadID = new Regex(parameter.getDownloadURL(), "/download/album/(\\d+)").getMatch(0);
        if (AlbumDownloadID != null) {
            br.getPage("http://www.jamendo.com/en/album/" + AlbumDownloadID);
            String Album = br.getRegex("<div class='page_title' style=''>(.*?)</div>").getMatch(0);
            String Artist = br.getRegex("<div class='page_title' style=''>.*?</div>.*?class=\"g_artist_name\" title=\"\" >(.*?)</a").getMatch(0);
            if (Album == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            parameter.setName(Album);
            parameter.setProperty("linktyp", "downloadalbum");
            return true;
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        getFileInformation(link);
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
        if (dlurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = br.openDownload(link, dlurl, true, 1);
        dl.startDownload();
    }

    private String prepareDownload(String typ, String ID, DownloadLink link) throws IOException, PluginException {
        String dlurl = null;
        br.getPage("http://www.jamendo.com/en/download/" + typ + "/" + ID + "/?output=contentonly");
        String filename = br.getRegex("encodeURIComponent\\(\"(.*?)\"\\);").getMatch(0);
        String dl_unit = br.getRegex("var dl_unit = \"(.*?)\";").getMatch(0);
        String dl_serverno = br.getRegex("var dl_serverno = '(\\d+)';").getMatch(0);
        String dl_encoding = br.getRegex("var dl_encoding = \"(.*?)\";").getMatch(0);
        for (int i = 0; i < 10; i++) {
            br.getPage("http://download" + dl_serverno + ".jamendo.com/request/" + dl_unit + "/" + ID + "/" + dl_encoding + "/" + Math.random());
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
        if (dlurl == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Currently not available", 10 * 60 * 1000l);
        return dlurl;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_HIGHQUALITY, JDLocale.L("plugins.hoster.jamendo", "Prefer High Quality Download")).setDefaultValue(true));
    }

}
