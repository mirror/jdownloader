package org.jdownloader.updatev2;

import java.util.List;

import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.shutdown.ShutdownVetoListener;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;

public class InstallUpdatesOnExitRestartRequest implements RestartRequest {

    private ShutdownRequest shutdownRequest;
    private boolean         runUpdates = true;

    public InstallUpdatesOnExitRestartRequest(ShutdownRequest filter) {
        this.shutdownRequest = filter;
    }

    @Override
    public boolean askForVeto(ShutdownVetoListener listener) {
        boolean ret = shutdownRequest.askForVeto(listener);

        return ret;
    }

    @Override
    public void addVeto(ShutdownVetoException e) {
        shutdownRequest.addVeto(e);
    }

    @Override
    public boolean isSilent() {
        return shutdownRequest.isSilent();
    }

    @Override
    public List<ShutdownVetoException> getVetos() {
        return shutdownRequest.getVetos();
    }

    @Override
    public boolean hasVetos() {
        return shutdownRequest.hasVetos();
    }

    @Override
    public void onShutdown() {
        // ask
        this.runUpdates = true;
        if (JsonConfig.create(UpdateSettings.class).isAskMyBeforeInstallingAnUpdateEnabled()) {
            // DialogHook.showConfirmDialog(0, _UPDATE._.confirmdialog_new_update_available_frametitle(),
            // _UPDATE._.confirmdialog_new_update_available_for_install_message_launcher(), null,
            // _UPDATE._.confirmdialog_new_update_available_answer_now_install(),
            // _UPDATE._.confirmdialog_new_update_available_answer_later_install());

            ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_for_install_message_launcher(), null, _UPDATE._.confirmdialog_new_update_available_answer_now_install(), _UPDATE._.confirmdialog_new_update_available_answer_later_install());

            try {

                UIOManager.I().show(ConfirmDialogInterface.class, dialog).throwCloseExceptions();

            } catch (DialogNoAnswerException e) {
                runUpdates = false;
            } catch (Throwable e) {

            }

        }
    }

    @Override
    public String[] getArguments() throws NoRestartException {
        if (!runUpdates) {
            throw new NoRestartException();
        } else {
            return new String[] { "-updateOnExit" };
        }
    }

    @Override
    public void onShutdownVeto() {
    }

}
