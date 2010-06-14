package jd.http.ext;

import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.domimpl.HTMLElementImpl;

public interface ExtHTMLFrameElement {

    public String getFrameBorder();

    public String getHeight();

    public String getMarginHeight();

    public String getMarginWidth();

    public String getName();

    public String getScrolling();

    public String getSrc();

    public String getWidth();

    public HTMLDocumentImpl getDocument();

    public HtmlFrameController getHtmlFrameController();

    public HTMLElementImpl getImpl();
}
