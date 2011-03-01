package jd.controlling.captcha;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.utils.locale.JDL;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;

public class CaptchaDialogQueueEntry extends QueueAction<String, RuntimeException> {

    private final CaptchaController captchaController;
    private final int               flag;
    private final String            def;

    public CaptchaDialogQueueEntry(CaptchaController captchaController, int flag, String def) {
        this.captchaController = captchaController;
        this.flag = flag;
        this.def = def;
    }

    public String getHost() {
        return captchaController.getHost();
    }

    protected String run() {
        if (CaptchaController.getCaptchaSolver() != null) {
            String result = CaptchaController.getCaptchaSolver().solveCaptcha(captchaController.getHost(), captchaController.getIcon(), captchaController.getCaptchafile(), def, captchaController.getExplain());
            if (result != null && result.length() > 0) return result;
        }

        UserIO.setCountdownTime(SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.JAC_SHOW_TIMEOUT, 20));
        try {
            return UserIO.getInstance().requestCaptchaDialog(flag | Dialog.LOGIC_COUNTDOWN, captchaController.getHost(), captchaController.getIcon(), captchaController.getCaptchafile(), def, captchaController.getExplain());
        } catch (DialogNoAnswerException e) {
            if (!e.isCausedByTimeout()) {
                String[] options = new String[] { JDL.L("captchacontroller.cancel.dialog.allorhost.next", "Show all further pending Captchas"), JDL.LF("captchacontroller.cancel.dialog.allorhost.cancelhost", "Do not show pending Captchas for %s", captchaController.getHost()), JDL.L("captchacontroller.cancel.dialog.allorhost.all", "Cancel all pending Captchas") };
                try {
                    int defSelection = JSonStorage.getPlainStorage("CaptchaController").get("lastCancelOption", 0);

                    switch (Dialog.getInstance().showComboDialog(Dialog.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, JDL.L("captchacontroller.cancel.dialog.allorhost", "Canceled Captcha Dialog"), JDL.L("captchacontroller.cancel.dialog.allorhost.msg", "You canceled a Captcha Dialog!\r\nHow do you want to continue?"), options, defSelection, captchaController.getIcon(), null, null, null)) {
                    case 0:
                        // nothing
                        JSonStorage.getPlainStorage("CaptchaController").put("lastCancelOption", 0);
                        break;
                    case 1:
                        CaptchaDialogQueue.getInstance().blockByHost(captchaController.getHost());
                        JSonStorage.getPlainStorage("CaptchaController").put("lastCancelOption", 1);
                        break;
                    case 2:
                        CaptchaDialogQueue.getInstance().blockAll();
                        JSonStorage.getPlainStorage("CaptchaController").put("lastCancelOption", 2);
                        break;
                    }
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                } catch (StorageException e1) {
                    e1.printStackTrace();
                }
            }
        }
        UserIO.setCountdownTime(-1);
        return null;
    }

    public long getInitTime() {
        return this.captchaController.getInitTime();
    }

}
