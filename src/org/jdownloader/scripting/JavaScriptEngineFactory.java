package org.jdownloader.scripting;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import javax.script.SimpleScriptContext;

import org.appwork.utils.reflection.Clazz;
import org.mozilla.javascript.ConsString;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.ImporterTopLevel;
import org.mozilla.javascript.JavaAdapter;
import org.mozilla.javascript.JavaScriptException;
import org.mozilla.javascript.LazilyLoadedCtor;
import org.mozilla.javascript.NativeJavaClass;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.ScriptRuntime;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Synchronizer;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import jd.parser.Regex;
import jd.plugins.components.ThrowingRunnable;

public class JavaScriptEngineFactory {

    /**
     * ExternalScriptable is an implementation of Scriptable backed by a JSR 223 ScriptContext instance.
     *
     * @author Mike Grogan
     * @author A. Sundararajan
     * @since 1.6
     */
    public static class ExternalScriptable implements Scriptable {

        /*
         * Underlying ScriptContext that we use to store named variables of this scope.
         */
        private ScriptContext       context;
        /*
         * JavaScript allows variables to be named as numbers (indexed properties). This way arrays, objects (scopes) are treated uniformly. Note
         * that JSR 223 API supports only String named variables and so we can't store these in Bindings. Also, JavaScript allows name of the
         * property name to be even empty String! Again, JSR 223 API does not support empty name. So, we use the following fallback map to store
         * such variables of this scope. This map is not exposed to JSR 223 API. We can just script objects "as is" and need not convert.
         */
        private Map<Object, Object> indexedProps;
        // my prototype
        private Scriptable          prototype;
        // my parent scope, if any
        private Scriptable          parent;

        ExternalScriptable(ScriptContext context) {
            this(context, new HashMap<Object, Object>());
        }

        ExternalScriptable(ScriptContext context, Map<Object, Object> indexedProps) {
            if (context == null) {
                throw new NullPointerException("context is null");
            }
            this.context = context;
            this.indexedProps = indexedProps;
        }

        ScriptContext getContext() {
            return context;
        }

        private boolean isEmpty(String name) {
            return name.equals("");
        }

        /**
         * Return the name of the class.
         */
        public String getClassName() {
            return "Global";
        }

        /**
         * Returns the value of the named property or NOT_FOUND.
         *
         * If the property was created using defineProperty, the appropriate getter method is called.
         *
         * @param name
         *            the name of the property
         * @param start
         *            the object in which the lookup began
         * @return the value of the property (may be null), or NOT_FOUND
         */
        public synchronized Object get(String name, Scriptable start) {
            if (isEmpty(name)) {
                if (indexedProps.containsKey(name)) {
                    return indexedProps.get(name);
                } else {
                    return NOT_FOUND;
                }
            } else {
                synchronized (context) {
                    int scope = context.getAttributesScope(name);
                    if (scope != -1) {
                        Object value = context.getAttribute(name, scope);
                        if (value instanceof ConsString) {
                            value = value.toString();
                        }
                        return Context.javaToJS(value, this);
                    } else {
                        return NOT_FOUND;
                    }
                }
            }
        }

        /**
         * Returns the value of the indexed property or NOT_FOUND.
         *
         * @param index
         *            the numeric index for the property
         * @param start
         *            the object in which the lookup began
         * @return the value of the property (may be null), or NOT_FOUND
         */
        public synchronized Object get(int index, Scriptable start) {
            Integer key = new Integer(index);
            if (indexedProps.containsKey(index)) {
                return indexedProps.get(key);
            } else {
                return NOT_FOUND;
            }
        }

        /**
         * Returns true if the named property is defined.
         *
         * @param name
         *            the name of the property
         * @param start
         *            the object in which the lookup began
         * @return true if and only if the property was found in the object
         */
        public synchronized boolean has(String name, Scriptable start) {
            if (isEmpty(name)) {
                return indexedProps.containsKey(name);
            } else {
                synchronized (context) {
                    return context.getAttributesScope(name) != -1;
                }
            }
        }

        /**
         * Returns true if the property index is defined.
         *
         * @param index
         *            the numeric index for the property
         * @param start
         *            the object in which the lookup began
         * @return true if and only if the property was found in the object
         */
        public synchronized boolean has(int index, Scriptable start) {
            Integer key = new Integer(index);
            return indexedProps.containsKey(key);
        }

