package jd.http.ext;

import java.awt.event.MouseEvent;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import jd.http.Request;
import jd.http.ext.events.HtmlFrameControllerEventsender;
import jd.http.ext.events.JSInteraction;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;

import org.appwork.utils.logging.Log;
import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.FormInput;
import org.lobobrowser.html.HtmlObject;
import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.HTMLFrameElementImpl;
import org.lobobrowser.html.io.WritableLineReader;
import org.lobobrowser.html.js.Executor;
import org.lobobrowser.html.js.Window;
import org.lobobrowser.html.js.event.BasicEvent;
import org.lobobrowser.html.js.event.JSEventListener;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.w3c.dom.html2.HTMLCollection;
import org.w3c.dom.html2.HTMLElement;
import org.w3c.dom.html2.HTMLLinkElement;

/**
 * This class represents a single Frame, and thus a single rendercontext. A
 * frame may contain various subframes.
 * 
 * Subframes may work on the DOM of it's parentframe.
 * 
 * @author thomas
 * 
 */
public class HtmlFrameController implements HtmlRendererContext, ExtHTMLFrameElement {

    private static final String DOM_CONTENT_LOADED = "DOMContentLoaded";
    private ExtBrowser extBrowser;
    private HTMLDocumentImpl htmlDocument;
    private HtmlFrameController parentFrameController;

    private HtmlFrameControllerEventsender eventSender;
    private ExtHTMLFrameImpl frame = null;
    private HashMap<String, ArrayList<JSEventListener>> listenerMap = new HashMap<String, ArrayList<JSEventListener>>();

    public ExtHTMLFrameImpl getFrame() {
        return frame;
    }

    public HtmlFrameController(ExtBrowser extBrowser) {
        this.extBrowser = extBrowser;
        eventSender = new HtmlFrameControllerEventsender();

    }

    public HtmlFrameController(ExtBrowser extBrowser, ExtHTMLFrameImpl f) {
        this.extBrowser = extBrowser;
        eventSender = new HtmlFrameControllerEventsender();

        frame = f;

    }

    public HtmlFrameControllerEventsender getEventSender() {
        return eventSender;
    }

    public void alert(String arg0) {

        getEventSender().fireEvent(new JSInteraction(this, JSInteraction.Types.ALERT, arg0));
    }

    public void back() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void blur() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void close() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public boolean confirm(String arg0) {
        JSInteraction event = new JSInteraction(this, JSInteraction.Types.CONFIRM, arg0);
        getEventSender().fireEvent(event);
        return event.getAnswer() == JSInteraction.AnswerTypes.OK;

    }

    public BrowserFrame createBrowserFrame() {
        return new ExtBrowserFrame(this);
    }

