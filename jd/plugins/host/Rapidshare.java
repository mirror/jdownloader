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

public class Rapidshare extends PluginForHost{
    private String  host    = "rapidshare.com";
    private String  version = "1.0.0.0";
    private Pattern patternSupported = Pattern.compile("http://rapidshare\\.com/files[^\\s\"]*");
    
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
        currentStep = steps.firstElement();
    }
    @Override
    public URLConnection getURLConnection() {
        return null;
    }
    @Override
    public PluginStep getNextStep(Object parameter) {
        //
        // Nur ein Test
        //
        DownloadLink downloadLink = (DownloadLink)parameter;
        try {
            downloadLink.setFileOutput(new File("D:/test.pdf"));
            downloadLink.setUrlConnection(new URL("http://www.mediamarkt.de/multimedia-prospekt/mm_flyer_kw3207.pdf").openConnection());
        }
        catch (MalformedURLException e) { }
        catch (IOException e)           { }
        
        doDownload(downloadLink);
        
        currentStep.setParameter(new Long(40));
        return null;
    }
    private void doDownload(DownloadLink downloadLink){
        URLConnection urlConnection = downloadLink.getUrlConnection();
        int length = urlConnection.getContentLength();
        File fileOutput = downloadLink.getFileOutput();
        downloadLink.getProgressBar().setMaximum(length);
        download(downloadLink);
    } 
}
