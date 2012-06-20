package jd;

import org.appwork.javaexe.JavaExe_SystemEventManagement;
import org.appwork.javaexe.LogOffTyp;
import org.appwork.javaexe.PowerBroadcastEvent;
import org.appwork.javaexe.SystemEventHandler;
import org.appwork.utils.swing.dialog.Dialog;

/**
 * Class for javaexe launcher
 * 
 * @author Thomas
 * 
 */
public class Main_SystemEventManagement extends JavaExe_SystemEventManagement implements SystemEventHandler {
    private static final Main_SystemEventManagement INSTANCE = new Main_SystemEventManagement();

    /**
     * get the only existing instance of Main_SystemEventManagement. This is a singleton
     * 
     * @return
     */
    public static Main_SystemEventManagement getInstance() {
        return Main_SystemEventManagement.INSTANCE;
    }

    /**
     * Create a new instance of Main_SystemEventManagement. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private Main_SystemEventManagement() {
        super();
    }

    public static int notifyEvent(int msg, int val1, int val2, String val3, int[] arr1, byte[] arr2) {
        return getInstance().onEvent(msg, val1, val2, val3, arr1, arr2);
    }

    @Override
    protected SystemEventHandler getHandler() {
        return this;
    }

    @Override
    public boolean onQueryEndSession(LogOffTyp logOffTyp) {
        return true;
    }

    @Override
    public void onSessionEnd(boolean b, LogOffTyp logOffTyp) {
    }

    @Override
    public void onDisplayChange(int w, int h, int val1) {

        Dialog.getInstance().showMessageDialog("Display: " + w + "x" + h + " - " + val1 + " bits/pixel");
    }

    @Override
    public void onScreenSaverState(boolean b) {
    }

    @Override
    public void onPowerBroadcast(PowerBroadcastEvent ev) {
    }

    @Override
    public boolean onQuerySuspend(boolean b) {
        return true;
    }

    @Override
    public void onPowerStatusChanged(boolean ac, byte chargingStatus, String percentageCharging, int secondsLeft, int secondsTotal) {
    }

    @Override
    public void onOEMEvent(int val2) {
    }

    @Override
    public void onCompacting(double percent) {
    }

}
