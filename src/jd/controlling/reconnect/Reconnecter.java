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

package jd.controlling.reconnect;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDLogger;
import jd.controlling.LinkCheck;
import jd.controlling.ProgressController;
import jd.controlling.reconnect.ipcheck.IPController;
import jd.gui.UserIF;
import jd.gui.UserIO;
import jd.utils.JDUtilities;

import org.appwork.controlling.State;
import org.appwork.controlling.StateMachine;
import org.appwork.controlling.StateMachineInterface;
import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.storage.StorageEvent;
import org.appwork.storage.StorageKeyAddedEvent;
import org.appwork.storage.StorageValueChangeEvent;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.event.DefaultEventSender;
import org.jdownloader.translate._JDT;

public final class Reconnecter implements StateMachineInterface {
    public static final String         RECONNECT_SUCCESS_COUNTER        = "RECONNECT_SUCCESS_COUNTER";
    public static final String         RECONNECT_FAILED_COUNTER         = "RECONNECT_FAILED_COUNTER";
    public static final State          IDLE                             = new State("IDLE");
    public static final State          RECONNECT_RUNNING                = new State("RECONNECT_RUNNING");
    // private static final State RECONNECT_REQUESTED = new
    // State("RECONNECT_REQUESTED");

    static {
        Reconnecter.IDLE.addChildren(Reconnecter.RECONNECT_RUNNING);

        Reconnecter.RECONNECT_RUNNING.addChildren(Reconnecter.IDLE);

    }
    private static final Reconnecter   INSTANCE                         = new Reconnecter();

    private static final Logger        LOG                              = JDLogger.getLogger();
    public static final String         RECONNECT_FAILED_COUNTER_GLOBAL  = "RECONNECT_FAILED_COUNTER_GLOBAL";
    public static final String         RECONNECT_SUCCESS_COUNTER_GLOBAL = "RECONNECT_SUCCESS_COUNTER_GLOBAL";

    private static final AtomicInteger reconnectCounter                 = new AtomicInteger(0);
    private static long                lastReconnect                    = 0;

    /*
     * TODO: eyxternal IP check if automode is disabled
     */

    // public static boolean doReconnectIfRequestedInternal(final boolean doit)
    // {
    // boolean ret = false;
    // final Configuration configuration = JDUtilities.getConfiguration();
    // /* überhaupt ein reconnect angefragt? */
    // if (Reconnecter.isReconnectRequested()) {
    // if (!doit &&
    // !configuration.getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT,
    // true)) {
    // /*
    // * auto reconnect ist AUS, dann nur noch schaun ob sich ip
    // * geändert hat
    // */
    // if (System.currentTimeMillis() - Reconnecter.LAST_UP_UPDATE_TIME > 1000 *
    // 60 *
    // JSonWrapper.get("DOWNLOAD").getIntegerProperty("EXTERNAL_IP_CHECK_INTERVAL2",
    // 10)) { return Reconnecter.checkExternalIPChange(); }
    // return false;
    //
    // } else {
    // /* auto reconnect ist AN */
    // try {
    // ret = Reconnecter.doReconnect();
    // if (ret) {
    // Reconnecter.LOG.info("Reconnect successful!");
    // } else {
    // Reconnecter.LOG.info("Reconnect failed!");
    // }
    // } catch (final Exception e) {
    // Reconnecter.LOG.finest("Reconnect failed.");
    // }
    // if (ret == false) {
    // /* reconnect failed, increase fail counter */
    // final ProgressController progress = new
    // ProgressController(JDL.L("jd.controlling.reconnect.Reconnector.progress.failed",
    // "Reconnect failed! Please check your reconnect Settings and try a Manual Reconnect!"),
    // 100, "reconnect_warning");
    // progress.doFinalize(10000l);
    // final int counter =
    // configuration.getIntegerProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER,
    // 0) + 1;
    // configuration.setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER,
    // counter);
    // if (counter > 5) {
    // /*
    // * more than 5 failed reconnects in row, disable
    // * autoreconnect and show message
    // */
    // configuration.setProperty(Configuration.PARAM_RECONNECT_OKAY, false);
    // configuration.setProperty(Configuration.PARAM_ALLOW_RECONNECT, false);
    // UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN |
    // UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL,
    // JDL.L("jd.controlling.reconnect.Reconnector.progress.failed2",
    // "Reconnect failed too often! Autoreconnect is disabled! Please check your reconnect Settings!"));
    // configuration.setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER,
    // 0);
    // }
    // configuration.save();
    // } else {
    // /* reconnect okay, reset fail counter */
    // configuration.setProperty(Configuration.PARAM_RECONNECT_FAILED_COUNTER,
    // 0);
    // configuration.setProperty(Configuration.PARAM_RECONNECT_OKAY, true);
    // configuration.save();
    // }
    //
    // }
    // }
    // return ret;
    // }

