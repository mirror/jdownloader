package org.jdownloader.api;

import org.appwork.uio.UserIODefinition;

public class ApiHandle {

    private long                              id;
    private long                              created;
    private boolean                           disposed = false;
    private UserIODefinition                  impl;
    private Thread                            thread;
    private Class<? extends UserIODefinition> iface;
    private UserIODefinition                  answer;

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

    public void setId(long id) {
        this.id = id;
    }

    public ApiHandle(Class<? extends UserIODefinition> iface, UserIODefinition impl, long id, Thread thread) {
        this.thread = thread;
        this.id = id;
        created = System.currentTimeMillis();
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

        while (!disposed && answer == null) {
            synchronized (this) {
                wait();
            }
        }
    }

    public void dispose() {
        disposed = true;
        synchronized (this) {
            notifyAll();
        }
    }

    public void setAnswer(UserIODefinition ret) {
        this.answer = ret;
        dispose();
    }

}
