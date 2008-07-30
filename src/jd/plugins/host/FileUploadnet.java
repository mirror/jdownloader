package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;

public class FileUploadnet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "file-upload.net";

    private static final String PLUGIN_NAME = HOST;

    //private static final String new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    //private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    static private final Pattern PAT_Member = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/member/data3\\.php\\?user=(.*?)&name=(.*)", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_Download = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(member/){0,1}download-\\d+/(.*?).html", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_View = Pattern.compile("http://[\\w\\.]*?file-upload\\.net/(view-\\d+/(.*?).html|member/view_\\d+_(.*?).html)", Pattern.CASE_INSENSITIVE);
    static private final Pattern PAT_SUPPORTED = Pattern.compile(PAT_Download.pattern() + "|" + PAT_View.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String downloadurl;

    public FileUploadnet() {
        super();
        //steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }

    
    public String getCoder() {
        return CODER;
    }

    
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    
    public String getHost() {
        return HOST;
    }

    
    public String getVersion() {
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    
        
    

    
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    
    public void reset() {
    }

    
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    
    public boolean getFileInformation(DownloadLink downloadLink) { LinkStatus linkStatus=downloadLink.getLinkStatus();
        try {
            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
                /* LinkCheck für DownloadFiles */
                downloadurl = downloadLink.getDownloadURL();
                requestInfo = HTTP.getRequest(new URL(downloadurl));
                if (!requestInfo.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = requestInfo.getRegexp("<h1>Download \"(.*?)\"</h1>").getFirstMatch();
                    String filesize;
                    if ((filesize = requestInfo.getRegexp("e:</b></td><td>(.*?)Kbyte<td>").getFirstMatch()) != null) {
                        downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
                    }
                    downloadLink.setName(filename);
                    return true;
                }
            } else if (new Regex(downloadLink.getDownloadURL(), PAT_View).matches()) {
                /* LinkCheck für DownloadFiles */
                downloadurl = downloadLink.getDownloadURL();
                requestInfo = HTTP.getRequest(new URL(downloadurl));
                if (!requestInfo.containsHTML("Datei existiert nicht auf unserem Server")) {
                    String filename = requestInfo.getRegexp("<h1>Bildeigenschaften von \"(.*?)\"</h1>").getFirstMatch();
                    String filesize;
                    if ((filesize = requestInfo.getRegexp("e:</b>(.*?)Kbyte").getFirstMatch()) != null) {
                        downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize.trim())) * 1024);
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

     public void handle(DownloadLink downloadLink) throws Exception{ LinkStatus linkStatus=downloadLink.getLinkStatus();
        
   
            /* Nochmals das File überprüfen */
            if (!getFileInformation(downloadLink)) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                //step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }

            if (new Regex(downloadLink.getDownloadURL(), Pattern.compile(PAT_Download.pattern() + "|" + PAT_Member.pattern(), Pattern.CASE_INSENSITIVE)).matches()) {
                /* DownloadFiles */
                downloadurl = requestInfo.getRegexp("action=\"(.*?)\" method=\"post\"").getFirstMatch();
                Form form = requestInfo.getForms()[0];
                form.withHtmlCode = false;
                requestInfo = form.getRequestInfo(false);
            } else if (new Regex(downloadLink.getDownloadURL(), PAT_View).matches()) {
                /* DownloadFiles */
                downloadurl = requestInfo.getRegexp("<center>\n<a href=\"(.*?)\" rel=\"lightbox\"").getFirstMatch();
                requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(downloadurl), null, downloadLink.getDownloadURL(), false);
            } else {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                //step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }

            /* Datei herunterladen */
            HTTPConnection urlConnection = requestInfo.getConnection();
            String filename = getFileNameFormHeader(urlConnection);
            if (urlConnection.getContentLength() == 0) {
                linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                //step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            downloadLink.setDownloadMax(urlConnection.getContentLength());
            downloadLink.setName(filename);
            long length = downloadLink.getDownloadMax();
            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setFilesize(length);
            dl.setChunkNum(1);
            dl.setResume(false);
           dl.startDownload(); 
    }

    
    public void resetPluginGlobals() {
       
    }

    
    public String getAGBLink() {
        return "http://www.file-upload.net/to-agb.html";
    }
}
