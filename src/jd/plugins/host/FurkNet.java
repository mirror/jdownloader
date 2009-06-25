package jd.plugins.host;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class FurkNet extends PluginForHost {

    public FurkNet(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "https://www.furk.net/terms";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Slots limit for free downloads")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        Form form = br.getForm(0);
        if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        form.remove(null);
        br.setFollowRedirects(false);
        dl = br.openDownload(link, form, false, 1);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Slots limit for free downloads")) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60000);
            }
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("File not found") || br.containsHTML("This torrent is not ready")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("value=\"Premium Download\" onclick=\"document\\.location.href='/registration\\?pfile=(.*?)'\" />").getMatch(0);
        String filesize = br.getRegex("<li>File size: <b>(.*?)</b></li>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        parameter.setName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
