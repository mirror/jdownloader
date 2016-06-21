package jd.controlling.linkcollector;

import java.util.WeakHashMap;

import org.appwork.utils.StringUtils;

public class LinkOriginDetails {

    protected LinkOriginDetails(LinkOrigin origin) {
        this(origin, null);
    }

    private final static WeakHashMap<LinkOriginDetails, Object> POOL = new WeakHashMap<LinkOriginDetails, Object>();

    public static LinkOriginDetails getInstance(final LinkOrigin origin, final String details) {
        if (origin == null) {
            throw new IllegalArgumentException("origin is null");
        }
        if (StringUtils.isEmpty(details)) {
            return origin.getLinkOriginDetails();
        }
        synchronized (POOL) {
            for (final LinkOriginDetails pool : POOL.keySet()) {
                if (pool.getOrigin().equals(origin) && StringUtils.equals(pool.getDetails(), details)) {
                    return pool;
                }
            }
            final LinkOriginDetails pool = new LinkOriginDetails(origin, details);
            POOL.put(pool, Boolean.TRUE);
            return pool;
        }
    }

    // add pool method
    protected LinkOriginDetails(final LinkOrigin origin, final String details) {
        if (origin == null) {
            throw new IllegalArgumentException("origin is null");
        }
        this.origin = origin;
        this.details = details;
    }

    final private LinkOrigin origin;

    public final LinkOrigin getOrigin() {
        return origin;
    }

    public final String getDetails() {
        return details;
    }

    final private String details;
}
