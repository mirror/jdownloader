package jd.plugins;

import java.util.Vector;

import jd.controlling.ProgressController;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die nach Links suchen sollen. z.B.
 * mp3 search plugins
 * 
 * @author coalado
 */
public abstract class PluginForSearch extends Plugin {

    private String searchPattern;
    protected ProgressController progress;
    protected Vector<String> default_password=new Vector<String>();;
    public Vector<DownloadLink> findLinks(String pattern) {
        if(progress==null){
            progress=new ProgressController("Search: "+this.getLinkName());
            progress.setStatusText("Search-"+getPluginName()+": "+pattern);
        }
        Vector<DownloadLink> foundLinks = new Vector<DownloadLink>();

        foundLinks.addAll(parseContent(pattern));
        progress.finalize();
        return foundLinks;
    }

    /**
     * Diese methode sucht nach pattern. Pattern hat das Format
     * Kategorie:::Suchstring Dieses apttern wird zerlegt und anhand der
     * kategorie die Plugins gefiltert
     * 
     * @param pattern
     * @return Aller Ergebnisse die in den suchplugins für die gewählte
     *         Kategorie und den gewählten suchstring gef7unden wurden
     */
    @SuppressWarnings("unchecked")
	private Vector<DownloadLink> parseContent(String pattern) {
        String[] s = pattern.split("\\:\\:\\:");
        if (s.length < 2) return new Vector<DownloadLink>();

        if (!hasCategory(s[0])) return new Vector<DownloadLink>();
        this.searchPattern = s[1];

        PluginStep step = null;
        while ((step = nextStep(step)) != null) {
            logger.info("do step " + steps.indexOf(step));
            doStep(step, searchPattern);
            if (nextStep(step) == null) {
                try {
                    if (step.getParameter() == null) {
                        logger.severe("ACHTUNG Search Plugins müssen im letzten schritt einen  Vector<String[]> parameter  übergeben!");
                        return new Vector<DownloadLink>();
                    }
                    Vector<DownloadLink> foundLinks = (Vector<DownloadLink>) step.getParameter();
                    logger.info("Got " + foundLinks.size() + " links ");
                    return foundLinks;
                }
                catch (Exception e) {
                   e.printStackTrace();
                }
            }
        }
        return new Vector<DownloadLink>();

    }
    protected DownloadLink createDownloadlink(String link){
        DownloadLink dl= new DownloadLink(this, null, this.getHost(), JDUtilities.htmlDecode(link), true);
       return dl;
    }
    
    public String getDefaultPassword(){
        if(default_password.size()==0)return null;
       String ret="{";
       for( int i=0; i<default_password.size();i++){
           ret+="\""+default_password.get(i)+"\"";
               if(i<default_password.size()-1){
                   ret+=", ";
               }
       }
       ret+="}";
       return ret;
    }
    /**
     * Liefert alle Möglichen kategorien des Plugins zurück
     * 
     * @return
     */
    public abstract String[] getCategories();

    /**
     * Prüft ob das Plugin die angegebene Category liefern kann (filetyoe.
     * z.B.Audio)
     * 
     * @param cat
     * @return
     */
    public boolean hasCategory(String cat) {
        logger.info(cat);
        for (int i = 0; i < getCategories().length; i++) {
            if (getCategories()[i].equalsIgnoreCase(cat)) return true;
        }
        return false;
    }

    /**
     * Wird von der parentklasse für jeden step aufgerufen. Diese Methode muss
     * alle steps abarbeiten und abgecshlossene schritte zurückgeben
     * 
     * @param step
     * @param parameter
     * @return
     */
    public abstract PluginStep doStep(PluginStep step, String parameter);

    /**
     * Deligiert den doStepcall mit einem Parameter cast an die Searchplugins
     * weiter
     */
    public PluginStep doStep(PluginStep step, Object parameter) {
        return doStep(step, (String) parameter);
    }

    /**
     * Wirdnicht benötigt muss aber implementiert werden
     */
    @Override
    public String getLinkName() {

        return searchPattern;
    }

}