        /**
         * Sets the value of the named property, creating it if need be.
         *
         * @param name
         *            the name of the property
         * @param start
         *            the object whose property is being set
         * @param value
         *            value to set the property to
         */
        public void put(String name, Scriptable start, Object value) {
            if (start == this) {
                synchronized (this) {
                    if (isEmpty(name)) {
                        indexedProps.put(name, value);
                    } else {
                        synchronized (context) {
                            int scope = context.getAttributesScope(name);
                            if (scope == -1) {
                                scope = ScriptContext.ENGINE_SCOPE;
                            }
                            context.setAttribute(name, jsToJava(value), scope);
                        }
                    }
                }
            } else {
                start.put(name, start, value);
            }
        }

        /**
         * Sets the value of the indexed property, creating it if need be.
         *
         * @param index
         *            the numeric index for the property
         * @param start
         *            the object whose property is being set
         * @param value
         *            value to set the property to
         */
        public void put(int index, Scriptable start, Object value) {
            if (start == this) {
                synchronized (this) {
                    indexedProps.put(new Integer(index), value);
                }
            } else {
                start.put(index, start, value);
            }
        }

        /**
         * Removes a named property from the object.
         *
         * If the property is not found, no action is taken.
         *
         * @param name
         *            the name of the property
         */
        public synchronized void delete(String name) {
            if (isEmpty(name)) {
                indexedProps.remove(name);
            } else {
                synchronized (context) {
                    int scope = context.getAttributesScope(name);
                    if (scope != -1) {
                        context.removeAttribute(name, scope);
                    }
                }
            }
        }

        /**
         * Removes the indexed property from the object.
         *
         * If the property is not found, no action is taken.
         *
         * @param index
         *            the numeric index for the property
         */
        public void delete(int index) {
            indexedProps.remove(new Integer(index));
        }

        /**
         * Get the prototype of the object.
         *
         * @return the prototype
         */
        public Scriptable getPrototype() {
            return prototype;
        }

        /**
         * Set the prototype of the object.
         *
         * @param prototype
         *            the prototype to set
         */
        public void setPrototype(Scriptable prototype) {
            this.prototype = prototype;
        }

        /**
         * Get the parent scope of the object.
         *
         * @return the parent scope
         */
        public Scriptable getParentScope() {
            return parent;
        }

        /**
         * Set the parent scope of the object.
         *
         * @param parent
         *            the parent scope to set
         */
        public void setParentScope(Scriptable parent) {
            this.parent = parent;
        }

        /**
         * Get an array of property ids.
         *
         * Not all property ids need be returned. Those properties whose ids are not returned are considered non-enumerable.
         *
         * @return an array of Objects. Each entry in the array is either a java.lang.String or a java.lang.Number
         */
        public synchronized Object[] getIds() {
            String[] keys = getAllKeys();
            int size = keys.length + indexedProps.size();
            Object[] res = new Object[size];
            System.arraycopy(keys, 0, res, 0, keys.length);
            int i = keys.length;
            // now add all indexed properties
            for (Object index : indexedProps.keySet()) {
                res[i++] = index;
            }
            return res;
        }

        /**
         * Get the default value of the object with a given hint. The hints are String.class for type String, Number.class for type Number,
         * Scriptable.class for type Object, and Boolean.class for type Boolean.
         * <p>
         *
         * A <code>hint</code> of null means "no hint".
         *
         * See ECMA 8.6.2.6.
         *
         * @param hint
         *            the type hint
         * @return the default value
         */
        public Object getDefaultValue(Class typeHint) {
            for (int i = 0; i < 2; i++) {
                boolean tryToString;
                if (typeHint == ScriptRuntime.StringClass) {
                    tryToString = (i == 0);
                } else {
                    tryToString = (i == 1);
                }
                String methodName;
                Object[] args;
                if (tryToString) {
                    methodName = "toString";
                    args = ScriptRuntime.emptyArgs;
                } else {
                    methodName = "valueOf";
                    args = new Object[1];
                    String hint;
                    if (typeHint == null) {
                        hint = "undefined";
                    } else if (typeHint == ScriptRuntime.StringClass) {
                        hint = "string";
                    } else if (typeHint == ScriptRuntime.ScriptableClass) {
                        hint = "object";
                    } else if (typeHint == ScriptRuntime.FunctionClass) {
                        hint = "function";
                    } else if (typeHint == ScriptRuntime.BooleanClass || typeHint == Boolean.TYPE) {
                        hint = "boolean";
                    } else if (typeHint == ScriptRuntime.NumberClass || typeHint == ScriptRuntime.ByteClass || typeHint == Byte.TYPE || typeHint == ScriptRuntime.ShortClass || typeHint == Short.TYPE || typeHint == ScriptRuntime.IntegerClass || typeHint == Integer.TYPE || typeHint == ScriptRuntime.FloatClass || typeHint == Float.TYPE || typeHint == ScriptRuntime.DoubleClass || typeHint == Double.TYPE) {
                        hint = "number";
                    } else {
                        throw Context.reportRuntimeError("Invalid JavaScript value of type " + typeHint.toString());
                    }
                    args[0] = hint;
                }
                Object v = ScriptableObject.getProperty(this, methodName);
                if (!(v instanceof Function)) {
                    continue;
                }
                Function fun = (Function) v;
                Context cx = RhinoScriptEngine.enterContext();
                try {
                    v = fun.call(cx, fun.getParentScope(), this, args);
                } finally {
                    cx.exit();
                }
                if (v != null) {
                    if (!(v instanceof Scriptable)) {
                        return v;
                    }
                    if (typeHint == ScriptRuntime.ScriptableClass || typeHint == ScriptRuntime.FunctionClass) {
                        return v;
                    }
                    if (tryToString && v instanceof Wrapper) {
                        // Let a wrapped java.lang.String pass for a primitive
                        // string.
                        Object u = ((Wrapper) v).unwrap();
                        if (u instanceof String) {
                            return u;
                        }
                    }
                }
            }
            // fall through to error
            String arg = (typeHint == null) ? "undefined" : typeHint.getName();
            throw Context.reportRuntimeError("Cannot find default value for object " + arg);
        }

