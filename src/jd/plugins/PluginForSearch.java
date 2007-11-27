package jd.plugins;

import java.util.Vector;

import jd.controlling.ProgressController;

/**
 * Dies ist die Oberklasse für alle Plugins, die nach Links suchen sollen. z.B.
 * mp3 search plugins
 * 
 * @author coalado
 */
public abstract class PluginForSearch extends Plugin {

    private String searchPattern;
    protected ProgressController progress;
    public Vector<String[]> findLinks(String pattern) {
        if(progress==null){
            progress=new ProgressController();
            progress.setStatusText("Search-"+getPluginName()+": "+pattern);
        }
        Vector<String[]> foundLinks = new Vector<String[]>();

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
    private Vector<String[]> parseContent(String pattern) {
        String[] s = pattern.split("\\:\\:\\:");
        if (s.length < 2) return new Vector<String[]>();

        if (!hasCategory(s[0])) return new Vector<String[]>();
        this.searchPattern = s[1];

        PluginStep step = null;
        while ((step = nextStep(step)) != null) {
            logger.info("do step " + steps.indexOf(step));
            doStep(step, searchPattern);
            if (nextStep(step) == null) {
                try {
                    if (step.getParameter() == null) {
                        logger.severe("ACHTUNG Search Plugins müssen im letzten schritt einen  Vector<String[]> parameter  übergeben!");
                        return new Vector<String[]>();
                    }
                    Vector<String[]> foundLinks = (Vector<String[]>) step.getParameter();
                    logger.info("Got " + foundLinks.size() + " links");
                    return foundLinks;
                }
                catch (Exception e) {
                    try {
                        Vector<String> foundLinksTmp = (Vector<String>) step.getParameter();
                        Vector<String[]> foundLinks = (Vector<String[]>) step.getParameter();
                        for (int i = 0; i < foundLinksTmp.size(); i++) {
                            foundLinks.add(new String[] { foundLinksTmp.get(i), null, null });
                        }
                        logger.info("Got " + foundLinks.size() + " links");
                        return foundLinks;
                    }
                    catch (Exception e2) {
                        logger.severe("Searcherror: " + e2.getMessage());
                    }
                }
            }
        }
        return new Vector<String[]>();

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
