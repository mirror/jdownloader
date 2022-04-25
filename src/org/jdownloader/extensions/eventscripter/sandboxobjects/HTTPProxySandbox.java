package org.jdownloader.extensions.eventscripter.sandboxobjects;

import jd.controlling.proxy.AbstractProxySelectorImpl;
import jd.controlling.proxy.PacProxySelectorImpl;
import jd.controlling.proxy.SingleBasicProxySelectorImpl;

public class HTTPProxySandbox {
    private final AbstractProxySelectorImpl proxy;

    public HTTPProxySandbox() {
        this(null);
    }

    public HTTPProxySandbox(AbstractProxySelectorImpl proxy) {
        this.proxy = proxy;
    }

    public AbstractProxySelectorImpl.Type getType() {
        return proxy != null ? proxy.getType() : AbstractProxySelectorImpl.Type.NONE;
    }

    public String toExportString() {
        return proxy != null ? proxy.toExportString() : "none://";
    }

    public String getUsername() {
        if (proxy instanceof PacProxySelectorImpl) {
            return ((PacProxySelectorImpl) proxy).getUser();
        } else if (proxy instanceof SingleBasicProxySelectorImpl) {
            final SingleBasicProxySelectorImpl p = ((SingleBasicProxySelectorImpl) proxy);
            switch (p.getType()) {
            case DIRECT:
            case NONE:
                return null;
            default:
                return p._getUsername();
            }
        } else {
            return null;
        }
    }

    public String getPassword() {
        if (proxy instanceof PacProxySelectorImpl) {
            return ((PacProxySelectorImpl) proxy).getPassword();
        } else if (proxy instanceof SingleBasicProxySelectorImpl) {
            final SingleBasicProxySelectorImpl p = ((SingleBasicProxySelectorImpl) proxy);
            switch (p.getType()) {
            case DIRECT:
            case NONE:
                return null;
            default:
                return p._getPassword();
            }
        } else {
            return null;
        }
    }

    public String getHost() {
        if (proxy instanceof PacProxySelectorImpl) {
            final PacProxySelectorImpl p = ((PacProxySelectorImpl) proxy);
            return p.getPACUrl();
        } else if (proxy instanceof SingleBasicProxySelectorImpl) {
            final SingleBasicProxySelectorImpl p = ((SingleBasicProxySelectorImpl) proxy);
            switch (p.getType()) {
            case DIRECT:
                return p.getProxy().getLocal();
            case NONE:
                return null;
            default:
                return p.getHost();
            }
        } else {
            return null;
        }
    }

    public int getPort() {
        if (proxy instanceof SingleBasicProxySelectorImpl) {
            final SingleBasicProxySelectorImpl p = ((SingleBasicProxySelectorImpl) proxy);
            switch (p.getType()) {
            case DIRECT:
            case NONE:
                return -1;
            default:
                return p.getPort();
            }
        } else {
            return -1;
        }
    }
}
