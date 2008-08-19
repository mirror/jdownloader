package jd.plugins.host;

import java.io.File;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class FastShareorg extends PluginForHost {

    private static final String HOST = "fastshare.org";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?fastshare\\.org/download/(.*)", Pattern.CASE_INSENSITIVE);
    private String url;

    //

    public FastShareorg() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.fastshare.org/discl.php";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {

        try {
            Browser.clearCookies(HOST);
            br.setFollowRedirects(false);
            String url = downloadLink.getDownloadURL();
            br.getPage(url);
            if (!br.containsHTML("No filename specified or the file has been deleted")) {
                downloadLink.setName(Encoding.htmlDecode(br.getRegex("Wenn sie die Datei \"<b>(.*?)<\\/b>\"").getMatch(0)));
                String filesize = null;
                if ((filesize = br.getRegex("<i>\\((.*)MB\\)</i>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = br.getRegex("<i>\\((.*)KB\\)</i>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024);
                }
                return true;
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        url = downloadLink.getDownloadURL();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        /* Link holen */
        Form form = br.getForm(0);
        br.submitForm(form);
        if ((url = new Regex(br, "Link: <a href=(.*)><b>").getMatch(0)) == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        /* Zwangswarten, 10seks */
        sleep(10000, downloadLink);

        HTTPConnection urlConnection = br.openGetConnection(url);
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.startDownload();

    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
