package jd.parser;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.io.WritableLineReader;
public class Document extends HTMLDocumentImpl{
    public StringBuffer content = new StringBuffer();
    public Document(HtmlRendererContext rcontext) {
        super(rcontext);
        // TODO Auto-generated constructor stub
    }
    public Document(UserAgentContext ucontext, HtmlRendererContext rcontext, WritableLineReader reader, String documentURI) {
        super(ucontext, rcontext, reader, documentURI);
    }

    @Override
    public void write(String arg0) {
        content.append(arg0);
        super.write(arg0);
    }

}
