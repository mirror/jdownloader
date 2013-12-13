package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;

public class CancelRegistrationTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        final String email = Dialog.getInstance().showInputDialog(0, "Email", "Enter", config.get("email", ""), null, null, null);
        final String key = Dialog.getInstance().showInputDialog(0, "Email Cancel Key", "Enter", null, null, null, null);
        api.cancelRegistrationEmail(email, key);
    }

}
