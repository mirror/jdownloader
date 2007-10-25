package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.utils.JDUtilities;

public class oXygenWarez extends PluginForDecrypt {
    static private final String host = "oxygen-warez.com";
    private String version = "1.0.0.0";
    private Pattern patternSupported =Pattern.compile("http://oxygen-warez.com/(category|\\?id=).*",Pattern.CASE_INSENSITIVE);
    private Pattern CAPTCHAPATTERN = Pattern.compile("<TD><IMG SRC=\"(/gfx/secure/[^\"]+)\" ALT=\"\" BORDER=\"0\" WIDTH=\"90\" HEIGHT=\"30\">",Pattern.CASE_INSENSITIVE);
    private Pattern FORMLOCATION = Pattern.compile("(?s)FORM ACTION=\"(/\\?id=[0-9]+)\" ENCTYPE=\"multipart/form-data\"",
            Pattern.CASE_INSENSITIVE);
    //private Pattern PASSWORT = Pattern.compile("<P><B>Passwort:</B> <A HREF=\"\" onClick=\"CopyToClipboard\\(this\\); return\\(false\\);\">(.+?)</A></P>");
    private static final String DL_LINK = "<FORM ACTION=\"°\" METHOD=\"POST\" STYLE=\"display: inline;\" TARGET=\"_blank\">";
    private static final String ERROR_CAPTCHA = "Der Sichheitscode wurde falsch eingeben!";
    private static final String ERROR_CAPTCHA_TIME = "Der Sichheitscode ist abgelaufen!";
    public oXygenWarez() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
    }
    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override public String getCoder() { return "DwD"; }
    @Override public String getHost() { return host; }
    @Override public String getPluginID() { return "oXygenWarez-1.0.0."; }
    @Override public String getPluginName() { return host; }
    @Override public Pattern getSupportedLinks() { return patternSupported; }
    @Override public String getVersion() { return version; }
   


  
    @Override
    public boolean doBotCheck(File file) {        
        return false;
    }
    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT:
                Vector<String> decryptedLinks = new Vector<String>();
                RequestInfo reqinfo;
                try {
                    while (true) {
                    reqinfo = getRequest(new URL(parameter));

                    // Links herausfiltern
                    String captchaurl="http://oxygen-warez.com"+getFirstMatch(reqinfo.getHtmlCode(), CAPTCHAPATTERN, 1);
                    File file = this.getLocalCaptchaFile(this);
                    if (!JDUtilities.download(file, captchaurl) || !file.exists()) {
                        logger.severe("Captcha Download fehlgeschlagen: " + captchaurl);
                        step.setParameter(null);
                        step.setStatus(PluginStep.STATUS_ERROR);
                        return step;
                    }
                    String baseurl="http://oxygen-warez.com"+getFirstMatch(reqinfo.getHtmlCode(), FORMLOCATION, 1);
                    String plainCaptcha = getCaptchaCode(file, this);
                    String inpHidden = "code="+plainCaptcha +"&"+getFormInputHidden(reqinfo.getHtmlCode());
                    //logger.severe("Password:"+getFirstMatch(reqinfo.getHtmlCode(), PASSWORT, 1));
                    reqinfo = postRequest(new URL(baseurl), inpHidden);
                    if (reqinfo.getHtmlCode().contains(ERROR_CAPTCHA)) {
                        if(file!=null && plainCaptcha != null){
                            JDUtilities.appendInfoToFilename(file,plainCaptcha, false);
                        }
                        logger.severe("falscher Captcha-Code");
                        continue; // retry

                    } else if (reqinfo.getHtmlCode().contains(ERROR_CAPTCHA_TIME)) {
                        logger.severe("Captcha-Code abgelaufen");
                        continue; // retry
                    }
                    
                    // Captcha erkannt
                    if(file!=null && plainCaptcha != null){
                        JDUtilities.appendInfoToFilename(file,plainCaptcha, true);
                    }
                    break;
                    }
                    
                    Vector<Vector<String>> links = getAllSimpleMatches(reqinfo.getHtmlCode(), DL_LINK);
                    firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_MAX, links.size()));
                    
                    for (int i=0; i<links.size(); i++) {
                        String link = JDUtilities.urlEncode(links.get(i).get(0));
                        link = link.replaceAll("http://.*http://", "http://");
                        decryptedLinks.add(link);

                        firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                    }
                    

                //Decrypt abschliessen
                firePluginEvent(new PluginEvent(this,PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
                
                } catch (IOException e) {
                    e.printStackTrace();
                }
            
            return null;

        }
        return null;
    }
}
