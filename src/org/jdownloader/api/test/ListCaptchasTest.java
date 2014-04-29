package org.jdownloader.api.test;

import java.lang.reflect.Type;
import java.util.List;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.MyJDCaptchasClient;
import org.jdownloader.myjdownloader.client.json.DeviceData;

public class ListCaptchasTest extends Test {
    
    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        if (SolveCaptchaTest.device == null) SolveCaptchaTest.device = new DeviceData("", "jd", "testClient");
        SolveCaptchaTest.device = api.bindDevice(SolveCaptchaTest.device);
        MyJDCaptchasClient captchaAPI = new MyJDCaptchasClient<Type>(api);
        List response = captchaAPI.list();
        Dialog.getInstance().showMessageDialog(JSonStorage.toString(response));
    }
}
