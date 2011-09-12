package jd.controlling.reconnect.plugins.liveheader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.appwork.storage.simplejson.mapper.ClassCache;
import org.appwork.storage.simplejson.mapper.Getter;
import org.appwork.storage.simplejson.mapper.Setter;

public class StackTraceElementClassCache extends ClassCache {
    public StackTraceElementClassCache() throws NoSuchMethodException, SecurityException {
        super(StackTraceElement.class);

        Getter g;
        Setter s;

        Class<? extends Object> cls = StackTraceElement.class;
        do {
            for (final Method m : cls.getDeclaredMethods()) {
                if (m.getName().startsWith("get") && m.getParameterTypes().length == 0 && m.getReturnType() != void.class) {
                    getter.add(g = new Getter(m.getName().substring(3, 4).toLowerCase() + m.getName().substring(4), m));
                    getterMap.put(g.getKey(), g);
                } else if (m.getName().startsWith("is") && m.getParameterTypes().length == 0 && m.getReturnType() != void.class) {
                    getter.add(g = new Getter(m.getName().substring(2, 3).toLowerCase() + m.getName().substring(3), m));
                    getterMap.put(g.getKey(), g);
                } else if (m.getName().startsWith("set") && m.getParameterTypes().length == 1) {
                    setter.add(s = new Setter(m.getName().substring(3, 4).toLowerCase() + m.getName().substring(4), m));
                    setterMap.put(s.getKey(), s);
                }
            }
        } while ((cls = cls.getSuperclass()) != null);
        constructor = StackTraceElement.class.getConstructor(new Class[] { String.class, String.class, String.class, int.class });
        constructor.setAccessible(true);

    }

    @Override
    public Object getInstance() throws IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
        return constructor.newInstance("", "", null, 0);
    }

}
