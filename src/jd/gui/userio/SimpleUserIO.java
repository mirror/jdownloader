package jd.gui.userio;

import java.io.File;

import javax.swing.ImageIcon;

import jd.gui.UserIO;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.userio.dialog.CaptchaDialog;
import jd.gui.userio.dialog.ConfirmDialog;
import jd.utils.JDTheme;

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
        // String res = UserIO.getInstance().requestCaptchaDialog(0,
        // "megaupload.com", new File(
        // "C:\\Users\\oem\\.jd_home\\captchas\\megaupload.com\\23.04.2009_12.28.22.245.jpg"
        // ), "01234", null);
        // System.out.println("result: " + res);

        UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN|UserIO.NO_OK_OPTION, "title", "message final\r\n int flag, f\r\ninal String title, final\r\n S\r\ntring message, final Ima\r\ngeIcon icon, final String okOption, final String cancelOption) {", JDTheme.II("gui.clicknload", 32, 32), null, null);
    }

    @Override
    protected boolean showConfirmDialog(final int flag, final String title, final String message, final ImageIcon icon, final String okOption, final String cancelOption) {
        if ((flag & UserIO.NO_USER_INTERACTION) > 0) return false;
        return new GuiRunnable<Boolean>() {

            @Override
            public Boolean runSave() {
                return new ConfirmDialog(flag, title, message, icon, okOption, cancelOption).getReturnValue();
            }
        }.getReturnValue();
    }
}
