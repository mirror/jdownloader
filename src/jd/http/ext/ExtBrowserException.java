package jd.http.ext;

public class ExtBrowserException extends Exception {

    private static final long serialVersionUID = -2957611433056761360L;

    public ExtBrowserException(String message) {
        super(message);
    }

    public ExtBrowserException(Throwable cause) {
        super(cause);
    }

}
