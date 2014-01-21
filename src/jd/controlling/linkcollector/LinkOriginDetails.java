package jd.controlling.linkcollector;

public class LinkOriginDetails {
    /**
     * @param origin
     * @param details
     */
    public LinkOriginDetails(LinkOrigin origin, String details) {
        super();
        this.origin = origin;
        this.details = details;
    }

    private LinkOrigin origin;

    public LinkOrigin getOrigin() {
        return origin;
    }

    public void setOrigin(LinkOrigin origin) {
        this.origin = origin;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    private String details;
}
