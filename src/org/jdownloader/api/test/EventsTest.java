package org.jdownloader.api.test;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.bindings.events.EventDistributor;
import org.jdownloader.myjdownloader.client.bindings.events.EventsDistributorListener;
import org.jdownloader.myjdownloader.client.json.DeviceData;
import org.jdownloader.myjdownloader.client.json.DeviceList;

public class EventsTest extends Test {

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {
        final DeviceList list = api.listDevices();
        if (list.getList().size() == 0) { throw new RuntimeException("No Device Connected"); }
        final int device = Dialog.getInstance().showComboDialog(0, "Choose Device", "Choose Device", list.getList().toArray(new DeviceData[] {}), 0, null, null, null, null);

        final EventDistributor ed = new EventDistributor(api, list.getList().get(device).getId());

        ed.subscribe(new String[] { ".*" }, null);
        ed.getEventSender().addListener(new EventsDistributorListener() {

            @Override
            public void onNewMyJDEvent(final String publisher, final String eventid, final Object eventData) {
                try {
                    Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "REsult", publisher + "." + eventid + ":" + JSonStorage.toString(eventData));
                } catch (final DialogNoAnswerException e) {
                    throw new RuntimeException(e);
                }

            }
        });
        new Thread(ed).start();
    }

}
