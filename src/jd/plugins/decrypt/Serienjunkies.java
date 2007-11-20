package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.utils.JDUtilities;

public class Serienjunkies extends PluginForDecrypt {
    static private final String host             = "serienjunkies.org";
    private static final String DEFAULT_PASSWORD = "serienjunkies.dl.am";
    private String              version          = "2.0.0.0";
    // http://85.17.177.195/sjsafe/f-e657c0c256dd9e58/rc_h324.html
    //http://serienjunki.es/sjsafe/f-3221260b2fc369e2/rc_scrubs610.html
    private Pattern             patternSupported = getSupportPattern("http://(serienjunkies.org|85.17.177.195|serienjunki\\.es)/s[+]");
//    private Pattern             patternCaptcha   = Pattern.compile("e/secure/");
    private Pattern             patternCaptcha   = null;

    private String dynamicCaptcha = "<FORM ACTION=\".*?%s\" METHOD=\"post\"(?s).*?(?-s)<INPUT TYPE=\"HIDDEN\" NAME=\"s\" VALUE=\"([\\w]*)\">(?s).*?(?-s)<IMG SRC=\"([^\"]*)\"";    
    public Serienjunkies() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigElements();
       
    }
    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override
    public String getCoder() {
        //von  coa gefixed
        return "DwD aka James / Botzi";
    }
    @Override
    public String getHost() {
        return host;
    }
    @Override
    public String getPluginID() {
        return "Serienjunkies-2.0.1.";
    }
    @Override
    public String getPluginName() {
        return "SerienJunkies.dl.am";
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
    public boolean doBotCheck(File file) {
        return false;
    }
    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        switch (step.getStep()) {
            case PluginStep.STEP_DECRYPT:
                Vector<String[]> decryptedLinks = new Vector<String[]>();
                try {
                    URL url = new URL(parameter);
                    String modifiedURL = url.toString();
                    modifiedURL = modifiedURL.replaceAll("safe/rc", "safe/frc");
                    modifiedURL = modifiedURL.replaceAll("save/rc", "save/frc");
                    modifiedURL = modifiedURL.substring(modifiedURL.lastIndexOf("/"));

                    patternCaptcha = Pattern.compile(String.format(dynamicCaptcha, new Object[]{modifiedURL}));
                    logger.fine("using patternCaptcha:"+patternCaptcha);
                    RequestInfo reqinfo = getRequest(url,null,null,true);
                    
                    String furl=getSimpleMatch(reqinfo.getHtmlCode(), "<FRAME SRC=\"°"+modifiedURL+"\"", 0);
                    if(furl!=null){
                        url=new URL(furl+modifiedURL);
                        logger.info("Frame found. frame url: "+furl+modifiedURL);
                        reqinfo = getRequest(url,null,null,true);
                        parameter=furl+modifiedURL;
                        
                    }
               
                    logger.info(reqinfo.getHtmlCode());
                   
                   
                    Vector<Vector<String>> links;
                    links = getAllSimpleMatches(reqinfo.getHtmlCode(), " <a href=\"http://°\"");
                    Vector<String> helpvector = new Vector<String>();
                    String helpstring = "";
                    // Einzellink
                    if (parameter.indexOf("/safe/") >= 0 || parameter.indexOf("/save/") >= 0) {
                        logger.info("safe link");
                        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, 1));
                        helpstring = EinzelLinks(parameter);
                        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                        if (check(helpstring)) decryptedLinks.add(new String[]{helpstring,DEFAULT_PASSWORD,null});
                    }
                    else if (parameter.indexOf("/sjsafe/") >= 0) {
                        logger.info("sjsafe link");
                        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, 1));
                        helpvector = ContainerLinks(parameter);
                        
                        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                        for (int j = 0; j < helpvector.size(); j++) {
                            if (check(helpvector.get(j))) decryptedLinks.add(new String[]{helpvector.get(j),DEFAULT_PASSWORD,null});
                        }
                    }
                    else {
                        logger.info("else link");
                        firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, links.size()));
                        // Kategorien
                        for (int i = 0; i < links.size(); i++) {
                            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                            if (links.get(i).get(0).indexOf("/safe/") >= 0) {
                                helpstring = EinzelLinks(links.get(i).get(0));
                                if (check(helpstring)) decryptedLinks.add(new String[]{helpstring,DEFAULT_PASSWORD,null});
                            }
                            else if (links.get(i).get(0).indexOf("/sjsafe/") >= 0) {
                                helpvector = ContainerLinks(links.get(i).get(0));
                                for (int j = 0; j < helpvector.size(); j++) {
                                    if (check(helpvector.get(j))) decryptedLinks.add(new String[]{helpvector.get(j),DEFAULT_PASSWORD,null});
                                }
                            }
                            else {
                                decryptedLinks.add(new String[]{links.get(i).get(0),DEFAULT_PASSWORD,null});
                                if (check(links.get(i).get(0))) decryptedLinks.add(new String[]{links.get(i).get(0),DEFAULT_PASSWORD,null});
                            }
                        }
                    }
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                // Decrypt abschliessen
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
        }
        return null;
    }
  
    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Default Passwort"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), "DEFAULT_PASSWORT", "Passwort").setDefaultValue(DEFAULT_PASSWORD));
        
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHAREDE", "Rapidshare.de"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
    }
    // Für Links die bei denen die Parts angezeigt werden
    private Vector<String> ContainerLinks(String url) {
        Vector<String> links = new Vector<String>();
        boolean fileDownloaded = false;
        if (!url.startsWith("http://")) url = "http://" + url;
        try {
            RequestInfo reqinfo = getRequest(new URL(url));
            String cookie = reqinfo.getCookie();
            File captchaFile = null;
            String capTxt = null;
            while(true) { // for() läuft bis kein Captcha mehr abgefragt
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    if(captchaFile!=null && capTxt != null){
                        JDUtilities.appendInfoToFilename(captchaFile,capTxt, false);
                    }
                    Vector<Vector<String>> gifs = getAllSimpleMatches(reqinfo.getHtmlCode(), patternCaptcha);
                    String captchaAdress = "http://85.17.177.195" + gifs.firstElement().get(1);
//                    for (int i = 0; i < gifs.size(); i++) {
//                        if (gifs.get(i).get(0).indexOf("secure") >= 0 && JDUtilities.filterInt(gifs.get(i).get(2)) > 0 && JDUtilities.filterInt(gifs.get(i).get(3)) > 0) {
//                            captchaAdress = "http://85.17.177.195" + gifs.get(i).get(0);
//                            logger.info(gifs.get(i).get(0));
//                        }
//                    }
                    captchaFile = getLocalCaptchaFile(this,".gif");
                    fileDownloaded = JDUtilities.download(captchaFile, getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection());
                    if(!fileDownloaded || !captchaFile.exists() || captchaFile.length()==0){
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");
                        try {
                            Thread.sleep(1000);
                            reqinfo = getRequest(new URL(url));
                            cookie = reqinfo.getCookie();
                        }
                        catch (InterruptedException e) { }
                        continue;
                    }
                        
                    capTxt = Plugin.getCaptchaCode(captchaFile, this);
                    reqinfo = postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&action=Download");
                   
                }
                else {
                    if(captchaFile!=null && capTxt != null){
                        JDUtilities.appendInfoToFilename(captchaFile,capTxt, true);
                    }
                    break;
                }
            }
            if(reqinfo.getLocation()!=null){
                links.add(reqinfo.getLocation());
            }
            Vector<Vector<String>> links1 = getAllSimpleMatches(reqinfo.getHtmlCode(), "FORM ACTION=\"°\"");
            for (int i = 0; i < links1.size(); i++) {
                reqinfo = getRequest(new URL(links1.get(i).get(0)));
                reqinfo = getRequest(new URL(getBetween(reqinfo.getHtmlCode(), "SRC=\"", "\"")));
                links.add(reqinfo.getLocation());
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }
    // Für Links die gleich auf den Hoster relocaten
    private String EinzelLinks(String url) {
        String links = "";
        boolean fileDownloaded=false;
        if (!url.startsWith("http://")) url = "http://" + url;
        try {
            url = url.replaceAll("safe/rc", "safe/frc");
            url = url.replaceAll("save/rc", "save/frc");
            RequestInfo reqinfo = getRequest(new URL(url));
            String cookie = reqinfo.getCookie();
            File captchaFile = null;
            String capTxt = null;
            while(true) { // for() läuft bis kein Captcha mehr abgefragt
                if(captchaFile!=null && capTxt != null){
                    JDUtilities.appendInfoToFilename(captchaFile,capTxt, false);
                }
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    String captchaAdress = "http://85.17.177.195" + matcher.group(2);
                    captchaFile = getLocalCaptchaFile(this,".gif");
                    fileDownloaded = JDUtilities.download(captchaFile, getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection());
                    if(!fileDownloaded || !captchaFile.exists() || captchaFile.length()==0){
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");
                        try {
                            Thread.sleep(1000);
                            reqinfo = getRequest(new URL(url));
                            cookie = reqinfo.getCookie();
                        }
                        catch (InterruptedException e) { }
                        continue;
                    }
                    capTxt = Plugin.getCaptchaCode(captchaFile, this);
                    reqinfo = postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&dl.start=Download");
                }
                else {
                    break;
                }
            }
            if(captchaFile!=null && capTxt != null){
                JDUtilities.appendInfoToFilename(captchaFile,capTxt, true);
            }
            links = reqinfo.getLocation();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }
    /**
     * Überprüft ob der übergebene Link in der Konfig ausgewählt wurde
     * 
     * @param link
     * @return
     */
    private Boolean check(String link) {
        if (link == null) return false;
        if ((Boolean) this.getProperties().getProperty("USE_NETLOAD", true) && link.indexOf("netload.in") >= 0) return true;
        if ((Boolean) this.getProperties().getProperty("USE_RAPIDSHARE", true) && link.indexOf("rapidshare.com") >= 0) return true;
        System.out.println(link);
        if ((Boolean) this.getProperties().getProperty("USE_RAPIDSHAREDE", true) && link.indexOf("rapidshare.de") >= 0) return true;
        return false;
    }
}
