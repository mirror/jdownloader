package jd.http.ext;

public class NetworkRequest {
    /**
     * The uninitialized request state.
     */
    public static final int STATE_UNINITIALIZED = 0;

    /**
     * The loading request state. The <code>open</code> method has been called,
     * but a response has not been received yet.
     */
    public static final int STATE_LOADING = 1;

    /**
     * The loaded request state. Headers and status are now available.
     */
    public static final int STATE_LOADED = 2;

    /**
     * The interactive request state. Downloading response.
     */
    public static final int STATE_INTERACTIVE = 3;

    /**
     * The complete request state. All operations are finished.
     */
    public static final int STATE_COMPLETE = 4;
}
