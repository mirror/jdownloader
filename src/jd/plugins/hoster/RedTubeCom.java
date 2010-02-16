package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision: 9508 $", interfaceVersion = 2, names = { "redtube.com" }, urls = { "http://[\\w\\.]*?redtube\\.com/\\d+" }, flags = { 0 })
public class RedTubeCom extends PluginForHost {
    private String dlink = null;

    public RedTubeCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.redtube.com/terms";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
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
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        br.setCookie("http://www.redtube.com", "language", "en");
        br.getPage(link.getDownloadURL());
        String fileName = br.getRegex("<h1 class=\"videoTitle\">(.*?)</h1>").getMatch(0);
        if (fileName == null) fileName = br.getRegex("<title>(.*?)- RedTube - Free Porn Videos</title>").getMatch(0);
        if (fileName != null) link.setName(fileName.trim() + ".flv");
        br.setFollowRedirects(true);
        dlink = getDownloadUrl(link);
        try {
            if (!br.openGetConnection(dlink).getContentType().contains("html")) {
                link.setDownloadSize(br.getHttpConnection().getLongContentLength());
                br.getHttpConnection().disconnect();
                return AvailableStatus.TRUE;
            }
        } finally {
            if (br.getHttpConnection() != null) br.getHttpConnection().disconnect();
        }

        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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

    public String getDownloadUrl(DownloadLink link) {
        String[] splitted = link.getDownloadURL().split("/");
        String videoId = splitted[splitted.length - 1];
        return decodeUrl(videoId);
    }

    /**
     * An obfuscated method to decode url which created by reading decompiled
     * code of swf player of redtube
     */
    public String decodeUrl(String videoId) {
        String loc6 = String.valueOf((int) Math.floor(Float.parseFloat(videoId) / 1000));
        int idLength = videoId.length();
        for (int i = 1; i <= 7 - idLength; ++i)
            videoId = "0" + videoId;

        int loc6Length = loc6.length();
        for (int i = 1; i <= 7 - loc6Length; ++i)
            loc6 = "0" + loc6;
        String loc9 = "R15342O7K9HBCDXFGAIJ8LMZ6PQ0STUVWEYN";

        int loc11 = 0;
        for (int i = 0; i <= 6; ++i)
            loc11 += (videoId.charAt(i) - '0') * (i + 1);

        String loc12 = String.valueOf(loc11);
        loc11 = 0;
        for (int i = 0; i < loc12.length(); ++i)
            loc11 += loc12.charAt(i) - '0';

        String loc13 = String.valueOf(loc11);
        if (loc11 < 10) loc13 = "0" + loc13;

        String loc10 = "";

        loc10 += loc9.charAt(videoId.codePointAt(3) - 48 + loc11 + 3);
        loc10 += loc13.charAt(1);
        loc10 += loc9.charAt(videoId.codePointAt(0) - 48 + loc11 + 2);
        loc10 += loc9.charAt(videoId.codePointAt(2) - 48 + loc11 + 1);
        loc10 += loc9.charAt(videoId.codePointAt(5) - 48 + loc11 + 6);
        loc10 += loc9.charAt(videoId.codePointAt(1) - 48 + loc11 + 5);
        loc10 += loc13.charAt(0);
        loc10 += loc9.charAt(videoId.codePointAt(4) - 48 + loc11 + 7);
        loc10 += loc9.charAt(videoId.codePointAt(6) - 48 + loc11 + 4);
        String dl = "http://dlembed.redtube.com/_videos_t4vn23s9jc5498tgj49icfj4678/" + loc6 + "/" + loc10 + ".flv";
        return dl;
    }

}
