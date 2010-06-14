package jd.http.ext;

import java.net.URL;
import java.util.ArrayList;

import jd.http.Browser;
import jd.http.ext.interfaces.BrowserEnviroment;
import jd.http.ext.security.JSPermissionRestricter;
import jd.parser.Regex;

import org.appwork.utils.logging.Log;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLDivElementImpl;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLFrameElementImpl;
import org.lobobrowser.html.domimpl.HTMLIFrameElementImpl;
import org.lobobrowser.html.domimpl.HTMLLinkElementImpl;
import org.lobobrowser.html.style.AbstractCSS2Properties;
import org.w3c.dom.html2.HTMLCollection;

public class ExtBrowser {
    static {
        // this is important to disallow js to execute java methods/classes
        try {
            JSPermissionRestricter.init();

        } catch (Throwable e) {

        }

    }

    public static void main(String args[]) throws ExtBrowserException {
        ExtBrowser br = new ExtBrowser();

        br.setUserAgent(new BasicBrowserEnviroment(new String[] { ".*blank.html", ".*scorecardresearch.*", "http://www.mediafire.com/", "http://www.mediafire.com/.*?/.*?/.*" }, null));

        Browser.init();
        Browser.setLogger(Log.L);
        Browser.setVerbose(true);
        br.getCommContext().forceDebug(true);

        br.getPage("http://jdownloader.org/advert/jstest.html");

        HTMLCollection links = br.getDocument().getLinks();
        for (int i = 0; i < links.getLength(); i++) {
            HTMLLinkElementImpl l = (HTMLLinkElementImpl) links.item(i);

            if (l.toString().startsWith("http://download")) {
                HTMLDivElementImpl div = (HTMLDivElementImpl) l.getParentNode();
                AbstractCSS2Properties s = div.getStyle();
                if (s.getTop() == null || !s.getTop().equals("-250px")) {
                    String myURL = l.getAbsoluteHref();
                    System.out.println("\r\n\r\n" + myURL);
                }

            }
        }

        // for (int i = 0; i < frames.getLength(); i++) {
        // Node frame = frames.item(i);
        // if (frame.getAttributes().getNamedItem("id") != null)
        // System.out.println("ID: " +
        // frame.getAttributes().getNamedItem("id").getNodeValue() + " : " +
        // frame.getAttributes().getNamedItem("src").getNodeValue());
        //
        // }

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

    public ExtBrowser() {

        uac = new UserAgentDelegate(this);
        htmlFrameController = new HtmlFrameController(this);

        // Context.enter().setDebugger(new ExtDebugger(), null);

        commContext = new Browser();
        commContext.setFollowRedirects(true);
        commContext.setCookiesExclusive(true);

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

    private BrowserEnviroment userAgent = new BasicBrowserEnviroment(null, null);

    public BrowserEnviroment getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(BrowserEnviroment userAgent) {
        this.userAgent = userAgent;

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

}
