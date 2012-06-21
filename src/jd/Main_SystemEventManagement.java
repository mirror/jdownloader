package jd;

import org.appwork.javaexe.ConsoleEventType;
import org.appwork.javaexe.DeviceChangeType;
import org.appwork.javaexe.DeviceType;
import org.appwork.javaexe.JavaExe_SystemEventManagement;
import org.appwork.javaexe.LogOffTyp;
import org.appwork.javaexe.NetworkType;
import org.appwork.javaexe.PowerBroadcastEvent;
import org.appwork.javaexe.SessionEvent;
import org.appwork.javaexe.SystemEventHandler;
import org.appwork.shutdown.ShutdownController;

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
        System.out.println("SystemEvent..... " + msg);
        while (!Launcher.GUI_COMPLETE.isReached()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return getInstance().onEvent(msg, val1, val2, val3, arr1, arr2);
    }

    @Override
    protected SystemEventHandler getHandler() {
        return this;
    }

    @Override
    public boolean onQueryEndSession(LogOffTyp logOffTyp) {

        return ShutdownController.getInstance().requestShutdown(false);
    }

    @Override
    public void onSessionEnd(boolean queryResponse, LogOffTyp logOffTyp) {
        System.out.println("SessuinEnd: REsult: " + queryResponse + " Type: " + logOffTyp);
    }

    @Override
    public void onDisplayChange(int w, int h, int val1) {

        System.out.println("Display: " + w + "x" + h + " - " + val1 + " bits/pixel");
    }

    @Override
    public void onScreenSaverState(boolean b) {
        System.out.println("Screensaver Active: " + b);
    }

    @Override
    public void onPowerBroadcast(PowerBroadcastEvent ev) {
        System.out.println("Powerbroadcast: " + ev);
    }

    @Override
    public boolean onQuerySuspend(boolean b) {
        System.out.println("Suspend system: " + b);
        return true;
    }

    @Override
    public void onPowerStatusChanged(boolean ac, byte chargingStatus, String percentageCharging, int secondsLeft, int secondsTotal) {
        System.out.println("PowerStatus: AC:" + ac + " Charging: " + chargingStatus + " " + percentageCharging + "% seconds left: " + secondsLeft + "/" + secondsTotal);
    }

    @Override
    public void onOEMEvent(int val2) {
        System.out.println("OEM " + val2);
    }

    @Override
    public void onCompacting(double percent) {
        System.out.println("Compacting RAM " + percent);
    }

    @Override
    public void onSessionChange(SessionEvent sessionEvent, int sessionid, String username, boolean current) {
        System.out.println("Session CHanged: " + sessionEvent + " " + sessionid + " " + username + " " + current);
    }

    @Override
    public void onDeviceChangeEvent(DeviceChangeType deviceChangeType, DeviceType deviceType) {
        System.out.println("DeviceChange: " + deviceChangeType + " - " + deviceChangeType);
    }

    @Override
    public boolean onDeviceRemoveQuery(DeviceType deviceType) {
        System.out.println("Remove ? " + deviceType);
        return true;
    }

    @Override
    public boolean onDeviceConfigChangeQuery() {
        System.out.println("onDeviceConfigChangeQuery");
        return true;
    }

    @Override
    public void onDeviceConfigChange() {
        System.out.println("onDeviceConfigChange");
    }

    @Override
    public void onDeviceConfigChangeCanceled() {
        System.out.println("onDeviceConfigChangeCanceled");
    }

    @Override
    public void onNetworkConnected(String device, NetworkType networkType, String ip, String gateway, String mask) {
        System.out.println("Network: " + device + " " + networkType + " IP:" + ip + " gateway:" + gateway + " Mask:" + mask);
    }

    @Override
    public void onNetworkDisconnect(String device) {
        System.out.println("Network disconnect: " + device);
    }

    @Override
    public void onNetworkConnecting(String device) {
        System.out.println("Network connecting: " + device);
    }

    @Override
    public boolean onConsoleEvent(ConsoleEventType type) {
        System.out.println(type);
        return true;
    }

}