        /**
         * Implements the instanceof operator.
         *
         * @param instance
         *            The value that appeared on the LHS of the instanceof operator
         * @return true if "this" appears in value's prototype chain
         *
         */
        public boolean hasInstance(Scriptable instance) {
            // Default for JS objects (other than Function) is to do prototype
            // chasing.
            Scriptable proto = instance.getPrototype();
            while (proto != null) {
                if (proto.equals(this)) {
                    return true;
                }
                proto = proto.getPrototype();
            }
            return false;
        }

        private String[] getAllKeys() {
            ArrayList<String> list = new ArrayList<String>();
            synchronized (context) {
                for (int scope : context.getScopes()) {
                    Bindings bindings = context.getBindings(scope);
                    if (bindings != null) {
                        list.ensureCapacity(bindings.size());
                        for (String key : bindings.keySet()) {
                            list.add(key);
                        }
                    }
                }
            }
            String[] res = new String[list.size()];
            list.toArray(res);
            return res;
        }

        /**
         * We convert script values to the nearest Java value. We unwrap wrapped Java objects so that access from Bindings.get() would return
         * "workable" value for Java. But, at the same time, we need to make few special cases and hence the following function is used.
         */
        private Object jsToJava(Object jsObj) {
            if (jsObj instanceof Wrapper) {
                Wrapper njb = (Wrapper) jsObj;
                /*
                 * importClass feature of ImporterTopLevel puts NativeJavaClass in global scope. If we unwrap it, importClass won't work.
                 */
                if (njb instanceof NativeJavaClass) {
                    return njb;
                }
                /*
                 * script may use Java primitive wrapper type objects (such as java.lang.Integer, java.lang.Boolean etc) explicitly. If we unwrap, then
                 * these script objects will become script primitive types. For example,
                 *
                 * var x = new java.lang.Double(3.0); print(typeof x);
                 *
                 * will print 'number'. We don't want that to happen.
                 */
                Object obj = njb.unwrap();
                if (obj instanceof Number || obj instanceof String || obj instanceof Boolean || obj instanceof Character) {
                    // special type wrapped -- we just leave it as is.
                    return njb;
                } else {
                    // return unwrapped object for any other object.
                    return obj;
                }
            } else { // not-a-Java-wrapper
                return jsObj;
            }
        }
    }

    /**
     * This class serves as top level scope for Rhino. This class adds 3 top level functions (bindings, scope, sync) and two constructors
     * (JSAdapter, JavaAdapter).
     *
     * @author A. Sundararajan
     * @since 1.6
     */
    public static class RhinoTopLevel extends ImporterTopLevel {

        // variables defined always to help Java access from JavaScript
        // private static final String builtinVariables = "var com = Packages.com; \n" +
        // "var edu = Packages.edu; \n" + "var javax = Packages.javax; \n" +
        // "var net = Packages.net; \n" + "var org = Packages.org; \n";
        RhinoTopLevel(Context cx, RhinoScriptEngine engine) {
            super(cx);
            this.engine = engine;
            // initialize JSAdapter lazily. Reduces footprint & startup time.
            new LazilyLoadedCtor(this, "JSAdapter", "com.sun.script.javascript.JSAdapter", false);
            /*
             * initialize JavaAdapter. We can't lazy initialize this because lazy initializer attempts to define a new property. But, JavaAdapter is an
             * exisiting property that we overwrite.
             */
            JavaAdapter.init(cx, this, false);
            // add top level functions
            String names[] = { "bindings", "scope", "sync" };
            defineFunctionProperties(names, RhinoTopLevel.class, ScriptableObject.DONTENUM);
            // define built-in variables
            // cx.evaluateString(this, builtinVariables, "<builtin>", 1, null);
        }

