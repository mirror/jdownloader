package jd.http.ext;

import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLIFrameElementImpl;
import org.lobobrowser.html.io.WritableLineReader;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

public class ExtHTMLDocumentImpl extends HTMLDocumentImpl {

    public ExtHTMLDocumentImpl(UserAgentContext userAgentContext, FrameController htmlFrameController, WritableLineReader wis, String string) {
        super(userAgentContext, htmlFrameController, wis, string);

    }

    public void write(String text) {
        super.write(text);
    }

    public Element createElement(String tagName) throws DOMException {
        Element ret = super.createElement(tagName);
        if ("iframe".equalsIgnoreCase(tagName)) {

            ((HTMLIFrameElementImpl) ret).setBrowserFrame(((FrameController) getHtmlRendererContext()).createParentFrameController(new ExtHTMLFrameImpl((HTMLIFrameElementImpl) ret)));
        }
        return ret;
    }

    public Element getElementById(String elementId) {
        if (elementId.equals("workframe2")) {
            System.out.println("workframe2");
        }
        return super.getElementById(elementId);
    }

    public void writeln(String text) {
        super.writeln(text);
    }

}