    /**
     * @return the reconnectcounter
     */
    public static int getReconnectCounter() {
        return reconnectCounter.get();
    }

    public static long getLastReconnect() {
        return lastReconnect;
    }

    public static Reconnecter getInstance() {
        return Reconnecter.INSTANCE;
    }

    /**
     * can be removed after next stable
     * 
     * @param i
     * @param b
     * @return
     */
    public static boolean waitForNewIP(final int i, final boolean b) {
        return Reconnecter.getInstance().forceReconnect();
    }

    private final DefaultEventSender<ReconnecterEvent> eventSender;
    private final StateMachine                         statemachine;

    private final Storage                              storage;

    private Reconnecter() {
        this.eventSender = new DefaultEventSender<ReconnecterEvent>();
        this.statemachine = new StateMachine(this, Reconnecter.IDLE, Reconnecter.IDLE);
        this.storage = JSonStorage.getPlainStorage("RECONNECT");
        // propagate
        this.storage.getEventSender().addListener(new DefaultEventListener<StorageEvent<?>>() {

            public void onEvent(final StorageEvent<?> event) {
                // TODO Auto-generated method stub
                if (event instanceof StorageValueChangeEvent<?> || event instanceof StorageKeyAddedEvent<?>) {
                    Reconnecter.this.eventSender.fireEvent(new ReconnecterEvent(ReconnecterEvent.SETTINGS_CHANGED, event));
                }
            }

        });
    }

    /**
     * Führt einen Reconnect durch.
     * 
     * @return <code>true</code>, wenn der Reconnect erfolgreich war, sonst
     *         <code>false</code>
     */
    public boolean doReconnect() {
        if (!this.statemachine.isStartState()) { return false; }
        this.eventSender.fireEvent(new ReconnecterEvent(ReconnecterEvent.BEFORE));

        Reconnecter.LOG.info("Try to reconnect...");
        this.statemachine.setStatus(Reconnecter.RECONNECT_RUNNING);
        DownloadWatchDog.getInstance().abortAllSingleDownloadControllers();

        int retry;
        int maxretries = JDUtilities.getConfiguration().getIntegerProperty(Configuration.PARAM_RETRIES, 5);
        boolean ret = false;
        retry = 0;
        if (maxretries < 0) {
            maxretries = Integer.MAX_VALUE;
        } else if (maxretries == 0) {
            maxretries = 1;
        }
        final ProgressController progress = new ProgressController(this.toString(), maxretries + 10, "reconnect");
        progress.increase(5);
        IPController.getInstance().invalidate();
        try {
            for (retry = 0; retry < maxretries; retry++) {
                ReconnectPluginController.LOG.info("Starting " + this.toString() + " #" + (retry + 1));
                progress.increase(1);
                progress.setStatusText(_JDT._.jd_controlling_reconnect_plugins_ReconnectPluginController_doReconnect_1() + (retry + 1));
                ret = ReconnectPluginController.getInstance().doReconnect();
                if (ret) {
                    reconnectCounter.incrementAndGet();
                    lastReconnect = System.currentTimeMillis();
                    break;
                }
            }

        } catch (final InterruptedException e) {
            e.printStackTrace();
            ret = false;
        } catch (final ReconnectException e) {
            e.printStackTrace();
            ret = false;
        } finally {
            progress.doFinalize(1000);
        }

        this.eventSender.fireEvent(new ReconnecterEvent(ReconnecterEvent.AFTER, ret));

        this.statemachine.setStatus(Reconnecter.IDLE);
        if (!ret) {
            this.storage.increase(Reconnecter.RECONNECT_FAILED_COUNTER);
            this.storage.increase(Reconnecter.RECONNECT_FAILED_COUNTER_GLOBAL);
            this.storage.put(Reconnecter.RECONNECT_SUCCESS_COUNTER, 0);

        } else {
            this.storage.increase(Reconnecter.RECONNECT_SUCCESS_COUNTER);
            this.storage.increase(Reconnecter.RECONNECT_SUCCESS_COUNTER_GLOBAL);
            this.storage.put(Reconnecter.RECONNECT_FAILED_COUNTER, 0);
        }
        return ret;
    }

