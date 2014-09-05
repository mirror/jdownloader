package org.jdownloader.api.test;

import java.util.ArrayList;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.Storage;
import org.appwork.utils.Application;
import org.jdownloader.api.test.TestClient.Test;
import org.jdownloader.myjdownloader.client.AbstractMyJDClientForDesktopJVM;
import org.jdownloader.myjdownloader.client.bindings.events.EventDistributor;
import org.jdownloader.myjdownloader.client.bindings.events.EventsDistributorListener;
import org.jdownloader.myjdownloader.client.bindings.events.json.PublisherResponse;
import org.jdownloader.myjdownloader.client.bindings.interfaces.DownloadsEventsInterface;
import org.jdownloader.myjdownloader.client.bindings.interfaces.EventsInterface;

public class EventsTest extends Test {

    private EventDistributor ed;

    @Override
    public void run(Storage config, AbstractMyJDClientForDesktopJVM api) throws Exception {

        String devID;
        ed = new EventDistributor(api, devID = chooseDevice(api));
        ed.setConnectionConfig(10 * 60000, 20 * 60000);
        ArrayList<PublisherResponse> publishers = api.link(EventsInterface.class, devID).listpublisher();

        final String pattern;
        // final String event = Dialog.getInstance().showInputDialog("Subscribe to");
        // final String exclude = Dialog.getInstance().showInputDialog("Exclude");

        final String event = "downloads\\..+";
        final String exclude = "";
        ed.addListener(new EventsDistributorListener() {

            @Override
            public void onNewMyJDEvent(final String publisher, final String eventid, final Object eventData) {
                // try {
                // Dialog.getInstance().showInputDialog(Dialog.STYLE_LARGE, "REsult:" + event + " include " + getEventPattern() +
                // " but not" + exclude, publisher + "." + eventid + ":" + JSonStorage.toString(eventData), null, null, "Next",
                // "Remove Listener");
                // } catch (DialogCanceledException e) {
                // ed.removeListener(this);
                // } catch (final DialogNoAnswerException e) {
                // throw new RuntimeException(e);
                // }

                System.err.println("Result:" + event + " -> " + eventid);
                // System.out.println(publisher + "." + eventid + ":" + JSonStorage.toString(eventData));
                System.err.println(JSonStorage.toString(eventData));

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
        api.link(DownloadsEventsInterface.class, devID).setStatusEventInterval(ed.getSubscription().getSubscriptionid(), 1000);
        // disable stdout
        Application.STD_OUT.setBufferEnabled(true);
        ed.run();

    }
}
