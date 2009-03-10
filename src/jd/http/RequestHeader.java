package jd.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RequestHeader {

    private ArrayList<String> keys;
    private ArrayList<String> values;
    private boolean dominant=false;
/**
 * if a header is dominant, it will not get merged with existing headers. It will replace it completly
 * @param dominant
 */
    public void setDominant(boolean dominant) {
        this.dominant = dominant;
    }

    public RequestHeader() {
        this.keys = new ArrayList<String>();
        this.values = new ArrayList<String>();
    }

    public RequestHeader(HashMap<String, String> h) {
        this.putAll(h);
    }

    public void put(String key, String value) {
        int i;
        if ((i = keys.indexOf(key)) >= 0) {
            keys.set(i, key);
            values.set(i, value);

        } else {
            keys.add(key);
            values.add(value);
        }

    }

    public String getKey(int i) {
        // TODO Auto-generated method stub
        return keys.get(i);
    }

    public String getValue(int i) {
        // TODO Auto-generated method stub
        return values.get(i);
    }

    public int size() {
        // TODO Auto-generated method stub
        return keys.size();
    }

    public Object clone() {
        RequestHeader mh = new RequestHeader();
        mh.keys.addAll(keys);
        mh.values.addAll(values);
        return mh;
    }

    public String remove(String key) {
        int index = keys.indexOf(key);
        if (index >= 0) {
            keys.remove(index);
            return values.remove(index);
        }
        return null;
    }

    public void putAll(HashMap<String, String> properties) {
        for (Iterator<String> it = properties.keySet().iterator(); it.hasNext();) {
            String key = it.next();
            String value;
            if ((value = properties.get(key)) == null) {
                remove(key);
            } else {
                put(key, value);
            }

        }

    }

    public void putAll(RequestHeader headers) {
        for (int i = 0; i < headers.size(); i++) {
            String key = headers.getKey(i);
            String value = headers.getValue(i);
            if (value == null) {
                remove(key);
            } else {
                put(key, value);
            }
        }

    }

    public String get(String key) {
        int index = keys.indexOf(key);
        if (index >= 0) { return values.get(index); }
        return null;
    }

    public void clear() {
        keys.clear();
        values.clear();
    }

    public boolean contains(String string) {
        // TODO Auto-generated method stub
        return keys.contains(string);
    }

    public void setAt(int i, String key, String value) {
        keys.add(keys.get(keys.size() - 1));
        values.add(values.get(values.size() - 1));
        for (int e = keys.size() - 3; e >= i; e--) {

            keys.set(e + 1, keys.get(e));
            values.set(e + 1, values.get(e));
        }
        keys.set(i, key);
        values.set(i, value);

    }  

    public boolean isDominant() {
        // TODO Auto-generated method stub
        return dominant;
    }

}
