package org.jdownloader.extensions.eventscripter;

import net.sourceforge.htmlunit.corejs.javascript.ScriptRuntime;

import org.appwork.utils.Exceptions;

public class EnvironmentException extends Exception {

    public EnvironmentException(Throwable e) {
        super(Exceptions.getStackTrace(ScriptRuntime.constructError("Stacktrace", null)), e);
    }

}
