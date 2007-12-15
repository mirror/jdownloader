package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;

public class ShareOnlineBiz extends PluginForHost {
    private static final String CODER = "DwD";

    private static final String HOST = "share-online.biz";

    private static final String PLUGIN_NAME = HOST;

    private static final String PLUGIN_VERSION = "1.0.0.0";

    private static final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    // http://share-online.biz/dl/1/48DQA0U39
    private static final Pattern PAT_SUPPORTED = getSupportPattern("http://[*]share-online.biz/dl/[+]");

    private static String ERROR_DOWNLOAD_NOT_FOUND = "Invalid download link";
    private static Pattern firstIFrame = Pattern.compile("<iframe src=\"(http://.*?share-online\\.biz/dl_page.php\\?file=.*?)\" style=\"border", Pattern.CASE_INSENSITIVE);
    private static Pattern dlButtonFrame = Pattern.compile("<iframe name=.download..*?src=.(dl_button.php\\?file=.*?). marginwidth\\=", Pattern.CASE_INSENSITIVE);
    private static Pattern downloadl = Pattern.compile("window\\.location=\\\\'(http://.*?share-online.biz/download.*?)\\\\'\">\'", Pattern.CASE_INSENSITIVE);
    private static Pattern countDown = Pattern.compile("<script language=\"Javascript\">[\n\r]+.*?\\=([0-9]+)", Pattern.CASE_INSENSITIVE);
    private static Pattern filename = Pattern.compile("<center><b>File Name:</b><br>(.*?)<br><i>", Pattern.CASE_INSENSITIVE);
    private static Pattern filesize = Pattern.compile("</i><br><b>File Size:</b> ([0-9]+)", Pattern.CASE_INSENSITIVE);
    private static Pattern dloc = Pattern.compile("src=\'(dl.php?.*?)\'", Pattern.CASE_INSENSITIVE);
    //src='dl.php?a=48DQA0U39&b=344f4df81f101a8393e73fe5c61a6fe2'
    private long ctime = 0;
    private String dlink = "";
    // <center><b>File
    // Name:</b><br>kompas.www.godlike-project.com.part6.rar<br><i>(M&ouml;glicherweise
    // gek&uuml;rzt)</i><br><b>File Size:</b> 100 MB<br><b>
    public ShareOnlineBiz() {
        super();

        steps.add(new PluginStep(PluginStep.STEP_PAGE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }

    /*
     * Funktionen
     */
    // muss aufgrund eines Bugs in DistributeData true zurÃ¼ckgeben, auch wenn
    // die Zwischenablage nicht vom Plugin verarbeitet wird
    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

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
            return 1;
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            requestInfo = getRequest(new URL(downloadLink.getUrlDownloadDecrypted()));
            URL fFrame = new URL(getFirstMatch(requestInfo.getHtmlCode(), firstIFrame, 1));
            requestInfo = getRequest(fFrame);
            if (requestInfo.containsHTML(ERROR_DOWNLOAD_NOT_FOUND)) {
                logger.severe("download not found");
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                return false;
            }
            String name = getFirstMatch(requestInfo.getHtmlCode(), filename, 1);
            String size = getFirstMatch(requestInfo.getHtmlCode(), filesize, 1);
            downloadLink.setName(name);
            if (name != null) {
                try {
                    int length = (int) (Double.parseDouble(size) * 1024 * 1024);
                    downloadLink.setDownloadMax(length);
                } catch (Exception e) {
                }
            }
            return true;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return false;
    }

    public PluginStep doStep(PluginStep step, DownloadLink downloadLink) {
        try {

            URL downloadUrl = new URL(downloadLink.getUrlDownloadDecrypted());

            switch (step.getStep()) {
                case PluginStep.STEP_PAGE :
                    requestInfo = getRequest(downloadUrl);
                    URL fFrame = new URL(getFirstMatch(requestInfo.getHtmlCode(), firstIFrame, 1));
                    requestInfo = getRequest(fFrame);
                    if (requestInfo.containsHTML(ERROR_DOWNLOAD_NOT_FOUND)) {
                        logger.severe("download not found");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    URL dlb = new URL("http://" + fFrame.getHost() + "/" +getFirstMatch(requestInfo.getHtmlCode(), dlButtonFrame, 1));
                    
                    requestInfo = getRequest(dlb);
                    dlink = getFirstMatch(requestInfo.getHtmlCode(), downloadl, 1);
                    ctime = Integer.parseInt(getFirstMatch(requestInfo.getHtmlCode(), countDown, 1));

                    return step;

                case PluginStep.STEP_PENDING :
                    step.setParameter(ctime * 1000);
                    return step;

                case PluginStep.STEP_DOWNLOAD :
                    URL url = new URL(dlink);
                    requestInfo = getRequest(url);
                    dlink="http://"+url.getHost()+"/"+getFirstMatch(requestInfo.getHtmlCode(), dloc, 1);
                    requestInfo = getRequestWithoutHtmlCode(new URL(dlink), null, null, false);
                    URLConnection urlConnection = requestInfo.getConnection();
                    int length = urlConnection.getContentLength();
                    downloadLink.setDownloadMax(length);
                    downloadLink.setName(getFileNameFormHeader(urlConnection));
                    if (!hasEnoughHDSpace(downloadLink)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    boolean downloadSuccess = download(downloadLink, urlConnection);

                    if (downloadSuccess == false) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

                    } else {
                        step.setStatus(PluginStep.STATUS_DONE);
                        downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    }

                    return step;

            }
            return step;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub

    }

}
