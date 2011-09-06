package jd.controlling.reconnect.ipcheck.event;

import jd.controlling.reconnect.ipcheck.IPConnectionState;
import jd.controlling.reconnect.ipcheck.IPController;

import org.appwork.utils.event.SimpleEvent;

public class IPControllEvent extends SimpleEvent<IPController, IPConnectionState, IPControllEvent.Type> {

    public static enum Type {
        /**
         * Parameter: current IPConnectionState
         */
        INVALIDATED,
        /**
         * Parameter 0: old INvalid State <br>
         * 
         * Parameter 1: current valid IPConnectionState
         */
        VALIDATED,
        /**
         * 
         * Parameter: current IPConnectionState
         */
        FORBIDDEN_IP,

        /**
         * Parameter 0: old INvalid State <br>
         * 
         * Parameter 1: current valid IPConnectionState
         */
        IP_CHANGED, OFFLINE,

        /**
         * Parameter: current IPConnectionState
         */
        ONLINE,

        /**
         * Parameter 0: old INvalid State <br>
         * 
         * Parameter 1: current valid IPConnectionState
         */
        STATECHANGED
    }

    public IPControllEvent(Type type, IPConnectionState... parameters) {
        super(IPController.getInstance(), type, parameters);
    }
}