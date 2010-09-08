package jd.controlling.reconnect;

public class IPCheckException extends Exception {

    private static final long serialVersionUID = -616841297490493575L;

    public static final int   FAILED           = 0;

    public static final int   SEQ_FAILED       = 1;

    private final int         id;

    public IPCheckException(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

}
