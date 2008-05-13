//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling.interaction;

import java.io.Serializable;
import java.util.Vector;
import java.util.logging.Logger;

import jd.config.ConfigContainer;
import jd.config.Configuration;
import jd.config.Property;
import jd.controlling.JDController;
import jd.event.ControlEvent;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Mit dieser Klasse werden Interaktionen (mit dem System) umgesetzt
 * 
 * @author astaldo
 */
public abstract class Interaction extends Property implements Serializable {
    /**
     * serialVersionUID
     */
    private transient static final long serialVersionUID = -5609631258725998799L;
    protected static Logger logger = JDUtilities.getLogger();
    /**
     * Gibt das Event an bei dem Diese Interaction aktiv wird
     */
    private InteractionTrigger trigger;
    /**
     * Thread der für die Interaction verwendet werden kann
     */
    protected transient Thread thread = null;

    /**
     * Code der abgerufe werden kann um details über den Ablauf der Interaction
     * zu kriegen
     */
    protected transient int lastCallCode = 0;
    /**
     * Zeigt an dass diese Interaction noch nie aufgerufen wurde
     */
    public transient final static int INTERACTION_CALL_NEVERCALLED = 0;
    /**
     * Zeigt an dass die Interaction erfolgreioch beendet wurde
     */
    public transient final static int INTERACTION_CALL_SUCCESS = 1;
    /**
     * Zeigt an dass die Interaction mit Fehlern beendet wurde
     */
    public transient final static int INTERACTION_CALL_ERROR = 2;
    /**
     * Zeigt dass die Interaction gerade läuft
     */
    public transient final static int INTERACTION_CALL_RUNNING = 3;
    // Download IDS
    /**
     * Zeigt an, daß ein einzelner Download beendet wurde
     */
    public static InteractionTrigger INTERACTION_NO_EVENT = new InteractionTrigger(0, JDLocale.L("interaction.trigger.no_event", "Kein Event"), JDLocale.L("interaction.trigger.no_event.desc", "kein Event"));
    /**
     * Reconnect nötig
     */
    // public static InteractionTrigger INTERACTION_NEED_RECONNECT = new
    // InteractionTrigger(11, "Reconnect nötig", "Alle Trigger bei denen ein
    // Reconnect sinnvoll ist zusammengefasst");
    /**
     * Zeigt an, daß ein einzelner Download beendet wurde
     */
    public static InteractionTrigger INTERACTION_SINGLE_DOWNLOAD_FINISHED = new InteractionTrigger(1, JDLocale.L("interaction.trigger.download_successfull", "Download erfolgreich beendet"), JDLocale.L("interaction.trigger.download_successfull.desc", "Wird aufgerufen sobald ein Download erfolgreich beendet wurde"));
    /**
     * Zeigt an, daß alle Downloads abgeschlossen wurden
     */
    public static InteractionTrigger INTERACTION_ALL_DOWNLOADS_FINISHED = new InteractionTrigger(2, JDLocale.L("interaction.trigger.all_downloads_finished", "Alle Downloads beendet"), JDLocale.L("interaction.trigger.all_downloads_finished.desc", "Wird aufgerufen sobald alle Downloads beendet oder abgebrochen wurden"));
    /**
     * Zeigt, daß ein einzelner Download nicht fertiggestellt werden konnte
     */
    public static InteractionTrigger INTERACTION_DOWNLOAD_FAILED = new InteractionTrigger(3, JDLocale.L("interaction.trigger.single_download_failed", "Download fehlgeschlagen"), JDLocale.L("interaction.trigger.single_download_failed.desc", "Wird aufgerufen wenn ein Download wegen Fehlern abgebrochen wurde"));
    /**
     * Zeigt, daß ein einzelner Download wegen Wartezeit nicht starten konnte
     */
    // public static InteractionTrigger INTERACTION_DOWNLOAD_WAITTIME = new
    // InteractionTrigger(4, "Download hat Wartezeit", "Das Plugin meldet eine
    // Wartezeit");
    /**
     * Zeigt, daß ein der Bot erkannt wurde
     */
    public static InteractionTrigger INTERACTION_DOWNLOAD_BOT_DETECTED = new InteractionTrigger(5, JDLocale.L("interaction.trigger.bot_detected", "Bot erkannt"), JDLocale.L("interaction.trigger.bot_detected.desc", "jDownloader wurde als Bot erkannt"));
    /**
     * Zeigt, daß ein Captcha erkannt werden will
     */
    // public static InteractionTrigger INTERACTION_DOWNLOAD_CAPTCHA = new
    // InteractionTrigger(6, "Captcha Erkennung", "Ein Captcha-Bild muss
    // verarbeitet werden");
    /**
     * Letztes package file geladen
     */
    public static final InteractionTrigger INTERACTION_DOWNLOAD_PACKAGE_FINISHED = new InteractionTrigger(12, JDLocale.L("interaction.trigger.package_finished", "Paket fertig"), JDLocale.L("interaction.trigger.package_finished.desc", "Wird aufgerufen wenn ein Paket fertig geladen wurde"));
    public static final InteractionTrigger INTERACTION_BEFORE_DOWNLOAD = new InteractionTrigger(17, JDLocale.L("interaction.trigger.before_download", "Vor einem Download"), JDLocale.L("interaction.trigger.before_download.desc", "Wird aufgerufen bevor ein neuer Download gestartet wird"));

