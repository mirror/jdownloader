package jd.http.ext;

import org.lobobrowser.html.BrowserFrame;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLElementImpl;
import org.lobobrowser.html.domimpl.HTMLFrameElementImpl;
import org.lobobrowser.html.domimpl.HTMLIFrameElementImpl;

public class ExtHTMLFrameImpl implements ExtHTMLFrameElement {

    public static enum Type {
        IFRAME, FRAME
    }

    private HTMLElementImpl _impl;
    private Type type;

    public ExtHTMLFrameImpl(HTMLElementImpl item) {
        this._impl = item;
        type = (_impl instanceof HTMLFrameElementImpl) ? Type.FRAME : Type.IFRAME;
    }

    public Type getType() {
        return type;
    }

    public String getFrameBorder() {
        if (type == Type.FRAME) {
            return ((HTMLFrameElementImpl) _impl).getFrameBorder();
        } else {
            return ((HTMLIFrameElementImpl) _impl).getFrameBorder();
        }
    }

    public String getHeight() {
        if (type == Type.FRAME) {
            return "0 px";
        } else {
            return ((HTMLIFrameElementImpl) _impl).getHeight();
        }
    }

    public HtmlFrameController getHtmlFrameController() {
        if (type == Type.FRAME) {
            return (HtmlFrameController) ((HTMLFrameElementImpl) _impl).getHtmlRendererContext();
        } else {
            return (HtmlFrameController) ((HTMLIFrameElementImpl) _impl).getHtmlRendererContext();
        }
    }

    public HTMLElementImpl getImpl() {
        return _impl;
    }

    public String getMarginHeight() {
        if (type == Type.FRAME) {
            return ((HTMLFrameElementImpl) _impl).getMarginHeight();
        } else {
            return ((HTMLIFrameElementImpl) _impl).getMarginHeight();
        }
    }

    public String getMarginWidth() {
        if (type == Type.FRAME) {
            return ((HTMLFrameElementImpl) _impl).getMarginWidth();
        } else {
            return ((HTMLIFrameElementImpl) _impl).getMarginWidth();
        }
    }

    public String getName() {
        if (type == Type.FRAME) {
            return ((HTMLFrameElementImpl) _impl).getName();
        } else {
            return ((HTMLIFrameElementImpl) _impl).getName();
        }
    }

    public String getScrolling() {
        if (type == Type.FRAME) {
            return ((HTMLFrameElementImpl) _impl).getScrolling();
        } else {
            return ((HTMLIFrameElementImpl) _impl).getScrolling();
        }
    }

    public String getSrc() {
        if (type == Type.FRAME) {
            return ((HTMLFrameElementImpl) _impl).getSrc();
        } else {
            return ((HTMLIFrameElementImpl) _impl).getSrc();
        }
    }

    public String getWidth() {
        if (type == Type.FRAME) {
            return "0 px";
        } else {
            return ((HTMLIFrameElementImpl) _impl).getWidth();
        }
    }

    public void setBrowserFrame(BrowserFrame createBrowserFrame) {
        if (type == Type.FRAME) {
            ((HTMLFrameElementImpl) _impl).setBrowserFrame(createBrowserFrame);
        } else {
            ((HTMLIFrameElementImpl) _impl).setBrowserFrame(createBrowserFrame);
        }
    }

    public HTMLDocumentImpl getDocument() {
        return this.getHtmlFrameController().getDocument();
    }

}
