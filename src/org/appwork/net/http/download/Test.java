package org.appwork.net.http.download;

import java.io.File;

import jd.http.requests.GetRequest;

import org.appwork.net.http.download.event.DownloadEvent;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.logging.Log;

/**
 * @author thomas
 * 
 */
public class Test {
    /**
     * Dummylistener to output downloadevents
     */
    protected static final DefaultEventListener<DownloadEvent> LISTENER = new DefaultEventListener<DownloadEvent>() {

        public void onEvent(DownloadEvent event) {
            System.out.println(event);

        }

    };

    /**
     * @param args
     */
    public static void main(String[] args) {
        for (int i = 0; i < 5; i++) {
            createDownloadThread(i);
        }
    }

    private static void createDownloadThread(final int i) {

        new Thread("DL Thread " + i) {
            public void run() {
                try {
                    // this simulates a plugin that starts an download.

                    DownloadHandler downloadhandler;

                    downloadhandler = new DownloadHandler(new GetRequest("http://update3.jdownloader.org/100.rar"), new File("file_" + i + ".test"));

                    downloadhandler.getEventSender().addListener(LISTENER);
                    DLController.getInstance().addAndWait(downloadhandler);
                    Log.L.info("Download " + i + " Finished.");
                } catch (Exception e) {
                    Log.exception(e);
                }
            }
        }.start();
    }
}