    /**
     * Zeigt den Programmstart an
     */
    public static InteractionTrigger INTERACTION_APPSTART = new InteractionTrigger(7, JDLocale.L("interaction.trigger.app_start", "Programmstart"), JDLocale.L("interaction.trigger.app_start.desc", "Direkt nach dem Initialisieren von jDownloader"));
    /**
     * Zeigt den TestTrigger an
     */
    public static InteractionTrigger INTERACTION_TESTTRIGGER = new InteractionTrigger(8, JDLocale.L("interaction.trigger.testtrigger", "Testtrigger"), JDLocale.L("interaction.trigger.testtrigger.desc", "Dieser trigger kann über das Menü ausgelöst werden"));

    public static InteractionTrigger INTERACTION_BEFORE_RECONNECT = new InteractionTrigger(13, JDLocale.L("interaction.trigger.before_reconnect", "Vor dem Reconnect"), JDLocale.L("interaction.trigger.before_reconnect.desc", "Vor dem eigentlichen Reconnect"));
    public static InteractionTrigger INTERACTION_AFTER_RECONNECT = new InteractionTrigger(14, JDLocale.L("interaction.trigger.after_reconnect", "Nach dem Reconnect"), JDLocale.L("interaction.trigger.after_reconnect.desc", "Nach dem eigentlichen Reconnect"));

    public static InteractionTrigger INTERACTION_AFTER_UNRAR = new InteractionTrigger(9, JDLocale.L("interaction.trigger.after_extract", "Nach dem Entpacken"), JDLocale.L("interaction.trigger.after_extract.desc", "Wird aufgerufen wenn die Unrar-Aktion beendet wurde."));

    public static InteractionTrigger INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS = new InteractionTrigger(15, JDLocale.L("interaction.trigger.downloads_and_interactions_finished", "Downloads & Interactionen abgeschlossen"), JDLocale.L("interaction.trigger.downloads_and_interactions_finished.desc", "Wird aufgerufen wenn alle Downloads und alle Interactionen beendet sind."));

    public static final InteractionTrigger INTERACTION_CONTAINER_DOWNLOAD = new InteractionTrigger(16, JDLocale.L("interaction.trigger.container_download", "Linkcontainer geladen"), JDLocale.L("interaction.trigger.download_download.desc", "Wird aufgerufen wenn ein LinkContainer(DLC,RSDF,CCF,...) geladen wurde"));

    // /**
    // * Nach einem IP wechsel
    // */
    private static int interactionsRunning = 0;
    protected transient ConfigContainer config;

    // public final static InteractionTrigger INTERACTION_AFTER_RECONNECT = new
    // InteractionTrigger(10, "Nach einem Reconnect", "inaktiv");
    public Interaction() {
        config = null;

        this.setTrigger(Interaction.INTERACTION_NO_EVENT);
    }

    protected abstract boolean doInteraction(Object arg);

    public abstract String toString();

    public abstract String getInteractionName();

