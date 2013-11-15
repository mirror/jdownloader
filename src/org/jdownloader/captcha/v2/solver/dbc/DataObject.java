package org.jdownloader.captcha.v2.solver.dbc;

import java.util.HashMap;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;

/**
 * Dummy class that replaces the originally used JSOnObject class
 * 
 * @author thomas
 * 
 */
public class DataObject extends HashMap<String, Object> {
    public DataObject(String sendAndReceive) {
        super();
        DataObject nD = JSonStorage.restoreFromString(sendAndReceive, new TypeRef<DataObject>() {
        });
        if (nD != null) putAll(nD);
    }

    public DataObject() {
    }

    @Override
    public DataObject put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    public String optString(String string, String object) {
        Object ret = get(string);
        if (ret != null && ret instanceof String) return (String) ret;
        return object;
    }

    public int optInt(String string, int i) {
        Object ret = get(string);
        if (ret != null && ret instanceof Integer) return (Integer) ret;
        return i;
    }

    public double optDouble(String string, double d) {
        Object ret = get(string);
        if (ret != null && ret instanceof Double) return (Double) ret;
        return d;
    }

    public boolean optBoolean(String string, boolean b) {
        Object ret = get(string);
        if (ret != null && ret instanceof Boolean) return (Boolean) ret;
        return b;
    }
}
