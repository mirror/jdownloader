package org.jdownloader.extensions.shutdown;

import org.appwork.uio.UIOManager;
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.shutdown.translate.T;

public class ShutdownThread extends Thread {
    private final ShutdownInterface shutdownInterface;
    private final ShutdownConfig    settings;
    private final LogSource         logger;
    private final ShutdownExtension shutdownExtension;

    public ShutdownThread(ShutdownExtension shutdownExtension) {
        this.shutdownExtension = shutdownExtension;
        shutdownInterface = shutdownExtension.getShutdownInterface();
        settings = shutdownExtension.getSettings();
        logger = shutdownExtension.getLogger();
    }

    @Override
    public void run() {
        if (settings.isShutdownActive() == false) {
            return;
        }
        final Mode mode = settings.getShutdownMode();
        if (!shutdownInterface.isSupported(mode)) {
            logger.warning("Mode '" + mode + "' is not supported!");
            return;
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            logger.log(e);
        }
        final ExtractionExtension extractor = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();
        if (extractor != null) {
            while (!extractor.getJobQueue().isEmpty()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    logger.log(e);
                }
            }
        }
        if (settings.isShowWarningDialog()) {
            final String dialogMessage;
            final String dialogTitle;
            switch (mode) {
            case SHUTDOWN:
                dialogMessage = T.T.interaction_shutdown_dialog_msg_shutdown();
                dialogTitle = T.T.interaction_shutdown_dialog_title_shutdown();
                break;
            case STANDBY:
                dialogMessage = T.T.interaction_shutdown_dialog_msg_standby();
                dialogTitle = T.T.interaction_shutdown_dialog_title_standby();
                break;
            case CLOSE:
                dialogMessage = T.T.interaction_shutdown_dialog_msg_closejd();
                dialogTitle = T.T.interaction_shutdown_dialog_title_closejd();
                break;
            case HIBERNATE:
                dialogMessage = T.T.interaction_shutdown_dialog_msg_hibernate();
                dialogTitle = T.T.interaction_shutdown_dialog_title_hibernate();
                break;
            case LOGOFF:
                dialogMessage = T.T.interaction_shutdown_dialog_msg_logoff();
                dialogTitle = T.T.interaction_shutdown_dialog_title_logoff();
                break;
            default:
                logger.info("Unsupported Mode:" + mode);
                return;
            }
            logger.info("ask user about '" + mode + "'");
            final WarningDialog d = new WarningDialog(shutdownExtension, dialogTitle, dialogMessage);
            final WarningDialogInterface io = UIOManager.I().show(WarningDialogInterface.class, d);
            switch (io.getCloseReason()) {
            case OK:
            case TIMEOUT:
                break;
            default:
                logger.info("User aborted Mode:" + mode + "|Reason:" + io.getCloseReason());
                return;
            }
        }
        shutdownInterface.requestMode(mode, settings.isForceShutdownEnabled());
    }
}
