package jd.controlling.interaction;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import jd.event.ControlEvent;

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
       this.start();
        if (JOptionPane.showConfirmDialog(new JFrame(), "Dummy Interaction bestätigen?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
            //this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
            return true;
        }
        else {
            //this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
            return false;
        }
       

    }

public void run(){
    logger.info("UN DUMMY THREAD!!!");
    
    if (JOptionPane.showConfirmDialog(new JFrame(), "Dummy Thread Interaction bestätigen?", "Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == 0) {
        this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);    
    }
    else {
        this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
        
    }
    
   
    
}
    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {

        return NAME;
    }
}
