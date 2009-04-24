package jd.gui;

import java.io.File;

import javax.swing.ImageIcon;

public abstract class UserIO {
    /**
     * Flag to set that no Automatic captcha detection should be used in the
     * userinput
     */
    public static final int NO_JAC = 1 << 0;
    /**
     * TO not query user. Try to fill automaticly, or return null
     */
    public static final int NO_USER_INTERACTION = 1<<1;
    public static final int NO_COUNTDOWN = 1<<2;
    public static final int NO_OK_OPTION = 1<<3;
    public static final int NO_CANCEL_OPTION = 1<<4;
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

    public boolean requestConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption) {
        synchronized (INSTANCE) {
            return showConfirmDialog(flag, title, message, icon, okOption,cancelOption);
        }
        
    }

    abstract protected  boolean showConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption);

}