        /**
         * The bindings function takes a JavaScript scope object of type ExternalScriptable and returns the underlying Bindings instance.
         *
         * var page = scope(pageBindings); with (page) { // code that uses page scope } var b = bindings(page); // operate on bindings here.
         */
        public static Object bindings(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            if (args.length == 1) {
                Object arg = args[0];
                if (arg instanceof Wrapper) {
                    arg = ((Wrapper) arg).unwrap();
                }
                if (arg instanceof ExternalScriptable) {
                    ScriptContext ctx = ((ExternalScriptable) arg).getContext();
                    Bindings bind = ctx.getBindings(ScriptContext.ENGINE_SCOPE);
                    return Context.javaToJS(bind, ScriptableObject.getTopLevelScope(thisObj));
                }
            }
            return cx.getUndefinedValue();
        }

        /**
         * The scope function creates a new JavaScript scope object with given Bindings object as backing store. This can be used to create a script
         * scope based on arbitrary Bindings instance. For example, in webapp scenario, a 'page' level Bindings instance may be wrapped as a scope
         * and code can be run in JavaScripe 'with' statement:
         *
         * var page = scope(pageBindings); with (page) { // code that uses page scope }
         */
        public static Object scope(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            if (args.length == 1) {
                Object arg = args[0];
                if (arg instanceof Wrapper) {
                    arg = ((Wrapper) arg).unwrap();
                }
                if (arg instanceof Bindings) {
                    ScriptContext ctx = new SimpleScriptContext();
                    ctx.setBindings((Bindings) arg, ScriptContext.ENGINE_SCOPE);
                    Scriptable res = new ExternalScriptable(ctx);
                    res.setPrototype(ScriptableObject.getObjectPrototype(thisObj));
                    res.setParentScope(ScriptableObject.getTopLevelScope(thisObj));
                    return res;
                }
            }
            return cx.getUndefinedValue();
        }

        /**
         * The sync function creates a synchronized function (in the sense of a Java synchronized method) from an existing function. The new
         * function synchronizes on the <code>this</code> object of its invocation. js> var o = { f : sync(function(x) { print("entry");
         * Packages.java.lang.Thread.sleep(x*1000); print("exit"); })}; js> thread(function() {o.f(5);}); entry js> thread(function() {o.f(5);});
         * js> exit entry exit
         */
        public static Object sync(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
            if (args.length == 1 && args[0] instanceof Function) {
                return new Synchronizer((Function) args[0]);
            } else {
                throw Context.reportRuntimeError("wrong argument(s) for sync");
            }
        }

        RhinoScriptEngine getScriptEngine() {
            return engine;
        }

        private RhinoScriptEngine engine;
    }

    /**
     * Represents compiled JavaScript code.
     *
     * @author Mike Grogan
     * @since 1.6
     */
    public static class RhinoCompiledScript extends CompiledScript {

        private RhinoScriptEngine engine;
        private Script            script;

        RhinoCompiledScript(RhinoScriptEngine engine, Script script) {
            this.engine = engine;
            this.script = script;
        }

        public Object eval(ScriptContext context) throws ScriptException {
            Object result = null;
            Context cx = RhinoScriptEngine.enterContext();
            try {
                Scriptable scope = engine.getRuntimeScope(context);
                Object ret = script.exec(cx, scope);
                result = engine.unwrapReturnValue(ret);
            } catch (RhinoException re) {
                int line = (line = re.lineNumber()) == 0 ? -1 : line;
                String msg;
                if (re instanceof JavaScriptException) {
                    msg = String.valueOf(((JavaScriptException) re).getValue());
                } else {
                    msg = re.toString();
                }
                ScriptException se = new ScriptException(msg, re.sourceName(), line);
                se.initCause(re);
                throw se;
            } finally {
                Context.exit();
            }
            return result;
        }

        public ScriptEngine getEngine() {
            return engine;
        }
    }

    public static class InterfaceImplementor {

        private Invocable engine;

        /** Creates a new instance of Invocable */
        public InterfaceImplementor(Invocable engine) {
            this.engine = engine;
        }

        public class InterfaceImplementorInvocationHandler implements InvocationHandler {

            private Invocable engine;
            private Object    thiz;

            public InterfaceImplementorInvocationHandler(Invocable engine, Object thiz) {
                this.engine = engine;
                this.thiz = thiz;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws java.lang.Throwable {
                // give chance to convert input args
                args = convertArguments(method, args);
                Object result = engine.invokeMethod(thiz, method.getName(), args);
                // give chance to convert the method result
                return convertResult(method, result);
            }
        }

