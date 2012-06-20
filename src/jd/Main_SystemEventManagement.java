package jd;

import org.appwork.javaexe.JavaExe_SystemEventManagement;

/**
 * Class for javaexe launcher
 * 
 * @author Thomas
 * 
 */
public class Main_SystemEventManagement extends JavaExe_SystemEventManagement {
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

}
