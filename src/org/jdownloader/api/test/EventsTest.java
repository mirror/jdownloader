package org.jdownloader.api.test;

import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClient;
import org.jdownloader.myjdownloader.client.bindings.events.EventDistributor;
import org.jdownloader.myjdownloader.client.bindings.events.EventsDistributorListener;
import org.jdownloader.myjdownloader.client.bindings.events.json.PublisherResponse;
import org.jdownloader.myjdownloader.client.bindings.interfaces.EventsInterface;

public class EventsTest extends Test {

    private Thread           thread;
    private EventDistributor ed;

    @Override
    public void run(Storage config, AbstractMyJDClient api) throws Exception {
        if (thread != null) {
            final String pattern;
            final String event = Dialog.getInstance().showInputDialog("Subscribe to");
            final String exclude = Dialog.getInstance().showInputDialog("Exclude");
            ed.addListener(new EventsDistributorListener() {

                @Override
                public void onNewMyJDEvent(final String publisher, final String eventid, final Object eventData) {
                    try {
                        Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "REsult:" + event + " include " + getEventPattern() + " but not" + exclude, publisher + "." + eventid + ":" + JSonStorage.toString(eventData), null, null, "Next", "Remove Listener");
                    } catch (DialogCanceledException e) {
                        ed.removeListener(this);
                    } catch (final DialogNoAnswerException e) {
                        throw new RuntimeException(e);
                    }

                }

                @Override
                public String getEventPattern() {
                    return event;
                }

                @Override
                public String getFilterPattern() {
                    return exclude;
                }

            });

            return;

        }
        String devID;
        ed = new EventDistributor(api, devID = chooseDevice(api));
        ed.setConnectionConfig(60000, 2 * 60000);
        ArrayList<PublisherResponse> publishers = api.link(EventsInterface.class, devID).listpublisher();

        thread = new Thread(ed);
        thread.start();
        run(config, api);
    }

}
