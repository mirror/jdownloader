package jd.controlling.interaction;

import java.io.Serializable;
import java.util.Date;
import java.util.Vector;

import jd.config.Configuration;
import jd.controlling.ProgressController;
import jd.update.WebUpdater;
import jd.utils.JDUtilities;

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

    @Override
    public boolean doInteraction(Object arg) {
        logger.info("Starting WebUpdate");
        updater = new WebUpdater(null);
        start();
        return true;
    }

    /**
     * Gibt den verwendeten Updater zurück
     * 
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
        
        Vector<Vector<String>> files = updater.getAvailableFiles();
        if(files==null||files.size()==0)return;
        int org;
        ProgressController progress = new ProgressController("Webupdater",org=files.size());
        progress.setStatusText("Update Check");
        if (files != null) {
          
            updater.filterAvailableUpdates(files,JDUtilities.getResourceFile("."));
            progress.setStatus(org-files.size());
            if (files.size() > 0) {
                logger.info("New Updates Available! "+files);
                JDUtilities.download(JDUtilities.getResourceFile("webupdater.jar"), "http://jdownloader.ath.cx/webupdater.jar");
                JDUtilities.download(JDUtilities.getResourceFile("changeLog.txt"), "http://www.syncom.org/projects/jdownloader/log/?format=changelog");
                
             
                if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_AUTO_RESTART, true)) {
                   //Eine checkfile schreiben. Diese CheckFile wird vom webupdater gelöscht. Wird sie beim restart von JD wiedergefunden wird eine warnmeldung angezeigt, weild as darauf hindeutet dass der webupdater fehlerhaft funktioniert hat
                    JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString()+"\r\n(Revision"+JDUtilities.getRevision()+")");
                    logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart","/rt"+JDUtilities.getRunType()}, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                    System.exit(0);
                }
                else {
                    if (JDUtilities.getController().getUiInterface().showConfirmDialog(files.size() + " update(s) available. Start Webupdater now?")) {
                        JDUtilities.writeLocalFile(JDUtilities.getResourceFile("webcheck.tmp"), new Date().toString()+"\r\n(Revision"+JDUtilities.getRevision()+")");
                        logger.info(JDUtilities.runCommand("java", new String[] { "-jar", "webupdater.jar", JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_WEBUPDATE_LOAD_ALL_TOOLS, false) ? "/all" : "", "/restart" ,"/rt"+JDUtilities.getRunType()}, JDUtilities.getResourceFile(".").getAbsolutePath(), 0));
                        System.exit(0);
                    }

                }

            }
  

        }
        
        this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        progress.finalize();
logger.info(updater.getLogger().toString());
        // updater.run();
        // if (updater.getUpdatedFiles() > 0) {
        // this.setCallCode(Interaction.INTERACTION_CALL_SUCCESS);
        // }
        // else {
        // this.setCallCode(Interaction.INTERACTION_CALL_ERROR);
        // }
    }

    @Override
    public void initConfig() {}

    @Override
    public void resetInteraction() {}
}
