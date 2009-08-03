package jd.plugins.hoster;

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharehub.com" }, urls = { "http://(go.sharehub.com|sharehub.me|follow.to|kgt.com|krt.com)/.*" }, flags = { 0 })
public class ShareHubCom extends PluginForHost {

    public ShareHubCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://sharehub.com/tos.php";
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setDebug(true);
        AvailableStatus av = requestFileInformation(downloadLink);
        if (av != AvailableStatus.TRUE) throw new PluginException(LinkStatus.ERROR_DOWNLOAD_FAILED);
        Form form = br.getForm(0);
        form.setProperty("x", (int) (Math.random() * 134));
        form.setProperty("y", (int) (Math.random() * 25));
        dl = br.openDownload(downloadLink, form, true, 2);
        dl.setFilesizeCheck(false);
        dl.startDownload();

    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<h1>File not found!</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<h1 id=\"fileName\">(.*?)</h1>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex("<td><strong>File size:</strong></td>.*?<td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {

    }

}
