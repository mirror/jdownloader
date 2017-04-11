package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.requests.PostRequest;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "retro.sx" }, urls = { "https?://(?:www\\.)?retro\\.sx/rest/\\d+/\\d+" })
public class RetroSx extends PluginForHost {

    public RetroSx(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        final PostRequest postRequest = new PostRequest("https://retro.sx/rest");
        final String id = new Regex(parameter.getPluginPatternMatcher(), "rest/(\\d+)").getMatch(0);
        final String trackId = new Regex(parameter.getPluginPatternMatcher(), "rest/\\d+/(\\d+)").getMatch(0);
        postRequest.addVariable("type", "trackinfo");
        postRequest.addVariable("id", trackId);
        postRequest.setContentType("application/x-www-form-urlencoded");
        br.setCurrentURL("https://retro.sx/music/" + id);
        br.getPage(postRequest);
        final String track_hbit = br.getRegex("track_hbit\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String track_hash = br.getRegex("track_hash\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        if (track_hash == null || track_hbit == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String track_hbit = br.getRegex("track_hbit\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String track_hash = br.getRegex("track_hash\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String finalURL = br.getURL("/bank/" + track_hbit + "/" + track_hash + ".mp3").toString();
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalURL, false, 1);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}
