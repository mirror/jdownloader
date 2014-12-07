package org.jdownloader.extensions.eventscripter;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;

import jd.controlling.downloadcontroller.DownloadController;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.Application;
import org.appwork.utils.reflection.Clazz;
import org.jdownloader.extensions.eventscripter.sandboxobjects.DownloadLinkSandBox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.EventSandbox;
import org.jdownloader.extensions.eventscripter.sandboxobjects.FilePackageSandBox;
import org.jdownloader.gui.views.ArraySet;

public enum EventTrigger implements LabelInterface {
    ON_DOWNLOAD_CONTROLLER_START {
        @Override
        public String getLabel() {
            return T._.ON_DOWNLOAD_CONTROLLER_START();
        }

        public HashMap<String, Object> getTestProperties() {
            HashMap<String, Object> ret = new HashMap<String, Object>();
            ret.put("link", new DownloadLinkSandBox());
            ret.put("package", new FilePackageSandBox());
            return ret;
        }

        public String getAPIDescription() {
            return defaultAPIDescription(this);
        }
    },
    ON_DOWNLOAD_CONTROLLER_STOPPED {
        @Override
        public String getLabel() {
            return T._.ON_DOWNLOAD_CONTROLLER_STOPPED();
        }

        public HashMap<String, Object> getTestProperties() {
            return ON_DOWNLOAD_CONTROLLER_START.getTestProperties();
        }

        public String getAPIDescription() {
            return ON_DOWNLOAD_CONTROLLER_START.getAPIDescription();
        }

    },

    ON_JDOWNLOADER_STARTED {
        @Override
        public String getLabel() {
            return T._.ON_JDOWNLOADER_STARTED();
        }

        public HashMap<String, Object> getTestProperties() {
            return NONE.getTestProperties();
        }

        public String getAPIDescription() {
            return NONE.getAPIDescription();
        }

    },
    NONE {
        @Override
        public String getLabel() {
            return T._.NONE();
        }

    },
    ON_OUTGOING_REMOTE_API_EVENT {
        @Override
        public String getLabel() {
            return T._.ON_OUTGOING_REMOTE_API_EVENT();
        }

        public HashMap<String, Object> getTestProperties() {
            HashMap<String, Object> props = new HashMap<String, Object>();
            props.put("event", new EventSandbox());

            return props;
        }

        public String getAPIDescription() {
            return defaultAPIDescription(this);
        }
        // public String getAPIDescription() {
        // return defaultAPIDescription(this);
        // }
        // public String getAPIDescription() {
        // StringBuilder sb = new StringBuilder();
        // sb.append(T._.properties_for_eventtrigger(getLabel())).append("\r\n");
        // sb.append("var myString=eventID;/*NEW|EXPIRED|...*/").append("\r\n");
        // sb.append("var myString=eventPublisher; /*downloadwatchdog|dialogs|captchas|...*/").append("\r\n");
        // sb.append("var myObject=eventData;/*additional data like the dialog id in case of dialog events*/").append("\r\n");
        //
        // return sb.toString();
        // }
    },
    ON_NEW_FILE {
        @Override
        public String getLabel() {
            return T._.ON_NEW_FILE();
        }

        public HashMap<String, Object> getTestProperties() {
            HashMap<String, Object> props = new HashMap<String, Object>();

            props.put("files", new String[] { Application.getResource("license.txt").getAbsolutePath() });
            props.put("caller", DownloadController.class.getName());

            return props;
        }

        public String getAPIDescription() {
            StringBuilder sb = new StringBuilder();
            sb.append(T._.properties_for_eventtrigger(getLabel())).append("\r\n");
            sb.append("var myStringArray=files;").append("\r\n");
            sb.append("var myString=caller; /*Who created the files*/").append("\r\n");

            return sb.toString();
        }
    };

    public String getAPIDescription() {
        return T._.none_trigger();
    }

