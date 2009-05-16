package jd.plugins.host;

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class IFileIt extends PluginForHost {

    public IFileIt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://ifile.it/tos";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("file not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<title>(.*?)\\s+. Ticket").getMatch(0);
        String filesize = br.getRegex("nbsp;\\s+\\((.*?)\\)\\s").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        /* Nochmals das File überprüfen */
        requestFileInformation(downloadLink);
        String dlLink;
        String previousLink = downloadLink.getStringProperty("directLink", null);
        if (previousLink == null) {
            String it = br.getRegex("file_key\" value=\"(.*?)\"").getMatch(0);
            Browser br2 = br.cloneBrowser();
            br2.getPage("http://ifile.it/download:dl_request?it=" + it + ",type=na,esn=1");
            if (br2.containsHTML("show_captcha")) {
                URLConnectionAdapter con = br.openGetConnection("http://ifile.it/download:captcha?" + Math.random());
                File file = this.getLocalCaptchaFile();
                Browser.download(file, con);
                String code = getCaptchaCode(file, downloadLink);
                br2 = br.cloneBrowser();
                br2.getPage("http://ifile.it/download:dl_request?it=" + it + ",type=simple,esn=1,0d149=" + code + ",0d149x=0");
                if (br2.containsHTML("retry")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            br.getPage("http://ifile.it/dl");
            dlLink = br.getRegex("var __url\\s+=\\s+'(http://.*?)'").getMatch(0);
            if (dlLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dlLink = dlLink.replaceAll(" ", "%20");
            downloadLink.setProperty("directLink", dlLink);

        } else {
            dlLink = previousLink;
        }
        br.setDebug(true);
        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, dlLink, true, -2);
        URLConnectionAdapter con = dl.getConnection();
        if (!con.isOK()) {
            if (previousLink != null) {
                downloadLink.setProperty("directLink", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            }
        }
        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void reset_downloadlink(DownloadLink link) {
        link.setProperty("directLink", null);
    }
}
