package org.jdownloader.plugins.components;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.nutils.encoding.Encoding;
import jd.plugins.components.ThrowingRunnable;

public class DDoSProtectionRu {

    public static class ScriptEnv {

        private ScriptEngine engine;

        public ScriptEnv(ScriptEngine engine) {
            this.engine = engine;
        }

        public void log(String log) {
            System.out.println(log);
        }

        public void eval(String eval) throws ScriptException {
            engine.eval(eval);
        }

        public String atob(String string) {
            String ret = Encoding.Base64Decode(string);
            return ret;
        }
    }

    public static String getWindowLocation(final String js) throws Exception {
        try {
            // its on single line
            if (js != null) {
                ScriptEngineManager mgr = JavaScriptEngineFactory.getScriptEngineManager(null);
                final ScriptEngine engine = mgr.getEngineByName("JavaScript");
                engine.eval("document={};document.body={};");
                engine.eval("window={};window.location={};");
                engine.eval("history=[];");
                // load java environment trusted
                JavaScriptEngineFactory.runTrusted(new ThrowingRunnable<ScriptException>() {

                    @Override
                    public void run() throws ScriptException {
                        ScriptEnv env = new ScriptEnv(engine);
                        // atob requires String to be loaded for its parameter and return type
                        engine.put("env", env);
                        engine.eval("var string=" + String.class.getName() + ";");
                        engine.eval("log=function(str){return env.log(str);};");
                        engine.eval("eval=function(str){return env.eval(str);};");
                        engine.eval("atob=function(str){return env.atob(str);};");
                        // cleanup
                        engine.eval("delete java;");
                        engine.eval("delete jd;");
                        // load Env in Trusted Thread
                        engine.eval("log('Java Env Loaded');");
                    }
                });
                engine.eval(js);
                Object redirect = engine.eval("window.location.href");
                return redirect != null ? (String) String.valueOf(redirect) : null;
            }
        } catch (Exception e) {
            throw e;
        } finally {
        }
        return null;
    }

}