        public <T> T getInterface(Object thiz, Class<T> iface) throws ScriptException {
            if (iface == null || !iface.isInterface()) {
                throw new IllegalArgumentException("interface Class expected");
            }
            return iface.cast(Proxy.newProxyInstance(iface.getClassLoader(), new Class[] { iface }, new InterfaceImplementorInvocationHandler(engine, thiz)));
        }

        // called to convert method result after invoke
        protected Object convertResult(Method method, Object res) throws ScriptException {
            // default is identity conversion
            return res;
        }

        // called to convert method arguments before invoke
        protected Object[] convertArguments(Method method, Object[] args) throws ScriptException {
            // default is identity conversion
            return args;
        }
    }

    /**
     * Implementation of <code>ScriptEngine</code> using the Mozilla Rhino interpreter.
     *
     * @author Mike Grogan
     * @author A. Sundararajan
     * @since 1.6
     */
    public static class RhinoScriptEngine extends AbstractScriptEngine implements Invocable, Compilable {

        private static final boolean DEBUG = false;
        /*
         * Scope where standard JavaScript objects and our extensions to it are stored. Note that these are not user defined engine level global
         * variables. These are variables have to be there on all compliant ECMAScript scopes. We put these standard objects in this top level.
         */
        private RhinoTopLevel        topLevel;
        /*
         * map used to store indexed properties in engine scope refer to comment on 'indexedProps' in ExternalScriptable.java.
         */
        private Map<Object, Object>  indexedProps;
        private ScriptEngineFactory  factory;
        private InterfaceImplementor implementor;

        /**
         * Creates a new instance of RhinoScriptEngine
         */
        public RhinoScriptEngine() {
            Context cx = enterContext();
            try {
                topLevel = new RhinoTopLevel(cx, this);
            } finally {
                Context.exit();
            }
            indexedProps = new HashMap<Object, Object>();
            // construct object used to implement getInterface
            implementor = new InterfaceImplementor(this) {

                protected Object convertResult(Method method, Object res) throws ScriptException {
                    Class desiredType = method.getReturnType();
                    if (desiredType == Void.TYPE) {
                        return null;
                    } else {
                        return Context.jsToJava(res, desiredType);
                    }
                }
            };
        }

        public Object eval(Reader reader, ScriptContext ctxt) throws ScriptException {
            Object ret;
            Context cx = enterContext();
            try {
                Scriptable scope = getRuntimeScope(ctxt);
                String filename = (String) get(ScriptEngine.FILENAME);
                filename = filename == null ? "<Unknown source>" : filename;
                ret = cx.evaluateReader(scope, reader, filename, 1, null);
            } catch (RhinoException re) {
                if (DEBUG) {
                    re.printStackTrace();
                }
                int line = (line = re.lineNumber()) == 0 ? -1 : line;
                String msg;
                if (re instanceof JavaScriptException) {
                    msg = String.valueOf(((JavaScriptException) re).getValue());
                } else {
                    msg = re.toString();
                }
                ScriptException se = new ScriptException(msg, re.sourceName(), line);
                se.initCause(re);
                throw se;
            } catch (IOException ee) {
                throw new ScriptException(ee);
            } finally {
                cx.exit();
            }
            return unwrapReturnValue(ret);
        }

        public Object eval(String script, ScriptContext ctxt) throws ScriptException {
            if (script == null) {
                throw new NullPointerException("null script");
            }
            return eval(new StringReader(script), ctxt);
        }

        public ScriptEngineFactory getFactory() {
            if (factory != null) {
                return factory;
            } else {
                return new CustomRhinoScriptEngineFactory();
            }
        }

        public Bindings createBindings() {
            return new SimpleBindings();
        }

        // Invocable methods
        public Object invokeFunction(String name, Object... args) throws ScriptException, NoSuchMethodException {
            return invoke(null, name, args);
        }

        public Object invokeMethod(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
            if (thiz == null) {
                throw new IllegalArgumentException("script object can not be null");
            }
            return invoke(thiz, name, args);
        }

        private Object invoke(Object thiz, String name, Object... args) throws ScriptException, NoSuchMethodException {
            Context cx = enterContext();
            try {
                if (name == null) {
                    throw new NullPointerException("method name is null");
                }
                if (thiz != null && !(thiz instanceof Scriptable)) {
                    thiz = cx.toObject(thiz, topLevel);
                }
                Scriptable engineScope = getRuntimeScope(context);
                Scriptable localScope = (thiz != null) ? (Scriptable) thiz : engineScope;
                Object obj = ScriptableObject.getProperty(localScope, name);
                if (!(obj instanceof Function)) {
                    throw new NoSuchMethodException("no such method: " + name);
                }
                Function func = (Function) obj;
                Scriptable scope = func.getParentScope();
                if (scope == null) {
                    scope = engineScope;
                }
                Object result = func.call(cx, scope, localScope, wrapArguments(args));
                return unwrapReturnValue(result);
            } catch (RhinoException re) {
                if (DEBUG) {
                    re.printStackTrace();
                }
                int line = (line = re.lineNumber()) == 0 ? -1 : line;
                throw new ScriptException(re.toString(), re.sourceName(), line);
            } finally {
                cx.exit();
            }
        }

