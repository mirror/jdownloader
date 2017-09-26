package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 34675 $", interfaceVersion = 2, names = { "filetonet.com" }, urls = { "https?://(www\\.)?filetonet\\.com/[a-fA-F0-9]{35}" })
public class FileToNetCom extends PluginForHost {
    public FileToNetCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filetonet.com/help/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(parameter.getDownloadURL());
        final String iframe = br.getRegex("src=\"(https?://dn\\d+\\.filetonet\\.com/file/\\?v=[a-fA-F0-9]+)\"").getMatch(0);
        if (iframe == null) {
            if (br.containsHTML("<h1>\\s*404")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getPage(iframe);
        final String fileName = br.getRegex("Название:\\s*<span>\\s*(.*?)\\s*<").getMatch(0);
        final String fileSize = br.getRegex("Размер:\\s*<span>\\s*(.*?)\\s*\\.?<").getMatch(0);
        if (fileName == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        parameter.setName(fileName);
        if (fileSize != null) {
            parameter.setDownloadSize(SizeFormatter.getSize(fileSize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        final Form form = br.getFormbyActionRegex("getlink.*");
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, form, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
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
