package jd.plugins.decrypt;  import jd.plugins.DownloadLink;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Serienjunkies extends PluginForDecrypt {
    private static final String host = "Serienjunkies.org";

    private String version = "4.2.0.0";

    private Pattern patternCaptcha = null;

    private boolean next = false;

    private String dynamicCaptcha = "<FORM ACTION=\".*?%s\" METHOD=\"post\"(?s).*?(?-s)<INPUT TYPE=\"HIDDEN\" NAME=\"s\" VALUE=\"([\\w]*)\">(?s).*?(?-s)<IMG SRC=\"([^\"]*)\"";

    public Serienjunkies() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigElements();
        
        default_password.add("serienjunkies.dl.am");
        default_password.add("serienjunkies.org");

    }

    /*
     * Diese wichtigen Infos sollte man sich unbedingt durchlesen
     */
    @Override
    public String getCoder() {
        // von coa gefixed
        return "DwD | Botzi";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    private String isNext() {
        if (next)
            return "|";
        else
            next = true;
        return "";

    }

    @Override
    public Pattern getSupportedLinks() {
        boolean rscom = (Boolean) this.getProperties().getProperty("USE_RAPIDSHARE", true);
        boolean rsde = (Boolean) this.getProperties().getProperty("USE_RAPIDSHAREDE", false);
        boolean net = (Boolean) this.getProperties().getProperty("USE_NETLOAD", false);
        boolean uploaded = (Boolean) this.getProperties().getProperty("USE_UPLOADED", false);
        boolean cat = (Boolean) this.getProperties().getProperty("USE_CAT", true);
        next = false;
        String hosterStr = "";
        if (rscom || rsde || net || uploaded) {
            hosterStr += "(";
            if (rscom)
                hosterStr += isNext() + "rc[\\_\\-]";
            if (rsde)
                hosterStr += isNext() + "rs[\\_\\-]";
            if (net)
                hosterStr += isNext() + "nl[\\_\\-]";
            if (uploaded)
                hosterStr += isNext() + "ut[\\_\\-]";
            if (cat)
                hosterStr += isNext() + "cat\\=[\\d]+";
            hosterStr += ")";
        } else {
            hosterStr += "not";
        }
        return Pattern.compile("http://(download\\.serienjunkies\\.org|"+(cat? "serienjunkies.org|":"")+"serienjunkies\\.org/s|85\\.17\\.177\\.195/s|serienjunki\\.es/s).*" + hosterStr + ".*", Pattern.CASE_INSENSITIVE);
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
            case PluginStep.STEP_DECRYPT :
           
                Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
                try {
                    URL url = new URL(parameter);
                    if(parameter.matches(".*\\?cat\\=[\\d]+"))
                    {
                        RequestInfo reqinfo = getRequest(new URL("http://serienjunkies.org/"));
                        if(reqinfo.getLocation().matches(".*enter.*"))
                            reqinfo = getRequest(new URL("http://serienjunkies.org/"));
                        int cat = Integer.parseInt(parameter.replaceFirst(".*cat\\=", "").replaceFirst("[^\\d].*", ""));
                        Pattern pattern = Pattern.compile("<a href=\"http://serienjunkies.org/\\?cat\\=([\\d]+)\">(.*?)</a><br", Pattern.CASE_INSENSITIVE);
                        Matcher matcher = pattern.matcher(reqinfo.getHtmlCode());
                        String name = null;
                        while (matcher.find()) {
                            if(Integer.parseInt(matcher.group(1))==cat)
                            {
                                name=matcher.group(2).toLowerCase();
                                break;
                            }
                        }
                        if(name==null)
                            return null;
                        reqinfo = getRequest(url, null, null, true);
                        pattern = Pattern.compile("(?s)<p><strong>"+name+"</strong>(.*?)</p>", Pattern.CASE_INSENSITIVE);
                        String bet = getFirstMatch(reqinfo.getHtmlCode(), pattern, 1);
                        pattern = Pattern.compile("<strong>.*?\\:</strong> <a href=\"(.*?)\"", Pattern.CASE_INSENSITIVE);
                        matcher = pattern.matcher(bet);
                        while (matcher.find()) {
                            decryptedLinks.add(this.createDownloadlink(matcher.group(1)));
                            
                        }
                        step.setParameter(decryptedLinks);
                        return null;
                    }
                    String modifiedURL = url.toString();
                    modifiedURL = modifiedURL.replaceAll("safe/rc", "safe/frc");
                    modifiedURL = modifiedURL.replaceAll("save/rc", "save/frc");
                    modifiedURL = modifiedURL.substring(modifiedURL.lastIndexOf("/"));

                    patternCaptcha = Pattern.compile(String.format(dynamicCaptcha, new Object[]{modifiedURL}));
                    logger.fine("using patternCaptcha:" + patternCaptcha);
                    RequestInfo reqinfo = getRequest(url, null, null, true);
                    if(reqinfo.getLocation().matches(".*enter.*"))
                        reqinfo = getRequest(url, null, null, true);
                    String furl = getSimpleMatch(reqinfo.getHtmlCode(), "<FRAME SRC=\"°" + modifiedURL + "\"", 0);
                    if (furl != null) {
                        url = new URL(furl + modifiedURL);
                        logger.info("Frame found. frame url: " + furl + modifiedURL);
                        reqinfo = getRequest(url, null, null, true);
                        parameter = furl + modifiedURL;

                    }

                    // logger.info(reqinfo.getHtmlCode());

                    Vector<Vector<String>> links;
                    links = getAllSimpleMatches(reqinfo.getHtmlCode(), " <a href=\"http://°\"");
                    Vector<String> helpvector = new Vector<String>();
                    String helpstring = "";
                    // Einzellink

                    if (parameter.indexOf("/safe/") >= 0 || parameter.indexOf("/save/") >= 0) {
                        logger.info("safe link");
                        progress.setRange(1);
                        helpstring = EinzelLinks(parameter);
                        progress.increase(1);
                        decryptedLinks.add(this.createDownloadlink(helpstring));
                    } else if (parameter.indexOf("download.serienjunkies.org") >= 0) {
                        logger.info("sjsafe link");
                        progress.setRange(1);
                        helpvector = ContainerLinks(parameter);
                        progress.increase(1);
                        for (int j = 0; j < helpvector.size(); j++) {
                            decryptedLinks.add(this.createDownloadlink(helpvector.get(j)));
                        }
                    } else if (parameter.indexOf("/sjsafe/") >= 0) {
                        logger.info("sjsafe link");
                        progress.setRange(1);
                        helpvector = ContainerLinks(parameter);

                        progress.increase(1);
                        for (int j = 0; j < helpvector.size(); j++) {
                            decryptedLinks.add(this.createDownloadlink(helpvector.get(j)));
                        }
                    } else {
                        logger.info("else link");
                        progress.setRange(links.size());
                        // Kategorien
                        for (int i = 0; i < links.size(); i++) {
                            progress.increase(1);
                            if (links.get(i).get(0).indexOf("/safe/") >= 0) {
                                helpstring = EinzelLinks(links.get(i).get(0));
                                decryptedLinks.add(this.createDownloadlink(helpstring));
                            } else if (links.get(i).get(0).indexOf("/sjsafe/") >= 0) {
                                helpvector = ContainerLinks(links.get(i).get(0));
                                for (int j = 0; j < helpvector.size(); j++) {
                                    decryptedLinks.add(this.createDownloadlink(helpvector.get(j)));
                                }
                            } else {
                                decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
                                decryptedLinks.add(this.createDownloadlink(links.get(i).get(0)));
                            }
                        }
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // veraltet: firePluginEvent(new PluginEvent(this,
                // PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
        }
        return null;
    }

    private void setConfigElements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_CAT", "Bei Kategorien den neusten Download verwenden"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL, "Hoster Auswahl"));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPERATOR));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHARE", "Rapidshare.com"));
        cfg.setDefaultValue(true);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_RAPIDSHAREDE", "Rapidshare.de"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_NETLOAD", "Netload.in"));
        cfg.setDefaultValue(false);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_UPLOADED", "Uploaded.to"));
        cfg.setDefaultValue(false);

    }

    // Für Links die bei denen die Parts angezeigt werden
    private Vector<String> ContainerLinks(String url) {

        Vector<String> links = new Vector<String>();
        boolean fileDownloaded = false;
        if (!url.startsWith("http://"))
            url = "http://" + url;
        try {
            RequestInfo reqinfo = getRequest(new URL(url));
            String cookie = reqinfo.getCookie();
            File captchaFile = null;
            String capTxt = null;
            while (true) { // for() läuft bis kein Captcha mehr abgefragt
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(captchaFile, capTxt, false);
                    }
                    Vector<Vector<String>> gifs = getAllSimpleMatches(reqinfo.getHtmlCode(), patternCaptcha);
                    String captchaAdress = "http://download.serienjunkies.org" + gifs.firstElement().get(1);
                    // for (int i = 0; i < gifs.size(); i++) {
                    // if (gifs.get(i).get(0).indexOf("secure") >= 0 &&
                    // JDUtilities.filterInt(gifs.get(i).get(2)) > 0 &&
                    // JDUtilities.filterInt(gifs.get(i).get(3)) > 0) {
                    // captchaAdress = "http://85.17.177.195" +
                    // gifs.get(i).get(0);
                    // logger.info(gifs.get(i).get(0));
                    // }
                    // }
                    captchaFile = getLocalCaptchaFile(this, ".gif");
                    fileDownloaded = JDUtilities.download(captchaFile, getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection());
                    if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");
                        try {
                            Thread.sleep(1000);
                            reqinfo = getRequest(new URL(url));
                            cookie = reqinfo.getCookie();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                    capTxt = Plugin.getCaptchaCode(captchaFile, this);
                    reqinfo = postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&action=Download");

                } else {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(captchaFile, capTxt, true);
                    }
                    break;
                }
            }
            if (reqinfo.getLocation() != null) {
                links.add(reqinfo.getLocation());
            }
            Pattern pattern = Pattern.compile("FORM ACTION=\"(.*?)\"[^>]*?>[^>]*?VALUE=\"Download\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(reqinfo.getHtmlCode());
            while (matcher.find()) {
                reqinfo = getRequest(new URL(matcher.group(1)));
                reqinfo = getRequest(new URL(getBetween(reqinfo.getHtmlCode(), "SRC=\"", "\"")));
                String loc = reqinfo.getLocation();
                if(loc!=null)
                links.add(loc);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

    // Für Links die gleich auf den Hoster relocaten
    private String EinzelLinks(String url) {
        String links = "";
        boolean fileDownloaded = false;
        if (!url.startsWith("http://"))
            url = "http://" + url;
        try {
            url = url.replaceAll("safe/rc", "safe/frc");
            url = url.replaceAll("save/rc", "save/frc");
            RequestInfo reqinfo = getRequest(new URL(url));
            String cookie = reqinfo.getCookie();
            File captchaFile = null;
            String capTxt = null;
            while (true) { // for() läuft bis kein Captcha mehr abgefragt
                Matcher matcher = patternCaptcha.matcher(reqinfo.getHtmlCode());
                if (matcher.find()) {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(captchaFile, capTxt, false);
                    }
                    String captchaAdress = "http://serienjunki.es" + matcher.group(2);
                    captchaFile = getLocalCaptchaFile(this, ".gif");
                    fileDownloaded = JDUtilities.download(captchaFile, getRequestWithoutHtmlCode(new URL(captchaAdress), cookie, null, true).getConnection());
                    if (!fileDownloaded || !captchaFile.exists() || captchaFile.length() == 0) {
                        logger.severe("Captcha nicht heruntergeladen, warte und versuche es erneut");
                        try {
                            Thread.sleep(1000);
                            reqinfo = getRequest(new URL(url));
                            cookie = reqinfo.getCookie();
                        } catch (InterruptedException e) {
                        }
                        continue;
                    }
                    capTxt = JDUtilities.getCaptcha(this,"einzellinks.Serienjunkies.org", captchaFile,false); 
                    reqinfo = postRequest(new URL(url), "s=" + matcher.group(1) + "&c=" + capTxt + "&dl.start=Download");
                } else {
                    if (captchaFile != null && capTxt != null) {
                        JDUtilities.appendInfoToFilename(captchaFile, capTxt, true);
                    }
                    break;
                }
            }

            links = reqinfo.getLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return links;
    }

}