        public <T> T getInterface(Class<T> clasz) {
            try {
                return implementor.getInterface(null, clasz);
            } catch (ScriptException e) {
                return null;
            }
        }

        public <T> T getInterface(Object thiz, Class<T> clasz) {
            if (thiz == null) {
                throw new IllegalArgumentException("script object can not be null");
            }
            try {
                return implementor.getInterface(thiz, clasz);
            } catch (ScriptException e) {
                return null;
            }
        }

        private static final String printSource = "function print(str, newline) {                \n" + "    if (typeof(str) == 'undefined') {         \n" + "        str = 'undefined';                    \n" + "    } else if (str == null) {                 \n" + "        str = 'null';                         \n" + "    }                                         \n" + "    var out = context.getWriter();            \n" + "    out.print(String(str));                   \n" + "    if (newline) out.print('\\n');            \n" + "    out.flush();                              \n" + "}\n" + "function println(str) {                       \n" + "    print(str, true);                         \n" + "}";

        Scriptable getRuntimeScope(ScriptContext ctxt) {
            if (ctxt == null) {
                throw new NullPointerException("null script context");
            }
            // we create a scope for the given ScriptContext
            Scriptable newScope = new ExternalScriptable(ctxt, indexedProps);
            // Set the prototype of newScope to be 'topLevel' so that
            // JavaScript standard objects are visible from the scope.
            newScope.setPrototype(topLevel);
            // define "context" variable in the new scope
            newScope.put("context", newScope, ctxt);
            // define "print", "println" functions in the new scope
            Context cx = enterContext();
            try {
                cx.evaluateString(newScope, printSource, "print", 1, null);
            } finally {
                cx.exit();
            }
            return newScope;
        }

        // Compilable methods
        public CompiledScript compile(String script) throws ScriptException {
            return compile(new StringReader(script));
        }

        public CompiledScript compile(java.io.Reader script) throws ScriptException {
            CompiledScript ret = null;
            Context cx = enterContext();
            try {
                String fileName = (String) get(ScriptEngine.FILENAME);
                if (fileName == null) {
                    fileName = "<Unknown Source>";
                }
                Scriptable scope = getRuntimeScope(context);
                Script scr = cx.compileReader(scope, script, fileName, 1, null);
                ret = new RhinoCompiledScript(this, scr);
            } catch (Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
                throw new ScriptException(e);
            } finally {
                cx.exit();
            }
            return ret;
        }

        // package-private helpers
        static Context enterContext() {
            // call this always so that initializer of this class runs
            // and initializes custom wrap factory and class shutter.
            return Context.enter();
        }

        void setEngineFactory(ScriptEngineFactory fac) {
            factory = fac;
        }

        Object[] wrapArguments(Object[] args) {
            if (args == null) {
                return Context.emptyArgs;
            }
            Object[] res = new Object[args.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = Context.javaToJS(args[i], topLevel);
            }
            return res;
        }

        Object unwrapReturnValue(Object result) {
            if (result instanceof Wrapper) {
                result = ((Wrapper) result).unwrap();
            }
            return result instanceof Undefined ? null : result;
        }

        public static void main(String[] args) throws Exception {
            if (args.length == 0) {
                System.out.println("No file specified");
                return;
            }
            InputStreamReader r = new InputStreamReader(new FileInputStream(args[0]));
            ScriptEngine engine = new RhinoScriptEngine();
            engine.put("x", "y");
            engine.put(ScriptEngine.FILENAME, args[0]);
            engine.eval(r);
            System.out.println(engine.get("x"));
        }
    }

    public static class CustomRhinoScriptEngineFactory implements ScriptEngineFactory {

        public CustomRhinoScriptEngineFactory() {
        }

        public Object getParameter(String key) {
            if (key.equals(ScriptEngine.NAME)) {
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
                throw new IllegalArgumentException("Invalid key");
            }
        }

