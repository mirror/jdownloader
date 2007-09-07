package jd.controlling.interaction;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/**
 * Diese Klasse führt eine Test INteraction durch
 * 
 * @author coalado
 */
public class DummyInteraction extends Interaction {

    /**
     * serialVersionUID
     */
    private static final long   serialVersionUID = 1332164738388120767L;

    private static final String NAME             = "Dummy";

    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting Dummy");

        if (JOptionPane.showConfirmDialog(new JFrame(), "Dummy Interaction bestätigen?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
            this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
            return true;
        }
        else {
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            return false;
        }

    }

    public String toString() {
        return NAME;
    }

    @Override
    public String getName() {

        return NAME;
    }
}
