package jd.plugins.hoster;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: 36558 $", interfaceVersion = 3, names = { "simfileshare.net" }, urls = { "https?://(?:www\\.)?(simfileshare\\.net/download/|simfil\\.es/)\\d+/?" })
public class SimFileShareNet extends PluginForHost {
    public SimFileShareNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return null;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        final URLConnectionAdapter con = br.openGetConnection(parameter.getDownloadURL());
        if (con.getResponseCode() == 404) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (con.isContentDisposition()) {
            parameter.setVerifiedFileSize(con.getLongContentLength());
            parameter.setFinalFileName(getFileNameFromDispositionHeader(con));
            con.disconnect();
            return AvailableStatus.TRUE;
        } else {
            br.followConnection();
        }
        if (br.getRequest().getHttpConnection().getResponseCode() == 404 || br.containsHTML("couldn't find that file")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fileInfos[] = br.getRegex("<h3.*?>\\s*(.*?)\\s*\\((.*?)\\)\\s*<").getRow(0);
        if (fileInfos == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        parameter.setDownloadSize(SizeFormatter.getSize(fileInfos[1]));
        parameter.setName(fileInfos[0]);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 1);
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