    /**
     * Thread Funktion. Diese Funktion wird aufgerufen wenn Interaction.start()
     * aufgerufen wird. Dabei wird ein neuer thread erstellt
     */
    public abstract void run();

    /**
     * Gibt den callcode zurück. Dieser gibt Aufschlussdarüber wie die
     * Interaction abgelaufen ist
     * 
     * @return callcode
     */
    public int getCallCode() {
        return lastCallCode;
    }

    /**
     * Verwendet den JDcontroller um ein ControlEvent zu broadcasten
     * 
     * @param controlID
     * @param param
     */
    public void fireControlEvent(int controlID, Object param) {

        JDUtilities.getController().fireControlEvent(new ControlEvent(this, controlID, param));
    }

    public boolean getWaitForTermination() {
        return true;
    }

    /**
     * ruft die doInteraction Funktion auf. Und setzt das Ergebnis als callCode.
     * Der Statuscode kann mit getCallCode abgerufen werden
     * 
     * @param arg
     * @return
     */
    public boolean interact(Object arg) {
        interactionsRunning++;
        logger.finer("Interactions(start) running: " + interactionsRunning + " - " + this);
        fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, arg);
        resetInteraction();
        this.setCallCode(Interaction.INTERACTION_CALL_RUNNING);
        boolean success = doInteraction(arg);
        // fireControlEvent(new ControlEvent(this,
        // ControlEvent.CONTROL_PLUGIN_INTERACTION_RETURNED, this));
        if (!this.isAlive()) {
            fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, arg);

