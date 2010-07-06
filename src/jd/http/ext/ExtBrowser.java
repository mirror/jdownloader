package jd.http.ext;

import java.net.URL;
import java.util.ArrayList;

import jd.http.Browser;
import jd.http.Request;
import jd.http.ext.interfaces.BrowserEnviroment;
import jd.http.ext.security.JSPermissionRestricter;
import jd.parser.Regex;

import org.appwork.utils.logging.Log;
import org.lobobrowser.html.UserAgentContext;
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
        br.setUserAgent(new FullBrowserEnviroment() {
            public void prepareContents(Request req) {

                req = req;

            }

        });

        Browser.init();
        Browser.setLogger(Log.L);
        Browser.setVerbose(true);
        br.getCommContext().forceDebug(true);

        ExtBrowser eb = new ExtBrowser();
        eb.setUserAgent(new jd.http.ext.BasicBrowserEnviroment(null, null));
        try {
            eb.getPage("http://www.mediafire.com/?dzmzuzmh2md");

            System.out.println(eb.getHtmlText());
            eb.cleanUp();
            org.w3c.dom.html2.HTMLCollection links = eb.getDocument().getLinks();
            String txt = eb.getHtmlText();
            for (int i = 0; i < links.getLength(); i++) {
                org.lobobrowser.html.domimpl.HTMLLinkElementImpl l = (org.lobobrowser.html.domimpl.HTMLLinkElementImpl) links.item(i);
                String inner = l.getInnerHTML();
                System.out.println(inner + " - " + l);
                if (inner.toLowerCase().contains("start download")) {
                    org.lobobrowser.html.domimpl.HTMLDivElementImpl div = (org.lobobrowser.html.domimpl.HTMLDivElementImpl) l.getParentNode();
                    System.out.println("          *    " + inner + " - " + div.getOuterHTML());
                    org.lobobrowser.html.style.AbstractCSS2Properties s = div.getStyle();
                    if (!"-250px".equalsIgnoreCase(s.getTop()) && !"none".equalsIgnoreCase(s.getDisplay())) {
                        String myURL = l.getAbsoluteHref();

                    }

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        while (true) {
            Thread.sleep(100);
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

    private InputController getInputController() {

        return this.inputController;
    }

    private Element getElementByID(ExtHTMLFrameElement frame, String id) {
        if (frame == null) frame = this.getHtmlFrameController();
        Element ret = frame.getDocument().getElementById(id);
        return ret;

    }

    private ArrayList<HTMLFormElementImpl> getForms(ExtHTMLFrameElement frame) {
        if (frame == null) frame = this.getHtmlFrameController();
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
     * @return The root {@link jd.http.ext.HtmlFrameController} of thes Instance
     */
    public HtmlFrameController getHtmlFrameController() {

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
    private HtmlFrameController htmlFrameController;
    private UserAgentDelegate uac;
    private Browser commContext;
    private String url;
    private InputController inputController;

    public ExtBrowser() {

        uac = new UserAgentDelegate(this);
        htmlFrameController = new HtmlFrameController(this);

        // Context.enter().setDebugger(new ExtDebugger(), null);

        commContext = new Browser();
        commContext.setFollowRedirects(true);
        commContext.setCookiesExclusive(true);
        inputController = new InputController();

    }

    public void setInputController(InputController inputController) {
        this.inputController = inputController;
    }

    public Regex getRegex(final String pattern) {
        return htmlFrameController.getRegex(pattern);
    }

    public ExtBrowser(Browser br) {
        uac = new UserAgentDelegate(this);
        htmlFrameController = new HtmlFrameController(this);

        // Context.enter().setDebugger(new ExtDebugger(), null);

        commContext = br.cloneBrowser();
        commContext.setFollowRedirects(true);
        commContext.setCookiesExclusive(true);
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

    public void setUserAgent(BrowserEnviroment userAgent) {
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
        if (baseFrame == null) baseFrame = getHtmlFrameController();
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
        return this.getHtmlFrameController().getScriptableVariable(string);

    }

    public void cleanUp() {
        // TODO Auto-generated method stub

    }

}
