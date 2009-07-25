package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flyupload.com" }, urls = { "http://[\\w\\.]*?flyupload\\.com/.*[\\?fid=].*[0-9]" }, flags = { 2 })
public class FlyUploadCom extends PluginForHost {

    public FlyUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }
    
    // @Override
    public String getAGBLink() {
        return "http://www.flyupload.com/tos";
    }
    
    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("The file you requested has either expired or the URL has an invalid fid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename:</td><td style=\"border-top: 1px none #cccccc;\">(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("File Size:</td><td style=\"border-top: 1px dashed #cccccc;\">(.*?)</td>").getMatch(0);
        if (filename == null || filesize  == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }
    
    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String linkurl = "http://www28.flyupload.com/dl?fid="
                         + br.getRegex("<a href=\"http://www28.flyupload.com/dl\\?fid=(.*?)\">Download Now</a>").getMatch(0);
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, linkurl, true, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getResponseCode() != 200 && con.getResponseCode() != 206) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 1000l);
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }
    
    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
