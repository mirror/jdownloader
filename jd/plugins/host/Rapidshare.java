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
import jd.plugins.RequestInfo;

public class Rapidshare extends PluginForHost{
    private String  host    = "rapidshare.com";
    private String  version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://rapidshare\\.com/files[^\\s\"]*");
    /**
     * Das findet die ZielURL für den Post
     */
    private Pattern patternForNewHost = Pattern.compile("<form *name *= *\"dl\" *action *= *\"([^\\n\"]*)\"");
    /**
     * Das findet die Captcha URL
     * <form *name *= *"dl" (?s).*<img *src *= *"([^\n"]*)">
     */
    private Pattern patternForCaptcha = Pattern.compile("<form *name *= *\"dl\" (?s).*<img *src *= *\"([^\\n\"]*)\">");
    
    private int waitTime = 5000;
    private String captchaAddress=null;
    
    @Override public String getCoder()            { return "astaldo";        }
    @Override public String getHost()             { return host;             }
    @Override public String getPluginName()       { return host;             }
    @Override public Pattern getSupportedLinks()  { return patternSupported; }
    @Override public String getVersion()          { return version;          }
    @Override public boolean isClipboardEnabled() { return true;             }
    public Rapidshare(){
        super();
        steps.add(new PluginStep(PluginStep.WAIT_TIME, null));
        steps.add(new PluginStep(PluginStep.CAPTCHA,  null));
        steps.add(new PluginStep(PluginStep.DOWNLOAD, null));
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
                String captchaAdress = getFirstMatch(requestInfo.getHtmlCode(),patternForCaptcha,1);
                System.out.println(captchaAdress);
                currentStep = steps.firstElement();
            }
            catch (MalformedURLException e) { e.printStackTrace(); }
            catch (IOException e)           { e.printStackTrace(); }
        }
        int index = steps.indexOf(currentStep);
        toDo = currentStep;
        currentStep = steps.elementAt(index+1);
        switch(currentStep.getStep()){
            case PluginStep.WAIT_TIME:
                toDo.setParameter(new Long(waitTime));
            case PluginStep.CAPTCHA:
                toDo.setParameter(captchaAddress);
                
        }
        return toDo;
    }
    private void doDownload(DownloadLink downloadLink){
        try {
            URLConnection urlConnection = downloadLink.getUrlDownload().openConnection();
            int length = urlConnection.getContentLength();
            File fileOutput = downloadLink.getFileOutput();
            downloadLink.getProgressBar().setMaximum(length);
            download(downloadLink);
        }
        catch (IOException e) { logger.severe("URL could not be opened. "+e.toString());}
    } 
}
