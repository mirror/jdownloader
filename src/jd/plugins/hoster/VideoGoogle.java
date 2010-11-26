package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "video.google.com" }, urls = { "http://[\\w\\.]*?video\\.google\\.com/videoplay\\?docid=-?\\d+" }, flags = { 0 })
public class VideoGoogle extends PluginForHost {

    public VideoGoogle(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.google.com/accounts/TOS?loc=ZZ&hl=en";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String url = br.getRegex("videoUrl.x3d(http://.*?).x26th").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        url = Encoding.urlDecode(url, true);
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, url, true, 1);
        if (!dl.getConnection().isContentDisposition() && !dl.getConnection().getContentType().startsWith("video")) {
            this.dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        String name = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (br.containsHTML("but this video may not be availabl")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (name == null || "Google Videos Error".equals(name)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(name + ".flv");
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
