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
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

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

    private LogSource logger;

    /**
     * Create a new instance of Main_SystemEventManagement. This is a singleton class. Access the only existing instance by using {@link #getInstance()}.
     */
    private Main_SystemEventManagement() {
        super();
        logger = LogController.getInstance().getLogger("WindowsSystemEventHandling");
        logger.setInstantFlush(true);
    }

    public static int notifyEvent(int msg, int val1, int val2, String val3, int[] arr1, byte[] arr2) {

        while (!Launcher.GUI_COMPLETE.isReached()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        getInstance().logger.info("Event " + msg + "," + val1 + "," + val2 + "," + val3);
        int ret = getInstance().onEvent(msg, val1, val2, val3, arr1, arr2);
        getInstance().logger.info("Event " + msg + " Returned " + ret);
        return ret;
    }

    @Override
    protected SystemEventHandler getHandler() {
        return this;
    }

    @Override
    public boolean onQueryEndSession(LogOffTyp logOffTyp) {
        logger.info("Querry End Session: " + logOffTyp);
        return false;
    }

    @Override
    public void onSessionEnd(boolean queryResponse, LogOffTyp logOffTyp) {
        logger.info("SessuinEnd: REsult: " + queryResponse + " Type: " + logOffTyp);
        if (queryResponse) {
            ShutdownController.getInstance().requestShutdown(true);
        }
    }

    @Override
    public void onDisplayChange(int w, int h, int val1) {

        logger.info("Display: " + w + "x" + h + " - " + val1 + " bits/pixel");
    }

    @Override
    public void onScreenSaverState(boolean b) {
        logger.info("Screensaver Active: " + b);
    }

    @Override
    public void onPowerBroadcast(PowerBroadcastEvent ev) {
        logger.info("Powerbroadcast: " + ev);
    }

    @Override
    public boolean onQuerySuspend(boolean b) {
        logger.info("Suspend system: " + b);
        return true;
    }

    @Override
    public void onPowerStatusChanged(boolean ac, byte chargingStatus, String percentageCharging, int secondsLeft, int secondsTotal) {
        logger.info("PowerStatus: AC:" + ac + " Charging: " + chargingStatus + " " + percentageCharging + "% seconds left: " + secondsLeft + "/" + secondsTotal);
    }

    @Override
    public void onOEMEvent(int val2) {
        logger.info("OEM " + val2);
    }

    @Override
    public void onCompacting(double percent) {
        logger.info("Compacting RAM " + percent);
    }

    @Override
    public void onSessionChange(SessionEvent sessionEvent, int sessionid, String username, boolean current) {
        logger.info("Session CHanged: " + sessionEvent + " " + sessionid + " " + username + " " + current);
    }

    @Override
    public void onDeviceChangeEvent(DeviceChangeType deviceChangeType, DeviceType deviceType) {
        logger.info("DeviceChange: " + deviceChangeType + " - " + deviceChangeType);
    }

    @Override
    public boolean onDeviceRemoveQuery(DeviceType deviceType) {
        logger.info("Remove ? " + deviceType);
        return true;
    }

    @Override
    public boolean onDeviceConfigChangeQuery() {
        logger.info("onDeviceConfigChangeQuery");
        return true;
    }

    @Override
    public void onDeviceConfigChange() {
        logger.info("onDeviceConfigChange");
    }

    @Override
    public void onDeviceConfigChangeCanceled() {
        logger.info("onDeviceConfigChangeCanceled");
    }

    @Override
    public void onNetworkConnected(String device, NetworkType networkType, String ip, String gateway, String mask) {
        logger.info("Network: " + device + " " + networkType + " IP:" + ip + " gateway:" + gateway + " Mask:" + mask);
    }

    @Override
    public void onNetworkDisconnect(String device) {
        logger.info("Network disconnect: " + device);
    }

    @Override
    public void onNetworkConnecting(String device) {
        logger.info("Network connecting: " + device);
    }

    @Override
    public boolean onConsoleEvent(ConsoleEventType type) {
        logger.info(type + "");
        return true;
    }

}
