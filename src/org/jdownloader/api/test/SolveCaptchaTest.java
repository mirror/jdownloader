package org.jdownloader.api.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Type;

import org.appwork.storage.Storage;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.MyJDCaptchasClient;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.MyCaptchaChallenge;
import org.jdownloader.myjdownloader.client.json.MyCaptchaSolution;

public class SolveCaptchaTest extends Test {

    public static DeviceData device = null;

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        if (device == null) device = new DeviceData("", "jd", "testClient");
        device = api.bindDevice(device);
        MyJDCaptchasClient captchaAPI = new MyJDCaptchasClient<Type>(api);
        ExtFileChooserDialog dialog = new ExtFileChooserDialog(0, "Select an Image", "Solve", "Abort");
        dialog.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
        dialog.setMultiSelection(false);
        dialog.setType(FileChooserType.OPEN_DIALOG);
        Dialog.getInstance().showDialog(dialog);
        File load = dialog.getSelectedFile();
        MyCaptchaChallenge dataURL = new MyCaptchaChallenge();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
        final Base64OutputStream b64os = new Base64OutputStream(bos);

        b64os.write(IO.readFile(load));
        b64os.close();
        String type = Files.getExtension(load.getName());
        dataURL.setDataURL("data:image/" + type + ";base64," + new String(bos.toByteArray(), "UTF-8"));
        System.out.println(bos.size());
        MyCaptchaSolution response = captchaAPI.solve(dataURL);
        Dialog.getInstance().showMessageDialog("ID: " + response.getId());
    }
}
