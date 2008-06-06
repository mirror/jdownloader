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
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class Wordpress extends PluginForDecrypt {
    static private final String host = "Wordpress Parser";
    private String version = "1.0.0.0";
    private String Supportpattern = "(http://[*]movie-blog.org/[+]/[+]/[+]/[+])" + "|(http://[*]xxx-blog.org/blog.php\\?id=[+])" + "|(http://[*]sky-porn.info/blog/\\?p=[+])";
    private Pattern patternSupported = getSupportPattern(Supportpattern);    
    private Vector<String> passwordpattern =new Vector<String>();
    private Vector<String> partpattern = new Vector<String>();
    
    public Wordpress() {
        super();
        passwordpatterns();
        partpatterns();
        steps.add(new PluginStep(PluginStep.STEP_DECRYPT, null));
    }    
    
    private void partpatterns(){
        /*Diese Pattern dienen zum auffinden der einzelnen Parts*/
        partpattern.add("<a href=\"([^>]*?)\" target=\"_blank\">Part \\d{0,3}</A>");
    }
    private void passwordpatterns(){
        /*diese Pattern dienen zum auffinden des Passworts*/
        passwordpattern.add("<B>Passwort:</B> ° |");
        passwordpattern.add("<strong>Passwort:</strong> ° |");
        passwordpattern.add("<strong>Passwort: </strong>°</p>");   
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
        return "Rlslog Comment Parser";
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
                String[] Links = Plugin.getHttpLinks(reqinfo.getHtmlCode(), null);
                
                String password = null;
                /*Passwort suchen*/
                for (int i=0;i<passwordpattern.size();i++){
                    password=JDUtilities.htmlDecode(getSimpleMatch(reqinfo, passwordpattern.get(i), 1));
                    if (password != null)
                    {
                        default_password.add(password);
                        break;
                    }
                };
                
                
             
                /*Alle Parts suchen*/
                ArrayList <String> parts =null;
                for (int i=0;i<partpattern.size();i++){
                    parts=getAllSimpleMatches(reqinfo,Pattern.compile( partpattern.get(i), Pattern.CASE_INSENSITIVE), 1);
                    if (parts != null)
                    {
                        logger.info("gefunden");
                        for (int ii=0;ii<parts.size();ii++)
                        {
                            logger.info("test"+parts.get(ii));
                        };                        
                    }
                };               
                
                
                //step.setParameter(decryptedLinks);*/
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