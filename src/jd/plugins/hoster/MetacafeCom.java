package jd.plugins.hoster;

import java.net.URLDecoder;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision: 10609 $", interfaceVersion = 2, names = { "metacafe.com" }, urls = { "http://[\\w\\.]*?metacafe\\.com/watch/\\d+.*" }, flags = { 0 })
public class MetacafeCom extends PluginForHost {
    private String dlink = null;
    
    public MetacafeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.metacafe.com/terms/";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dlink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setAcceptLanguage("en-us,en;q=0.5");
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.getPage(link.getDownloadURL());
        
        String fileName = br.getRegex("name=\"title\" content=\"(.*?) - Video\"").getMatch(0);
        if (fileName == null)
            fileName = br.getRegex("<h1 id=\"ItemTitle\" >(.*?)</h1>").getMatch(0);
        if (fileName != null)
            link.setName(fileName.trim() + ".flv");
        
        dlink = br.getRegex("mediaURL=(.*?\\.flv)&").getMatch(0);
        dlink = URLDecoder.decode(dlink, "utf-8");
        System.out.println(dlink);
        try {
            if (!br.openGetConnection(dlink).getContentType().contains("html")) {
                link.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null)
                br.getHttpConnection().disconnect();
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}