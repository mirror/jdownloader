//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.event.ControlEvent;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Mit dieser Klasse werden Interaktionen (mit dem System) umgesetzt
 * 
 * @author astaldo
 */
public abstract class Interaction extends Property implements Serializable {

    public static InteractionTrigger INTERACTION_NO_EVENT;
    public static InteractionTrigger INTERACTION_SINGLE_DOWNLOAD_FINISHED;
    public static InteractionTrigger INTERACTION_ALL_DOWNLOADS_FINISHED;
    public static InteractionTrigger INTERACTION_DOWNLOAD_FAILED;
    public static InteractionTrigger INTERACTION_LINKLIST_STRUCTURE_CHANGED;
    // public static InteractionTrigger INTERACTION_DOWNLOAD_BOT_DETECTED;
    public static InteractionTrigger INTERACTION_APPSTART;
    // public static InteractionTrigger INTERACTION_AFTER_UNRAR;
    public static InteractionTrigger INTERACTION_DOWNLOAD_PACKAGE_FINISHED;
    public static InteractionTrigger INTERACTION_BEFORE_RECONNECT;
    public static InteractionTrigger INTERACTION_AFTER_RECONNECT;
    public static InteractionTrigger INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS;
    public static InteractionTrigger INTERACTION_CONTAINER_DOWNLOAD;
    public static InteractionTrigger INTERACTION_BEFORE_DOWNLOAD;
    public static InteractionTrigger INTERACTION_EXIT;

    private static Integer interactionsRunning = 0;

    protected static Logger logger = JDUtilities.getLogger();

    private transient static final long serialVersionUID = -5609631258725998799L;

    /**
     * Gibt eine Liste aller vefügbaren Interactions zurück. Bei neuen
     * Interactions muss diese hier eingefügt werden
     * 
     * @return Liste mit allen Interactionen
     */
    public static Interaction[] getInteractionList() {
        return new Interaction[] { new SimpleExecute(), new ExternExecute(), new JDExit(), new ResetLink() };
    }

