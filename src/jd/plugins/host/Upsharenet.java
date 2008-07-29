package jd.plugins.host;

import java.io.File;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Upsharenet extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "upshare.net";

    private static final String PLUGIN_NAME = HOST;

    private static final String PLUGIN_VERSION = "1.0.0.0";

    private static final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?upshare\\.(net|eu)/download\\.php\\?id=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    private RequestInfo requestInfo;
    private String downloadurl;
    private File captchaFile;
    private String captchaCode;
    private String passCode=null;

    public Upsharenet() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getVersion() {
        return PLUGIN_VERSION;
    }

    @Override
    public String getPluginID() {
        return PLUGIN_ID;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        /*.eu zu .net weiterleitung*/        
        downloadLink.setUrlDownload(downloadLink.getDownloadURL().replaceAll("upshare\\.(net|eu)", "upshare\\.net"));
        
        downloadurl = downloadLink.getDownloadURL();        
        try {
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (!requestInfo.containsHTML("Your requested file is not found")) {
                String linkinfo[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<b>File size:</b></td>[\\r\\n\\s]*<td align=left>([0-9\\.]*) ([GKMB]*)</td>", Pattern.CASE_INSENSITIVE)).getMatches();

                if (linkinfo[0][1].matches("MB")) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024 * 1024));
                } else if (linkinfo[0][1].matches("KB")) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(linkinfo[0][0]) * 1024));
                }
                downloadLink.setName(new URL(downloadurl).getQuery().substring(3));
                return true;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        if (step == null) return null;
        try {
            /* Nochmals das File überprüfen */
            if (!getFileInformation(downloadLink)) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                //step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            Form form = requestInfo.getForms()[1];
            /* Captcha File holen */
            captchaFile = getLocalCaptchaFile(this);
            HTTPConnection captcha_con = new HTTPConnection(new URL("http://www.upshare.net/captcha.php").openConnection());
            captcha_con.setRequestProperty("Referer", downloadLink.getDownloadURL());
            captcha_con.setRequestProperty("Cookie", requestInfo.getCookie());
            if (!JDUtilities.download(captchaFile, captcha_con) || !captchaFile.exists()) {
                /* Fehler beim Captcha */
                logger.severe("Captcha Download fehlgeschlagen!");
                //step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                return;
            }
            /* CaptchaCode holen */
            if ((captchaCode = Plugin.getCaptchaCode(captchaFile, this)) == null) {
                //step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                return;
            }
            form.vars.put("captchacode", captchaCode);
            /* Passwort holen holen */
            if (form.vars.containsKey("downloadpw")) {
                if (downloadLink.getStringProperty("pass", null) == null) {
                    if ((passCode = JDUtilities.getGUI().showUserInputDialog("Code?")) == null) passCode = "";
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                form.vars.put("downloadpw", passCode);
            }
            /* Pass/Captcha check */
            requestInfo = form.getRequestInfo(false);
            if (requestInfo.containsHTML("<span>Password Error</span>")){
                /* PassCode war falsch, also Löschen */
                downloadLink.setProperty("pass", null);
                //step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                return;
            }            
            if (requestInfo.containsHTML("<span>Captcha number error or expired</span>")){                
                //step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                return;
            }            
            if (requestInfo.containsHTML("<span>You have got max allowed download sessions from the same IP!</span>")){                
                //step.setStatus(PluginStep.STATUS_ERROR);
               // step.setParameter(60 * 60 * 1000L);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                return;
            }
            /* PassCode war richtig, also Speichern */
            downloadLink.setProperty("pass", passCode);
            /*DownloadLink holen*/
            String link = new Regex(requestInfo.getHtmlCode(),Pattern.compile("document.location=\"(.*?)\"",Pattern.CASE_INSENSITIVE)).getFirstMatch();
            if (link== null){
                //step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                return;
            }
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(link), requestInfo.getCookie(), downloadLink.getDownloadURL(), false);
            if (requestInfo.getLocation()!=null){
                //step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                return;
            }
            /* Datei herunterladen */
            HTTPConnection urlConnection = requestInfo.getConnection();
            String filename = getFileNameFormHeader(urlConnection);
            if (urlConnection.getContentLength() == 0) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
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
            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_TEMPORARILY_UNAVAILABLE);
                //step.setStatus(PluginStep.STATUS_ERROR);
                return;
            }
            return;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        return;
    }

    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.upshare.net/faq.php?setlang=en";
    }

}
