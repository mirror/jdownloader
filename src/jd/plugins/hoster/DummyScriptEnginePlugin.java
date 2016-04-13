package jd.plugins.hoster;

import java.util.Map;

import javax.script.ScriptEngineManager;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.plugins.components.ThrowingRunnable;

@Deprecated
/**
 * @deprecated use JavaScriptEngineFactory instead
 * @author thomas
 *
 */
public class DummyScriptEnginePlugin {

    public static ScriptEngineManager getScriptEngineManager(Object requestor) {
        return JavaScriptEngineFactory.getScriptEngineManager(requestor);
    }

    public static Map<String, Object> jsonToJavaMap(String string) throws Exception {
        return JavaScriptEngineFactory.jsonToJavaMap(string);
    }

    public static Object walkJson(final Object json, final String crawlstring) {
        return JavaScriptEngineFactory.walkJson(json, crawlstring);
    }

    public static Object jsonToJavaObject(String string) throws Exception {
        return JavaScriptEngineFactory.jsonToJavaObject(string);
    }

    public static <T extends Exception> void runTrusted(ThrowingRunnable<T> runnable) throws T {
        JavaScriptEngineFactory.runTrusted(runnable);
    }

    public static long toLong(final Object o, final long defaultvalue) {
        return JavaScriptEngineFactory.toLong(o, defaultvalue);
    }

}
