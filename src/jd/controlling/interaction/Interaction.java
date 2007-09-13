package jd.controlling.interaction;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import jd.JDUtilities;
import jd.Property;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.plugins.Plugin;
import jd.plugins.event.PluginEvent;

/**
 * Mit dieser Klasse werden Interaktionen (mit dem System) umgesetzt
 * 
 * @author astaldo
 */
public abstract class Interaction extends Property implements Serializable {
    /**
     * serialVersionUID
     */
    private transient static final long       serialVersionUID                   = -5609631258725998799L;

    protected transient static Logger         logger                             = Plugin.getLogger();

  
    /**
     * Gibt das Event an bei dem Diese INteraction aktiv wird
     */
    private InteractionTrigger                trigger;
    
    /**
     * THread der für die Interaction verwendet werden kann
     */

    protected transient Thread                 thread                             = null;

    /**
     * Hiermit wird der Eventmechanismus realisiert. Alle hier eingetragenen
     * Listener werden benachrichtigt, wenn mittels
     * {@link #firePluginEvent(PluginEvent)} ein Event losgeschickt wird.
     */
    private transient Vector<ControlListener> controlListener                    = null;

    /**
     * Code der abgerufe werden kann um details über den Ablauf der Interaction
     * zu kriegen
     */
    protected transient int                    lastCallCode                       = 0;

    /**
     * Zeigt an dass diese Interaction noch nie aufgerufen wurde
     */
    public transient final static int         INTERACTION_CALL_NEVERCALLED       = 0;

    /**
     * Zeigt an dass die INteraction erfolgreioch beendet wurde
     */
    public transient final static int         INTERACTION_CALL_SUCCESS           = 1;

    /**
     * Zeigt an dass die Interaction mit Fehlern beendet wurde
     */
    public transient final static int         INTERACTION_CALL_ERROR             = 2;

    /**
     * Zeigt dass die INteraction gerade läuft
     */
    public transient final static int         INTERACTION_CALL_RUNNING           = 3;

    // Download IDS

    /**
     * Zeigt an, daß ein einzelner Download beendet wurde
     */
    public static InteractionTrigger          INTERACTION_NO_EVENT               = new InteractionTrigger(0, "Kein Event", "kein Event");

    /**
     * Zeigt an, daß ein einzelner Download beendet wurde
     */
    public static InteractionTrigger          INTERACTION_SINGLE_DOWNLOAD_FINISHED      = new InteractionTrigger(1, "Download beendet", "Wird aufgerufen sobald ein Download beendet wurde");

    /**
     * Zeigt an, daß alle Downloads abgeschlossen wurden
     */
    public static InteractionTrigger          INTERACTION_ALL_DOWNLOADS_FINISHED = new InteractionTrigger(2, "Alle Downloads beendet", "Wird aufgerufen sobald alle Downloads beendet oder abgebrochen wurden");

    /**
     * Zeigt, daß ein einzelner Download nicht fertiggestellt werden konnte
     */
    public static InteractionTrigger          INTERACTION_DOWNLOAD_FAILED        = new InteractionTrigger(3, "Download fehlgeschlagen", "Wird aufgerufen wenn ein Download wegen Fehlern abgebrochen wurde");

    /**
     * Zeigt, daß ein einzelner Download wegen Wartezeit nicht starten konnte
     */
    public static InteractionTrigger          INTERACTION_DOWNLOAD_WAITTIME      = new InteractionTrigger(4, "Download hat Wartezeit", "Das Plugin meldet eine Wartezeit");

    /**
     * Zeigt, daß ein der Bot erkannt wurde
     */
    public static InteractionTrigger          INTERACTION_DOWNLOAD_BOT_DETECTED  = new InteractionTrigger(5, "Bot erkannt", "jDownloader wurde als Bot erkannt");

    /**
     * Zeigt, daß ein Captcha erkannt werden will
     */
    public static InteractionTrigger          INTERACTION_DOWNLOAD_CAPTCHA       = new InteractionTrigger(6, "Captcha Erkennung", "Ein Captcha-Bild muss verarbeitet werden");

    /**
     * Zeigt den Programmstart an
     */
    public static InteractionTrigger          INTERACTION_APPSTART               = new InteractionTrigger(7, "Programmstart", "Direkt nach dem Initialisieren von jDownloader");

    /**
     * Zeigt den Programmende an
     */
    public static InteractionTrigger          INTERACTION_APPTERMINATE           = new InteractionTrigger(8, "Programmende", "inaktiv");

    /**
     * Zeigt, dass vermutlich JAC veraltet ist
     */
    public static InteractionTrigger          INTERACTION_JAC_UPDATE_NEEDED      = new InteractionTrigger(9, "Captcha Update nötig", "inaktiv");

    /**
     * Nach einem IP wechsel
     */

    public final static InteractionTrigger    INTERACTION_AFTER_RECONNECT        = new InteractionTrigger(10, "Nach einem Reconnect", "inaktiv");

    /**
     * Reconnect nötig
     */
    public static InteractionTrigger          INTERACTION_NEED_RECONNECT         = new InteractionTrigger(11, "Reconnect nötig", "Alle Trigger bei denen ein Reconnect sinnvoll ist zusammengefasst");

    public Interaction() {
        controlListener = new Vector<ControlListener>();
        this.setTrigger(Interaction.INTERACTION_NO_EVENT);
      
    }

