package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "nudogram.com" }, urls = { "https?://(www\\.)?nudogram\\.com/videos/\\d+/[^/]+/" })
public class NudogramCom extends PluginForHost {
    public NudogramCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        br.getPage(parameter.getDownloadURL());
        final String title = br.getRegex("<title>\\s*(.*?)\\s*(\\|\\s*Nudogram.*?)?</title>").getMatch(0);
        final String mp4 = br.getRegex("video_url\\s*:\\s*'(function/\\d+/https?://.*?mp4/)'").getMatch(0);
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (mp4 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!parameter.isNameSet()) {
            parameter.setFinalFileName(title + ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String mp4 = br.getRegex("video_url\\s*:\\s*'(function/\\d+/https?://.*?mp4/)'").getMatch(0);
        if (mp4 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        mp4 = KernelVideoSharingCom.getDllinkCrypted(br, mp4);
        br.getHeaders().put("Accept", "*/*");
        br.getHeaders().put("Accept-Encoding", "identity;q=1, *;q=0");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, mp4 + "?rnd=" + System.currentTimeMillis(), true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            try {
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
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
