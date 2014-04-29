package org.jdownloader.api.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Type;

import org.appwork.storage.Storage;
import org.appwork.utils.Files;
import org.appwork.utils.net.Base64OutputStream;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.ExtFileChooserDialog;
import org.appwork.utils.swing.dialog.FileChooserSelectionMode;
import org.appwork.utils.swing.dialog.FileChooserType;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.MyJDCaptchasClient;
import org.jdownloader.myjdownloader.client.json.CaptchaResponse;
import org.jdownloader.myjdownloader.client.json.DataURL;
import org.seamless.util.io.IO;

public class SolveCaptchaTest extends Test {
    
    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        MyJDCaptchasClient captchaAPI = new MyJDCaptchasClient<Type>(api);
        ExtFileChooserDialog dialog = new ExtFileChooserDialog(0, "Select an Image", "Solve", "Abort");
        dialog.setFileSelectionMode(FileChooserSelectionMode.FILES_ONLY);
        dialog.setMultiSelection(false);
        dialog.setType(FileChooserType.OPEN_DIALOG);
        Dialog.getInstance().showDialog(dialog);
        File load = dialog.getSelectedFile();
        DataURL dataURL = new DataURL();
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(16384);
        final Base64OutputStream b64os = new Base64OutputStream(bos);
        b64os.write(IO.readBytes(load));
        b64os.close();
        String type = Files.getExtension(load.getName());
        dataURL.setDataURL("data:image/" + type + ";base64," + new String(bos.toByteArray(), "UTF-8"));
        CaptchaResponse response = captchaAPI.solve(dataURL);
        Dialog.getInstance().showMessageDialog("ID: " + response.getId());
    }
}
