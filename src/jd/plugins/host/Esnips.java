package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.JDUtilities;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Esnips extends PluginForHost {

    static private final String host               = "esnips.com";

    private String              version            = "1.0.0.0";

    private Pattern             patternSupported   = getSupportPattern("http://[*]esnips.com/doc/[+]");


    private static final String SWF_PLAYER_TO_FILE = "autoPlay=no&amp;theFile=°&amp;theName=°&amp;thePlayerURL";

    private static final String WMP_PLAYER_TO_FILE = "<param name=\"URL\" value=\"°\" ref=\"\">";

    @Override
    public String getCoder() {
        return "Coalado";
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
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public String getPluginID() {
        return host + version;
    }

    @Override
    public void init() {
        currentStep = null;
    }

    public Esnips() {
        super();

        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    // @Override
    // public URLConnection getURLConnection() {
    // return null;
    // }

    @Override
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {

        DownloadLink downloadLink = null;
        try {
            logger.info("Step: " + step);
            downloadLink = (DownloadLink) parameter;

            switch (step.getStep()) {

                case PluginStep.STEP_DOWNLOAD:
                    URL url;
                    String fileUrl = null;

                    try {
                        url = new URL(downloadLink.getUrlDownloadDecrypted());

                        requestInfo = getRequest(url, null, null, true);

                        fileUrl = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 0);
                        String fileName = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 1);
                        if (fileUrl == null) {

                            fileUrl = "http://" + host + getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 0);
                            fileName = getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 1);
                        }

                    }
                    catch (Exception e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        e.printStackTrace();
                    }

                    requestInfo = getRequestWithoutHtmlCode(new URL(fileUrl), requestInfo.getCookie(), null, true);

                    int length = requestInfo.getConnection().getContentLength();
                    downloadLink.setDownloadMax(length);
                    logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));

                    downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
                    if (!download(downloadLink, (URLConnection) requestInfo.getConnection())) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    }
                    else {
                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    }
                    return step;

            }
        }
        catch (Exception e) {
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            e.printStackTrace();
            return step;
        }
        return step;

    }

    private String getLinkDetails(String string) {
        URL url;

        try {
            url = new URL("http://" + host + string);

            requestInfo = getRequest(url, null, null, true);

            String fileUrl = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 0);
            String fileName = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 1);
            if (fileUrl == null) {

                fileUrl = "http://" + host + getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 0);
                fileName = getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 1);
            }
            setStatusText(fileName);
            // requestInfo = getRequest(new URL(fileUrl),
            // requestInfo.getCookie(), null, false);

            return fileUrl;
        }
        catch (MalformedURLException e) {

            // e.printStackTrace();
        }
        catch (IOException e) {
            // e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {

        requestInfo = null;

    }
    public String getFileInformationString(DownloadLink downloadLink){
        return downloadLink.getName()+" ("+JDUtilities.formatBytesToMB(downloadLink.getDownloadMax())+")";
    }
    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        // TODO Auto-generated method stub
        try {
            URL url = new URL(downloadLink.getUrlDownloadDecrypted());

            requestInfo = getRequest(url, null, null, true);

            String fileUrl = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 0);
            String fileName = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 1);
            if (fileUrl == null) {

                fileUrl = "http://" + host + getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 0);
                fileName = getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 1);
            }
            requestInfo = getRequestWithoutHtmlCode(new URL(fileUrl), requestInfo.getCookie(), null, true);

            int length = requestInfo.getConnection().getContentLength();
            downloadLink.setDownloadMax(length);        

            downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));           
             if(downloadLink.getName()==null ||downloadLink.getName().length()==0||length==0)return false;
            
            if (fileUrl != null) return true;
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

  

}
