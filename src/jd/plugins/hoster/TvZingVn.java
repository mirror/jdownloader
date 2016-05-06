package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 *
 * @author noone2407
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tv.zing.vn" }, urls = { "http://tv.zing.vn/video/(\\S+).html" }, flags = { 2 })
public class TvZingVn extends PluginForHost {

    public TvZingVn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://tv.zing.vn/huong-dan/terms";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        final String url = downloadLink.getDownloadURL();
        // get the name of the movie from url
        final String filename = new Regex(url, "http://tv\\.zing\\.vn/video/([\\w-]+)\\/([\\w\\d]+).html").getMatch(0);
        downloadLink.setFinalFileName(filename.replace("-", " ") + ".mp4");
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // get the list of urls
        br.getPage("http://getlinkfs.com/getfile/zingtv.php?link=" + Encoding.urlEncode(downloadLink.getDownloadURL()));
        final String[] allMatches = br.getRegex("\"(https?://stream.*?)\"").getColumn(0);
        // get the url of best quality video, its lowest to highest.
        String link = null;
        for (final String s : allMatches) {
            link = s;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
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
    public void resetDownloadlink(DownloadLink downloadLink) {
    }
}
