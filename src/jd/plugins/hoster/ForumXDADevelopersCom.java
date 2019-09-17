package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "forum.xda-developers.com" }, urls = { "https?://forum\\.xda-developers\\.com/devdb/project/dl/\\?id=\\d+" })
public class ForumXDADevelopersCom extends antiDDoSForHost {
    public ForumXDADevelopersCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.xda-developers.com/xda-tos/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        br.addAllowedResponseCodes(403);// rate limiting
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download/Rate limit reached", 10 * 60 * 1000l);
        } else if (br.containsHTML("This download file is not currently available")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("Filename\\s*:\\s*</label>\\s*<div>\\s*(.*?)\\s*</div>").getMatch(0);
        final String md5 = br.getRegex("MD5 Hash\\s*:\\s*</label>\\s*<div>\\s*([0-9a-fA-F]{32})\\s*</div>").getMatch(0);
        final String size = br.getRegex("Size\\s*:\\s*</label>\\s*<div>\\s*([0-9\\.KBMTG]+)\\s*</div>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (filename != null && link.getFinalFileName() == null) {
            link.setFinalFileName(filename);
        }
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        if (size != null) {
            link.setDownloadSize(SizeFormatter.getSize(size));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        this.br.getHeaders().put("Accept-Encoding", "identity");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher() + "&task=get", true, 1);
        if (!dl.getConnection().isOK() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
            br.followConnection(true);
            if (canHandle(br.getURL()) || dl.getConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download/Rate limit reached", 10 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
