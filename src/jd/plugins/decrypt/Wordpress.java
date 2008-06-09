package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginStep;
import jd.plugins.Regexp;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Wordpress extends PluginForDecrypt {
    static private final String host = "Wordpress Parser";
    private String version = "1.0.0.0";
//    private String Supportpattern = "(http://[*]movie-blog.org/[+]/[+]/[+]/[+])" + "|(http://[*]doku.cc/[+]/[+]/[+]/[+])" + "|(http://[*]xxx-blog.org/blog.php\\?id=[+])" + "|(http://[*]sky-porn.info/blog/\\?p=[+])" + "|(http://[*]best-movies.us/\\?p=[+]) + |(http://[*]game-blog.us/game-[+].html)";
    private Pattern patternSupported = Pattern.compile("http://.*?(movie-blog.org/.+/.+/.+/.+|doku.cc/.+/.+/.+/.+|xxx-blog.org/blog.php\\?id=[\\d]+|sky-porn.info/blog/\\?p=.+|best-movies.us/\\?p=.+|game-blog.us/game-.+\\.html).*", Pattern.CASE_INSENSITIVE);
    private Vector<String> passwordpattern = new Vector<String>();
//    private Vector<String> partpattern = new Vector<String>();
    private ArrayList<String[]> defaultpasswords = new ArrayList<String[]>();

    public Wordpress() {
        super();
        add_passwordpatterns();
//        add_partpatterns();
        add_defaultpasswords();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }

    private void add_defaultpasswords() {
        /* Die defaultpasswörter der einzelnen seiten */
        /* Host, defaultpw1, defaultpw2, usw */
        defaultpasswords.add(new String[] { "doku.cc", "doku.cc", "doku.dl.am" });
        defaultpasswords.add(new String[] { "movie-blog.org", "movie-blog.org", "movie-blog.dl.am" });
        defaultpasswords.add(new String[] { "xxx-blog.org", "xxx-blog.org", "xxx-blog.dl.am" });
    }

//    private void add_partpatterns() {
        /* Diese Pattern dienen zum auffinden der einzelnen Parts */
        /* ACHTUNG: url muss an erster stelle im pattern sein*/
//        partpattern.add("");
        //partpattern.add("href=\"?([^>]*?)\"?\\s?(target=\"_blank\")?\\s?>\\s?Part\\s?\\d{0,3}\\s?<\\/a>");        
        //partpattern.add("href=\"?([^>]*?)\"?\\s?(target=\"_blank\")?\\s?>[^<]*(Rapidshare|CCF|RSD|CCL|DLC|RSDF|Xirror|Upload|Share|Netload|Bluehost|Crypt|File){1,}[^<]*<\\/a>");
//        partpattern.add("href=\"?([^>\\s]*?)\"?\\s?(target=\"_blank\")?\\s?>[^<]*(Rapidshare|CCF|RSD|CCL|DLC|RSDF|Xirror|Upload|Share-Online|Netload|Bluehost|Crypt|Parts? [\\d]*)[^<]*<\\/a>");
//    }

    private void add_passwordpatterns() {
        /* diese Pattern dienen zum auffinden des Passworts */
        /* ACHTUNG: passwort muss an erster stelle im pattern sein*/
        passwordpattern.add("<b>Passwort\\:<\\/b>\\s?(.*?)\\s?\\|");
        passwordpattern.add("<b>Passwort\\:<\\/b>\\s?(.*?)\\s?\\<\\/p>");
        passwordpattern.add("<strong>Passwort\\:<\\/strong>\\s?(.*?)\\s?\\|");
        passwordpattern.add("<strong>Passwort\\:<\\/strong>\\s?(.*?)\\s?<\\/p>");
        passwordpattern.add("<strong>Passwort\\: <\\/strong>(.*?)<strong>");
        passwordpattern.add("<strong>Passwort<\\/strong>\\:\\s?(.*?)\\s?<strong>");
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
        return "Wordpress Parser";
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

    @Override
    public PluginStep doStep(PluginStep step, String parameter) {
        if (step.getStep() == PluginStep.STEP_DECRYPT) {
            Vector<DownloadLink> decryptedLinks = new Vector<DownloadLink>();
            try {
                URL url = new URL(parameter);
                RequestInfo reqinfo = getRequest(url);

                /* Defaultpasswörter der Seite setzen */
                for (int j = 0; j < defaultpasswords.size(); j++) {
                    if (defaultpasswords.get(j)[0].contains(url.getHost())) {
                        for (int jj = 1; jj < defaultpasswords.get(j).length; jj++) {
                            default_password.add(defaultpasswords.get(j)[jj]);                            
                        }
                        break;
                    }
                }
                ArrayList<String> password = null;
                /* Passwort suchen */
                for (int i = 0; i < passwordpattern.size(); i++) {
                    password = getAllSimpleMatches(reqinfo, Pattern.compile(passwordpattern.get(i), Pattern.CASE_INSENSITIVE), 1);
                    if (password.size() != 0) {
                        for (int ii = 0; ii < password.size(); ii++) {
                            logger.info("PW:"+password.get(ii));
                            default_password.add(JDUtilities.htmlDecode(password.get(ii)));                            
                        }
                        break;
                    }
                }
                String[] links = reqinfo.getRegexp("(<a.*?</a>)").getMatches(1);
                for (int i = 0; i < links.length; i++) {
                    try {
                        if(links[i].matches(".*(Rapidshare|CCF|RSD|CCL|DLC|RSDF|Xirror|Upload|Share-Online|Netload|Bluehost|Crypt|Parts?[\\s\\d]*?).*"))
                        {
                        String lnk = Plugin.getHttpLinks(links[i], parameter)[0];
                        if(!new Regexp(lnk, patternSupported).matches())
                        {
                        decryptedLinks.add(this.createDownloadlink(lnk));       
                        }
                        }
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
                /* Alle Parts suchen */
                /*
                ArrayList<String> parts = null;
                for (int i = 0; i < partpattern.size(); i++) {
                    parts = getAllSimpleMatches(reqinfo, Pattern.compile(partpattern.get(i), Pattern.CASE_INSENSITIVE), 1);
                    if (parts.size() != 0) {
                        for (int ii = 0; ii < parts.size(); ii++) {
                            logger.info("LINK:"+JDUtilities.htmlDecode(parts.get(ii)));
//                            decryptedLinks.add(this.createDownloadlink(JDUtilities.htmlDecode(parts.get(ii))));                          
                        }
                        break;
                    }
                }
                */

                step.setParameter(decryptedLinks);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }
}