            interactionsRunning--;
            logger.info("Interaction finished: " + interactionsRunning + " - " + this);

        } else if (!getWaitForTermination()) {

            interactionsRunning--;
            logger.info("Interaction finished: " + interactionsRunning + " - " + this);
        }

        return success;
    }

    /**
     * Setzt eine INteraction in den Ausgangszustand zurück. z.B. Counter
     * zurückstellen etc.
     */
    public abstract void resetInteraction();

    /**
     * Initialisiert die Interaction beim JD start
     */
    public void initInteraction() {
        // nothing to init
    }

    /**
     * Setzt den callCode
     * 
     * @param callCode
     */
    public void setCallCode(int callCode) {
        this.lastCallCode = callCode;
    }

    /**
     * Wird vom neuen Thread aufgerufen, setzt die ThreadVariable
     */
    private void runThreadAction() {
        this.run();
        fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, null);
        if (getWaitForTermination()) {
            interactionsRunning--;
            logger.finer("Interaction finaly finished: " + interactionsRunning + " - " + this);
        }
        if (interactionsRunning == 0 && JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING && JDUtilities.getController().getFinishedLinks().size() > 0) {
            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS, null);
        }

    }

    /**
     * 
     * @return Anzahl der gerade aktiven Interactionen
     */
    public static int getRunningInteractionsNum() {
        return interactionsRunning;
    }

    /**
     * Gibt an ob der Thread aktiv ist
     * 
     * @return
     */
    public boolean isAlive() {
        if (thread == null) return false;
        return thread.isAlive();
    }

    /**
     * Erstellt einen neuen Thread und führt den zugehörigen Code aus (run()
     */
    protected void start() {
        final Interaction _this = this;
        thread = new Thread() {
            public void run() {
                _this.runThreadAction();
            }
        };
        thread.start();
    }

    /**
     * Führt die Interactions aus
     * 
     * @param interactionevent
     *            Trigger der Interaction
     * @param param
     *            Parameter
     * 
     * @return wahr, wenn die Interaction abgearbeitet werden konnte, ansonsten
     *         falsch
     */
    @SuppressWarnings("unchecked")
    public static boolean handleInteraction(InteractionTrigger interactionevent, Object param) {
        boolean ret = true;
        logger.finer("Interaction start: Trigger: " + interactionevent.getName());
        JDUtilities.getController().fireControlEvent(new ControlEvent(interactionevent, ControlEvent.CONTROL_INTERACTION_CALL, param));
        Vector<Interaction> interactions = (Vector<Interaction>) JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS, new Vector<Interaction>());
        int interacts = 0;
        for (int i = 0; i < interactions.size(); i++) {
            Interaction interaction = interactions.get(i);
            if (interaction == null || interaction.getTrigger() == null || interactionevent == null) continue;

            if (interaction.getTrigger().getID() == interactionevent.getID()) {

                interacts++;

                logger.finer("Aktion start: " + interaction.getInteractionName() + "(" + param + ")");
                if (!interaction.interact(param)) {
                    ret = false;
                    logger.severe("interaction failed: " + interaction);

                } else {
                    logger.info("interaction successfull: " + interaction);
                }

            }
        }
        if(interactionevent==Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED&&interactionsRunning == 0 && JDUtilities.getController().getDownloadStatus() == JDController.DOWNLOAD_NOT_RUNNING && JDUtilities.getController().getFinishedLinks().size() > 0){
            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS, null);
        }
        if (interacts == 0) return false;
        return ret;
    }

    /**
     * Führt nur die i-te Interaction aus
     * 
     * @param interactionEvent
     *            Trigger der Interaction
     * @param param
     *            Parameter für die Interaction
     * @param id
     *            der Interaktion
     * @return wahr, wenn die Interaction abgearbeitet werden konnte, ansonsten
     *         falsch
     */
    @SuppressWarnings("unchecked")
    public static boolean handleInteraction(InteractionTrigger interactionEvent, Object param, int id) {
        Vector<Interaction> interactions = (Vector<Interaction>) JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS, new Vector<Interaction>());

        for (int i = 0; i < interactions.size(); i++) {
            Interaction interaction = interactions.get(i);
            if (interaction == null || interaction.getTrigger() == null || interactionEvent == null) continue;
            if (interaction.getTrigger().getID() == interactionEvent.getID()) {
                if (id == 0) {

                    if (!interaction.interact(param)) {
                        logger.severe("interaction failed: " + interaction);
                        return false;
                    } else {
                        logger.info("interaction successfull: " + interaction);
                        return true;
                    }
                }
                id--;
            }
        }
        return false;
    }

    /**
     * Gibt alle Interactionen zum Trigger zurück
     * 
     * @param trigger
     * @return Alle Interactionen zum Trigger zurück
     */
    @SuppressWarnings("unchecked")
    public static Interaction[] getInteractions(InteractionTrigger trigger) {

        Vector<Interaction> interactions = (Vector<Interaction>) JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS, new Vector<Interaction>());

        Vector<Interaction> ret = new Vector<Interaction>();
        for (int i = 0; i < interactions.size(); i++) {
            if (interactions.get(i).getTrigger().getID() == trigger.getID()) {
                ret.add(interactions.get(i));
            }
        }
        return ret.toArray(new Interaction[] {});
    }

    /**
     * Gibt die Interaction ID zurück bei diese Interactiona aktiv wird
     * 
     * @return Interaction ID
     */
    public InteractionTrigger getTrigger() {
        return trigger;
    }

    /**
     * Setzt die Interaction ID (event ID)
     * 
     * @param trigger
     *            Der Trigger
     */
    public void setTrigger(InteractionTrigger trigger) {
        this.trigger = trigger;
    }

    /**
     * Gibt den namen des EventTriggers zurück
     * 
     * @return Name des Triggers
     */
    public String getTriggerName() {
        return getTrigger().toString();
    }

    /**
     * Gibt eine Liste aller vefügbaren Interactions zurück. Bei neuen
     * Interactions muss diese hier eingefügt werden
     * 
     * @return Liste mit allen Interactionen
     */
    public static Interaction[] getInteractionList() {
        return new Interaction[] { new SimpleExecute(), new ExternExecute(), new JDExit(), new ResetLink(), /*
                                                                                                             * new
                                                                                                             * DLCConverter(),
                                                                                                             */new Backup() };
    }

    /**
     * Da die Knfigurationswünsche nicht gespeichert werden, muss der
     * ConfigContainer immer wieder aufs neue Initialisiert werden. Alle
     * Interactionen müssend azu die initConifg Methode implementieren
     */
    public abstract void initConfig();

    public ConfigContainer getConfig() {
        logger.info(config + " # ");
        if (config == null) {
            config = new ConfigContainer(this);
            initConfig();
        }

        return config;
    }

    public void setConfig(ConfigContainer config) {
        this.config = config;
    }

    public static int getInteractionsRunning() {
        return interactionsRunning;
    }

}
