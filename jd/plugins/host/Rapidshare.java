package jd.plugins.host;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;

public class Rapidshare extends PluginForHost{
    private String  host    = "rapidshare.com";
    private String  version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://rapidshare\\.com/files/[0-9]*/[^\\s\"]+");
    /**
     * Das findet die Ziel URL für den Post
     */
    private Pattern patternForNewHost = Pattern.compile("<form *action *= *\"([^\\n\"]*)\"");
    /**
     * Das findet die Captcha URL
     * <form *name *= *"dl" (?s).*<img *src *= *"([^\n"]*)">
     */
    private Pattern patternForCaptcha = Pattern.compile("<form *name *= *\"dl\" (?s).*<img *src *= *\"([^\\n\"]*)\">");
    /**
     * 
     * <form name="dl".* action="([^\n"]*)"(?s).*?<input type="submit" name="actionstring" value="[^\n"]*"
     */
    private Pattern patternForFormData = Pattern.compile("<form name=\"dl\".* action=\"([^\\n\"]*)\"(?s).*?<input type=\"submit\" name=\"actionstring\" value=\"([^\\n\"]*)\"");
    
    private int waitTime          = 500;
    private String captchaAddress;
    private String postTarget;
    private String actionString;
    private HashMap<String, String> postParameter = new HashMap<String, String>(); 

    @Override public String getCoder()            { return "astaldo";        }
    @Override public String getHost()             { return host;             }
    @Override public String getPluginName()       { return host;             }
    @Override public Pattern getSupportedLinks()  { return patternSupported; }
    @Override public String getVersion()          { return version;          }
    @Override public boolean isClipboardEnabled() { return true;             }
    
    public Rapidshare(){
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
        RequestInfo requestInfo;
        PluginStep toDo=null;
        if(currentStep == null){
            try {
                //Der Download wird bestätigt
                requestInfo = getRequest(downloadLink.getUrlDownload());
                String newURL = getFirstMatch(requestInfo.getHtmlCode(), patternForNewHost, 1);
                
                //Auswahl ob free oder prem
                requestInfo = postRequest(new URL(newURL),"dl.start=free");
                
                // captcha Adresse finden
                captchaAddress = getFirstMatch(requestInfo.getHtmlCode(),patternForCaptcha,1);
                
                //post daten lesen
                postTarget   = getFirstMatch(requestInfo.getHtmlCode(), patternForFormData, 1);
                actionString = getFirstMatch(requestInfo.getHtmlCode(), patternForFormData, 2);
                currentStep  = steps.firstElement();
                if(captchaAddress == null || postTarget == null || actionString == null){
                    currentStep.setStatus(PluginStep.STATUS_ERROR);
                    return currentStep;
                }
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }
        int index = steps.indexOf(currentStep);
        toDo = currentStep;
        if(index+1 < steps.size())
            currentStep = steps.elementAt(index+1);
        switch(toDo.getStep()){
            case PluginStep.STEP_WAIT_TIME:
                toDo.setParameter(new Long(waitTime));
                break;
            case PluginStep.STEP_CAPTCHA:
                toDo.setParameter(captchaAddress);
                toDo.setStatus(PluginStep.STATUS_INPUT_FROM_USER);
                break;
            case PluginStep.STEP_DOWNLOAD:
                postParameter.put("mirror",      "on");
                postParameter.put("accesscode",  (String)steps.elementAt(1).getParameter());
                postParameter.put("actionString",actionString);
                
                prepareDownload(downloadLink);
                break;
        }
        return toDo;
    }
    private void prepareDownload(DownloadLink downloadLink){
        try {
            URLConnection urlConnection = new URL(postTarget).openConnection();
            urlConnection.setDoOutput(true);
            
            //Post Parameter vorbereiten
            String postParams = createPostParameterFromHashMap(postParameter);
            OutputStreamWriter wr = new OutputStreamWriter(urlConnection.getOutputStream());
            wr.write(postParams);
            wr.flush();
            
            int length = urlConnection.getContentLength();
            File fileOutput = downloadLink.getFileOutput();
            downloadLink.setDownloadLength(length);
            
            download(downloadLink, urlConnection);
        }
        catch (IOException e) { logger.severe("URL could not be opened. "+e.toString());}
    } 
}
