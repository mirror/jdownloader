package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;

public class GetUptimeTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        final String dev = chooseDevice(api);
        final Long uptime = api.callAction(dev, "/jd/uptime", long.class);
        Dialog.getInstance().showMessageDialog("Uptime: " + uptime);
    }

}
