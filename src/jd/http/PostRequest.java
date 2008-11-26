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
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import jd.parser.Form;

public class PostRequest extends Request {
    private HashMap<String, String> postData = new HashMap<String, String>();
    private String postDataString = null;

    public PostRequest(Form form) throws MalformedURLException {
        super(form.getAction(null));

        postData = form.getVarsMap();
    }

    public PostRequest(String url) throws MalformedURLException {
        super(url);

    }

    // public Request toHeadRequest() throws MalformedURLException {
    //        
    //  
    // PostRequest ret = new PostRequest(this.getUrl()+"") {
    //
    // @Override
    // public void preRequest(HTTPConnection httpConnection) throws IOException
    // {
    // httpConnection.setRequestMethod("HEAD");
    //                
    // }
    // };
    // ret.setConnectTimeout(this.getConnectTimeout());
    // ret.getCookies().addAll(this.getCookies());
    // ret.setFollowRedirects(this.isFollowRedirects());
    // ret.getHeaders().putAll(this.getHeaders());
    // ret.setProxy(this.getProxyip(), this.getProxyport());
    // ret.postData=this.postData;
    // ret.postDataString=this.postDataString;
    // 
    // ret.setReadTimeout(this.getReadTimeout());
    //    
    // ret.httpConnection=this.httpConnection;
    //       
    // return ret;
    //        
    // }

    public HashMap<String, String> getPostData() {
        return postData;
    }

    public String getPostDataString() {
        if (postData.isEmpty()) { return null; }

        StringBuilder buffer = new StringBuilder();

        for (Map.Entry<String, String> entry : postData.entrySet()) {
            buffer.append("&");
            buffer.append(entry.getKey());
            buffer.append("=");
            buffer.append(entry.getValue());
        }
        return buffer.toString().substring(1);
    }

    public void setPostDataString(String post) {
        this.postDataString = post;
    }

    @Override
    public void postRequest(HTTPConnection httpConnection) throws IOException {
        httpConnection.setDoOutput(true);
        String parameter = postDataString != null ? postDataString : getPostDataString();
        if (parameter != null) {
            parameter = parameter.trim();
            httpConnection.setRequestProperty("Content-Length", parameter.length() + "");
            httpConnection.connect();
            httpConnection.post(parameter);

        } else {
            httpConnection.setRequestProperty("Content-Length", "0");
        }
    }

    @Override
    public void preRequest(HTTPConnection httpConnection) throws IOException {
        httpConnection.setRequestMethod("POST");

    }

    public void setPostVariable(String key, String value) {
        postData.put(key, value);

    }

    public void setPostVariableString(String vars) throws MalformedURLException {
        postData.putAll(Request.parseQuery(vars));
    }

}
