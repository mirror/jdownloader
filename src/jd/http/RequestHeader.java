//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.http;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class RequestHeader {

    private ArrayList<String> keys;
    private ArrayList<String> values;
    private boolean dominant = false;

    /**
     * if a header is dominant, it will not get merged with existing headers. It
     * will replace it completly
     * 
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
        this();
        this.putAll(h);
    }

    public void put(String key, String value) {
        for (int i = 0; i < keys.size(); i++) {
            if (keys.get(i).equalsIgnoreCase(key.trim())) {
                keys.set(i, key);
                values.set(i, value);
                return;
            }
        }
        keys.add(key);
        values.add(value);
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
        if (keys.size() == 0) {
            this.put(key, value);
            return;
        }
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
