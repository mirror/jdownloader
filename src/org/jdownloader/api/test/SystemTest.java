package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.DeviceList;

public class SystemTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        final DeviceList list = api.listDevices();
        if (list.getList().size() == 0) {
            throw new RuntimeException("No Device Connected");
        }

        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);

        String[] options = { "exitJD", "restartJD", "shutdownOS", "hibernateOS", "standbyOS" };
        String task = (String) Dialog.getInstance().showComboDialog(0, "Test System", "Choose Task", options, options[0], null, null, null, null);
        if (task.equals("shutdownOS")) {
            task += "?true";
        }
        api.callAction(list.getList().get(device).getId(), "/system/" + task, Object.class);
        Dialog.getInstance().showMessageDialog("JD should react on action '" + task + "'.");
    }
}
