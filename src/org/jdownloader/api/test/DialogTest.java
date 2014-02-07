package org.jdownloader.api.test;

import java.util.HashMap;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.bindings.DialogStorable;
import org.jdownloader.myjdownloader.client.bindings.DialogTypeStorable;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DialogInterface;

public class DialogTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {

        DialogInterface dialogs = api.link(DialogInterface.class, chooseDevice(api));
        long[] list = dialogs.list();
        if (list.length > 0) {
            DialogStorable info = dialogs.get(list[0], true, true);
            DialogTypeStorable typeInfo = dialogs.getTypeInfo(info.getType());
            //
            DialogTypeStorable t = dialogs.getTypeInfo("org.jdownloader.gui.dialog.AskCrawlerPasswordDialogInterface");
            dialogs.answer(list[0], JSonStorage.restoreFromString(Dialog.getInstance().showInputDialog(info.getType()), new TypeRef<HashMap<String, Object>>() {
            }));

            System.out.println(info);
        }
    }

}
