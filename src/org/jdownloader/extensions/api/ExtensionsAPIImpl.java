package org.jdownloader.extensions.api;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.LazyExtension;
import org.jdownloader.updatev2.UpdateController;

public class ExtensionsAPIImpl implements ExtensionsAPI {

    @Override
    public boolean isInstalled(String id) {

        if (!UpdateController.getInstance().isHandlerSet()) throw new WTFException("UpdateHandler not set");
        return UpdateController.getInstance().isExtensionInstalled(id);
    }

    @Override
    public void install(final String id) {

        new Thread("Install Extension") {
            public void run() {
                try {
                    Dialog.getInstance().showConfirmDialog(0, "Extension Installation requested", "Do you want to install the " + id + "-extension?");

                    UpdateController.getInstance().setGuiVisible(true);
                    UpdateController.getInstance().runExtensionInstallation(id);

                    while (true) {
                        Thread.sleep(500);
                        if (!UpdateController.getInstance().isRunning()) break;
                        UpdateController.getInstance().waitForUpdate();

                    }
                } catch (Exception e) {
                    Log.exception(e);
                } finally {

                }
            }
        }.start();

    }

    @Override
    public boolean isEnabled(String classname) {
        LazyExtension lazy = ExtensionController.getInstance().getExtension(classname);
        return lazy != null && lazy._isEnabled();
    }

    @Override
    public void setEnabled(String classname, boolean b) {
        LazyExtension lazy = ExtensionController.getInstance().getExtension(classname);
        ExtensionController.getInstance().setEnabled(lazy, b);

    }

}