        public ScriptEngine getScriptEngine() {
            try {
                RhinoScriptEngine ret = new RhinoScriptEngine();
                ret.setEngineFactory(this);
                return ret;
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            } finally {
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

        private static List<String> NAMES;
        private static List<String> MIME_TYPES;
        private static List<String> EXTENSIONS;
        static {
            NAMES = new ArrayList<String>(6);
            NAMES.add("js");
            NAMES.add("rhino");
            NAMES.add("JavaScript");
            NAMES.add("javascript");
            NAMES = Collections.unmodifiableList(NAMES);
            MIME_TYPES = new ArrayList<String>(4);
            MIME_TYPES.add("application/javascript");
            MIME_TYPES.add("application/ecmascript");
            MIME_TYPES.add("text/javascript");
            MIME_TYPES.add("text/ecmascript");
            MIME_TYPES = Collections.unmodifiableList(MIME_TYPES);
            EXTENSIONS = new ArrayList<String>(1);
            EXTENSIONS.add("js");
            EXTENSIONS = Collections.unmodifiableList(EXTENSIONS);
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

    public static class CustomizedScriptEngineManager extends ScriptEngineManager {

        @Override
        public ScriptEngine getEngineByName(String shortName) {
            final ScriptEngine ret = super.getEngineByName(shortName);
            if (ret instanceof RhinoScriptEngine) {
                return ret;
            }
            throw new RuntimeException("Bad ScriptEngine: " + ret.getClass());
        }

        public CustomizedScriptEngineManager() {
            final CustomRhinoScriptEngineFactory factory = new CustomRhinoScriptEngineFactory();
            this.registerEngineName("javascript", factory);
            this.registerEngineName("js", factory);
            this.registerEngineName("JavaScript", factory);
        }
    }

    public static ScriptEngineManager getScriptEngineManager(Object requestor) {
        return new CustomizedScriptEngineManager();
    }

    public static Object jsonToJavaObject(String string) throws Exception {
        try {
            return org.appwork.storage.JSonStorage.restoreFromString(string, new org.appwork.storage.TypeRef<Object>() {
            });
        } catch (Throwable e) {
            // jd 09 workaround. use rhino
            try {
                ScriptEngineManager mgr = getScriptEngineManager(null);
                ScriptEngine engine = mgr.getEngineByName("JavaScript");
                engine.eval("var response=" + string + ";");
                return toMap(engine.get("response"));
            } catch (ScriptException e2) {
                throw new Exception("JavaScript to Java failed: " + string);
            }
        }
    }

    /**
     * Converts single json parser Objects to long. Works around 2 issues: 1. Often people use Strings instead of number data types in json. 2.
     * Our parser decides whether to use Long or Integer but most times we need Long also we always need more code to ensure to get the connect
     * data type. This makes it easier.
     */
    public static long toLong(final Object o, final long defaultvalue) {
        long lo = defaultvalue;
        try {
            if (o instanceof String) {
                lo = Long.parseLong((String) o);
            } else if (o instanceof Long) {
                lo = ((Long) o).longValue();
            } else {
                lo = ((Integer) o).intValue();
            }
        } catch (final Throwable e) {
        }
        return lo;
    }

    /**
     * @param json
     *            Object that was previously parsed via any jsonToJavaObject function.
     *
     * @param crawlstring
     *            String that contains info on what to get in this format: /String/String/{number representing the number of the object inside
     *            the ArrayList}/String/and_so_on
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Object walkJson(final Object json, final String crawlstring) {
        if (crawlstring == null) {
            return null;
        }
        final String[] crawlparts = crawlstring.split("/");
        String walkedBegin = "";
        String walkedRemains = crawlstring;
        Object currentObject = json;
        try {
            for (int i = 0; i < crawlparts.length; i++) {
                // speed up code by existing when null. also future proof against npe
                if (currentObject == null) {
                    return null;
                }
                final String crawlpart = crawlparts[i];
                walkedBegin = walkedBegin.length() == 0 ? crawlpart : walkedBegin + "/" + crawlpart;
                // last value wont have trailing /
                walkedRemains = walkedRemains.replaceFirst("^" + Pattern.quote(crawlpart) + "/?", "");
                final int crawlentry_number;
                {
                    final String crawlpart_crawlnumber = new Regex(crawlpart, "\\{(\\d*)\\}").getMatch(0);
                    if (crawlpart_crawlnumber != null) {
                        if (crawlpart_crawlnumber.matches("\\d+")) {
                            crawlentry_number = Integer.parseInt(crawlpart_crawlnumber);
                        } else {
                            crawlentry_number = -1;
                        }
                    } else {
                        crawlentry_number = -2;
                    }
                }
                if (currentObject instanceof LinkedHashMap && crawlentry_number >= -1) {
                    /*
                     * Get Object from LinkedHashMap from desired position - this is a rare case but good to have it covered here in this way!
                     */
                    final LinkedHashMap<String, Object> tmp_linkedmap = (LinkedHashMap<String, Object>) currentObject;
                    currentObject = tmp_linkedmap.get(crawlpart);
                    final Iterator<Entry<String, Object>> it = tmp_linkedmap.entrySet().iterator();
                    int position = 0;
                    while (it.hasNext()) {
                        final Entry<String, Object> entry = it.next();
                        if (crawlentry_number >= 0 && position == crawlentry_number) {
                            currentObject = entry.getKey();
                            break;
                        }
                        position++;
                    }
                } else if (currentObject instanceof LinkedHashMap) {
                    final LinkedHashMap<String, Object> tmp_linkedmap = (LinkedHashMap<String, Object>) currentObject;
                    currentObject = tmp_linkedmap.get(crawlpart);
                } else if (currentObject instanceof HashMap) {
                    final HashMap<String, Object> tmp_map = (HashMap<String, Object>) currentObject;
                    currentObject = tmp_map.get(crawlpart);
                } else if (currentObject instanceof ArrayList) {
                    if (crawlentry_number == -2) {
                        /* crawlpart does not match the DataType we have --> Return null */
                        return null;
                    }
                    final ArrayList<Object> tmp_list = (ArrayList) currentObject;
                    if (crawlentry_number == -1) {
                        // look forward to match next key
                        for (final Object list : tmp_list) {
                            final Object test = walkJson(list, walkedRemains);
                            if (test != null) {
                                return test;
                            }
                        }
                        // nullify
                        currentObject = null;
                    } else {
                        currentObject = tmp_list.get(crawlentry_number);
                    }
                } else {
                    break;
                }
            }
        } catch (final Exception e) {
            e.printStackTrace();
            return null;
        }
        return currentObject;
    }

