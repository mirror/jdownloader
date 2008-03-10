package jd.plugins.host;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.utils.JDUtilities;

public class Esnips extends PluginForHost {
    static private final String host               = "esnips.com";
    private String              version            = "1.0.0.0";
    //http://www.esnips.com/doc/ecbaae34-3c63-41af-b6b1-ee09638883b2/Morder
    //http://www.esnips.com/doc/b2ab414d-000e-4d0c-9fa7-42d625f4d02f/Partido-en-dos
    //http://www.esnips.com/doc/09bd1900-66ec-4145-8b35-b6c4bf4dfbec/Zero★約束
    //http://www.esnips.com/doc/9ce037c1-efe1-4ff3-93a1-f689bfdd4e3b/山根康広★Get-Along-Together
    static private final Pattern patternSupported = Pattern.compile("http://.*?esnips\\.com/doc/.{8}\\-.{4}\\-.{4}\\-.{4}\\-.{12}/.*", Pattern.CASE_INSENSITIVE);
    private static final String SWF_PLAYER_TO_FILE = "autoPlay=no&amp;theFile=°&amp;theName=°&amp;thePlayerURL";
    private static final String WMP_PLAYER_TO_FILE = "<param name=\"URL\" value=\"°\" ref=\"\">";
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
                        url = new URL(downloadLink.getDownloadURL());
                        requestInfo = getRequest(url, null, null, true);
                        fileUrl = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 0);
                      //  String fileName = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 1);
                        if (fileUrl == null) {
                            fileUrl = "http://" + host + getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 0);
                           // fileName = getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 1);
                        }
                    }
                    catch (MalformedURLException e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    catch (FileNotFoundException e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    catch (Exception e) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                         e.printStackTrace();
                        return step;
                    }
                    requestInfo = getRequestWithoutHtmlCode(new URL(fileUrl), requestInfo.getCookie(), null, true);
                    int length = requestInfo.getConnection().getContentLength();
                    downloadLink.setDownloadMax(length);
                    logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));
                    downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
                    if(!hasEnoughHDSpace(downloadLink)){
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    if (download(downloadLink, requestInfo.getConnection()) != DOWNLOAD_SUCCESS) {
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
        catch (MalformedURLException e) {
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
            return step;
        }
        catch (FileNotFoundException e) {
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
            step.setStatus(PluginStep.STATUS_ERROR);
            return step;
        }
        catch (Exception e) {
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            step.setStatus(PluginStep.STATUS_ERROR);
             e.printStackTrace();
            return step;
        }
        return step;
    }
    /*
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
        catch (MalformedURLException e) { }
        catch (IOException e)           { }
        return null;
    }
    */
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
    @Override
    public void reset() {
        requestInfo = null;
    }
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }
    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            URL url = new URL(downloadLink.getDownloadURL());
            requestInfo = getRequest(url, null, null, true);
            String fileUrl = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 0);
            //String fileName = getSimpleMatch(requestInfo.getHtmlCode(), SWF_PLAYER_TO_FILE, 1);
            if (fileUrl == null) {
                fileUrl = "http://" + host + getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 0);
                //fileName = getSimpleMatch(requestInfo.getHtmlCode(), WMP_PLAYER_TO_FILE, 1);
            }
            requestInfo = getRequestWithoutHtmlCode(new URL(fileUrl), requestInfo.getCookie(), null, true);
            int length = requestInfo.getConnection().getContentLength();
            downloadLink.setDownloadMax(length);
            downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
            if (downloadLink.getName() == null || downloadLink.getName().length() == 0 || length == 0) return false;
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
    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }
	@Override
	public String getAGBLink() {
		// TODO Automatisch erstellter Methoden-Stub
		return "http://esnips.com";
	}
}
