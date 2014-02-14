package jd.plugins.hoster;

import java.net.URLDecoder;

import jd.PluginWrapper;
import jd.http.Browser.BrowserException;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "metacafe.com" }, urls = { "http://(www\\.)?metacafedecrypted\\.com/watch/(sy\\-)?\\d+/.{1}" }, flags = { 0 })
public class MetacafeCom extends PluginForHost {
    private String dlink = null;

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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        // Offline links should also have nice filenames
        link.setName(new Regex(link.getDownloadURL(), "(\\d+)/.{1}$").getMatch(0));
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en-us,en;q=0.5");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        try {
            br.getPage(link.getDownloadURL());
        } catch (final BrowserException e) {
            if (br.getRequest().getHttpConnection().getResponseCode() == 400) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.getURL().contains("/?pageNotFound") || br.containsHTML("<title>Metacafe \\- Best Videos \\&amp; Funny Movies</title>") || br.getURL().contains("metacafe.com/?m=removed")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getURL().contains("metacafe.com/family_filter/")) {
            br.postPage("http://www.metacafe.com/f/index.php?inputType=filter&controllerGroup=user", "filters=0");
        }
        String fileName = br.getRegex("name=\"title\" content=\"(.*?) \\- Video\"").getMatch(0);
        if (fileName == null) fileName = br.getRegex("<h1 id=\"ItemTitle\" >(.*?)</h1>").getMatch(0);
        if (fileName != null) link.setFinalFileName(fileName.trim() + ".mp4");
        if (!link.getDownloadURL().contains("metacafe.com/watch/sy-")) {
            dlink = br.getRegex("mediaURL(.*?)&").getMatch(0);
            if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dlink = URLDecoder.decode(dlink, "utf-8");
            dlink = dlink.replace("\\", "");
            dlink = new Regex(dlink, ":\"(.*?)\"").getMatch(0) + "?__gda__=" + new Regex(dlink, "key\":\"(.*?)\"").getMatch(0);
            if (dlink == null || dlink.contains("null")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            try {
                if (!br.openGetConnection(dlink).getContentType().contains("html")) {
                    link.setDownloadSize(br.getHttpConnection().getLongContentLength());
                    br.getHttpConnection().disconnect();
                }
            } finally {
                if (br.getHttpConnection() != null) br.getHttpConnection().disconnect();
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
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