package jd.http.download;

public class BrowserException extends Exception {

    private static final long serialVersionUID = -1576784726086641221L;

    public static final int TYPE_RANGE = 1;
    public static final int TYPE_BADREQUEST = 2;
    public static final int TYPE_REDIRECT = 3;
    public static final int TYPE_LOCAL_IO = 4;

    private int type;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public BrowserException(String l) {
        super(l);
    }

    public BrowserException(String l, int type) {
        super(l);
        this.type = type;
    }

}
