package org.jdownloader.extensions.eventscripter;

import org.appwork.utils.Exceptions;

import net.sourceforge.htmlunit.corejs.javascript.EcmaError;
import net.sourceforge.htmlunit.corejs.javascript.ScriptRuntime;

public class EnvironmentException extends Exception {

    public EnvironmentException(Throwable e) {
        super(getLineInfo() + "\r\n" + Exceptions.getStackTrace(e), e);
    }

    private static String getLineInfo() {
        EcmaError ret = ScriptRuntime.constructError("Stacktrace", null);
        return "Line " + ret.lineNumber();
    }

    public EnvironmentException(String string) {
        super(string, ScriptRuntime.constructError("Stacktrace", null));
    }

}
