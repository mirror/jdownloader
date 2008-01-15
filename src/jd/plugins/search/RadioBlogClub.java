package jd.plugins.search;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForSearch;
import jd.plugins.PluginStep;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

/**
 * 
 * 
 * 
 * @author JD-Team
 * 
 */
public class RadioBlogClub extends PluginForSearch {
    static private final String host              = "radioblogclub.com";

    private String              version           = "1.0.0.1";
private static String[] CATEGORIES =new String[]{"Audio"};
   

    private static final String SEARCH_URL        = "http://www.radioblogclub.com/search/%s/%s";

    private static final String NUM_RESULTS       = "Results <b>° - °</b> of about <b>°</b> for <b>°</b>";

private static final String ENTRY_PATTERN="onClick=\"BlogThisTrack.start('°');\"></td><td valign=\"top\" width=\"14\"><input type=\"image\" src=\"http://stat.radioblogclub.com/images/heart_off.gif\" onClick=\"document.location='/login.php?notlogged=1';\"></td><td width=\"8\">&nbsp;</td><td valign=\"top\" width=\"30\"><div class=\"tracknum\">°</div></td><td>&nbsp;</td><td><a href=\"°\">°</a></td>";

 

    public RadioBlogClub() {
        super();
        steps.add(new PluginStep(PluginStep.STEP_SEARCH, null));

//        ConfigEntry cfg;
//        config.addEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, getProperties(), PARAM_MAX_MATCHES, "Maximale Treffer"));
//        cfg.setDefaultValue(50);
    }

    @Override
    public String getCoder() {
        return "JD-Team";
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
        return "radioBlogClub " + version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        String searchPattern = prepare((String) parameter);
  
        switch (step.getStep()) {
            case PluginStep.STEP_SEARCH:
                Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
                int siteNum = 0;
                // Zählen aller verfügbaren Treffer
                String address;
               // int results = 0;
                try {
                    while (true) {
                        address = String.format(SEARCH_URL, new Object[] { siteNum+"", URLEncoder.encode(searchPattern, "UTF-8") });
                        setStatusText("Seite " + siteNum);
                        logger.info("load " + address);
                        URL url = new URL(address);
                        RequestInfo requestInfo = getRequest(url, null, null, false);
                        int numResults = JDUtilities.filterInt(getSimpleMatch(requestInfo.getHtmlCode(), NUM_RESULTS, 2));
                        int currentResult = JDUtilities.filterInt(getSimpleMatch(requestInfo.getHtmlCode(), NUM_RESULTS, 0));
                        logger.info("Fount "+currentResult+" of "+numResults);
                        progress.setRange(numResults);
                        //logger.info("requestInf" +requestInfo.getHtmlCode());
                      Vector<Vector<String>> entries = getAllSimpleMatches(requestInfo.getHtmlCode(), ENTRY_PATTERN);
                      
                      for( int i=0; i<entries.size();i++){
                          DownloadLink link;
                        decryptedLinks.add(link=this.createDownloadlink(entries.get(i).get(0)));
                          progress.increase(1);
                         // logger.info(entries.get(i).get(0));
                     
                          progress.setStatusText(JDUtilities.htmlDecode(link.getName()));
                      }
                   // logger.info(entries+"");
                       if((currentResult+50)<numResults){
                           siteNum+=50;
                       }else{
                           break;
                       }
//                      
                    }
                    
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

    private String prepare(String parameter) {
        parameter=JDUtilities.filterString(parameter, "qwertzuiopasdfghjklyxcvbnmQWERTZUIOPASDFGHJKLYXCVBNM ").trim();
        return parameter.replace(" ", "_");
        
    }

    @Override
    public String[] getCategories() {
        return CATEGORIES;
    }

}
