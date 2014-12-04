package org.jdownloader.extensions.eventscripter;

import net.sourceforge.htmlunit.corejs.javascript.Context;
import net.sourceforge.htmlunit.corejs.javascript.Function;

public class ScriptReferenceThread extends Thread {

    private ScriptThread scriptThread;

    public ScriptThread getScriptThread() {
        return scriptThread;
    }

    public ScriptReferenceThread(ScriptThread env) {
        this.scriptThread = env;
    }

    public void executeCallback(Function callback, Object... params) {
        Context cx = Context.enter();
        try {
            callback.call(cx, getScriptThread().getScope(), getScriptThread().getScope(), params);
        } finally {
            Context.exit();
        }

    }
}
