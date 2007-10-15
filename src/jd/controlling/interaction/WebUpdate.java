package jd.controlling.interaction;

import java.io.Serializable;

import jd.update.WebUpdater;

/**
 * Diese Klasse führt ein Webupdate durch
 * 
 * @author coalado
 */
public class WebUpdate extends Interaction implements Serializable {
    /**
     * 
     */
    private static final long   serialVersionUID = 5345996658356704386L;
    /**
     * serialVersionUID
     */
    private static final String NAME             = "WebUpdate";
    private WebUpdater          updater;
    WebUpdater                  wu               = new WebUpdater(null);
    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting WebUpdate");
        updater = new WebUpdater(null);
        start();
        return true;
    }
    /**
     * Gibt den verwendeten Updater zurück
     * @return updater
     */
    public WebUpdater getUpdater() {
        return updater;
    }
    public String toString() {
        return "WebUpdate durchführen";
    }
    @Override
    public String getInteractionName() {
        return NAME;
    }
    @Override
    /**
     * Der eigentlich UpdaterVorgang läuft in einem eigenem Thread ab
     */
    public void run() {
        updater.run();
        if (updater.getUpdatedFiles() > 0) {
            this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        }
        else {
            this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
        }
    }
    @Override
    public void initConfig() {}
    @Override
    public void resetInteraction() {}
}
