package jd.plugins.search;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForSearch;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
/**
 * 
 * 
 * 
 * @author DwD
 * 
 */
public class FileSearch extends PluginForSearch {
    static private final String host = "allhosts";

    private String version = "1.0.0.1";
    private static String[] CATEGORIES = new String[]{"Alles (FileSearchplugin)"};
    public static final String PARAM_INST = "INST";
    private String MOZILLACOOKIE;
    private boolean useMozillaCookie = false;
    private Vector<String> passwords;
    public FileSearch() {
        super();

        this.setConfigElements();
        steps.add(new PluginStep(PluginStep.STEP_SEARCH, null));

    }

    @Override
    public String getCoder() {
        return "DwD";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        String ret = "";
        for (int i = 0; i < CATEGORIES.length; i++)
            ret += "|" + CATEGORIES[i];
        return Pattern.compile("[" + ret.substring(1) + "]:::*");
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return "filesearch " + version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    private Vector<String> getLinklist(String link, String cookie, URL referrer, int inst) {
        Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
        try {
            URL url = new URL(link);
            String ref = null;
            if (referrer != null) {
                ref = "http://" + referrer.getHost() + referrer.getFile();
                if (!url.getHost().matches(referrer.getHost()))
                    cookie = null;
            }
            if (cookie == null && useMozillaCookie)
                cookie = parseMozillaCookie(url, new File(MOZILLACOOKIE));
            RequestInfo requestInfo = getRequest(url, cookie, ref, true);
            passwords.addAll(findPasswords(requestInfo.getHtmlCode()));
            inst -= 1;
            if(progress!=null)progress.increase( 1);
            String[] links = getHttpLinks(requestInfo.getHtmlCode(), link);
            if (inst != 0)
            {
                for (int i = 0; i < links.length; i++) {
                    decryptedLinks.addAll(getLinklist(links[i], requestInfo.getCookie(), url, inst));
                    decryptedLinks.add(this.createDownloadlink(links[i]);
                }
            }
            else
                for (int i = 0; i < links.length; i++) {
                    decryptedLinks.add(this.createDownloadlink(links[i]);
                }

        } catch (MalformedURLException e) {
            logger.warning("Konnte zu "+link+"keine Verbindung aufbauen");
        } catch (IOException e) {
        }

        return decryptedLinks;

    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String searchPattern = (String) parameter;

        logger.info("Search " + parameter);
        switch (step.getStep()) {
            case PluginStep.STEP_SEARCH :
                if(!parameter.toLowerCase().matches("http://.*"))
                {
                    logger.severe(parameter+" ist keine Internetadresse");
                    return null;
                }
                MOZILLACOOKIE = this.getProperties().getStringProperty("MOZILLACOOKIE", null);
                useMozillaCookie = this.getProperties().getBooleanProperty("USE_MOZILLACOOKIE", false);
                Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
                int inst = getIntParam(PARAM_INST, 1);
                if(progress!=null)progress.setRange( inst);
                passwords=new Vector<String>();
                Vector<String> de = getLinklist(searchPattern, null, null, inst);
                
                Vector<String> unique = new Vector<String>();
                String pw = null;
                if(passwords.size()>0)
                {
                pw = "{\""+passwords.get(0)+"\"";
                unique.add(passwords.get(0));
                for (int i = 1; i < passwords.size(); i++) {
                    if(!unique.contains(passwords.get(i)))
                    {
                    pw += ",\""+passwords.get(i)+"\"";
                    unique.add(passwords.get(i));
                    }
                }
                pw +="}";
                }
                for (int i = 0; i < de.size(); i++) {
                    String[] link = new String[]{de.get(i), pw, null};
                    if (!decryptedLinks.contains(link))
                        decryptedLinks.add(this.createDownloadlink(link);
                }
                //veraltet: firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
                return step;
        }
        return null;

    }

    public int getIntParam(String param, int defaultValue) {
        int minFilesize;
        if (this.getProperties().getProperty(param) != null) {
            try {
                minFilesize = Integer.parseInt((String) this.getProperties().getProperty(param));
            } catch (Exception e) {
                this.getProperties().setProperty(param, defaultValue);
                minFilesize = defaultValue;
            }

        } else {
            minFilesize = defaultValue;
        }
        return minFilesize;

    }
    private void setConfigElements() {

        ConfigEntry cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PARAM_INST, "Instanzen durchsuchen: ");
        cfg.setDefaultValue(1);
        config.addEntry(cfg);
        cfg = new ConfigEntry(ConfigContainer.TYPE_BROWSEFILE, getProperties(), "MOZILLACOOKIE", "Mozilla/FireFox cookies.txt");
        cfg.setDefaultValue(null);
        config.addEntry(cfg);
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getProperties(), "USE_MOZILLACOOKIE", "Mozilla/Firefox Cookie verwenden:"));
        cfg.setDefaultValue(false);

    }

    @Override
    public String[] getCategories() {
        return CATEGORIES;
    }

}