    protected static String defaultAPIDescription(EventTrigger eventTrigger) {
        StringBuilder sb = new StringBuilder();
        sb.append(T._.properties_for_eventtrigger(eventTrigger.getLabel())).append("\r\n");

        for (Entry<String, Object> es : eventTrigger.getTestProperties().entrySet()) {
            sb.append("var ").append(toMy(cleanUpClass(es.getValue().getClass().getSimpleName()))).append(" = ").append(es.getKey()).append(";").append("\r\n");

        }
        // sb.append(T._.classes()).append("\r\n");
        // for (Class<?> cl : clazzes) {
        // sb.append("/* ===").append(cleanUpClass(cl.getSimpleName())).append("===*/").append("\r\n");
        // ScriptAPI clazzAnn = cl.getAnnotation(ScriptAPI.class);
        // if (clazzAnn != null && StringUtils.isNotEmpty(clazzAnn.description())) {
        // sb.append("/* ").append(clazzAnn.description()).append("*/").append("\r\n");
        // }
        //
        // for (Method m : cl.getDeclaredMethods()) {
        // if (!Modifier.isPublic(m.getModifiers())) {
        //
        // continue;
        // }
        // ScriptAPI ann = m.getAnnotation(ScriptAPI.class);
        // if (!Clazz.isVoid(m.getReturnType())) {
        // sb.append("var ").append(toMy(m.getReturnType().getSimpleName())).append(" = ");
        // }
        // sb.append(toMy(cleanUpClass(cl.getSimpleName()))).append(".").append(m.getName());
        // sb.append("(");
        // boolean first = true;
        // int i = 0;
        // for (Class<?> p : m.getParameterTypes()) {
        // if (!first) {
        // sb.append(", ");
        // }
        // first = false;
        // sb.append(toMy(p.getSimpleName()));
        // if (ann != null && ann.parameters().length == m.getParameterTypes().length && !StringUtils.isEmpty(ann.parameters()[i])) {
        // sb.append("/*").append(ann.parameters()[i]).append("*/");
        // }
        // i++;
        // }
        // sb.append(");");
        // if (ann != null && StringUtils.isNotEmpty(ann.description())) {
        // sb.append("/*").append(ann.description()).append("*/");
        // }
        // sb.append("\r\n");
        // if (ann != null && StringUtils.isNotEmpty(ann.example())) {
        // sb.append(ann.example());
        // sb.append("\r\n");
        // }
        // }
        //
        // }
        return sb.toString();
    }

    protected static void collectClasses(Class<? extends Object> cl, ArraySet<Class<?>> clazzes) {

        for (Method m : cl.getDeclaredMethods()) {
            if (m.getReturnType() == Object.class || !Modifier.isPublic(m.getModifiers()) || Clazz.isPrimitive(m.getReturnType()) || Clazz.isPrimitiveWrapper(m.getReturnType()) || Clazz.isString(m.getReturnType())) {
                continue;
            }
            if (clazzes.add(m.getReturnType())) {
                collectClasses(m.getReturnType(), clazzes);
            }
            for (Class<?> cl2 : m.getParameterTypes()) {
                if (cl2 == Object.class || Clazz.isPrimitive(cl2) || Clazz.isPrimitiveWrapper(cl2) || Clazz.isString(cl2)) {
                    continue;
                }
                if (clazzes.add(cl2)) {
                    collectClasses(cl2, clazzes);
                }

            }
            // for (Field f : cl.getFields()) {
            // if (clazzes.add(f.getType())) {
            // collectClasses(f.getType(), clazzes);
            // }
            //
            // }
        }
    }

    public static String cleanUpClass(String clazz) {
        return clazz.replace("Sandbox", "").replace("SandBox", "");
    }

    static String toMy(String key) {
        return "my" + key.substring(0, 1).toUpperCase(Locale.ENGLISH) + key.substring(1);
    }

    public HashMap<String, Object> getTestProperties() {
        return new HashMap<String, Object>();
    }

    public ArraySet<Class<?>> getAPIClasses() {
        ArraySet<Class<?>> clazzes = new ArraySet<Class<?>>();
        for (Entry<String, Object> es : getTestProperties().entrySet()) {
            clazzes.add(es.getValue().getClass());
            collectClasses(es.getValue().getClass(), clazzes);
        }
        return clazzes;
    }

}
