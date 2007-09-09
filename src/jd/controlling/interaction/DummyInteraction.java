package jd.controlling.interaction;

import java.io.Serializable;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Diese Klasse führt eine Test INteraction durch
 * 
 * @author coalado
 */
public class DummyInteraction extends Interaction implements Serializable {

    /**
     * 
     */
    private static final long   serialVersionUID  = -4390257509319544642L;

    /**
     * serialVersionUID
     */

    private static final String NAME              = "Dummy";

    /**
     * Führt die Normale INteraction zurück. Nach dem Aufruf dieser methode
     * läuft der Download wie geowhnt weiter.
     */
    public static String        PROPERTY_QUESTION = "INTERACTION_" + NAME + "_QUESTION";

    public DummyInteraction() {
        setProperty(PROPERTY_QUESTION, "Dummy Interaction. Dieser Text kann in der Konfiguration eingestellt werden");
    }

    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting Dummy");
        if (JOptionPane.showConfirmDialog(new JFrame(), getProperty(PROPERTY_QUESTION), "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
            this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
            return true;
        }
        else {
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            return false;
        }

    }

    /**
     * Nichts zu tun. WebUpdate ist ein Beispiel für eine ThreadInteraction
     */
    public void run() {

    }

    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {

        return NAME;
    }

}
