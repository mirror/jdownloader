package jd.plugins.host;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class JamendoCom extends PluginForHost {

    public JamendoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.jamendo.com/en/cgu_user";
    }

    @Override
    public boolean getFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        String TrackID = new Regex(parameter.getDownloadURL(), "track/(\\d+)").getMatch(0);
        br.getPage("http://www.jamendo.com/en/track/" + TrackID);
        String Track = br.getRegex("<div class='page_title' style=''>(.*?)</div>").getMatch(0);
        String Artist = br.getRegex("<div class='page_title' style=''>.*?</div>.*?class=\"g_artist_name\" title=\"\" >(.*?)</a").getMatch(0);
        if (Track == null || Artist == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(Track);
        return true;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        getFileInformation(link);
        br.setDebug(true);
        String TrackID = new Regex(link.getDownloadURL(), "track/(\\d+)").getMatch(0);
        String AlbumID = br.getRegex("Jamendo.page.play\\('(album/.*?)'\\)").getMatch(0);
        br.setFollowRedirects(true);
        br.getPage("http://www.jamendo.com/player/?url=" + AlbumID);
        br.setFollowRedirects(false);
        br.getPage("http://www.jamendo.com/en/get2/stream/track/redirect/?id=" + TrackID);
        br.setFollowRedirects(true);
        String FileName = Plugin.extractFileNameFromURL(br.getRedirectLocation());
        link.setFinalFileName(FileName);
        dl = br.openDownload(link, br.getRedirectLocation(), true, 0);
        dl.startDownload();
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

}
