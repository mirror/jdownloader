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

public class Dataupde extends PluginForHost {
    private static final String CODER = "JD-Team";

    private static final String HOST = "dataup.de";

    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?dataup\\.de/\\d+/(.*)", Pattern.CASE_INSENSITIVE);

    // private static final String new Regex("$Revision$","\\$Revision:
    // ([\\d]*?)\\$").getFirstMatch().*= "1.0.0.0";

    // private static final String PLUGIN_ID =PLUGIN_NAME + "-" + new
    // Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();

    private static final String PLUGIN_NAME = HOST;
    private String downloadurl;
    private RequestInfo requestInfo;

    public Dataupde() {
        super();
        // steps.add(new PluginStep(PluginStep.STEP_COMPLETE, null));
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getAGBLink() {
        return "http://www.dataup.de/agb";
    }

    @Override
    public String getCoder() {
        return CODER;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            downloadurl = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(downloadurl));
            if (!requestInfo.containsHTML(">Fehler!<")) {
                String filename = requestInfo.getRegexp("helvetica;\">(.*?)</div>").getFirstMatch();
                String filesize;
                if ((filesize = requestInfo.getRegexp("<label>Gr��e:(.*?)MB</label>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize.trim().replaceAll(",", "."))) * 1024 * 1024);
                } else if ((filesize = requestInfo.getRegexp("<label>Gr��e:(.*?)KB</label>").getFirstMatch()) != null) {
                    downloadLink.setDownloadMax((int) Math.round(Double.parseDouble(filesize.trim().replaceAll(",", "."))) * 1024);
                }
                downloadLink.setName(filename);
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

    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        /* Nochmals das File überprüfen */
        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }

        /* 10 seks warten, kann weggelassen werden */
        // this.sleep(10000, downloadLink);
        /* DownloadLink holen */
        Form form = requestInfo.getForms()[2];
        form.withHtmlCode = false;
        requestInfo = form.getRequestInfo(false);

        /* DownloadLimit? */
        if (requestInfo.getLocation() != null) {
            // step.setStatus(PluginStep.STATUS_ERROR);
            // step.setParameter(120000L);
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            return;
        }

        /* Datei herunterladen */
        HTTPConnection urlConnection = requestInfo.getConnection();        
        if (urlConnection.getContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            // step.setStatus(PluginStep.STATUS_ERROR);
            return;
        }
        dl = new RAFDownload(this, downloadLink, urlConnection);        
        dl.setChunkNum(1);
        dl.setResume(false);
        dl.startDownload();
        //           
        // \r\n if (!dl.startDownload() && step.getStatus() !=
        // PluginStep.STATUS_ERROR && step.getStatus() !=
        // PluginStep.STATUS_TODO) {
        // linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // return;
        // }
        // return;
        // } catch (Exception e) {
        // 
        // e.printStackTrace();
        // }
        // //step.setStatus(PluginStep.STATUS_ERROR);
        // linkStatus.addStatus(LinkStatus.ERROR_RETRY);
        // return;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {

    }
}
