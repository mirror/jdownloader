package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 15931 $", interfaceVersion = 2, names = { "bookfi.org" }, urls = { "http://(www\\.)?([a-z]+\\.)?bookfi\\.org/(book|dl)/\\d+(/[a-z0-9]+)?" }, flags = { 0 })
public class BookFiOrg extends PluginForHost {

    public BookFiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://bookfi.org";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink param) throws Exception {
        final String parameter = param.getDownloadURL();
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);

        // Gets page, filesize and download link
        br.getPage(parameter);
        String[] info = br.getRegex("<a class=\"button active\" href=\"([^\"]+)\">.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
        if (info == null || info[0] == null || info[1] == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        br.setFollowRedirects(false);
        br.getPage(info[0]);

        // Goes to download link to find out filename
        String filename = null;
        if (br.getRedirectLocation() != null) filename = new Regex(br.getRedirectLocation(), "/([^/]*)$").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        param.setFinalFileName(filename);
        param.setDownloadSize(SizeFormatter.getSize(info[1]));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), true, 0);
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
