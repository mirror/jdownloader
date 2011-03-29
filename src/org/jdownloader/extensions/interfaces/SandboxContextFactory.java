/**
 * 
 */
package org.jdownloader.extensions.interfaces;

import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.WrapFactory;

/**
 * @author unkown
 * 
 */
public class SandboxContextFactory extends ContextFactory {
    @Override
    protected Context makeContext() {
        Context cx = super.makeContext();
        cx.setWrapFactory(new SandboxWrapFactory());
        cx.setClassShutter(new ClassShutter() {
            public boolean visibleToScripts(String className) {
                if (className.startsWith("adapter")) {
                    return true;
                } else {

                    throw new RuntimeException("Security Violation");
                }

            }
        });
        return cx;
    }

    public static class SandboxWrapFactory extends WrapFactory {

        @SuppressWarnings("rawtypes")
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
            if (name.equals("getClass")) { return NOT_FOUND; }

            return super.get(name, start);
        }
    }

}