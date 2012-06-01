package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.nutils.encoding.HTMLEntities;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision: 15886 $", interfaceVersion = 2, names = { "docs.google.com" }, urls = { "https?://(www\\.)?docs.google.com/(leaf\\?|uc\\?export=download.*?&)id=[A-Za-z0-9\\-_]+(&.*?&pid=[A-Za-z0-9\\-_]++)?" }, flags = { 0 })
public class DocsGoogleCom extends PluginForHost {

    public DocsGoogleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.google.com/google-d-s/intl/en/terms.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        String url = link.getDownloadURL();
        url = url.replaceAll("uc\\?export=download.*?&", "leaf?");
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(url);

        String size = br.getRegex("<div role=\"button\"[^>]*>Download(?:&nbsp;|\\s)+\\(([0-9A-Za-z\\., ]+)\\)</div>").getMatch(0);
        if (size == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = br.getRegex("<title>(.*?) - Google Docs</title>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        link.setName(HTMLEntities.unhtmlentities(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(size.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        String url = downloadLink.getDownloadURL();
        url = url.replaceAll("/leaf\\?", "/uc?export=download&");
        br.setFollowRedirects(false);
        br.getPage(url);
        String dllink = br.getRedirectLocation();

        if (dllink == null) {
            // Tries to find download anyway link
            if (br.getURL().contains("/uc?")) dllink = br.getRegex("<a href=\"([^\"]+)\">Download anyway</a>").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

            // If it does, adds host, since links starts by /uc and repeats
            br.getPage("https://docs.google.com" + HTMLEntities.unhtmlentities(dllink));
            dllink = br.getRedirectLocation();
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
