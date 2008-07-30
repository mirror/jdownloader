//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

package jd.http;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import jd.parser.Form;
import jd.plugins.HTTPConnection;

public class PostRequest extends Request {
    private HashMap<String, String> postData = new HashMap<String, String>();

    public PostRequest(Form form) {
        super(form.getAction());

        postData = form.vars;
    }

    public PostRequest(String url) {
        super(url);

    }

    public HashMap<String, String> getPostData() {
        return postData;
    }

    public String getPostDataString() {
        if (postData.isEmpty()) { return null; }

        StringBuffer buffer = new StringBuffer();

        for (Map.Entry<String, String> entry : postData.entrySet()) {
            buffer.append("&");
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(entry.getValue());
        }
        return buffer.toString().substring(1);
    }

    @Override
    public void postRequest(HTTPConnection httpConnection) throws IOException {
        httpConnection.setDoOutput(true);
        String parameter = getPostDataString();
        if (!postData.isEmpty() && parameter != null) {
            parameter = parameter.trim();
            httpConnection.setRequestProperty("Content-Length", parameter.length() + "");
            httpConnection.connect();
            OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
            wr.write(parameter);
            wr.flush();
            wr.close();
        }
    }

    @Override
    public void preRequest(HTTPConnection httpConnection) throws IOException {
        httpConnection.setRequestMethod("POST");

    }

    public void setPostVariable(String key, String value) {
        postData.put(key, value);

    }

    public void setPostVariableString(String vars) {
        postData.putAll(Request.parseQuery(vars));
    }

}
