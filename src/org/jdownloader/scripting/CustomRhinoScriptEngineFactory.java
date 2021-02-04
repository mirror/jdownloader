package org.jdownloader.scripting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;

import org.jdownloader.logging.LogController;
import org.jdownloader.scripting.JavaScriptEngineFactory.RhinoScriptEngine;

public class CustomRhinoScriptEngineFactory implements ScriptEngineFactory {
    public CustomRhinoScriptEngineFactory() {
    }

    public Object getParameter(String key) {
        if (key == null) {
            throw new IllegalArgumentException("Invalid key:null");
        } else if (key.equals(ScriptEngine.NAME)) {
            return "javascript";
        } else if (key.equals(ScriptEngine.ENGINE)) {
            return "Mozilla Rhino";
        } else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            return "1.6 release 2";
        } else if (key.equals(ScriptEngine.LANGUAGE)) {
            return "javascript";
        } else if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return "1.6";
        } else if (key.equals("THREADING")) {
            return "MULTITHREADED";
        } else {
            throw new IllegalArgumentException("Invalid key:" + key);
        }
    }

    public ScriptEngine getScriptEngine() {
        try {
            final RhinoScriptEngine ret = new RhinoScriptEngine();
            ret.setEngineFactory(this);
            return ret;
        } catch (RuntimeException e) {
            LogController.CL().log(e);
            throw e;
        }
    }

    public String getMethodCallSyntax(String obj, String method, String... args) {
        String ret = obj + "." + method + "(";
        int len = args.length;
        if (len == 0) {
            ret += ")";
            return ret;
        }
        for (int i = 0; i < len; i++) {
            ret += args[i];
            if (i != len - 1) {
                ret += ",";
            } else {
                ret += ")";
            }
        }
        return ret;
    }

    public String getOutputStatement(String toDisplay) {
        StringBuffer buf = new StringBuffer();
        int len = toDisplay.length();
        buf.append("print(\"");
        for (int i = 0; i < len; i++) {
            char ch = toDisplay.charAt(i);
            switch (ch) {
            case '"':
                buf.append("\\\"");
                break;
            case '\\':
                buf.append("\\\\");
                break;
            default:
                buf.append(ch);
                break;
            }
        }
        buf.append("\")");
        return buf.toString();
    }

    public String getProgram(String... statements) {
        int len = statements.length;
        String ret = "";
        for (int i = 0; i < len; i++) {
            ret += statements[i] + ";";
        }
        return ret;
    }

    private static final List<String> NAMES;
    private static final List<String> MIME_TYPES;
    private static final List<String> EXTENSIONS;
    static {
        final List<String> names = new ArrayList<String>(6);
        names.add("js");
        names.add("rhino");
        names.add("JavaScript");
        names.add("javascript");
        NAMES = Collections.unmodifiableList(names);
        final List<String> mimeTypes = new ArrayList<String>(4);
        mimeTypes.add("application/javascript");
        mimeTypes.add("application/ecmascript");
        mimeTypes.add("text/javascript");
        mimeTypes.add("text/ecmascript");
        MIME_TYPES = Collections.unmodifiableList(mimeTypes);
        final List<String> extensions = new ArrayList<String>(1);
        extensions.add("js");
        EXTENSIONS = Collections.unmodifiableList(extensions);
    }

    public String getName() {
        return "javascript";
    }

    public String getEngineName() {
        return "Mozilla Rhino";
    }

    public String getEngineVersion() {
        return "1.6 release 2";
    }

    public String getLanguageName() {
        return "javascript";
    }

    public String getLanguageVersion() {
        return "1.6";
    }

    public List<String> getExtensions() {
        return EXTENSIONS;
    }

    public List<String> getMimeTypes() {
        return MIME_TYPES;
    }

    public List<String> getNames() {
        return NAMES;
    }
}