package jd.plugins.search;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForSearch;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.plugins.event.PluginEvent;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * 
 * @author coalado
 * 
 */
public class Esnips extends PluginForSearch {
    static private final String host              = "esnips.com";

    private String              version           = "1.0.0.1";
private static String[] CATEGORIES =new String[]{"Audio"};
   

    private static final String SEARCH_URL        = "http://www.esnips.com/_t_/%s/?tab=1&type=MUSIC_FILES&sort=RELEVANCY&q=%s&page=%s";

    private static final String NUM_RESULTS       = "<div class=\"numResults\">°&nbsp;audio &amp; music file(s)";

    private static final String COMMAND_URL       = " <a class=\"CommandLink\" href='°'><strong>°</strong>";

    public static final String  PARAM_MAX_MATCHES = "MAX_MATCHES";

    public Esnips() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_SEARCH, null));

        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PARAM_MAX_MATCHES, "Maximale Treffer"));
        cfg.setDefaultValue(50);
    }

    @Override
    public String getCoder() {
        return "Coalado";
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        String ret="";
        for( int i=0; i<CATEGORIES.length;i++)ret+="|"+CATEGORIES[i];
        return Pattern.compile("["+ret.substring(1)+"]:::*");
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public boolean isClipboardEnabled() {
        return true;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return "eSnipes " + version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String searchPattern = (String) parameter;
        int maxMatches;
        if (this.getProperties().getProperty(PARAM_MAX_MATCHES) != null) {
//            logger.info(this.getProperties().getProperty(PARAM_MAX_MATCHES).getClass().toString());
            try {
                maxMatches = Integer.parseInt((String) this.getProperties().getProperty(PARAM_MAX_MATCHES));
            }
            catch (Exception e) {
                this.getProperties().setProperty(PARAM_MAX_MATCHES, 50);
                maxMatches = 50;
            }

        }
        else {
            maxMatches = 50;
        }
        switch (step.getStep()) {
            case PluginStep.STEP_SEARCH:
                Vector<String> decryptedLinks = new Vector<String>();
                int page = 1;
                // Zählen aller verfügbaren Treffer
                String address;
                int results = 0;
                try {
                    while (true) {
                        address = String.format(SEARCH_URL, new Object[] { JDUtilities.urlEncode(searchPattern), URLEncoder.encode(searchPattern, "UTF-8"), page + "" });
                        setStatusText("Seite " + page);
                        logger.info("load " + address);
                        URL url = new URL(address);
                        RequestInfo requestInfo = getRequest(url, null, null, false);
                        if (page == 1) {
                            String numResults = getSimpleMatch(requestInfo.getHtmlCode(), NUM_RESULTS, 0);
                            if (numResults == null) {
                                logger.info("No Results");
                                step.setParameter(decryptedLinks);
                                return step;
                            }

                            try {
                                results = Integer.parseInt(numResults.trim());
                            }
                            catch (Exception e) {
                                logger.info("Number exc " + numResults);
                                step.setParameter(decryptedLinks);
                                return step;
                            }
                            results = Math.min(maxMatches, results);
                            //
                            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_MAX, results));
                        }
                        Vector<Vector<String>> matches = getAllSimpleMatches(requestInfo.getHtmlCode(), COMMAND_URL);
                        String link;

                        for (int i = 0; i < matches.size(); i++) {
                            // link = getLinkDetails(matches.get(i).get(0));
                            if (matches.get(i).get(0) != null) {
                                setStatusText(matches.get(i).get(1));
                                decryptedLinks.add("http://" + host + matches.get(i).get(0));
                            }
                            // if (link != null) {
                            // decryptedLinks.add(link);
                            //
                            // }
                            firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_INCREASE, null));
                            results--;
                        }
                        if (results <= 0|| matches.size()<=0) break;
                        page++;
                    }
                    // decryptedLinks.add(newURL);
                }
                catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
                step.setParameter(decryptedLinks);
                return step;

        }
        return null;

    }

    @Override
    public String[] getCategories() {
        return CATEGORIES;
    }

}
