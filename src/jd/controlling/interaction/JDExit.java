package jd.controlling.interaction;

import java.io.Serializable;

import jd.utils.JDUtilities;

/**
 * Diese Klasse führt eine Test Interaction durch
 * 
 * @author coalado
 */
public class JDExit extends Interaction implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -4825002404662625527L;
    /**
     * serialVersionUID
     */
    private static final String NAME              = "JD Beenden";
    /**
     * Führt die Normale Interaction zurück. Nach dem Aufruf dieser methode
     * läuft der Download wie geowhnt weiter.
     */
    public static String        PROPERTY_QUESTION = "INTERACTION_" + NAME + "_QUESTION";
    public JDExit() {
     }
    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting Exit");
      System.exit(0);
      return true;
    }
    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    public void run() {}
    public String toString() {
        return NAME;
    }
    @Override
    public String getInteractionName() {
        return NAME;
    }
    @Override
    public void initConfig() {}
    @Override
    public void resetInteraction() {}
}
