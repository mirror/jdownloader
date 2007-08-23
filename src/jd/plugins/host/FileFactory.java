package jd.plugins.host;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class FileFactory extends PluginForHost{
    /**
     * TODO hier muss noch getestet werden und ich ueberarbeite das auch noch aber jetzt bin ich einfach sehr muede
     * ich hab wirklich keine Ahung inwieweit das hier schon funktioniert
     * hab hier noch keinen test machen koennen 
     */
    private String  host    = "filefactory.com";
    private String  version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://www.filefactory.com/file/[^\\s\"]+/?");
    /**
     * Das findet die Ziel URL
     */
    private Pattern patternForNewHost = Pattern.compile("var link = '([^']*?' \\+ '[^']*?' \\+ '[^']*?' \\+ '[0-9]*?)';");
    /**
     * Das findet die Captcha URL
     * 
     */ 
    private Pattern frameForCaptcha = Pattern.compile("<iframe src=\"/(check[^\"]*)\" frameborder=\"0\"");
    private Pattern patternForCaptcha = Pattern.compile("src=\"(/captcha2/captcha.php\\?[^\"]*)\" alt=");
    /**
     *
     * <a target="_top" href="http://archive01.filefactory.com/dl/f/cd66d1//b/3/h/4bb297a8a6f12168/"><img src
     */
    private Pattern patternForDownloadlink = Pattern.compile("<a target=\"_top\" href=\"([^\"]*)\"><img src");
    //TODO
    private Pattern patternErrorCaptchaWrong         = Pattern.compile("(Sorry, the verification code you entered was incorrect)", Pattern.CASE_INSENSITIVE);

    /**
     * Pattern fue den Dateinamen
     */
    private Pattern filen = Pattern.compile("filename=['\"](.*?)['\"]", Pattern.CASE_INSENSITIVE);

    private int waitTime          = 500;
    private String captchaAddress;
    private String postTarget;
    private String actionString;
    private RequestInfo requestInfo;

    @Override public String getCoder()            { return "DwD aka James";               }
    @Override public String getHost()             { return host;                    }
    @Override public String getPluginName()       { return host;                    }
    @Override public Pattern getSupportedLinks()  { return patternSupported;        }
    @Override public String getVersion()          { return version;                 }
    @Override public boolean isClipboardEnabled() { return true;                    }
    @Override public String getPluginID()         { return "FILEFACTORY.COM-1.0.0."; }
    @Override
    public void init() {
        currentStep = null;
    }
    
    public FileFactory(){
        super();
        steps.add(new PluginStep(PluginStep.STEP_WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.STEP_CAPTCHA,  null));
        steps.add(new PluginStep(PluginStep.STEP_DOWNLOAD, null));
    }
    @Override
    public URLConnection getURLConnection() {
        return null;
    }
    @Override
    public PluginStep getNextStep(Object parameter) {
        DownloadLink downloadLink = (DownloadLink)parameter;
        
        PluginStep todo=null;
        if(currentStep == null){
            try {
                //Der Download wird bestätigt
                requestInfo = getRequest(downloadLink.getUrlDownload());
                String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);

                if(newURL != null){
                    newURL = "http://www.filefactory.com"+newURL.replaceAll("' \\+ '", "");
                    requestInfo = getRequest((new URL(newURL)),null,downloadLink.getName(), true );
                    
                    actionString = "http://www.filefactory.com/"+getFirstMatch(requestInfo.getHtmlCode(), frameForCaptcha, 1);

                    actionString = actionString.replaceAll("&amp;", "&");
                    requestInfo = getRequest((new URL(actionString)),"viewad11=yes",newURL, true );
                    // captcha Adresse finden
                    captchaAddress = "http://www.filefactory.com"+getFirstMatch(requestInfo.getHtmlCode(),patternForCaptcha,1);
                    captchaAddress = captchaAddress.replaceAll("&amp;", "&");
                    //post daten lesen
                    postTarget = getFormInputHidden(requestInfo.getHtmlCode());
                     
                }
                currentStep  = steps.firstElement();
                if(captchaAddress == null || postTarget == null ){


                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    logger.warning("could not get downloadInfo");
                    return currentStep;
                }
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }

        }
        int index = steps.indexOf(currentStep);
        todo = currentStep;
        if(index+1 < steps.size())
            currentStep = steps.get(index+1);
        logger.finer(todo.toString());
        switch(todo.getStep()){
            case PluginStep.STEP_WAIT_TIME:
                todo.setParameter(new Long(waitTime));
                break;
            case PluginStep.STEP_CAPTCHA:
                todo.setParameter(captchaAddress);
                todo.setStatus(PluginStep.STATUS_USER_INPUT);
                break;
            case PluginStep.STEP_DOWNLOAD:
                try {
                    requestInfo = postRequest((new URL(actionString)),requestInfo.getCookie(),actionString,postTarget+"&captcha="+(String)steps.get(1).getParameter(),true);
                    postTarget=getFirstMatch(requestInfo.getHtmlCode(), patternForDownloadlink, 1);
                    postTarget = postTarget.replaceAll("&amp;", "&");
                    
                } catch (MalformedURLException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
                
                
                boolean success = prepareDownload(downloadLink);
                if(success){
                    todo.setStatus(PluginStep.STATUS_DONE);
                    downloadLink.setStatus(DownloadLink.STATUS_DONE);
                    return null;
                }
                else{
                    logger.severe("captcha wrong");
                    downloadLink.setStatus(DownloadLink.STATUS_ERROR_CAPTCHA_WRONG);
                    todo.setStatus(PluginStep.STATUS_ERROR);
                }
                break;
        }
        return todo;
    }
    private boolean prepareDownload(DownloadLink downloadLink){
        try {
            URLConnection urlConnection = new URL(postTarget).openConnection();
            String filename = getFirstMatch(urlConnection.getHeaderField("content-disposition"), filen, 1);
            int length = urlConnection.getContentLength();
            downloadLink.setDownloadLength(length);
            downloadLink.setName(filename);
            return download(downloadLink, urlConnection);
        }
        catch (IOException e) { logger.severe("URL could not be opened. "+e.toString());}
        return false;
    }
}
