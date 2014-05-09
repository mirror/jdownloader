package jd.controlling.proxy;

import org.appwork.utils.event.SimpleEvent;

public class ProxyEvent<E> extends SimpleEvent<ProxyController, AbstractProxySelectorImpl, ProxyEvent.Types> {

    public ProxyEvent(ProxyController caller, Types type, AbstractProxySelectorImpl parameters) {
        super(caller, type, parameters);
    }

    public static enum Types {
        ADDED,
        REMOVED,
        REFRESH
    }

}
