package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ugotfile.com" }, urls = { "http://[\\w\\.]*?ugotfile.com/file/\\d+/[0-9A-Za-z.]+" }, flags = { 0 })
public class UgotFileCom extends PluginForHost {

    public UgotFileCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://ugotfile.com/doc/terms/";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.clearCookies(link.getDownloadURL());
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Your hourly traffic limit is exceeded.")) {
            int block = Integer.parseInt(br.getRegex("<div id='sessionCountDown' style='font-weight:bold; font-size:20px;'>(.*?)</div>").getMatch(0)) * 1000 + 1;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, block);
        }
        String Captcha = getCaptchaCode(br.getRegex("<img style=\"cursor: pointer\" onclick=\"captchaReload\\(\\);\" id=\"captchaImage\" src=\"(.*?)\" alt=\"captcha key\" />").getMatch(0), link);
        br.getPage("http://ugotfile.com/captcha?key=" + Captcha);
        if (br.containsHTML("invalid key")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        sleep(7001, link);
        br.getPage("http://ugotfile.com/file/get-file");
        if (br.containsHTML("Get premium")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000 * 30);
        String dllink = null;
        dllink = br.getRegex("(http://.*)").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = br.openDownload(link, dllink, false, 1);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("FileId and filename mismatched or file does not exist!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("has been deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<td>Filename:</td>\\s+<td>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("<td>Filesize:</td>\\s+<td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
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

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
