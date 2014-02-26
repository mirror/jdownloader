package org.jdownloader.api.test;

import java.util.LinkedHashMap;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.DeviceList;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfo;
import org.jdownloader.myjdownloader.client.json.DirectConnectionInfos;

public class VerifyDirectConnection extends Test {
    
    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        final DeviceList list = api.listDevices();
        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);
        final DirectConnectionInfos infos = api.getDirectConnectionInfos(list.getList().get(device).getId());
        LinkedHashMap<String, Boolean> ret = new LinkedHashMap<String, Boolean>();
        if (infos.getInfos() != null) {
            for (DirectConnectionInfo info : infos.getInfos()) {
                boolean verify = api.verifyDirectConnectionInfo(list.getList().get(device).getId(), info);
                ret.put(info.getIp() + ":" + info.getPort(), verify);
            }
        }
        Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "REsult", "" + JSonStorage.toString(ret));
    }
}
