package org.jdownloader.api.dialog;

import org.appwork.uio.UserIODefinition;
import org.jdownloader.controlling.UniqueAlltimeID;

public class ApiHandle {

    private final UniqueAlltimeID                   id       = new UniqueAlltimeID();
    private final long                              created  = System.currentTimeMillis();
    private volatile boolean                        disposed = false;
    private final UserIODefinition                  impl;
    private final Thread                            thread;
    private final Class<? extends UserIODefinition> iface;
    private volatile UserIODefinition               answer;

    public UserIODefinition getAnswer() {
        return answer;
    }

    public UserIODefinition getImpl() {
        return impl;
    }

    public long getCreated() {
        return created;
    }

    public UniqueAlltimeID getUniqueAlltimeID() {
        return id;
    }

    public ApiHandle(Class<? extends UserIODefinition> iface, UserIODefinition impl, Thread thread) {
        this.thread = thread;
        this.impl = impl;
        this.iface = iface;
    }

    public Class<? extends UserIODefinition> getIface() {
        return iface;
    }

    public Thread getThread() {
        return thread;
    }

    public void waitFor() throws InterruptedException {
        while (true) {
            synchronized (this) {
                if (disposed || answer != null) {
                    return;
                }
                wait();
            }
        }
    }

    public void dispose() {
        synchronized (this) {
            disposed = true;
            notifyAll();
        }
    }

    public void setAnswer(UserIODefinition ret) {
        synchronized (this) {
            this.answer = ret;
            dispose();
        }
    }

    public boolean isDisposed() {
        return disposed;
    }

}
