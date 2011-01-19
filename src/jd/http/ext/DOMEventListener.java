package jd.http.ext;

import java.net.URL;

import org.lobobrowser.html.domimpl.NodeImpl;
import org.lobobrowser.html.js.Executor;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.js.JavaScript;
import org.mozilla.javascript.BaseFunction;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.Document;
import org.w3c.dom.events.Event;
import org.w3c.dom.events.EventListener;

public class DOMEventListener implements EventListener {

    private FrameController owner;
    private Object          node;
    private String          type;
    private BaseFunction    action;
    private Object          useCapture;

    public DOMEventListener(FrameController htmlFrameController, Object nodeImpl, String type, BaseFunction listener, Object useCapture2) {
        this.owner = htmlFrameController;
        this.node = nodeImpl;
        this.type = type;
        this.action = listener;
        this.useCapture = useCapture2;
    }

    public FrameController getOwner() {
        return owner;
    }

    public Object getNode() {
        return node;
    }

    public String getType() {
        return type;
    }

    public BaseFunction getAction() {
        return action;
    }

    public Object isUseCapture() {
        return useCapture;
    }

    public void handleEvent(Event evt) {
        Context ctx = null;
        try {
            Document doc = owner.getDocument();
            if (doc == null) { throw new IllegalStateException("Element does not belong to a document."); }

            if (node instanceof Window) {

                ctx = Executor.createContext(new URL(((Window) node).getDocumentNode().getDocumentURI()), ((Window) node).getUserAgentContext());

            } else {
                ctx = Executor.createContext(((NodeImpl) node).getDocumentURL(), ((NodeImpl) node).getUserAgentContext());
            }

            Scriptable scope = (Scriptable) doc.getUserData(Executor.SCOPE_KEY);
            if (scope == null) { throw new IllegalStateException("Scriptable (scope) instance was expected to be keyed as UserData to document using " + Executor.SCOPE_KEY); }
            JavaScript js = JavaScript.getInstance();
            Scriptable thisScope = (Scriptable) js.getJavascriptObject(node, scope);

            Scriptable eventScriptable = (Scriptable) js.getJavascriptObject(null, thisScope);
            // e={bubbles:true,cancelable:true}
            // ScriptableObject.defineProperty(thisScope, "event",
            // eventScriptable, ScriptableObject.READONLY);
            System.out.println("Run Function \r\n" + ctx.decompileFunction(action, 1));
            Object result = action.call(ctx, thisScope, thisScope, new Object[1]);

        } catch (Throwable thrown) {
            Log.exception(thrown);

        } finally {
            if (ctx != null) Context.exit();
        }
    }

    public void handleEvent() {
        handleEvent(null);

    }

}
