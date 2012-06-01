package jd.controlling.reconnect.ipcheck.event;

import java.util.EventListener;

import jd.controlling.reconnect.ipcheck.IPConnectionState;

public interface IPControllListener extends EventListener {
    /**
     * If we have a external IP which does not match the ip validation pattern
     * 
     * @param parameter
     */
    public void onIPForbidden(IPConnectionState parameter);

    /**
     * IF someone ionvalidated the current ip. we need an ip change now to
     * validate the ip
     * 
     * @param parameter
     */
    public void onIPInvalidated(IPConnectionState parameter);

    /**
     * will be called if ip changed - no matter if ip was invalidated or not
     * 
     * @param parameter
     * @param parameter2
     */
    public void onIPChanged(IPConnectionState parameter, IPConnectionState parameter2);

    /**
     * WIll be called if ip is offline
     */
    public void onIPOffline();

    /**
     * will be called if ip is valid again after beieng invalidated
     * 
     * @param parameter
     * @param parameter2
     */
    public void onIPValidated(IPConnectionState parameter, IPConnectionState parameter2);

    /**
     * Will be called if we got online after beeing offline or in unknown state
     * 
     * @param parameter
     */
    public void onIPOnline(IPConnectionState parameter);

    /**
     * will be called for each ip state change
     * 
     * @param parameter
     * @param parameter2
     */
    public void onIPStateChanged(IPConnectionState parameter, IPConnectionState parameter2);
}