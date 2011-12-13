package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "multiupload.com" }, urls = { "http://(www\\.)?multiuploaddecrypted\\.com/([A-Z0-9]{2}_[A-Z0-9]+|[0-9A-Z]+)" }, flags = { 0 })
public class MultiUploadCom extends PluginForHost {

    public MultiUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://multiupload.com/terms";
    }

    /** All links come from a decrypter */
    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replace("multiuploaddecrypted.com/", "multiupload.com/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(">Unfortunately, the link you have clicked is not available")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex match = br.getRegex("<font[^>]+>\\(([\\d\\.]+\\s+[kmg]?b)\\)<.*?\\('dlbutton'\\)\\.href=\\'[^\\']+/files/[0-9A-F]+/([^\\']+)\\'");
        if (match.count() > 0) {
            link.setDownloadSize(SizeFormatter.getSize(match.getMatch(0)));
            link.setFinalFileName(match.getMatch(1).trim());
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String url = br.getRegex("\\('dlbutton'\\)\\.href=\\'([^\"\\'<>]+)\\'").getMatch(0);
        if (url == null) url = br.getRegex("\\'(http://www\\d+\\.multiupload\\.com:\\d+/files/[A-Z0-9]+/[^\"\\'<>]+)\\'").getMatch(0);
        if (url == null && br.containsHTML("(<div[^>]+id=\"downloadbutton_\"|document\\.getElementById\\(\\'dlbutton\\'\\))")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        if (url == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No multiupload directlink available at the moment", 10 * 60 * 1000l);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, url, true, 0);
        URLConnectionAdapter urlConnection = dl.getConnection();
        if (!urlConnection.isContentDisposition() && urlConnection.getHeaderField("Cache-Control") != null) {
            urlConnection.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}