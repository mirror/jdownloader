package jd.controlling.interaction;

import jd.update.WebUpdater;

/**
 * Diese Klasse führt ein Webupdate durch
 * 
 * @author coalado
 */
public class WebUpdate extends Interaction {

    /**
     * serialVersionUID
     */
    private static final long   serialVersionUID = 1332164738388120767L;

    private static final String NAME             = "WebUpdate";

    private WebUpdater          updater;

    WebUpdater                  wu               = new WebUpdater(null);

    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting WebUpdate");
        updater = new WebUpdater(null);
        updater.run();
        return wu.getUpdatedFiles() > 0;

    }
/**
 * GIbt den verwendeten Updater zurück
 * @return updater
 */
    public WebUpdater getUpdater() {
        return updater;
    }

    public String toString() {
        return NAME;
    }

    @Override
    public String getInteractionName() {

        return NAME;
    }
    @Override
    public void run() {
        //Nichts zu tun. INteraction braucht keinen Thread
        
    }
}
