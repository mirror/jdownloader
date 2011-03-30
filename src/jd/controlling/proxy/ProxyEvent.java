package jd.controlling.proxy;

import org.appwork.utils.event.SimpleEvent;

public class ProxyEvent<E> extends SimpleEvent<ProxyController, ProxyInfo, ProxyEvent.Types> {

    public ProxyEvent(ProxyController caller, Types type, ProxyInfo parameters) {
        super(caller, type, parameters);
    }

    public static enum Types {
        ADDED, REMOVED, REFRESH
    }

}
