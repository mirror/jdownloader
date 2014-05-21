package org.jdownloader.api.test;

import java.lang.reflect.Type;
import java.util.List;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.MyJDCaptchasClient;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution;

public class RemoveCaptchaTest extends Test {
    
    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        if (SolveCaptchaTest.device == null) SolveCaptchaTest.device = new DeviceData("", "jd", "testClient");
        SolveCaptchaTest.device = api.bindDevice(SolveCaptchaTest.device);
        MyJDCaptchasClient captchaAPI = new MyJDCaptchasClient<Type>(api);
        List<MyCaptchaSolution> response = captchaAPI.list();
        if (response == null || response.size() == 0) return;
        final int index = Dialog.getInstance().showComboDialog(0, "Choose CaptchaID", "Choose CaptchaID", response.toArray(new MyCaptchaSolution[] {}), 0, null, null, null, null);
        Dialog.getInstance().showMessageDialog("Result:" + captchaAPI.abort(response.get(index).getId()));
    }
}
