package jd.plugins;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dies ist die Oberklasse für alle Plugins, die nach Links suchen sollen. z.B.
 * mp3 search plugins
 * 
 * @author coalado
 */
public abstract class PluginForSearch extends Plugin {

    private String searchPattern;

    public Vector<String> findLinks(String pattern) {
        Vector<String> foundLinks = new Vector<String>();

        foundLinks.addAll(parseContent(pattern));

        return foundLinks;
    }

    public Vector<String> parseContent(String pattern) {
        String[] s=pattern.split("\\:\\:\\:");
        if(s.length<2)return  new Vector<String>();
        this.searchPattern =s[1] ;

        PluginStep step = null;
        while ((step = nextStep(step)) != null) {
            doStep(step, searchPattern);
            if (nextStep(step) == null) {
                try {
                    if (step.getParameter() == null) {
                        logger.severe("ACHTUNG Search PLugins müssen im letzten schritt einen  Vector<String> parameter  übergeben!");
                        return new Vector<String>();
                    }
                    Vector<String> foundLinks = (Vector<String>) step.getParameter();
                    logger.info("Got " + foundLinks.size() + " links");
                    return foundLinks;
                }
                catch (Exception e) {
                    logger.severe("SearchFehler! " + e.getMessage());
                }
            }
        }
        return new Vector<String>();

    }


    public abstract PluginStep doStep(PluginStep step, String parameter);

    public PluginStep doStep(PluginStep step, Object parameter) {
        return doStep(step, (String) parameter);
    }
    @Override
    public String getLinkName() {
   
        return searchPattern;
    }
}
