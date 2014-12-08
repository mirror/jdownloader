package org.jdownloader.extensions.eventscripter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class Utils {

    public static String toMy(String key) {
        return "my" + key.substring(0, 1).toUpperCase(Locale.ENGLISH) + key.substring(1);
    }

    public static String cleanUpClass(String clazz) {
        return clazz.replace("Sandbox", "").replace("SandBox", "");
    }

    public static List<Method> sort(Method[] declaredMethods) {
        ArrayList<Method> ret = new ArrayList<Method>();
        for (Method m : declaredMethods) {
            ret.add(m);
        }
        Collections.sort(ret, new Comparator<Method>() {

            @Override
            public int compare(Method o1, Method o2) {
                return o1.getName().compareTo(o2.getName());
            }

        });
        return ret;
    }

    public static List<Field> sort(Field[] declaredMethods) {
        ArrayList<Field> ret = new ArrayList<Field>();
        for (Field m : declaredMethods) {
            ret.add(m);
        }
        Collections.sort(ret, new Comparator<Field>() {

            @Override
            public int compare(Field o1, Field o2) {
                return o1.getName().compareTo(o2.getName());
            }

        });
        return ret;
    }

}
