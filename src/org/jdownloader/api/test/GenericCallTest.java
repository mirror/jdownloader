package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.DeviceList;

public class GenericCallTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        final DeviceList list = api.listDevices();
        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);
        final String cmd = Dialog.getInstance().showInputDialog(0, "Enter Command", "Enter", config.get("cmd", "/jd/doSomethingCool"), null, null, null);
        config.put("cmd", cmd);
        final Object uptime = api.callAction(list.getList().get(device).getId(), cmd, Object.class);
        Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "REsult", "" + uptime);

    }

}
