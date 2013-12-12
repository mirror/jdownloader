package org.jdownloader.api.test;

import java.util.ArrayList;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.DeviceList;

public class CaptchaListTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {
        final DeviceList list = api.listDevices();
        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);
        final ArrayList uptime = api.callAction(list.getList().get(device).getId(), "/captcha/list", ArrayList.class);
        Dialog.getInstance().showMessageDialog("Uptime: " + uptime);
    }

}
