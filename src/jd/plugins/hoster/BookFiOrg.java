package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bookfi.org" }, urls = { "http://(www\\.)?([a-z]+\\.)?bookfi\\.org/(book|dl)/\\d+(/[a-z0-9]+)?" }, flags = { 0 })
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
        return 1;
    }

    private String DLLINK = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink param) throws Exception {
        final String parameter = param.getDownloadURL();
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        final String[] info = br.getRegex("<a class=\"button active\" href=\"([^\"]+)\">.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
        if (info == null || info[0] == null || info[1] == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // Goes to download link to find out filename
        String filename = br.getRegex("<h2 style=\"display:inline\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1 style=\"color:#49AFD0\"  itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        param.setFinalFileName(filename + ".pdf");
        param.setDownloadSize(SizeFormatter.getSize(info[1]));
        DLLINK = info[0];
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fixFilename(link);
        dl.startDownload();
    }

    private void fixFilename(final DownloadLink downloadLink) {
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        final String newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
        if (newExtension != null && !downloadLink.getFinalFileName().endsWith(newExtension)) {
            final String oldExtension = downloadLink.getFinalFileName().substring(downloadLink.getFinalFileName().lastIndexOf("."));
            if (oldExtension != null)
                downloadLink.setFinalFileName(downloadLink.getFinalFileName().replace(oldExtension, newExtension));
            else
                downloadLink.setFinalFileName(downloadLink.getFinalFileName() + newExtension);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
