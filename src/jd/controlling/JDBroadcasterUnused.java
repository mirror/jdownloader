package jd.controlling;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;

public class JDBroadcasterUnused<T extends EventListener> {
    private HashMap<String, Method> methodMap;
    private ArrayList<T> listener;

    public JDBroadcasterUnused() {
        listener = new ArrayList<T>();
        methodMap = new HashMap<String, Method>();
    }

    public Method getMethod(Class<? extends EventListener> class1, String methodName, Class<?>... classes) throws SecurityException, NoSuchMethodException {
        Method ret = methodMap.get(methodName);
        if (ret == null) {
            ret = class1.getMethod(methodName, classes);
            methodMap.put(methodName, ret);

        }
        return ret;
    }

    /**
     * this is a default firesystem. it is about 6 times slower than a direkt
     * call. SO write an own fireFunction if speed is important.
     * 
     * @param methodName
     * @param objs
     */
    public void fire(String methodName, Object... objs) {
        synchronized (listener) {
            if (listener.size() == 0) return;
            T first = listener.get(0);
            Class<?>[] classes = new Class[objs.length];
            for (int i = 0; i < objs.length; i++)
                classes[i] = objs[i].getClass();
            try {
                Method method = getMethod(first.getClass(), methodName, classes);
                for (EventListener l : listener) {
                    method.invoke(l, objs);
                }

            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        }

    }

    private void removeListener(T l) {
        synchronized (listener) {
            this.listener.remove(l);
        }
    }

    public void add(T listener) {
        removeListener(listener);
        synchronized (listener) {
            this.listener.add(listener);
        }
    }

    public ArrayList<T> getListener() {
        return listener;
    }
}
