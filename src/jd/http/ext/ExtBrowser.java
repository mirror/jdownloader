package jd.http.ext;

import java.net.URL;
import java.util.ArrayList;

import jd.http.Browser;
import jd.http.Request;
import jd.http.ext.events.ExtBrowserEvent;
import jd.http.ext.events.ExtBrowserEventSender;
import jd.http.ext.events.ExtBrowserListener;
import jd.http.ext.events.JSInteraction;
import jd.http.ext.interfaces.BrowserEnviroment;
import jd.http.ext.security.JSPermissionRestricter;
import jd.parser.Regex;

import org.appwork.utils.logging.Log;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLDivElementImpl;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLFormElementImpl;
import org.lobobrowser.html.domimpl.HTMLFrameElementImpl;
import org.lobobrowser.html.domimpl.HTMLIFrameElementImpl;
import org.w3c.dom.Element;
import org.w3c.dom.html2.HTMLCollection;

public class ExtBrowser {
    static {
        // this is important to disallow js to execute java methods/classes
        try {
            JSPermissionRestricter.init();

        } catch (Throwable e) {

        }

    }

    @SuppressWarnings("all")
    public static void main(String args[]) throws ExtBrowserException, InterruptedException {

        ExtBrowser br = new ExtBrowser();
        br.setBrowserEnviroment(new FullBrowserEnviroment() {
            public void prepareContents(Request req) {

                req = req;

            }

        });

        Browser.init();
        Browser.setLogger(Log.L);
        Browser.setVerbose(true);
        br.getCommContext().forceDebug(true);

        ExtBrowser eb = new ExtBrowser();
        eb.setBrowserEnviroment(new jd.http.ext.BasicBrowserEnviroment(new String[] { ".*templates/linkto.*", ".*cdn.mediafire.com/css/.*", ".*/blank.html" }, null));
        try {
            eb.getPage("http://www.mediafire.com/?dzmzuzmh2md");
            eb.waitForFrame("workframe2", 10000);
            System.out.println(eb.getHtmlText());

            HTMLCollection links = eb.getDocument().getLinks();

            for (int i = 0; i < links.getLength(); i++) {
                org.lobobrowser.html.domimpl.HTMLLinkElementImpl l = (org.lobobrowser.html.domimpl.HTMLLinkElementImpl) links.item(i);
                if (RendererUtilities.isVisible(l)) {
                    String inner = l.getInnerHTML();

                    if (inner.toLowerCase().contains("start download")) {
                        HTMLDivElementImpl div = (HTMLDivElementImpl) l.getParentNode();

                        String myURL = l.getAbsoluteHref();

                        eb.getCommContext().openGetConnection(myURL);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // br.getPage("http://ifile.it/uvkog7y");
        // br.getPage("http://jdownloader.net:8081/advert/jstest.html");
        // ArrayList<HTMLFormElementImpl> forms = br.getForms(null);
        // String text = br.getHtmlText();
        // HTMLSpanElementImpl button = (HTMLSpanElementImpl)
        // br.getElementByID(null, "req_btn2");
        // br.getInputController().click(button);
        // // br.getInputController().mouseClick(button, null, 1, 4);
        //
        // br = br;
        // String str = br.getHtmlText();
        // str = str;
        // HTMLCollection links = br.getDocument().getLinks();
        // for (int i = 0; i < links.getLength(); i++) {
        // HTMLLinkElementImpl l = (HTMLLinkElementImpl) links.item(i);
        //
        // if (l.toString().startsWith("http://download")) {
        // HTMLDivElementImpl div = (HTMLDivElementImpl) l.getParentNode();
        // AbstractCSS2Properties s = div.getStyle();
        // if (s.getTop() == null || !s.getTop().equals("-250px")) {
        // String myURL = l.getAbsoluteHref();
        // System.out.println("\r\n\r\n" + myURL);
        // }
        //
        // }
        // }

        // for (int i = 0; i < frames.getLength(); i++) {
        // Node frame = frames.item(i);
        // if (frame.getAttributes().getNamedItem("id") != null)
        // System.out.println("ID: " +
        // frame.getAttributes().getNamedItem("id").getNodeValue() + " : " +
        // frame.getAttributes().getNamedItem("src").getNodeValue());
        //
        // }

    }

    // TODO: this should use AppWorks Statemachine later
    public void waitForFrame(final String frameID, int msTimeout) throws InterruptedException {

        for (ExtHTMLFrameImpl frame : this.getFrames(null)) {
            if (frameID.equalsIgnoreCase(frame.getID()) && frame.getInternalFrameController().isLoaded()) {

                System.out.println("Frame already loaded");
                return;

            }
        }
        ExtBrowserListener listener = new ExtBrowserListener() {

            public void onFrameEvent(ExtBrowserEvent event) {
                if (event instanceof FrameStatusEvent) {
                    if (((FrameStatusEvent) event).getType() == FrameStatusEvent.Types.EVAL_END && frameID.equalsIgnoreCase(((ExtHTMLFrameElement) ((FrameStatusEvent) event).getCaller()).getID())) {
                        synchronized (this) {

                            this.notify();
                        }

                    }

                }

            }

        };
        this.getEventSender().addListener(listener);

        synchronized (listener) {

            listener.wait(msTimeout);

        }

    }

    private InputController getInputController() {

        return this.inputController;
    }

    private Element getElementByID(ExtHTMLFrameElement frame, String id) {
        if (frame == null) frame = this.getFrameController();
        Element ret = frame.getDocument().getElementById(id);
        return ret;

    }

    private ArrayList<HTMLFormElementImpl> getForms(ExtHTMLFrameElement frame) {
        if (frame == null) frame = this.getFrameController();
        HTMLCollection forms = frame.getDocument().getForms();
        return getList(forms, new ArrayList<HTMLFormElementImpl>());

    }

    @SuppressWarnings("unchecked")
    private <E> ArrayList<E> getList(HTMLCollection forms, ArrayList<E> arrayList) {
        for (int i = 0; i < forms.getLength(); i++) {
            arrayList.add((E) forms.item(i));
        }
        return arrayList;
    }

    /**
     * 
     * @return The root {@link jd.http.ext.FrameController} of thes Instance
     */
    public FrameController getFrameController() {

        return this.htmlFrameController;
    }

    public String getHtmlText() {
        // TODO Auto-generated method stub
        return this.getDocument().getInnerHTML();
    }

    public HTMLDocumentImpl getDocument() {
        return this.htmlFrameController.getDocument();

    }

    // private HTMLDocumentImpl document;
    private FrameController htmlFrameController;
    private UserAgentDelegate uac;
    private Browser commContext;
    private String url;
    private InputController inputController;
    private ExtBrowserEventSender eventSender;

    public ExtBrowser() {

        uac = new UserAgentDelegate(this);

        // Context.enter().setDebugger(new ExtDebugger(), null);
        eventSender = new ExtBrowserEventSender();
        commContext = new Browser();
        commContext.setFollowRedirects(true);
        commContext.setCookiesExclusive(true);
        htmlFrameController = new FrameController(this);
        inputController = new InputController();

    }

    public ExtBrowserEventSender getEventSender() {
        return eventSender;
    }

    public void setInputController(InputController inputController) {
        this.inputController = inputController;
    }

    public Regex getRegex(final String pattern) {
        return htmlFrameController.getRegex(pattern);
    }

    public ExtBrowser(Browser br) {
        uac = new UserAgentDelegate(this);
        eventSender = new ExtBrowserEventSender();
        // Context.enter().setDebugger(new ExtDebugger(), null);

        commContext = br.cloneBrowser();
        commContext.setFollowRedirects(true);
        commContext.setCookiesExclusive(true);
        htmlFrameController = new FrameController(this);

        inputController = new InputController();
    }

    public void getPage(String url) throws ExtBrowserException {

        this.url = url;
        try {
            htmlFrameController.submitForm("GET", new URL(commContext.getURL(url)), null, null, null);

        } catch (Exception e) {
            throw new ExtBrowserException(e);
        }

    }

    // private HTMLDocumentImpl createDocument(String url) {
    //       
    // // HTMLDocumentImpl document = new HTMLDocumentImpl(uac, renderContext,
    // null, url);
    //       
    // return document;
    // }

    private BrowserEnviroment browserEnviroment = new BasicBrowserEnviroment(null, null);

    public BrowserEnviroment getBrowserEnviroment() {
        return browserEnviroment;
    }

    public void setBrowserEnviroment(BrowserEnviroment userAgent) {
        this.browserEnviroment = userAgent;

        commContext.setCookiesExclusive(false);
        commContext.setCookiesExclusive(true);

    }

    public UserAgentContext getUserAgentContext() {
        // TODO Auto-generated method stub
        return uac;
    }

    public Browser getCommContext() {
        // TODO Auto-generated method stub
        return commContext;
    }

    public void eval(Browser br) throws ExtBrowserException {

        this.url = br.getURL();
        commContext = br.cloneBrowser();
        commContext.setFollowRedirects(true);
        commContext.setCookiesExclusive(true);
        commContext.setRequest(br.getRequest());
        htmlFrameController.setCommContext(commContext);
        try {
            htmlFrameController.eval();

        } catch (Exception e) {
            throw new ExtBrowserException(e);
        }
    }

    public String getUrl() {
        return url;
    }

    /**
     * Returns all Subframes of the givven baseFrame
     * 
     * @param baseFrame
     * @return
     */
    public ArrayList<ExtHTMLFrameImpl> getFrames(ExtHTMLFrameElement baseFrame) {
        if (baseFrame == null) baseFrame = getFrameController();
        HTMLCollection frames = baseFrame.getHtmlFrameController().getFrames();
        ArrayList<ExtHTMLFrameImpl> ret = new ArrayList<ExtHTMLFrameImpl>();
        for (int i = 0; i < frames.getLength(); i++) {
            if (frames.item(i) instanceof HTMLFrameElementImpl) {
                ret.add(new ExtHTMLFrameImpl((HTMLFrameElementImpl) frames.item(i)));
            } else {
                if (frames.item(i) instanceof HTMLIFrameElementImpl) {
                    ret.add(new ExtHTMLFrameImpl((HTMLIFrameElementImpl) frames.item(i)));
                }
            }
        }
        return ret;

    }

    public String getScriptableVariable(String string) {
        return this.getFrameController().getScriptableVariable(string);

    }

    public void cleanUp() {
        // TODO Auto-generated method stub

    }

    public void onFrameEvalStart(FrameController htmlFrameController2) {
        getEventSender().fireEvent(new FrameStatusEvent(this, htmlFrameController2, FrameStatusEvent.Types.EVAL_START));

    }

    public void onFrameLoadStart(FrameController htmlFrameController2) {
        getEventSender().fireEvent(new FrameStatusEvent(this, htmlFrameController2, FrameStatusEvent.Types.LOAD_START));

    }

    public void onFrameLoadEnd(FrameController htmlFrameController2) {
        getEventSender().fireEvent(new FrameStatusEvent(this, htmlFrameController2, FrameStatusEvent.Types.LOAD_END));

    }

    public void onFrameEvalEnd(FrameController htmlFrameController2) {
        getEventSender().fireEvent(new FrameStatusEvent(this, htmlFrameController2, FrameStatusEvent.Types.EVAL_END));

    }

    public void onAlert(FrameController htmlFrameController2, String arg0) {
        getEventSender().fireEvent(new JSInteraction(this, htmlFrameController2, JSInteraction.Types.ALERT, arg0));
    }

    public boolean onConfirm(String arg0, FrameController htmlFrameController) {
        JSInteraction event = new JSInteraction(this, htmlFrameController, JSInteraction.Types.CONFIRM, arg0);
        getEventSender().fireEvent(event);
        return event.getAnswer() == JSInteraction.AnswerTypes.OK;
    }

    public String onPrompt(String arg0, String arg1, FrameController htmlFrameController2) {
        JSInteraction event = new JSInteraction(this, htmlFrameController2, JSInteraction.Types.PROMPT, arg0, arg1);
        getEventSender().fireEvent(event);
        return event.getAnswerString();
    }

}
