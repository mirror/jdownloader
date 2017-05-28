package jd.plugins.hoster;

import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metacafe.com" }, urls = { "http://(www\\.)?metacafedecrypted\\.com/watch/(sy\\-)?\\d+/.{1}" })
public class MetacafeCom extends PluginForHost {

    private String dllink = null;

    public MetacafeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("metacafedecrypted.com/", "metacafe.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.metacafe.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public static Browser prepBR(final Browser br) {
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(400);
        br.setAcceptLanguage("en-us,en;q=0.5");
        /* Important! */
        br.setCookie("metacafe.com", "user", "%7B%22ffilter%22%3Afalse%7D");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String url_filename = new Regex(link.getDownloadURL(), "(\\d+)/.{1}$").getMatch(0);
        // Offline links should also have nice filenames
        if (!link.isNameSet()) {
            link.setName(url_filename + ".mp4");
        }
        this.setBrowserExclusive();
        prepBR(this.br);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
            br.getPage(link.getDownloadURL());
        if (jd.plugins.decrypter.MetaCafeComDecrypter.isOffline(this.br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.getURL().contains("metacafe.com/family_filter/")) {
            br.postPage("http://www.metacafe.com/f/index.php?inputType=filter&controllerGroup=user", "filters=0");
        }
        String fileName = br.getRegex("name=\"title\" content=\"(.*?) \\- Video\"").getMatch(0);
        if (fileName == null) {
            fileName = br.getRegex("<h1 id=\"ItemTitle\" >(.*?)</h1>").getMatch(0);
        }
        if (fileName == null) {
            fileName = url_filename;
        }
        fileName = fileName.trim();
        if (fileName != null) {
            link.setFinalFileName(fileName.trim() + ".mp4");
        }
        if (!link.getDownloadURL().contains("metacafe.com/watch/sy-")) {
            dllink = br.getRegex("\"sources\":\\[\\{\"src\":\"(http[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            // dllink = URLDecoder.decode(dllink, "utf-8");
            dllink = dllink.replace("\\", "");
            // dllink = new Regex(dllink, ":\"(.*?)\"").getMatch(0) + "?__gda__=" + new Regex(dllink, "key\":\"(.*?)\"").getMatch(0);
            if (dllink == null || dllink.contains("null")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!dllink.contains(".m3u8")) {
                try {
                    if (!br.openGetConnection(dllink).getContentType().contains("html")) {
                        link.setDownloadSize(br.getHttpConnection().getLongContentLength());
                        br.getHttpConnection().disconnect();
                    }
                } finally {
                    if (br.getHttpConnection() != null) {
                        br.getHttpConnection().disconnect();
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dllink.contains(".m3u8")) {
            /* hls download */
            this.br.getPage(dllink);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String url_hls = hlsbest.getDownloadurl();
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, url_hls);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            dl.startDownload();
        }
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

}