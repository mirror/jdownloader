package jd.controlling.captcha;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.logging.Logger;

import jd.controlling.IOPermission;
import jd.controlling.IOPermission.CAPTCHA;
import jd.gui.swing.dialog.CaptchaDialog;
import jd.gui.swing.dialog.CaptchaDialogInterface;
import jd.gui.swing.dialog.CaptchaDialogInterface.DialogType;
import jd.gui.swing.dialog.ClickCaptchaDialog;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.storage.StorageException;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.ComboBoxDialog;
import org.appwork.utils.swing.dialog.ComboBoxDialogInterface;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.DomainInfo;
import org.jdownloader.controlling.UniqueAlltimeID;
import org.jdownloader.gui.uiserio.NewUIO;
import org.jdownloader.logging.LogController;
import org.jdownloader.translate._JDT;

public class CaptchaDialogQueueEntry extends QueueAction<CaptchaResult, RuntimeException> {

    private final UniqueAlltimeID id = new UniqueAlltimeID();

    /**
     * @return the iD
     */
    public UniqueAlltimeID getID() {
        return id;
    }

    private CaptchaController captchaController;

    /**
     * @return the captchaController
     */
    public CaptchaController getCaptchaController() {
        return captchaController;
    }

    private int                    flag;
    private CaptchaResult          def;
    private CaptchaResult          resp         = null;
    private boolean                externalSet  = false;
    private CaptchaDialogInterface textDialog;
    private IOPermission           ioPermission = null;

    public CaptchaDialogQueueEntry(CaptchaController captchaController, int flag, CaptchaResult def) {
        this.captchaController = captchaController;
        this.ioPermission = captchaController.getIOPermission();
        this.flag = flag;
        this.def = def;
    }

    public DomainInfo getHost() {
        return captchaController.getHost();
    }

    public File getCaptchaFile() {
        return captchaController.getCaptchaFile();
    }

    public File getPreparedCaptchaFile() {
        return captchaController.getPreparedCaptchaFile();
    }

    public void setResponse(CaptchaResult resp) {
        externalSet = true;
        this.resp = resp;
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                try {
                    if (textDialog != null) textDialog.dispose();
                } catch (final Throwable e) {
                    LogSource.exception(getLogger(), e);
                }
            }
        };
    }

    protected CaptchaResult run() {
        /* external response already set, no need to ask user */
        if (externalSet) return resp;
        CaptchaResult ret = viaGUI();
        /* external response set, return this instead */
        if (externalSet) return resp;
        return ret;
    }

    private Logger getLogger() {
        Logger logger = null;
        if (captchaController.getPlugin() instanceof PluginForHost) {
            logger = captchaController.getPlugin().getLogger();
        } else if (captchaController.getPlugin() instanceof PluginForDecrypt) {
            logger = captchaController.getPlugin().getLogger();
        }
        if (logger == null) logger = LogController.GL;
        return logger;
    }

    private CaptchaResult viaGUI() {
        if (ioPermission != null && !ioPermission.isCaptchaAllowed(getHost().getTld())) { return null; }
        try {
            DialogType dialogType = null;
            if (captchaController.getPlugin() instanceof PluginForHost) {
                dialogType = DialogType.HOSTER;
            } else if (captchaController.getPlugin() instanceof PluginForDecrypt) {
                dialogType = DialogType.CRAWLER;
            }
            int f = flag;
            if (CaptchaSettings.CFG.isCountdownEnabled() && CaptchaSettings.CFG.getCountdown() > 0) {
                f = f | Dialog.LOGIC_COUNTDOWN;
            }
            Image[] images = CaptchaDialog.getGifImages(captchaController.getCaptchaFile().toURI().toURL());
            if (images == null || images.length == 0) {
                BufferedImage img = IconIO.getImage(captchaController.getCaptchaFile().toURI().toURL());
                if (img != null) images = new Image[] { img };
            }

            if (images == null || images.length == 0 || images[0] == null) {
                getLogger().severe("Could not load CaptchaImage! " + captchaController.getCaptchaFile().getAbsolutePath());
                return null;
            }
            CaptchaDialogInterface answer = null;
            switch (captchaController.getCaptchaType()) {
            case CLICK:
                ClickCaptchaDialog dd = new ClickCaptchaDialog(f, dialogType, getHost(), images, captchaController.getExplain());
                dd.setPlugin(captchaController.getPlugin());
                dd.setCountdownTime(CaptchaSettings.CFG.getCountdown());
                this.textDialog = dd;
                answer = NewUIO.I().show(CaptchaDialogInterface.class, dd);
                break;
            case TEXT:
                CaptchaDialog d = new CaptchaDialog(f, dialogType, getHost(), images, def, captchaController.getExplain());
                d.setPlugin(captchaController.getPlugin());
                d.setCountdownTime(CaptchaSettings.CFG.getCountdown());
                this.textDialog = d;
                answer = NewUIO.I().show(CaptchaDialogInterface.class, d);
                break;

            }

            return answer.getCaptchaResult();
        } catch (DialogNoAnswerException e) {
            if (resp == null) {
                /* no external response available */
                if (!e.isCausedByTimeout()) {
                    String[] options = new String[] { _JDT._.captchacontroller_cancel_dialog_allorhost_next(), _JDT._.captchacontroller_cancel_dialog_allorhost_cancelhost(captchaController.getHost().getTld()), _JDT._.captchacontroller_cancel_dialog_allorhost_all() };
                    try {
                        int defSelection = CaptchaSettings.CFG.getLastCancelOption();
                        ComboBoxDialog combo = new ComboBoxDialog(Dialog.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _JDT._.captchacontroller_cancel_dialog_allorhost(), _JDT._.captchacontroller_cancel_dialog_allorhost_msg(), options, defSelection, null, null, null, null);

                        switch (NewUIO.I().show(ComboBoxDialogInterface.class, combo).getSelectedIndex()) {
                        case 0:
                            // nothing
                            CaptchaSettings.CFG.setLastCancelOption(0);
                            break;
                        case 1:
                            if (ioPermission != null) {
                                ioPermission.setCaptchaAllowed(getHost().getTld(), CAPTCHA.BLOCKHOSTER);
                            }
                            CaptchaSettings.CFG.setLastCancelOption(1);
                            break;
                        case 2:
                            if (ioPermission != null) {
                                ioPermission.setCaptchaAllowed(null, CAPTCHA.BLOCKALL);
                            }
                            CaptchaSettings.CFG.setLastCancelOption(2);
                            break;
                        }

                    } catch (DialogNoAnswerException e1) {
                    } catch (StorageException e1) {
                        LogSource.exception(getLogger(), e1);
                    }
                }
            }
        } catch (Throwable e) {
            LogSource.exception(getLogger(), e);
        }
        return null;
    }

    public IOPermission getIOPermission() {
        return ioPermission;
    }

}