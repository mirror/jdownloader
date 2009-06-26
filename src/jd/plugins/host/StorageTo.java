package jd.plugins.host;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class StorageTo extends PluginForHost {

    public StorageTo(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.storage.to/tos";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        String infolink = link.getDownloadURL().replace("http://www.storage.to/get", "http://www.storage.to/getlink");
        br.getPage(infolink);
        if (br.getRegex("'state' : '(.*?)'").getMatch(0).equals("wait")) {
            int wait = Integer.valueOf(br.getRegex("'countdown' : (.*?),").getMatch(0)).intValue();
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * wait);
        }
        sleep(5000, link);
        String dllink;
        dllink = br.getRegex("'link' : '(.*?)',").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        link.setFinalFileName(null);
        dl = br.openDownload(link, dllink, false, 1);
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("File not found.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<span class=\"orange\">Downloading:</span>(.*?)<span class=\"light\">(.*?)</span>").getMatch(0);
        String filesize = br.getRegex("<span class=\"orange\">Downloading:</span>(.*?)<span class=\"light\">(.*?)</span>").getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
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

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
