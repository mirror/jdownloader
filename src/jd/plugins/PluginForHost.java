package jd.plugins;

import java.util.Vector;

/**
 * Dies ist die Oberklasse für alle Plugins, die von einem Anbieter Dateien
 * herunterladen können
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {
    // public abstract URLConnection getURLConnection();

    private String data;

    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    /**
     * Führt alle restevorgänge aus und bereitet das Plugin dadurch auf einen
     * Neustart vor
     */
    public void resetPlugin() {
        this.resetSteps();
        this.reset();
        this.aborted = false;
    }

    /**
     * Diese methode führt den Nächsten schritt aus. Der gerade ausgeführte
     * Schritt wir zurückgegeben
     * 
     * @param parameter Ein Übergabeparameter
     * @return der nächste Schritt oder null, falls alle abgearbeitet wurden
     */
    public PluginStep doNextStep(Object parameter) {
        currentStep = nextStep(currentStep);
        if (currentStep == null) {
            logger.info(this + " PLuginende erreicht!");
            return null;
        }
        return doStep(currentStep, parameter);
    }

    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem Vector zurückgeliefert
     * 
     * @param data Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data) {
        this.data = data;
        Vector<DownloadLink> links = null;
        Vector<String> hits = getMatches(data, getSupportedLinks());
        if (hits != null && hits.size() > 0) {
            links = new Vector<DownloadLink>();
            for (int i = 0; i < hits.size(); i++) {
                String file = hits.get(i);
                while (file.charAt(0) == '"')
                    file = file.substring(1);
                while (file.charAt(file.length() - 1) == '"')
                    file = file.substring(0, file.length() - 1);
                links.add(new DownloadLink(this, file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true));
            }
        }
        return links;
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigröße, Dateiname,
     * verfügbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfügbar)
     */
    public abstract boolean getFileInformation(DownloadLink parameter);

    /**
     * Gibt einen String mit den Dateiinformationen zurück. Die Defaultfunktion
     * gibt nur den dateinamen zurück. Allerdings Sollte diese Funktion
     * überschrieben werden. So kann ein PLugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param parameter
     * @return
     */
    public String getFileInformationString(DownloadLink parameter) {
        return parameter.getName();
    }

    /**
     * Diese Funktion verarbeitet jeden Schritt des PLugins.
     * 
     * @param step
     * @param parameter
     * @return
     */
    public abstract PluginStep doStep(PluginStep step, DownloadLink parameter);

    /**
     * Delegiert den doStep Call mit einem Downloadlink als Parameter weiter an
     * die Plugins
     * 
     * @param parameter Downloadlink
     */
    public PluginStep doStep(PluginStep step, Object parameter) {
        return doStep(step, (DownloadLink) parameter);
    }

    /**
     * Wird nicht gebraucht muss aber implementiert werden.
     */
    @Override
    public String getLinkName() {

        return data;
    }
}