    public static Object toMap(Object obj) {
        if (obj == null) {
            return null;
        }
        if (Clazz.isPrimitiveWrapper(obj.getClass())) {
            return obj;
        } else if (obj instanceof String) {
            return obj;
        }
        if (obj instanceof org.mozilla.javascript.NativeObject) {
            HashMap<String, Object> ret = new HashMap<String, Object>();
            for (Object s : ((org.mozilla.javascript.NativeObject) obj).getIds()) {
                if (s instanceof String) {
                    ret.put((String) s, toMap(((org.mozilla.javascript.NativeObject) obj).get(s)));
                } else {
                    System.out.println("Unknown Key: " + s + " " + s.getClass());
                    ret.put(s + "", toMap(((org.mozilla.javascript.NativeObject) obj).get(s)));
                }
            }
            return ret;
        } else if (obj instanceof org.mozilla.javascript.NativeArray) {
            ArrayList<Object> ret = new ArrayList<Object>();
            for (int i = 0; i < ((org.mozilla.javascript.NativeArray) obj).getLength(); i++) {
                ret.add(toMap(((org.mozilla.javascript.NativeArray) obj).get(i)));
            }
            return ret;
        } else if (obj instanceof net.sourceforge.htmlunit.corejs.javascript.NativeObject) {
            HashMap<String, Object> ret = new HashMap<String, Object>();
            for (Object s : ((net.sourceforge.htmlunit.corejs.javascript.NativeObject) obj).getIds()) {
                if (s instanceof String) {
                    ret.put((String) s, toMap(((net.sourceforge.htmlunit.corejs.javascript.NativeObject) obj).get(s)));
                } else {
                    System.out.println("Unknown Key: " + s + " " + s.getClass());
                    ret.put(s + "", toMap(((org.mozilla.javascript.NativeObject) obj).get(s)));
                }
            }
            return ret;
        } else if (obj instanceof net.sourceforge.htmlunit.corejs.javascript.NativeArray) {
            ArrayList<Object> ret = new ArrayList<Object>();
            for (int i = 0; i < ((net.sourceforge.htmlunit.corejs.javascript.NativeArray) obj).getLength(); i++) {
                ret.add(toMap(((net.sourceforge.htmlunit.corejs.javascript.NativeArray) obj).get(i)));
            }
            return ret;
        }
        return null;
    }

    public static Map<String, Object> jsonToJavaMap(String string) throws Exception {
        return (Map<String, Object>) jsonToJavaObject(string);
    }

    public static <T extends Exception> void runTrusted(ThrowingRunnable<T> runnable) throws T {
        try {
            JSRhinoPermissionRestricter.TRUSTED_THREAD.put(Thread.currentThread(), true);
            JSHtmlUnitPermissionRestricter.TRUSTED_THREAD.put(Thread.currentThread(), true);
            runnable.run();
        } finally {
            JSHtmlUnitPermissionRestricter.TRUSTED_THREAD.remove(Thread.currentThread());
            JSRhinoPermissionRestricter.TRUSTED_THREAD.remove(Thread.currentThread());
        }
    }
}