    /**
     * Forces a reconnect NOW. This method will try to do a reconnect at any
     * cost and return the success
     * 
     * @return
     */
    public boolean forceReconnect() {
        final boolean ret = this.doReconnect();

        return ret;
    }

    public DefaultEventSender<ReconnecterEvent> getEventSender() {
        return this.eventSender;
    }

    public StateMachine getStateMachine() {
        return this.statemachine;
    }

    public Storage getStorage() {
        return this.storage;
    }

    /**
     * returns true if auto reconnect is enabled
     * 
     * @return
     */
    public boolean isAutoReconnectEnabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
    }

    /**
     * cheks if a reconnect is allowed right now.
     * 
     * @return
     */
    public boolean isReconnectAllowed() {
        boolean ret = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true);
        ret &= !LinkCheck.getLinkChecker().isRunning();
        ret &= DownloadWatchDog.getInstance().getForbiddenReconnectDownloadNum() == 0;
        return ret;
    }

    /**
     * Returns if there is a reconnect in progress
     * 
     * @return
     */
    public boolean isReconnectInProgress() {
        return this.statemachine.isState(Reconnecter.RECONNECT_RUNNING);
    }

    /**
     * should be called externally when a connection break is ok. this method
     * has to block until the connection is back
     */
    public boolean run() {
        if (!this.isReconnectAllowed()) { return false; }
        // no reconnect required
        if (!IPController.getInstance().isInvalidated()) { return false; }
        boolean ret = false;
        try {
            ret = this.doReconnect();
            if (ret) {
                Reconnecter.LOG.info("Reconnect successful!");
            } else {
                Reconnecter.LOG.info("Reconnect failed!");
            }
        } catch (final Exception e) {
            Reconnecter.LOG.finest("Reconnect failed.");
        }
        if (ret == false) {
            /* reconnect failed, increase fail counter */
            final ProgressController progress = new ProgressController(_JDT._.jd_controlling_reconnect_Reconnector_progress_failed(), 100, "reconnect_warning");
            progress.doFinalize(10000l);

            final long counter = this.storage.get(Reconnecter.RECONNECT_FAILED_COUNTER, 0);

            if (counter > 5) {
                /*
                 * more than 5 failed reconnects in row, disable autoreconnect
                 * and show message
                 */

                this.setAutoReconnectEnabled(false);

                UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN | UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL, _JDT._.jd_controlling_reconnect_Reconnector_progress_failed2());

            }

        }
        return ret;
    }

    /**
     * Enables or disables autoreconnection
     * 
     * @param b
     */
    public void setAutoReconnectEnabled(final boolean b) {

        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_ALLOW_RECONNECT, b);
        if (!b) {

            UserIF.getInstance().displayMiniWarning(_JDT._.gui_warning_reconnect_hasbeendisabled(), _JDT._.gui_warning_reconnect_hasbeendisabled_tooltip());

        }
        JDUtilities.getConfiguration().save();
    }

}