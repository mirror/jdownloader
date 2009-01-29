package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
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
        br.getPage(downloadLink.getDownloadURL());
        br.setFollowRedirects(true);
        br.getPage("http://www.uploading.com/lang/?lang=en");
        String filesize = br.getRegex("File size:(.*?)<br/>").getMatch(0);
        String filename = br.getRegex("<h3>Download file.*?<b>(.*?)</b>").getMatch(0);
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
        br.getPage("http://www.uploading.com/lang/?lang=en");
        Form form = br.getForm(2);
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            return;
        }
        br.submitForm(form);
        sleep(100000l, downloadLink);
        br.setFollowRedirects(false);
        form = br.getForm(1);
        br.submitForm(form);
        if (br.getRedirectLocation() == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, 1);
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
