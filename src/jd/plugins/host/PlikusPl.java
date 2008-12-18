package jd.plugins.host;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class PlikusPl extends PluginForHost {

    public PlikusPl(PluginWrapper wrapper) {
        super(wrapper);
        setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.plikus.pl/index.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filesize = br.getRegex("<p><b>Rozmiar pliku:</b>(.*?)</p>").getMatch(0);
        String filename = br.getRegex("<h1><img src=.*?/>(.*?)</h1>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 3397 $");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        Form form = br.getForm(0);
        br.setFollowRedirects(false);
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        dl = br.openDownload(downloadLink, form, true, 0);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
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