    public abstract boolean doInteraction(Object arg);

    public abstract String toString();

    public abstract String getInteractionName();

    /**
     * Thread Funktion. Diese Funktion wird aufgerufen wenn INteraction.start()
     * aufgerufen wird. Dabei wird ein neuer thread erstellt
     */
    public abstract void run();

    /**
     * Gibt den callcode zurück. Dieser gibt Aufschlussdarüber wie die
     * INteraction abgelaufen ist
     * 
     * @return callcode
     */
    public int getCallCode() {
        return lastCallCode;
    }

    /**
     * ruft die doInteraction Funktion auf. Und setzt das Ergebnis als callCode.
     * Der Statuscode kann mit getCallCode abgerufen werden
     * 
     * @param arg
     * @return
     */
    public boolean interact(Object arg) {
     
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_ACTIVE, this));
        this.setCallCode(Interaction.INTERACTION_CALL_RUNNING);
        boolean success = doInteraction(arg);
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_RETURNED, this));
        if (!this.isAlive()) {

            fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE, this));
        }
        return success;
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
     * Fügt einen Listener hinzu
     * 
     * @param listener Ein neuer Listener
     */
    public void addControlListener(ControlListener listener) {
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        if (controlListener.indexOf(listener) == -1) {
            controlListener.add(listener);
        }
    }

    /**
     * Emtfernt einen Listener
     * 
     * @param listener Der zu entfernende Listener
     */
    public void removeControlListener(ControlListener listener) {
        controlListener.remove(listener);
    }

    /**
     * Verteilt Ein Event an alle Listener
     * 
     * @param controlEvent ein abzuschickendes Event
     */
    public void fireControlEvent(ControlEvent controlEvent) {
        if (controlListener == null) controlListener = new Vector<ControlListener>();
        Iterator<ControlListener> iterator = controlListener.iterator();
        while (iterator.hasNext()) {
            ((ControlListener) iterator.next()).controlEvent(controlEvent);
        }
    }

    /**
     * Wird vom neuen Thread aufgerufen, setzt die ThreadVariable
     */
    private void runThreadAction() {
        this.run();
        fireControlEvent(new ControlEvent(this, ControlEvent.CONTROL_PLUGIN_INTERACTION_INACTIVE, this));
    }

    /**
     * Gibt an ob der Thread Aktiv ist
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
     * Führt die Interactionen aus
     * 
     * @param param Parameter für die Interactionen
     * @param interactionID Interactionen
     * 
     * @return
     */
    public static boolean handleInteraction(InteractionTrigger interactionevent, Object param) {
        boolean ret = true;
        Vector<Interaction> interactions = JDUtilities.getConfiguration().getInteractions(); 
               int interacts=0; 
               for (int i = 0; i < interactions.size(); i++) { 
      Interaction interaction = interactions.get(i); 
            if (interaction == null || interaction.getTrigger() == null || interactionevent == null) continue;
            if (interaction.getTrigger().getID() == interactionevent.getID()) {
                interaction.addControlListener(JDUtilities.getController());
                interacts++;
                if (!interaction.interact(param)) {
                    ret = false;
                    logger.severe("interaction failed: " + interaction);
                }
                else {
                    logger.info("interaction successfull: " + interaction);
                }
            }

        }
        if (interacts == 0) return false;
        return ret;

    }

    /**
     * Führt nur die i-te INteraction aus
     * 
     * @param param Parameter für die Interactionen
     * @param interactionID Interactionen *
     * @param i I-te INteraction
     * 
     * @return
     */
    public static boolean handleInteraction(InteractionTrigger interactionevent, Object param, int id) {

        Vector<Interaction> interactions = JDUtilities.getConfiguration().getInteractions();

        for (int i = 0; i < interactions.size(); i++) {
            Interaction interaction = interactions.get(i);
            if (interaction == null || interaction.getTrigger() == null || interactionevent == null) continue;
            if (interaction.getTrigger().getID() == interactionevent.getID()) {
                if (id == 0) {
                    interaction.addControlListener(JDUtilities.getController());
                    if (!interaction.interact(param)) {

                        logger.severe("interaction failed: " + interaction);
                        return false;

                    }
                    else {
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
 * Gibt alle Interactionen zum trigger zurück
 * @param trigger
 * @return
 */
    public static Interaction[] getInteractions(InteractionTrigger trigger) {
        Vector<Interaction> interactions = JDUtilities.getConfiguration().getInteractions();
        Vector<Interaction> ret = new Vector<Interaction>();
        for (int i = 0; i < interactions.size(); i++) {
            if (interactions.get(i).getTrigger().getID() == trigger.getID()) {
                ret.add(interactions.get(i));
            }
        }
        return ret.toArray(new Interaction[] {});
    }

    /**
     * Gibt die Interaction ID zurück bei diese INteractiona aktiv wird
     * 
     * @return Interaction ID
     */
    public InteractionTrigger getTrigger() {
        return trigger;
    }

    /**
     * Setzt die INteraction ID (event ID)
     * 
     * @param eventID
     */
    public void setTrigger(InteractionTrigger trigger) {
        this.trigger = trigger;
    }

    /**
     * Gibt den namen des EventTriggers zurück
     * 
     * @return
     */
    public String getTriggerName() {
        return getTrigger().toString();
    }

  

  
}
