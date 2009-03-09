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

package jd.http.requests;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;

public class PostRequest extends Request {
    private ArrayList<RequestVariable> postData;
    private String postDataString = null;

    public PostRequest(Form form) throws MalformedURLException {
        super(form.getAction(null));

        postData = new ArrayList<RequestVariable>();
    }

    public PostRequest(String url) throws MalformedURLException {
        super(url);
        postData = new ArrayList<RequestVariable>();

    }

    // public Request toHeadRequest() throws MalformedURLException {

    public String getPostDataString() {
        if (postData.isEmpty()) { return null; }

        StringBuilder buffer = new StringBuilder();

        for (RequestVariable rv : postData) {
            if (rv.getKey() != null) {
                buffer.append("&");
                buffer.append(rv.getKey());
                buffer.append("=");
                if (rv.getValue() != null) {
                    buffer.append(rv.getValue());
                } else {
                    buffer.append("");
                }
            }
        }
        if (buffer.length() == 0) return "";
        return buffer.toString().substring(1);
    }

    public void setPostDataString(String post) {
        this.postDataString = post;
    }

    @Override
    public void postRequest(URLConnectionAdapter httpConnection) throws IOException {
        httpConnection.setDoOutput(true);
        String parameter = postDataString != null ? postDataString : getPostDataString();
        if (parameter != null) {
            if (postDataString == null) parameter = parameter.trim();
            httpConnection.setRequestProperty("Content-Length", parameter.length() + "");
            httpConnection.connect();

            OutputStreamWriter wr = new OutputStreamWriter(httpConnection.getOutputStream());
            if (parameter != null) {
                wr.write(parameter);
            }
            wr.flush();
            wr.close();

        } else {
            httpConnection.setRequestProperty("Content-Length", "0");
        }
    }

    @Override
    public void preRequest(URLConnectionAdapter httpConnection) throws IOException {
        httpConnection.setRequestMethod("POST");

    }

    public void addVariable(String key, String value) {
        postData.add(new RequestVariable(key, value));

    }

    public static ArrayList<RequestVariable> variableMaptoArray(HashMap<String, String> post) {
        ArrayList<RequestVariable> ret = new ArrayList<RequestVariable>();
        Entry<String, String> next = null;
        for (Iterator<Entry<String, String>> it = post.entrySet().iterator(); it.hasNext();) {
            next = it.next();
            ret.add(new RequestVariable(next.getKey(), next.getValue()));
        }
        return ret;
    }

    public void addAll(HashMap<String, String> post) {

    }

    public void addAll(ArrayList<RequestVariable> post) {
        this.postData.addAll(post);

    }

}