    public static void initTriggers() {
        INTERACTION_NO_EVENT = new InteractionTrigger(0, JDLocale.L("interaction.trigger.no_event", "Kein Event"), JDLocale.L("interaction.trigger.no_event.desc", "kein Event"));
        INTERACTION_SINGLE_DOWNLOAD_FINISHED = new InteractionTrigger(1, JDLocale.L("interaction.trigger.download_successfull", "Download erfolgreich beendet"), JDLocale.L("interaction.trigger.download_successfull.desc", "Wird aufgerufen sobald ein Download erfolgreich beendet wurde"));
        INTERACTION_ALL_DOWNLOADS_FINISHED = new InteractionTrigger(2, JDLocale.L("interaction.trigger.all_downloads_finished", "Alle Downloads beendet"), JDLocale.L("interaction.trigger.all_downloads_finished.desc", "Wird aufgerufen sobald alle Downloads beendet oder abgebrochen wurden"));
        INTERACTION_DOWNLOAD_FAILED = new InteractionTrigger(3, JDLocale.L("interaction.trigger.single_download_failed", "Download fehlgeschlagen"), JDLocale.L("interaction.trigger.single_download_failed.desc", "Wird aufgerufen wenn ein Download wegen Fehlern abgebrochen wurde"));
        INTERACTION_LINKLIST_STRUCTURE_CHANGED = new InteractionTrigger(4, JDLocale.L("interaction.trigger.linklist_structure_changed", "Linklisten Struktur geändert"), JDLocale.L("interaction.trigger.linklist_structure_changed.desc", "Wird aufgerufen wenn sich die Struktur der Linkliste geändert hat (Links eingefügt, Links fertig, ...)"));
        // INTERACTION_DOWNLOAD_BOT_DETECTED = new InteractionTrigger(5,
        // JDLocale.L("interaction.trigger.bot_detected", "Bot erkannt"),
        // JDLocale.L("interaction.trigger.bot_detected.desc",
        // "jDownloader wurde als Bot erkannt"));
        INTERACTION_APPSTART = new InteractionTrigger(7, JDLocale.L("interaction.trigger.app_start", "Programmstart"), JDLocale.L("interaction.trigger.app_start.desc", "Direkt nach dem Initialisieren von jDownloader"));
        // INTERACTION_AFTER_UNRAR = new InteractionTrigger(9,
        // JDLocale.L("interaction.trigger.after_extract",
        // "Nach dem Entpacken"),
        // JDLocale.L("interaction.trigger.after_extract.desc",
        // "Wird aufgerufen wenn die Unrar-Aktion beendet wurde."));
        INTERACTION_DOWNLOAD_PACKAGE_FINISHED = new InteractionTrigger(12, JDLocale.L("interaction.trigger.package_finished", "Paket fertig"), JDLocale.L("interaction.trigger.package_finished.desc", "Wird aufgerufen wenn ein Paket fertig geladen wurde"));
        INTERACTION_BEFORE_RECONNECT = new InteractionTrigger(13, JDLocale.L("interaction.trigger.before_reconnect", "Vor dem Reconnect"), JDLocale.L("interaction.trigger.before_reconnect.desc", "Vor dem eigentlichen Reconnect"));
        INTERACTION_AFTER_RECONNECT = new InteractionTrigger(14, JDLocale.L("interaction.trigger.after_reconnect", "Nach dem Reconnect"), JDLocale.L("interaction.trigger.after_reconnect.desc", "Nach dem eigentlichen Reconnect"));
        INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS = new InteractionTrigger(15, JDLocale.L("interaction.trigger.downloads_and_interactions_finished", "Downloads & Interactionen abgeschlossen"), JDLocale.L("interaction.trigger.downloads_and_interactions_finished.desc", "Wird aufgerufen wenn alle Downloads und alle Interactionen beendet sind."));
        INTERACTION_CONTAINER_DOWNLOAD = new InteractionTrigger(16, JDLocale.L("interaction.trigger.container_download", "Linkcontainer geladen"), JDLocale.L("interaction.trigger.download_download.desc", "Wird aufgerufen wenn ein LinkContainer(DLC,RSDF,CCF,...) geladen wurde"));
        INTERACTION_BEFORE_DOWNLOAD = new InteractionTrigger(17, JDLocale.L("interaction.trigger.before_download", "Vor einem Download"), JDLocale.L("interaction.trigger.before_download.desc", "Wird aufgerufen bevor ein neuer Download gestartet wird"));
        INTERACTION_EXIT = new InteractionTrigger(20, JDLocale.L("interaction.trigger.exit", "JD wird beendet"), JDLocale.L("interaction.trigger.exit.desc", "Wird beim Beenden vor dem Schließen des Programms aufgerufen"));
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
     * 
     * @return Anzahl der gerade aktiven Interactionen
     */
    public synchronized static boolean areInteractionsInProgress() {
        return interactionsRunning == 0;
    }

    /**
     * Führt die Interactions aus
     * 
     * @param trigger
     *            Trigger der Interaction
     * @param param
     *            Parameter
     */
    @SuppressWarnings("unchecked")
    public static void handleInteraction(InteractionTrigger trigger, Object param) {
        logger.finer("Interaction start: Trigger: " + trigger.getName());
        JDUtilities.getController().fireControlEvent(new ControlEvent(trigger, ControlEvent.CONTROL_INTERACTION_CALL, param));
        Vector<Interaction> interactions = (Vector<Interaction>) JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS, new Vector<Interaction>());
        for (Interaction interaction : interactions) {
            if (interaction == null || interaction.getTrigger() == null) continue;

            if (interaction.getTrigger().getID() == trigger.getID()) {
                logger.finer("Aktion start: " + interaction.getInteractionName() + "(" + param + ")");
                if (!interaction.interact(param)) {
                    logger.severe("interaction failed: " + interaction);
                } else {
                    logger.info("interaction successfull: " + interaction);
                }
            }
        }
        if (trigger.equals(Interaction.INTERACTION_ALL_DOWNLOADS_FINISHED) && areInteractionsInProgress() && JDUtilities.getController().getFinishedLinks().size() > 0) {
            Interaction.handleInteraction(Interaction.INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS, null);
        }
    }

    protected transient ConfigContainer config;

    @Deprecated
    protected transient int lastCallCode = 0;

    @Deprecated
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

    public final ConfigContainer getConfig() {
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
    public final InteractionTrigger getTrigger() {
        return trigger;
    }

    /**
     * Gibt den namen des EventTriggers zurück
     * 
     * @return Name des Triggers
     */
    public final String getTriggerName() {
        return trigger.toString();
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
    @Deprecated
    public void initInteraction() {
        // Kann eigentlich entfernt werden, oder?
    }

    /**
     * ruft die doInteraction Funktion auf. Und setzt das Ergebnis als callCode.
     * Der Statuscode kann mit getCallCode abgerufen werden
     * 
     * @param arg
     * @return
     */
    public final boolean interact(Object arg) {
        synchronized (interactionsRunning) {
            interactionsRunning++;
        }
        logger.finer("Interaction start: " + interactionsRunning + " - " + this);

        fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, arg);
        boolean success = doInteraction(arg);
        fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, arg);

        synchronized (interactionsRunning) {
            interactionsRunning--;
        }
        logger.info("Interaction finished: " + interactionsRunning + " - " + this);

        return success;
    }

    /**
     * Setzt die Interaction ID (event ID)
     * 
     * @param trigger
     *            Der Trigger
     */
    public final void setTrigger(InteractionTrigger trigger) {
        this.trigger = trigger;
    }

    @Override
    public abstract String toString();

}
