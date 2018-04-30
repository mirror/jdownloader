package org.jdownloader.jna.windows;

public interface User32 extends com.sun.jna.platform.win32.User32 {
    public User32 INSTANCE = (User32) com.sun.jna.Native.loadLibrary("user32", User32.class);

    /**
     * Requires win 2000+
     *
     * @param hWnd
     * @return
     */
    boolean OpenIcon(HWND hWnd);

    /**
     * Requires win 2000+
     *
     * @param hWnd
     * @return
     */
    boolean CloseWindow(HWND hWnd);

    // https://msdn.microsoft.com/de-de/library/windows/desktop/ms633548(v=vs.85).aspx
    // Displays the window in its current size and position. This value is similar to SW_SHOW, except that the window is not activated.
    public static final int SHOW_SW_SHOWNA            = 8;
    // Maximizes the specified window.
    public static final int SHOW_WINDOW_SW_MAXIMIZE   = 3;
    // Minimizes the specified window and activates the next top-level window in the Z order.
    public static final int SHOW_WINDOW_SW_MINIMIZE   = 6;
    // Activates and displays the window. If the window is minimized or maximized, the system restores it to its original size and position.
    // An application should specify this flag when restoring a minimized window.
    public static final int SHOW_WINDOW_SW_RESTORE    = 9;
    // Activates the window and displays it in its current size and position.
    public static final int SHOW_WINDOW_SW_SHOW       = 5;
    // Activates and displays a window. If the window is minimized or maximized, the system restores it to its original size and position.
    // An application should specify this flag when displaying the window for the first time.
    public static final int SHOW_WINDOW_SW_SHOWNORMAL = 1;

    /**
     * Requires win 2000+
     *
     * @param hWnd
     * @return
     */
    boolean ShowWindow(HWND hWnd, int state);

    void AnimateWindow(HWND hwnd, long time, long animation);

    boolean SystemParametersInfo(int uiAction, int uiParam, Object pvParam, // Pointer or int
            int fWinIni);
}
