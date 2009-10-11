package standby;

/**
 * This class is used to detect is standby(sleep) was requested on the PC it is
 * running on and to disallow it optionally
 */
public class StandByDetector {
    static {
        System.loadLibrary("StandByDetector");
    }
    private StandByRequestListener listener;

    public StandByDetector(StandByRequestListener listener) {
        this.listener = listener;
        init();
    }

    public void fireStandByRequested() {
        listener.standByRequested();
    }

    private native boolean init();

    /**
     * allowStandBy==false means that no standby is allowed while this app is
     * running
     */
    public native void setAllowStandby(boolean allowStandby);

    public native void destroy();
}