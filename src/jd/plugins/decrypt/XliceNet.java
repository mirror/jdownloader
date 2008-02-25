package jd.plugins.decrypt;

import jd.plugins.DownloadLink;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDLocale;

public class XliceNet extends PluginForDecrypt {
    final static String host = "xlice.net";
    private static final String[] USEARRAY = new String[] { "Rapidshare.com",
            "Bluehost.to", "Datenklo.net", "Fastshare.org", "Files.to",
            "Share.Gulli.com", "Load.to", "Netload.in", "Sharebase.de",
            "Share-Online.biz", "Simpleupload.net", "Uploaded.to",
            "Uploadstube.de", "Fast-Load.net" };
    private String version = "2.1.0";
    // xlice.net/folder/13a3169edf4cfd3a438a40c2397724fe/
    // xlice.net/file/e46b767e51b8dbdf3afb6d3ea3852c4e/
    // xlice.net/file/ff139aafdf5c299c33b218b9750b3d17/%5BSanex%5D%20-
    private Pattern patternSupported = getSupportPattern("http://[*]xlice.net/(file|folder)/[a-zA-Z0-9]{32}[*]");
    public XliceNet() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
        currentStep = steps.firstElement();
        this.setConfigEelements();
    }
    @Override
    public String getCoder() {
        return "JD-Team";
    }
    @Override
    public String getHost() {
        return host;
    }
    @Override
    public String getPluginID() {
        return host + "-" + VERSION;
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
    private boolean getUseConfig(String link) {
        if(link==null)
            return false;
        link=link.toLowerCase();
        for (int i = 0; i < USEARRAY.length; i++) {
            if (link.matches(".*" + USEARRAY[i].toLowerCase() + ".*")) {
                return getProperties()
                .getBooleanProperty(USEARRAY[i], true);
            }
        }
        return false;
    }
    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url, null, null, true);
                String cookie = reqinfo.getCookie();
                String[] links = new Regexp(reqinfo.getHtmlCode(),
                        "<a href=\"(/file/go/.*?)\" target\\=\".blank\"><img src\\=\"/img/.*?_1.gif")
                        .getMatches(1);
    			progress.setRange(links.length);
                for (int i = 0; i < links.length; i++) {
                    reqinfo = getRequestWithoutHtmlCode(new URL("http://" + host + links[i]),
                            cookie, parameter, true);
                    String location = reqinfo.getConnection().getURL().toString();
    				progress.increase(1);
                    if(getUseConfig(location)) {
                    	if (location.contains("netload.in")) {
                    		location = location.replace(" ", "_");
                    	}
                        decryptedLinks.add(createDownloadlink(location));
                    }
                }
                logger.info(decryptedLinks.size() + " "
                		+ JDLocale.L("plugins.decrypt.general.downloadsDecrypted", "Downloads entschlüsselt"));
                
                step.setParameter(decryptedLinks);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    private void setConfigEelements() {
        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_LABEL,
                JDLocale.L("plugins.decrypt.general.hosterSelection", "Hoster Auswahl")));
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        for (int i = 0; i < USEARRAY.length; i++) {
            config.addEntry(cfg = new ConfigEntry(
                    ConfigContainer.TYPE_CHECKBOX, getProperties(),
                    USEARRAY[i], USEARRAY[i]));
            cfg.setDefaultValue(true);
        }
    }
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}