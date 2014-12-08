package jd.controlling.linkcollector;

public class LinkOriginDetails {

    public LinkOriginDetails(LinkOrigin origin) {
        this(origin, null);
    }

    public LinkOriginDetails(LinkOrigin origin, String details) {
        if (origin == null) {
            throw new IllegalArgumentException("origin is null");
        }
        this.origin = origin;
        this.details = details;
    }

    final private LinkOrigin origin;

    public LinkOrigin getOrigin() {
        return origin;
    }

    public String getDetails() {
        return details;
    }

    final private String details;
}
