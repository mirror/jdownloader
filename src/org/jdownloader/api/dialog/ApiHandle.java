package org.jdownloader.api.dialog;

import org.appwork.uio.UserIODefinition;

public class ApiHandle {

    private final long                        id;
    private final long                        created  = System.currentTimeMillis();
    private volatile boolean                  disposed = false;
    private final UserIODefinition            impl;
    private final Thread                      thread;
    private Class<? extends UserIODefinition> iface;
    private volatile UserIODefinition         answer;

    public UserIODefinition getAnswer() {
        return answer;
    }

    public UserIODefinition getImpl() {
        return impl;
    }

    public long getCreated() {
        return created;
    }

    public long getId() {
        return id;
    }

    public ApiHandle(Class<? extends UserIODefinition> iface, UserIODefinition impl, long id, Thread thread) {
        this.thread = thread;
        this.id = id;
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
                if (disposed || answer != null) return;
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

}
