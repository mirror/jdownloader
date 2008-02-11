package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Filer extends PluginForHost {
    /*
     * Pseudolink um getFileInformation zu ermöglichen
    */
    static private final Pattern PAT_SUPPORTED = Pattern.compile("http://[\\w\\.]*?filer.net/(file[\\d]+|get)/.*", Pattern.CASE_INSENSITIVE);
    static private final Pattern GETID = Pattern.compile("http://[\\w\\.]*?filer.net/file([\\d]+)/.*?", Pattern.CASE_INSENSITIVE);
    static private final String HOST = "filer.net";
    static private final String PLUGIN_NAME = HOST;
    static private final String PLUGIN_VERSION = "0.1";
    static private final String PLUGIN_ID = PLUGIN_NAME + "-" + PLUGIN_VERSION;
    static private final String CODER = "GforE";
    static private final String FILE_NOT_FOUND = "Konnte Download nicht finden";
    static private final String FILE_NOT_FOUND2 = "Oops! We couldn't find this page for you.";
    static private final String FREE_USER_LIMIT="Momentan sind die Limits f&uuml;r Free-Downloads erreicht";
    static private final String CAPTCHAADRESS = "http://www.filer.net/captcha.png";
    static private final Pattern DOWNLOAD = Pattern.compile("<form method=\"post\" action=\"(\\/dl\\/.*?)\">", Pattern.CASE_INSENSITIVE);
    static private final Pattern WAITTIME = Pattern.compile("Bitte warten Sie ([\\d]+)", Pattern.CASE_INSENSITIVE);
    static private final Pattern INFO = Pattern.compile("(?s)<td><a href=\"(\\/get\\/.*?.html)\">(.*?)</a></td>.*?<td>([0-9\\.]+) .*?</td>", Pattern.CASE_INSENSITIVE);
    static private final String WRONG_CAPTCHACODE = "<img src=\"/captcha.png\"";
    private String cookie;
    private String dlink = null;
    private String url;
    private int waitTime = 500;

    public Filer() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_GET_CAPTCHA_FILE, null));
        steps.add(new PluginStep(PluginStep.STEP_PENDING, null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }
    @Override
    public String getCoder() {
        return CODER;
    }
    @Override
    public String getPluginName() {
        return HOST;
    }
    @Override
    public Pattern getSupportedLinks() {
        return PAT_SUPPORTED;
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
    public PluginStep doStep(PluginStep step, DownloadLink parameter) {
        RequestInfo requestInfo;
        try {
            DownloadLink downloadLink = (DownloadLink) parameter;
            switch (step.getStep()) {
                case PluginStep.STEP_WAIT_TIME :
                    String strId = getFirstMatch(downloadLink.getDownloadURL(), GETID, 1);
                    if(strId!=null)
                    {
                        int id=Integer.parseInt(strId);
                        url=downloadLink.getDownloadURL().replaceFirst("filer.net\\/file[0-9]+\\/", "filer.net/folder/");
                        url=url.replaceFirst("\\/filename\\/.*","");
                        requestInfo = getRequest(new URL(url));
                            // Datei geloescht?
                            if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND2)) {
                                return step;
                            }
                            Vector<Vector<String>> matches = getAllSimpleMatches(requestInfo.getHtmlCode(), INFO);
                            if(matches.size()<id)
                                return step;
                            Vector<String> link = matches.get(id);
                        url="http://www.filer.net"+link.get(0);

                        requestInfo = getRequest(new URL(url));
                        cookie = requestInfo.getCookie();  
                    }
                    else
                        url=downloadLink.getDownloadURL();
                    logger.info(url);
                case PluginStep.STEP_GET_CAPTCHA_FILE :
                    File file = this.getLocalCaptchaFile(this);
                    requestInfo = getRequestWithoutHtmlCode(new URL(CAPTCHAADRESS), cookie, url, false);
                    if (!JDUtilities.download(file, requestInfo.getConnection()) || !file.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + CAPTCHAADRESS);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_IMAGEERROR);
                        return step;
                    } else {
                        step.setParameter(file);
                        step.setStatus(PluginStep.STATUS_USER_INPUT);
                    }
                    break;
                case PluginStep.STEP_PENDING :
                    String code = (String) steps.get(1).getParameter();
                    requestInfo = postRequest(new URL(url), cookie, url, null, "captcha=" + code + "&Download=Download", true);
                    dlink = getFirstMatch(requestInfo.getHtmlCode(), DOWNLOAD, 1);
                   
                    
                   
                    if (requestInfo.containsHTML(FREE_USER_LIMIT)) {
                        logger.severe("Free User Limit reached");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
                        step.setParameter("Free User Limit");
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND)) {
                        logger.severe("Die Datei existiert nicht");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_FILE_NOT_FOUND);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }

                    String strWaitTime = getFirstMatch(requestInfo.getHtmlCode(), WAITTIME, 1);
                    if (strWaitTime != null) {
                        logger.severe("wait " + strWaitTime + " minutes");
                        waitTime = Integer.parseInt(strWaitTime) * 60 * 1000;
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        logger.info(" WARTEZEIT SETZEN IN " + step + " : " + waitTime);
                        step.setParameter((long) waitTime);
                        return step;
                    }

                    if (dlink == null) {
                        logger.severe("Der Downloadlink konnte nicht gefunden werden");
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    dlink = "http://www.filer.net" +dlink;
                    if(requestInfo.getHtmlCode().contains(WRONG_CAPTCHACODE))
                    {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                        return step;
                    }
                    //auf 61 sec gesetzt weil der server sonst zickt
                    step.setParameter(61000l);
                    break;
                case PluginStep.STEP_DOWNLOAD :
                  

                    requestInfo = postRequestWithoutHtmlCode(new URL(dlink), cookie, url, "Download!=Download!", true);
                    if (requestInfo.getConnection().getHeaderField("Location") != null && requestInfo.getConnection().getHeaderField("Location").indexOf("error") > 0) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;
                    }
                    int length = requestInfo.getConnection().getContentLength();
                    downloadLink.setDownloadMax(length);
                    logger.info("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));
                    if (getFileNameFormHeader(requestInfo.getConnection()) == null || getFileNameFormHeader(requestInfo.getConnection()).indexOf("?") >= 0) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN_RETRY);
                        step.setParameter(20000l);
                        return step;
                    }
                    downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
                    if (!hasEnoughHDSpace(downloadLink)) {
                        downloadLink.setStatus(DownloadLink.STATUS_ERROR_NO_FREE_SPACE);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    if(download(downloadLink, (URLConnection) requestInfo.getConnection())!=DOWNLOAD_SUCCESS) {
                        step.setStatus(PluginStep.STATUS_ERROR);
                        
                    }
                    else {
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
    public boolean doBotCheck(File file) {
        return false;
    }
    @Override
    public void reset() {
        this.url = null;
    }
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadMax()) + ")";
    }
    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        RequestInfo requestInfo;
        try {
            String strId = getFirstMatch(downloadLink.getDownloadURL(), GETID, 1);
            if(strId==null)
                return true;
            int id=Integer.parseInt(strId);
            url=downloadLink.getDownloadURL().replaceFirst("filer.net\\/file[0-9]+\\/", "filer.net/folder/");
            url=url.replaceFirst("\\/filename\\/.*","");
            requestInfo = getRequest(new URL(url));
                // Datei geloescht?
                if (requestInfo.getHtmlCode().contains(FILE_NOT_FOUND2)) {
                    return false;
                }
                Vector<Vector<String>> matches = getAllSimpleMatches(requestInfo.getHtmlCode(), INFO);
                if(matches.size()<id)
                    return false;
                Vector<String> link = matches.get(id);
                downloadLink.setName(link.get(1));
                if (link != null) {
                    try {
                        int length = (int) (Double.parseDouble(link.get(2)) * 1024 * 1024);
                        downloadLink.setDownloadMax(length);
                    } catch (Exception e) {
                    }
                }
            
            return true;
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return false;
    }
    @Override
    public int getMaxSimultanDownloadNum() {
        return 1;
    }
    @Override
    public void resetPluginGlobals() {
        // TODO Auto-generated method stub
        
    }
    @Override
    public String getAGBLink() {
        // TODO Auto-generated method stub
        return "http://www.filer.net/faq";
    }
}
