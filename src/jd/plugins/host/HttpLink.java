package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;

public class HttpLink extends PluginForHost {
    static private final  String  host             = "Unknown";

    private String  version          = "1.0.0.0";

    // http://(?:[^.]*\.)*rapidshare\.com/files/[0-9]*/[^\s"]+
    private Pattern patternSupported = Pattern.compile("http://[^\\s\"].[^\\s\"].[^\\s\"]+");

    /**
     * Das findet die Ziel URL für den Post
     */

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
        return "Youporn.com-1.0.0.";
    }

    @Override
    public void init() {
        currentStep = null;
    }

    public HttpLink() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    @Override
    public URLConnection getURLConnection() {
        return null;
    }

    @Override
    public PluginStep doNextStep(Object parameter) {
        DownloadLink downloadLink = (DownloadLink) parameter;
        PluginStep todo = steps.firstElement();

        boolean success = prepareDownload(downloadLink);
        if (success) {
            todo.setStatus(PluginStep.STATUS_DONE);
            downloadLink.setStatus(DownloadLink.STATUS_DONE);
            return null;
        }
        else {
            logger.severe("Error");
            downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
            todo.setStatus(PluginStep.STATUS_ERROR);
        }

        return todo;
    }

    private boolean prepareDownload(DownloadLink downloadLink) {
        try {
            URLConnection urlConnection = new URL(downloadLink.getUrlDownloadDecrypted()).openConnection();

            int length = urlConnection.getContentLength();
            logger.info("" + length);
            downloadLink.setDownloadMax(length);
            return download(downloadLink, urlConnection);
        }
        catch (IOException e) {
            logger.severe("URL could not be opened. " + e.toString());
        }
        return false;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public void reset() {
    // TODO Auto-generated method stub

    }

    @Override
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean checkAvailability(DownloadLink parameter) {
        // TODO Auto-generated method stub
        return false;
    }

}
