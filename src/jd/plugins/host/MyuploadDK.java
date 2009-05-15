package jd.plugins.host;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class MyuploadDK extends PluginForHost {

    public MyuploadDK(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.myupload.dk/rules/";
    }

    public boolean getFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.myupload.dk", "lang", "en");
        br.getPage(parameter.getDownloadURL());
        String filename = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename);
        return true;
    }

    public void handleFree(DownloadLink link) throws Exception {
        getFileInformation(link);
        String url = br.getRegex("to download <a href='(/download/.*?)'>.*?</a><br />").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = br.openDownload(link, url);
        dl.startDownload();
    }

    public void reset() {
        // TODO Auto-generated method stub

    }

    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    public String getVersion() {
        return getVersion("$Revision$");

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

}
