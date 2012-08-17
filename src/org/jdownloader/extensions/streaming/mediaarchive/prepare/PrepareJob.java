package org.jdownloader.extensions.streaming.mediaarchive.prepare;

import org.appwork.utils.event.queue.QueueAction;

public class PrepareJob extends QueueAction<Void, RuntimeException> {

    private PrepareEntry jobEntry;

    public PrepareJob(PrepareEntry pe) {
        this.jobEntry = pe;
    }

    @Override
    protected Void run() throws RuntimeException {

        try {
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
    }

}
