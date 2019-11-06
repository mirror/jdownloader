package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "http://files.uploadit.host" }, urls = { "https?://files\\.uploadit\\.host/\\d+/[a-zA-Z0-9\\._\\-%]+\\.html" })
public class FilesUploaditHost extends antiDDoSForHost {
    public FilesUploaditHost(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        br.getPage(parameter.getPluginPatternMatcher());
        final String fileName = br.getRegex("class\\s*=\\s*\"filename\"\\s*>\\s*(.*?)\\s*<").getMatch(0);
        if (fileName != null) {
            parameter.setName(fileName);
            return AvailableStatus.UNCHECKED;
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        final String url = link.getPluginPatternMatcher().replaceFirst("(\\.html$)", "");
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, url, true, 1);
        if (!dl.getConnection().isOK() || StringUtils.containsIgnoreCase(dl.getConnection().getContentType(), "text")) {
            try {
                br.followConnection(true);
            } catch (IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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
