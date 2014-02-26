package org.jdownloader.api.test;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.AbstractMyJDDeviceClient;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.DeviceList;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfo;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfos;

public class DirectConnectionCall extends Test {
    
    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        final DeviceList list = api.listDevices();
        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);
        AbstractMyJDDeviceClient deviceAPI = new AbstractMyJDDeviceClient(list.getList().get(device), api);
        final DirectConnectionInfos infos = deviceAPI.getDirectConnectionInfos();
        if (infos.getInfos() == null) {
            Dialog.getInstance().showMessageDialog(Dialog.STYLE_LARGE, "No direct Connection available!");
            return;
        }
        final int info = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", infos.getInfos().toArray(new DirectConnectionInfo[] {}), 0, null, null, null, null);
        Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "REsult", "" + JSonStorage.toString(deviceAPI.verifyDirectConnectionInfo(infos.getInfos().get(info))));
    }
}
