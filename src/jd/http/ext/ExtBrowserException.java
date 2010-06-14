package jd.http.ext;

public class ExtBrowserException extends Exception {

    public ExtBrowserException(String string) {
        super(string);
    }

    public ExtBrowserException(Exception e) {
        super(e);
    }

}
