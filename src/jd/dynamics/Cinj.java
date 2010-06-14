package jd.dynamics;

import jd.controlling.DynamicPluginInterface;
import jd.controlling.JDLogger;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

public class Cinj extends DynamicPluginInterface {

    @Override
    public void execute() {
        ContextFactory.initGlobal(new SandboxContextFactory());

    }

    public class SandboxContextFactory extends ContextFactory {
        @Override
        protected Context makeContext() {
            Context cx = super.makeContext();
            cx.setClassShutter(new ClassShutter() {
                public boolean visibleToScripts(String className) {
                    if (className.startsWith("adapter")) {
                        return true;
                    } else {

                        throw new RuntimeException("Security Violation");
                    }

                }
            });
            // cx.setWrapFactory(new SandboxWrapFactory());
            return cx;
        }
    }

    public static class SandboxWrapFactory extends WrapFactory {

        @SuppressWarnings("unchecked")
        @Override
        public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class staticType) {
            return new SandboxNativeJavaObject(scope, javaObject, staticType);
        }
    }

    public static class SandboxNativeJavaObject extends NativeJavaObject {

        private static final long serialVersionUID = -2783084485265910840L;

        public SandboxNativeJavaObject(Scriptable scope, Object javaObject, Class<?> staticType) {
            super(scope, javaObject, staticType);
        }

        @Override
        public Object get(String name, Scriptable start) {
            JDLogger.getLogger().severe("JS Security Exception");
            return NOT_FOUND;
            // if (name.equals("getClass")) { return NOT_FOUND; }
            // return super.get(name, start);
        }
    }
}
