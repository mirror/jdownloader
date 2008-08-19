package jd.plugins.host;

import java.io.File;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class FileUploadnet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "file-upload.net";

    static private final Pattern PAT_Download = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html", Pattern.CASE_INSENSITIVE);

    static private final Pattern PAT_VIEW = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html)", Pattern.CASE_INSENSITIVE);

    static private final Pattern PAT_Member = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_SUPPORTED = Pattern.compile(PAT_Download.pattern() + "|" + PAT_VIEW.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE);
    private static final String PLUGIN_NAME = HOST;
    private String downloadurl;

    public FileUploadnet() {
        super();
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.file-upload.net/to-agb.html";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        Browser.clearCookies(HOST);
        br.setFollowRedirects(false);
        try {
            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
                /* LinkCheck für DownloadFiles */
                downloadurl = downloadLink.getDownloadURL();

                br.getPage(downloadurl);
                if (!br.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = br.getRegex("<h1>Download \"(.*?)\"</h1>").getMatch(0);
                    String filesize;
                    if ((filesize = br.getRegex("e:</b></td><td>(.*?)Kbyte<td>").getMatch(0)) != null) {
                        downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return true;
                }
            } else if (new Regex(downloadLink.getDownloadURL(), PAT_VIEW).matches()) {
                /* LinkCheck für DownloadFiles */
                downloadurl = downloadLink.getDownloadURL();
                br.getPage(downloadurl);
                if (!br.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = br.getRegex("<h1>Bildeigenschaften von \"(.*?)\"</h1>").getMatch(0);
                    String filesize;
                    if ((filesize = br.getRegex("e:</b>(.*?)Kbyte").getMatch(0)) != null) {
                        downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return true;
                }
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
        return PLUGIN_NAME;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
            /* DownloadFiles */
            downloadurl = br.getRegex("action=\"(.*?)\" method=\"post\"").getMatch(0);
            Form form = br.getForm(0);
            br.openFormConnection(form);

        } else if (new Regex(downloadLink.getDownloadURL(), PAT_VIEW).matches()) {
            /* DownloadFiles */
            downloadurl = br.getRegex("<center>\n<a href=\"(.*?)\" rel=\"lightbox\"").getMatch(0);
            br.openGetConnection(downloadurl);
        } else {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        /* Datei herunterladen */
        HTTPConnection urlConnection = br.getHttpConnection();
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }
}
