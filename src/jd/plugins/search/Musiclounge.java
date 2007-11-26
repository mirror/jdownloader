package jd.plugins.search;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.PluginForSearch;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * 
 * @author coalado
 * 
 */
public class Musiclounge extends PluginForSearch {
    static private final String host              = "musiclounge.dl.am";

    private String              version           = "1.0.0.1";
private static String[] CATEGORIES =new String[]{"Audio"};
   

    private static final String SEARCH_URL        = "http://ml0ung4.iespana.es/search1.php";
    public static final String  PARAM_MAX_MATCHES = "MAX_MATCHES";

    public Musiclounge() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_SEARCH, null));

        ConfigEntry cfg;
        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PARAM_MAX_MATCHES, "Maximale Treffer"));
        cfg.setDefaultValue(50);
    }

    @Override
    public String getCoder() {
        return "jdown";
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
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return "musiclounge " + version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String searchPattern = (String) parameter;
logger.info("Search "+parameter);
        switch (step.getStep()) {
            case PluginStep.STEP_SEARCH:
                Vector<String[]> decryptedLinks = new Vector<String[]>();
                int page = 1;
                // Zählen aller verfügbaren Treffer
                String address;
                int results = 0;
                try {
                   
                        
                      
                        URL url = new URL(SEARCH_URL);
                        RequestInfo requestInfo = postRequest(url, null,null,null,"q="+JDUtilities.urlEncode(searchPattern)+"&action=search",true);
                       
                     
                      
                       Vector<Vector<String>> matches = getAllSimpleMatches(requestInfo.getHtmlCode(), "<a href=\"°\" target=\"_blank\">°</a> |");
                        String link;
                       progress.setRange( matches.size());
                        for (int i = 0; i < matches.size(); i++) {
                            // link = getLinkDetails(matches.get(i).get(0));
                            if (matches.get(i).get(0) != null) {
                                setStatusText(matches.get(i).get(1));
                                decryptedLinks.add(new String[]{matches.get(i).get(0),"musiclounge.dl.am",null});
                            }
                            // if (link != null) {
                            // decryptedLinks.add(link);
                            //
                            // }
                             progress.increase( 1);
                            results--;
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
                //veraltet: firePluginEvent(new PluginEvent(this, PluginEvent.PLUGIN_PROGRESS_FINISH, null));
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
