package jd.plugins.host;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.utils.JDUtilities;

public class RadioBlogClub extends PluginForHost {
    static private final String  host             = "radioblogclub.com";

    private String               version          = "1.0.0.0";

    private String               realURL;

    // http://www.radioblogclub.com/listen?u=..wLzRmb192cvc2bsJmLvlGZhJ3LvlGZhJ3ZvxmYvUHauEmc0hXZuIDMuFWbyVGa/Jon%2520Bon%2520Jovi%2520-%2520Have%2520a%2520Nice%2520Day.mp3.rbs
    static private final Pattern patternSupported = Pattern.compile("http://.*?radioblogclub\\.com/listen\\?u\\=(.*?)\\.rbs", Pattern.CASE_INSENSITIVE);

    // http://www.radioblogclub.com/listen?u=.8yck5WdvN3Ln9Gbi5ybpRWYy9Sdo5yd15yYpR3cp92Zl92c/Shakira%2520-%2520Illegal.rbs
    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return host + version;
    }

    @Override
    public void init() {
        currentStep = null;
    }

    public RadioBlogClub() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    // @Override
    // public URLConnection getURLConnection() {
    // return null;
    // }
    @Override
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        DownloadLink downloadLink = (DownloadLink) parameter;
        if (step.getStep() == PluginStep.STEP_PAGE) {
            try {
                
                    URL url = prepare(downloadLink);
                    requestInfo = getRequestWithoutHtmlCode(url, null, null, false);
                    if (requestInfo.getLocation() != null) {
                        this.realURL = requestInfo.getLocation();
                        // downloadLink.setUrlDownload(realURL);

                    }
                    else {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        return step;
                    }
             
              
                    requestInfo = getRequestWithoutHtmlCode(new URL(realURL), null, null, true);
                    // logger.info(requestInfo.isOK()+" : "+realURL+"
                    // -"+requestInfo.getHeaders()+"");
                    downloadLink.setDownloadMax(requestInfo.getConnection().getContentLength());

                    if (requestInfo.isOK() && requestInfo.getConnection().getContentLength() > (1000 * 500)) {
                        String name = downloadLink.getName();
                        if (name.endsWith(".rbs")) {
                            name = name.substring(0, name.length() - 3) + "mp3";
                            downloadLink.setName(name);
                        }
                        return step;
                    }
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                    return step;
               

        
            }
            catch (Exception e) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);e.printStackTrace();
                return step;
            }
        }
        if (step.getStep() == PluginStep.STEP_DOWNLOAD) {
            if (requestInfo == null) {
                try {
                    logger.info(realURL);
                    requestInfo = getRequestWithoutHtmlCode(new URL(realURL), null, null, true);
                   logger.info(requestInfo.isOK()+" : "+realURL+"    // -"+requestInfo.getHeaders()+"");
                    downloadLink.setDownloadMax(requestInfo.getConnection().getContentLength());
                }
                catch (Exception e) {
                    step.setStatus(PluginStep.STATUS_ERROR);
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    e.printStackTrace();
                    return step;
                }
                if (requestInfo.isOK() && requestInfo.getConnection().getContentLength() > (1000 * 500)) {
                    String name = downloadLink.getName();
                    if (name.endsWith(".rbs")) {
                        name = name.substring(0, name.length() - 3) + "mp3";
                        downloadLink.setName(name);
                    }
                    return step;
                }
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                return step;

            }
            if (!download(downloadLink, (URLConnection) requestInfo.getConnection())) {
                step.setStatus(PluginStep.STATUS_ERROR);
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            }
            else {
                step.setStatus(PluginStep.STATUS_DONE);
                downloadLink.setStatus(DownloadLink.STATUS_DONE);
            }
        }

        return step;
    }

    public boolean isListOffline() {
        return false;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
        requestInfo = null;
    }

    public String getFileInformationString(DownloadLink downloadLink) {
        return " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }

    private URL prepare(DownloadLink downloadLink) throws MalformedURLException {

        return new URL(downloadLink.getUrlDownloadDecrypted() + "&k=657ecb3231ac0b275497d4d6f00b61a1");

    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            if (realURL == null) {
                URL url = prepare(downloadLink);
                requestInfo = getRequestWithoutHtmlCode(url, null, null, false);
                if (requestInfo.getLocation() != null) {
                    this.realURL = requestInfo.getLocation();
                    // downloadLink.setUrlDownload(realURL);

                }
            }
            if (realURL != null) {
                requestInfo = getRequestWithoutHtmlCode(new URL(realURL), null, null, true);
                // logger.info(requestInfo.isOK()+" : "+realURL+"
                // -"+requestInfo.getHeaders()+"");
                downloadLink.setDownloadMax(requestInfo.getConnection().getContentLength());

                if (requestInfo.isOK() && requestInfo.getConnection().getContentLength() > (1000 * 500)) {
                    String name = downloadLink.getName();
                    if (name.endsWith(".rbs")) {
                        name = name.substring(0, name.length() - 3) + "mp3";
                        downloadLink.setName(name);
                    }
                    return true;
                }
                return false;
            }

            return false;
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return 15;
    }

    @Override
    public void resetPluginGlobals() {
    // TODO Auto-generated method stub

    }
}
