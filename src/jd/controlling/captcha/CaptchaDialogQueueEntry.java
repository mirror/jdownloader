package jd.controlling.captcha;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.IOPermission;
import jd.controlling.IOPermission.CAPTCHA;
import jd.gui.UserIO;
import jd.gui.swing.dialog.CaptchaDialog;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.StorageException;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.translate._JDT;

public class CaptchaDialogQueueEntry extends QueueAction<String, RuntimeException> {
    private final static AtomicLong IDCounter = new AtomicLong(0);
    private final long              ID;

    /**
     * @return the iD
     */
    public long getID() {
        return ID;
    }

    private final CaptchaController captchaController;
    private final int               flag;
    private final String            def;
    private String                  resp         = null;
    private CaptchaDialog           dialog;
    private IOPermission            ioPermission = null;

    public CaptchaDialogQueueEntry(CaptchaController captchaController, int flag, String def) {
        this.captchaController = captchaController;
        this.ioPermission = captchaController.getIOPermission();
        this.flag = flag;
        this.def = def;
        this.ID = IDCounter.incrementAndGet();
    }

    public String getHost() {
        return captchaController.getHost();
    }

    public File getFile() {
        return captchaController.getCaptchafile();
    }

    public void setResponse(String resp) {
        this.resp = resp;
        try {
            dialog.dispose();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
    }

    protected String run() {
        /* external response already set, no need to ask user */
        if (resp != null) return resp;
        String ret = viaGUI();
        /* external response set, return this instead */
        if (resp != null) return resp;
        return ret;
    }

    private String viaGUI() {
        if (ioPermission != null && ioPermission.isCaptchaAllowed(getHost())) { return null; }
        UserIO.setCountdownTime(SubConfiguration.getConfig("JAC").getIntegerProperty(Configuration.JAC_SHOW_TIMEOUT, 20));
        try {
            this.dialog = new CaptchaDialog(flag | Dialog.LOGIC_COUNTDOWN, getHost(), captchaController.getCaptchafile(), def, captchaController.getExplain());
            return Dialog.getInstance().showDialog(dialog);
        } catch (DialogNoAnswerException e) {
            if (resp == null) {
                /* no external response available */
                if (!e.isCausedByTimeout()) {
                    String[] options = new String[] { _JDT._.captchacontroller_cancel_dialog_allorhost_next(), _JDT._.captchacontroller_cancel_dialog_allorhost_cancelhost(captchaController.getHost()), _JDT._.captchacontroller_cancel_dialog_allorhost_all() };
                    try {
                        int defSelection = JSonStorage.getPlainStorage("CaptchaController").get("lastCancelOption", 0);

                        switch (Dialog.getInstance().showComboDialog(Dialog.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _JDT._.captchacontroller_cancel_dialog_allorhost(), _JDT._.captchacontroller_cancel_dialog_allorhost_msg(), options, defSelection, null, null, null, null)) {
                        case 0:
                            // nothing
                            JSonStorage.getPlainStorage("CaptchaController").put("lastCancelOption", 0);
                            break;
                        case 1:
                            if (ioPermission != null) {
                                ioPermission.setCaptchaAllowed(getHost(), CAPTCHA.BLOCKHOSTER);
                            }
                            JSonStorage.getPlainStorage("CaptchaController").put("lastCancelOption", 1);
                            break;
                        case 2:
                            if (ioPermission != null) {
                                ioPermission.setCaptchaAllowed(null, CAPTCHA.BLOCKALL);
                            }
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
        }
        UserIO.setCountdownTime(-1);
        return null;
    }

    public IOPermission getIOPermission() {
        return ioPermission;
    }

}