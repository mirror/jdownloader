package org.jdownloader.statistics;

import java.io.IOException;
import java.util.logging.Level;

import jd.http.Browser;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.logging.Log;

public class AsynchLogger extends QueueAction<Void, RuntimeException> {

    private AbstractLogEntry logEntry;

    public AsynchLogger(AbstractLogEntry logEntry) {
        this.logEntry = logEntry;
    }

    public boolean handleException(final Throwable e) {
        Log.L.finer("Stats Logger Failed: " + e.getMessage());
        Log.exception(Level.FINEST, e);
        return false;
    }

    @Override
    final protected Void run() throws RuntimeException {

        Browser br = new Browser();

        try {
            br.postPage("http://stats.appwork.org/download/add", JSonStorage.serializeToJson(logEntry));
            System.out.println(br.getRequest().getHttpConnection());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
