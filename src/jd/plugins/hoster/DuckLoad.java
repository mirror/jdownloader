package jd.plugins.hoster;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class DuckLoad extends PluginForHost {

    public DuckLoad(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://duckload.com/impressum.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        sleep(10 * 1000l, link);
        Form form = br.getForm(0);
        String capurl = br.getRegex("img src=\"(/design/Captcha\\.php\\?key=.*?)\"").getMatch(0);
        String code = getCaptchaCode(capurl, link);
        form.put("cap", code);
        form.put("_____download.x", "0");
        form.put("_____download.y", "0");
        br.submitForm(form);
        String url = br.getRedirectLocation();
        br.setDebug(true);
        if (url != null && url.contains("error=wrongCaptcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.setFollowRedirects(true);
        dl = br.openDownload(link, url, true, 0);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        if (parameter.getDownloadURL().contains("duckload.com")) {
            br.getPage("http://duckload.com/english.html");
        } else {
            br.getPage("http://youload.to/english.html");
        }
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("File was not found!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("You want to download the file \"(.*?)\".*?!<br>").getMatch(0);
        String filesize = br.getRegex("You want to download the file \".*?\" \\((.*?)\\) !<br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.trim()));
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
        return getMaxSimultanDownloadNum();
    }

}
