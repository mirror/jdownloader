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

    public static final InteractionTrigger INTERACTION_NO_EVENT = new InteractionTrigger(0, JDLocale.L("interaction.trigger.no_event", "Kein Event"), JDLocale.L("interaction.trigger.no_event.desc", "kein Event"));
    public static final InteractionTrigger INTERACTION_SINGLE_DOWNLOAD_FINISHED = new InteractionTrigger(1, JDLocale.L("interaction.trigger.download_successfull", "Download erfolgreich beendet"), JDLocale.L("interaction.trigger.download_successfull.desc", "Wird aufgerufen sobald ein Download erfolgreich beendet wurde"));
    public static final InteractionTrigger INTERACTION_ALL_DOWNLOADS_FINISHED = new InteractionTrigger(2, JDLocale.L("interaction.trigger.all_downloads_finished", "Alle Downloads beendet"), JDLocale.L("interaction.trigger.all_downloads_finished.desc", "Wird aufgerufen sobald alle Downloads beendet oder abgebrochen wurden"));
    public static final InteractionTrigger INTERACTION_DOWNLOAD_FAILED = new InteractionTrigger(3, JDLocale.L("interaction.trigger.single_download_failed", "Download fehlgeschlagen"), JDLocale.L("interaction.trigger.single_download_failed.desc", "Wird aufgerufen wenn ein Download wegen Fehlern abgebrochen wurde"));
    public static final InteractionTrigger INTERACTION_LINKLIST_STRUCTURE_CHANGED = new InteractionTrigger(4, JDLocale.L("interaction.trigger.linklist_structure_changed", "Linklisten Struktur geändert"), JDLocale.L("interaction.trigger.linklist_structure_changed.desc", "Wird aufgerufen wenn sich die Struktur der Linkliste geändert hat (Links eingefügt, Links fertig, ...)"));
    public static final InteractionTrigger INTERACTION_DOWNLOAD_BOT_DETECTED = new InteractionTrigger(5, JDLocale.L("interaction.trigger.bot_detected", "Bot erkannt"), JDLocale.L("interaction.trigger.bot_detected.desc", "jDownloader wurde als Bot erkannt"));
    public static final InteractionTrigger INTERACTION_APPSTART = new InteractionTrigger(7, JDLocale.L("interaction.trigger.app_start", "Programmstart"), JDLocale.L("interaction.trigger.app_start.desc", "Direkt nach dem Initialisieren von jDownloader"));
    public static final InteractionTrigger INTERACTION_AFTER_UNRAR = new InteractionTrigger(9, JDLocale.L("interaction.trigger.after_extract", "Nach dem Entpacken"), JDLocale.L("interaction.trigger.after_extract.desc", "Wird aufgerufen wenn die Unrar-Aktion beendet wurde."));
    public static final InteractionTrigger INTERACTION_DOWNLOAD_PACKAGE_FINISHED = new InteractionTrigger(12, JDLocale.L("interaction.trigger.package_finished", "Paket fertig"), JDLocale.L("interaction.trigger.package_finished.desc", "Wird aufgerufen wenn ein Paket fertig geladen wurde"));
    public static final InteractionTrigger INTERACTION_BEFORE_RECONNECT = new InteractionTrigger(13, JDLocale.L("interaction.trigger.before_reconnect", "Vor dem Reconnect"), JDLocale.L("interaction.trigger.before_reconnect.desc", "Vor dem eigentlichen Reconnect"));
    public static final InteractionTrigger INTERACTION_AFTER_RECONNECT = new InteractionTrigger(14, JDLocale.L("interaction.trigger.after_reconnect", "Nach dem Reconnect"), JDLocale.L("interaction.trigger.after_reconnect.desc", "Nach dem eigentlichen Reconnect"));
    public static final InteractionTrigger INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS = new InteractionTrigger(15, JDLocale.L("interaction.trigger.downloads_and_interactions_finished", "Downloads & Interactionen abgeschlossen"), JDLocale.L("interaction.trigger.downloads_and_interactions_finished.desc", "Wird aufgerufen wenn alle Downloads und alle Interactionen beendet sind."));
    public static final InteractionTrigger INTERACTION_CONTAINER_DOWNLOAD = new InteractionTrigger(16, JDLocale.L("interaction.trigger.container_download", "Linkcontainer geladen"), JDLocale.L("interaction.trigger.download_download.desc", "Wird aufgerufen wenn ein LinkContainer(DLC,RSDF,CCF,...) geladen wurde"));
    public static final InteractionTrigger INTERACTION_BEFORE_DOWNLOAD = new InteractionTrigger(17, JDLocale.L("interaction.trigger.before_download", "Vor einem Download"), JDLocale.L("interaction.trigger.before_download.desc", "Wird aufgerufen bevor ein neuer Download gestartet wird"));
    public static final InteractionTrigger INTERACTION_EXIT = new InteractionTrigger(20, JDLocale.L("interaction.trigger.exit", "JD wird beendet"), JDLocale.L("interaction.trigger.exit.desc", "Wird beim Beenden vor dem Schließen des Programms aufgerufen"));

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

    private static int interactionsRunning = 0;

    protected static Logger logger = JDUtilities.getLogger();

    private transient static final long serialVersionUID = -5609631258725998799L;

    /**
     * Gibt eine Liste aller vefügbaren Interactions zurück. Bei neuen
     * Interactions muss diese hier eingefügt werden
     * 
     * @return Liste mit allen Interactionen
     */
    public static Interaction[] getInteractionList() {
        return new Interaction[] { new SimpleExecute(), new ExternExecute(), new JDExit(), new ResetLink()};
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

    public static int getInteractionsRunning() {
        return interactionsRunning;
    }

    /**
     * 
     * @return Anzahl der gerade aktiven Interactionen
     */
    public static int getRunningInteractionsNum() {
        return interactionsRunning;
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
            if (interaction == null || interaction.getTrigger() == null || interactionevent == null) {
                continue;
            }

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
        if (interactionevent.equals(INTERACTION_ALL_DOWNLOADS_FINISHED) && interactionsRunning == 0 && JDUtilities.getController().getFinishedLinks().size() > 0) {

            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS, null);
        }
        if (interacts == 0) { return false; }
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
            if (interaction == null || interaction.getTrigger() == null || interactionEvent == null) {
                continue;
            }
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

    protected transient ConfigContainer config;

    /**
     * Code der abgerufe werden kann um details über den Ablauf der Interaction
     * zu kriegen
     */
    protected transient int lastCallCode = 0;

    /**
     * Thread der für die Interaction verwendet werden kann
     */
    protected transient Thread thread = null;

    /**
     * Gibt das Event an bei dem Diese Interaction aktiv wird
     */
    private InteractionTrigger trigger;

    public Interaction() {
        config = null;

        setTrigger(Interaction.INTERACTION_NO_EVENT);
    }

    protected abstract boolean doInteraction(Object arg);

    /**
     * Verwendet den JDcontroller um ein ControlEvent zu broadcasten
     * 
     * @param controlID
     * @param param
     */
    public void fireControlEvent(int controlID, Object param) {

        JDUtilities.getController().fireControlEvent(new ControlEvent(this, controlID, param));
    }

    /**
     * Gibt den callcode zurück. Dieser gibt Aufschlussdarüber wie die
     * Interaction abgelaufen ist
     * 
     * @return callcode
     */
    public int getCallCode() {
        return lastCallCode;
    }

    public ConfigContainer getConfig() {
        if (config == null) {
            config = new ConfigContainer(this);
            initConfig();
        }

        return config;
    }

    public abstract String getInteractionName();

    /**
     * Gibt die Interaction ID zurück bei diese Interactiona aktiv wird
     * 
     * @return Interaction ID
     */
    public InteractionTrigger getTrigger() {
        return trigger;
    }

    /**
     * Gibt den namen des EventTriggers zurück
     * 
     * @return Name des Triggers
     */
    public String getTriggerName() {
        return getTrigger().toString();
    }

    public boolean getWaitForTermination() {
        return true;
    }

    /**
     * Da die Knfigurationswünsche nicht gespeichert werden, muss der
     * ConfigContainer immer wieder aufs neue Initialisiert werden. Alle
     * Interactionen müssend azu die initConifg Methode implementieren
     */
    public abstract void initConfig();

    /**
     * Initialisiert die Interaction beim JD start
     */
    public void initInteraction() {
        // nothing to init
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
        setCallCode(Interaction.INTERACTION_CALL_RUNNING);
        boolean success = doInteraction(arg);
        if (!isAlive()) {
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
     * Gibt an ob der Thread aktiv ist
     * 
     * @return
     */
    public boolean isAlive() {
        if (thread == null) { return false; }
        return thread.isAlive();
    }

    /**
     * Setzt eine INteraction in den Ausgangszustand zurück. z.B. Counter
     * zurückstellen etc.
     */
    public abstract void resetInteraction();

    /**
     * Thread Funktion. Diese Funktion wird aufgerufen wenn Interaction.start()
     * aufgerufen wird. Dabei wird ein neuer thread erstellt
     */
    public abstract void run();

    /**
     * Wird vom neuen Thread aufgerufen, setzt die ThreadVariable
     */
    private void runThreadAction() {
        run();
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
     * Setzt den callCode
     * 
     * @param callCode
     */
    public void setCallCode(int callCode) {
        lastCallCode = callCode;
    }

    public void setConfig(ConfigContainer config) {
        this.config = config;
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
     * Erstellt einen neuen Thread und führt den zugehörigen Code aus (run()
     */
    protected void start() {
        final Interaction _this = this;
        thread = new Thread() {
            @Override
            public void run() {
                _this.runThreadAction();
            }
        };
        thread.start();
    }

    @Override
    public abstract String toString();

}
