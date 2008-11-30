package jd.http.download;

import java.util.ArrayList;

public class Broadcaster<E> {
    private ArrayList<Object> listener;

    public Broadcaster() {
        listener = new ArrayList<Object>();

    }

    public synchronized void addListener(E listener) {
        this.listener.add(listener);
    }

    public synchronized void removeListener(E listener) {
        this.listener.remove(listener);
    };

    public int size() {
        return listener.size();
    }

    public synchronized E get(int i) {
        return (E)listener.get(i);
        
    }

}
