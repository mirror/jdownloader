package jd.plugins.search;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForSearch;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
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
    private static final String PARAM_INST = "INST";
    private String MOZILLACOOKIE;
    private boolean useMozillaCookie = false;
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
        Vector<String> decryptedLinks = new Vector<String>();
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
            inst -= 1;
            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
            String[] links = getHttpLinks(requestInfo.getHtmlCode(), link);
            if (inst != 0)
                for (int i = 0; i < links.length; i++) {
                    decryptedLinks.addAll(getLinklist(links[i], requestInfo.getCookie(), url, inst));
                    decryptedLinks.add(links[i]);
                }
            else
                for (int i = 0; i < links.length; i++) {
                    decryptedLinks.add(links[i]);
                }

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return decryptedLinks;

    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String searchPattern = (String) parameter;

        logger.info("Search " + parameter);
        switch (step.getStep()) {
            case PluginStep.STEP_SEARCH :
                MOZILLACOOKIE = this.getProperties().getStringProperty("MOZILLACOOKIE", null);
                useMozillaCookie = this.getProperties().getBooleanProperty("USE_MOZILLACOOKIE", false);
                Vector<String[]> decryptedLinks = new Vector<String[]>();
                int inst = getIntParam(PARAM_INST, 1);
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, inst));
                Vector<String> de = getLinklist(searchPattern, null, null, inst);
                for (int i = 0; i < de.size(); i++) {
                    String[] link = new String[]{de.get(i), null, null};
                    if (!decryptedLinks.contains(link))
                        decryptedLinks.add(link);
                }
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
                return step;
        }
        return null;

    }

    private int getIntParam(String param, int defaultValue) {
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
