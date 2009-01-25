//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.parser;

import org.lobobrowser.html.HtmlRendererContext;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.domimpl.HTMLDocumentImpl;
import org.lobobrowser.html.io.WritableLineReader;

public class Document extends HTMLDocumentImpl {
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
