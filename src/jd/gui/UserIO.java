package jd.gui;

import java.io.File;

public abstract class UserIO {
    /**
     * Flag to set that no Automatic captcha detection should be used in the
     * userinput
     */
    public static final int NO_JAC = 1 << 0;
    /**
     * TO not query user. Try to fill automaticly, or return null
     */
    public static final int NO_USER_INTERACTION = 0;
    protected static UserIO INSTANCE = null;

    protected UserIO() {

    }

    public static UserIO getInstance() {
        return INSTANCE;
    }

    public String requestCaptchaDialog(int flag, String methodname, File captchafile, String suggestion, String explain) {
        synchronized (INSTANCE) {
            return showCaptchaDialog(flag, methodname, captchafile, suggestion, explain);
        }

    }

    /**
     * abstracts
     * 
     * @param methodname
     * @param captchafile
     * @param suggestion
     * @return
     */
    abstract protected String showCaptchaDialog(int flag, String methodname, File captchafile, String suggestion, String explain);

    public static void setInstance(UserIO instance2) {
        INSTANCE = instance2;

    }

}
