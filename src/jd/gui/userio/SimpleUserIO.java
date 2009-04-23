package jd.gui.userio;

import java.io.File;

import jd.gui.UserIO;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.userio.dialog.CaptchaDialog;

public class SimpleUserIO extends UserIO {
    private SimpleUserIO() {
        super();
    }

    public static UserIO getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SimpleUserIO();
        }
        return INSTANCE;
    }

    @Override
    protected String showCaptchaDialog(final int flag, final String methodname, final File captchafile, final String suggestion, final String explain) {
        if ((flag & UserIO.NO_USER_INTERACTION) > 0) return suggestion;
        return new GuiRunnable<String>() {

            @Override
            public String runSave() {
                return new CaptchaDialog(flag, methodname, captchafile, suggestion, explain).getCaptchaText();
            }
        }.getReturnValue();

    }

    public static void main(String args[]) {
        UserIO.setInstance(SimpleUserIO.getInstance());
        String res = UserIO.getInstance().requestCaptchaDialog(0, "rapidshare.com", new File("C:\\Users\\oem\\.jd_home\\captchas\\stealth.to\\21.04.2009_21.38.26.327.jpg"), "01234", null);
        System.out.println("result: " + res);
    }
}