    public void focus() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void forward() {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public String getCurrentURL() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public String getDefaultStatus() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    /**
     * For internal use only. Use
     * {@link ExtBrowser#getFrames(HtmlFrameController)} or
     * {@link ExtBrowser#getFrames(HTMLFrameElementImpl)}
     */
    public HTMLCollection getFrames() {
        return htmlDocument.getFrames();
    }

    public int getHistoryLength() {
        // TODO Auto-generated method stub
        return 0;
    }

    public HtmlObject getHtmlObject(HTMLElement arg0) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public String getName() {
        if (frame == null) {
            return "";
        } else {
            return frame.getName();
        }
    }

    public String getNextURL() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public HtmlRendererContext getOpener() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public HtmlRendererContext getParent() {
        // TODO Auto-generated method stub

        return parentFrameController;
    }

    public String toString() {
        if (htmlDocument == null) return "no Doc";
        return this.htmlDocument.getInnerHTML();
    }

    public String getPreviousURL() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public String getStatus() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public HtmlRendererContext getTop() {
        HtmlRendererContext ancestor = this.parentFrameController;
        if (ancestor == null) { return this; }
        return ancestor.getTop();
    }

    public UserAgentContext getUserAgentContext() {
        // TODO Auto-generated method stub
        return extBrowser.getUserAgentContext();
    }

    public void goToHistoryURL(String arg0) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public boolean isClosed() {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public boolean isImageLoadingEnabled() {
        // TODO Auto-generated method stub
        return extBrowser.getBrowserEnviroment().isImageLoadingEnabled();
    }

    public boolean isVisitedLink(HTMLLinkElement arg0) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void linkClicked(HTMLElement arg0, URL arg1, String arg2) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void moveInHistory(int arg0) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public void navigate(URL arg0, String arg1) {
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;

    }

    public boolean onContextMenu(HTMLElement arg0, MouseEvent arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public boolean onDoubleClick(HTMLElement arg0, MouseEvent arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public boolean onMouseClick(HTMLElement arg0, MouseEvent arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void onMouseOut(HTMLElement arg0, MouseEvent arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void onMouseOver(HTMLElement arg0, MouseEvent arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public HtmlRendererContext open(String arg0, String arg1, String arg2, boolean arg3) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public HtmlRendererContext open(URL arg0, String arg1, String arg2, boolean arg3) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public String prompt(String arg0, String arg1) {
        JSInteraction event = new JSInteraction(this, JSInteraction.Types.PROMPT, arg0, arg1);
        getEventSender().fireEvent(event);
        return event.getAnswerString();
    }

    public void reload() {
        // TODO Auto-generated method stub
        // TODO: we do not reload
    }

    public void resizeBy(int arg0, int arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void resizeTo(int arg0, int arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void scroll(int arg0, int arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void scrollBy(int arg0, int arg1) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void setDefaultStatus(String arg0) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void setOpener(HtmlRendererContext arg0) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void setStatus(String arg0) {
        // TODO Auto-generated method stub
        RuntimeException e = new RuntimeException("Not implemented");
        Log.exception(e);
        throw e;
    }

    public void submitForm(String method, URL action, String target, String encoding, FormInput[] formInputs) {
        try {
            final String actualMethod = method.toUpperCase();

            if (actualMethod.equals("GET")) {
                if (formInputs == null || formInputs.length == 0) {
                    Request request = extBrowser.getCommContext().createGetRequest(action + "");
                    if (extBrowser.getBrowserEnviroment().doLoadContent(request)) {
                        extBrowser.getCommContext().openRequestConnection(request);
                        extBrowser.getCommContext().loadConnection(null);
                        extBrowser.getBrowserEnviroment().prepareContents(extBrowser.getCommContext().getRequest());
                    } else {
                        return;
                    }

                } else {
                    Form form = new Form();
                    form.setAction(action + "");
                    form.setMethod(Form.MethodType.GET);
                    for (FormInput i : formInputs) {
                        if (i.isFile()) {

                            InputField iff = new InputField(i.getName(), i.getTextValue());
                            iff.setFileToPost(i.getFileValue());
                            iff.setType("file");
                            form.addInputField(iff);
                        } else {
                            form.addInputField(new InputField(i.getName(), i.getTextValue()));
                        }

                    }

                    Request request = extBrowser.getCommContext().createFormRequest(form);
                    if (extBrowser.getBrowserEnviroment().doLoadContent(request)) {
                        extBrowser.getCommContext().openRequestConnection(request);
                        extBrowser.getCommContext().loadConnection(null);
                    } else {
                        return;
                    }
                }

            } else {
                RuntimeException e = new RuntimeException("Not implemented");
                Log.exception(e);
                throw e;
            }

            eval();

        } catch (Exception e) {
            Log.exception(e);
        }
    }

    public HTMLDocumentImpl getDocument() {
        return htmlDocument;
    }

    public void eval() throws ExtBrowserException {
        WritableLineReader wis = new WritableLineReader(new StringReader(extBrowser.getCommContext() + ""));

        htmlDocument = new HTMLDocumentImpl(this.getUserAgentContext(), this, wis, extBrowser.getCommContext().getURL() + "");

        try {
            htmlDocument.load();
            if (this.frame != null) {
                if (frame.getSrc() != null && frame.getSrc().length() > 0) {
                    dispatch(new BasicEvent("load", frame.getImpl()));
                }
            } else {
                dispatch(new BasicEvent("DOMContentLoaded", this.getDocument()));
                dispatch(new BasicEvent("load", Window.getWindow(this)));
            }
            if (extBrowser.getBrowserEnviroment().isAutoProcessSubFrames()) {
                processFrames();
            }

        } catch (Exception e) {
            throw new ExtBrowserException(e);
        }
    }

    private synchronized ArrayList<JSEventListener> getListenerList(String eventName) {

        ArrayList<JSEventListener> ret = listenerMap.get(eventName);
        if (ret == null) {
            ret = new ArrayList<JSEventListener>();
            listenerMap.put(eventName, ret);
        }
        return ret;

    }

    public void processFrames() throws ExtBrowserException {
        for (ExtHTMLFrameImpl frame : extBrowser.getFrames(this)) {
            processFrame(frame);
        }

    }

    public ExtHTMLFrameImpl processFrame(ExtHTMLFrameImpl f) throws ExtBrowserException {
        if (f.getSrc() != null) {
            HtmlFrameController rContext = new HtmlFrameController(this.extBrowser, f);
            rContext.parentFrameController = this;
            try {
                rContext.submitForm("GET", new URL(extBrowser.getCommContext().getURL(f.getSrc())), null, null, null);
                f.setBrowserFrame(rContext.createBrowserFrame());
                return f;
            } catch (Exception e) {
                throw new ExtBrowserException(e);
            }

        }
        return null;
    }

    public Regex getRegex(String pattern) {
        // TODO Auto-generated method stub
        return new Regex(htmlDocument.getInnerHTML(), pattern);
    }

    public String getFrameBorder() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getHeight() {
        // TODO Auto-generated method stub
        return null;
    }

    public HtmlFrameController getHtmlFrameController() {
        // TODO Auto-generated method stub
        return this;
    }

    public HTMLElementImpl getImpl() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getMarginHeight() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getMarginWidth() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getScrolling() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSrc() {
        // TODO Auto-generated method stub
        return this.htmlDocument.getDocumentURI();
    }

    public String getWidth() {
        // TODO Auto-generated method stub
        return null;
    }

    public Scriptable getScriptableScope() {
        return Window.getWindow(this).getWindowScope();

    }

    public String evalAndReturn(String script) throws ExtBrowserException {
        Scriptable scope = getScriptableScope();
        Context cx;
        try {
            cx = Executor.createContext(new URL(this.getDocument().getURL()), extBrowser.getUserAgentContext());

            Object result = cx.evaluateString(scope, "function qwertfbkdsiebdfia432hjfd83j(){return " + script + ";} qwertfbkdsiebdfia432hjfd83j();", "<cmd>", 1, null);
            String ret = Context.toString(result);
            Context.exit();
            return ret;
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            throw new ExtBrowserException(e);
        }
    }

    public String getScriptableVariable(String string) {

        Scriptable scope = getScriptableScope();
        return (String) scope.get(string, scope);
    }

    public void addEventListener(JSEventListener jsEventListener) {
        System.out.println("Listener " + jsEventListener);
        removeEventListener(jsEventListener);
        this.getListenerList(jsEventListener.getName()).add(jsEventListener);
    }

    public void removeEventListener(JSEventListener jsEventListener) {
        ArrayList<JSEventListener> listener = this.getListenerList(jsEventListener.getName());
        for (Iterator<JSEventListener> it = listener.iterator(); it.hasNext();) {
            JSEventListener l = it.next();
            if (l.getName().equals(jsEventListener.getName()) && (l.getCallback() == jsEventListener.getCallback()) && l.getOwner() == jsEventListener.getOwner()) {
                it.remove();
                return;
            }
        }

    }

    public void removeEventListeners(String name) {
        this.getListenerList(name).clear();
    }

    @SuppressWarnings("unchecked")
    public boolean dispatch(BasicEvent basicEvent) {
        ArrayList<JSEventListener> listener = (ArrayList<JSEventListener>) this.getListenerList(basicEvent.getType()).clone();
        boolean ret = false;
        for (Iterator<JSEventListener> it = listener.iterator(); it.hasNext();) {
            ret |= it.next().dispatch(basicEvent);
            // how to handle ret value?

        }
        return ret;
    }

    // public void addEventListener(Object nodeImpl, String type, BaseFunction
    // listener, Object useCapture) {
    //
    // System.out.println("Registered event " + type + " on " + nodeImpl +
    // ": \r\n" + listener);
    // removeEventListener(nodeImpl, type, listener, useCapture);
    // this.getEventListener(type).add(new DOMEventListener(this, nodeImpl,
    // type, listener, useCapture));
    //
    // }
    //
    // public void removeEventListener(Object htmlElementImpl, String type,
    // BaseFunction listener, Object useCapture) {
    // ArrayList<DOMEventListener> list = getEventListener(type);
    // for (Iterator<DOMEventListener> it = list.iterator(); it.hasNext();) {
    // DOMEventListener next = it.next();
    // if (next.getAction() == listener && next.getNode() == htmlElementImpl) {
    // it.remove();
    // }
    // }
    //
    // }

}
