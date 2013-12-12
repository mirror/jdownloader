package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;

public class FeedbackTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {
        final String message = Dialog.getInstance().showInputDialog(0, "Enter Feedback message", "Enter", null, null, null, null);
        api.feedback(message);
    }

}
