package org.jdownloader.api.test;

import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.AccessToken;

public class CallAccessTokenURL extends Test {
    
    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {
        final String service = Dialog.getInstance().showInputDialog(0, "Enter servicename", "Enter", null, null, null, null);
        AccessToken accessToken = api.requestAccessToken(service);
        final String url = Dialog.getInstance().showInputDialog(0, "Enter url", "Enter", null, null, null, null);
        Dialog.getInstance().showMessageDialog(String.valueOf(api.callAccessTokenURL(accessToken, url, Boolean.TYPE)));
    }
}
