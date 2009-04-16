package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class UploadingCom extends PluginForHost {

    public UploadingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://uploading.com/terms/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://www.uploading.com/", "_lang", "en");
        br.setCookie("http://www.uploading.com/", "setlang", "en");
        br.getPage(downloadLink.getDownloadURL());
        String filesize = br.getRegex("File size:(.*?)<br").getMatch(0);
        String filename = br.getRegex(Pattern.compile("Download file.*?<b>(.*?)</b>", Pattern.DOTALL)).getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        if (br.containsHTML("YOU REACHED YOUR COUNTRY DAY LIMIT")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "YOU REACHED YOUR COUNTRY DAY LIMIT", 60 * 60 * 1000l);
        Form form = br.getFormbyProperty("id", "downloadform");
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        br.submitForm(form);
        this.sleep(100000l, downloadLink);
        br.setFollowRedirects(false);
        form = br.getFormbyProperty("id", "downloadform");
        br.submitForm(form);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), false, 1);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2500;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }

}
