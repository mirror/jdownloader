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

package jd.plugins.optional.interfaces;

import java.util.HashMap;

public class Request {

    private HashMap<String, String> headers = new HashMap<String, String>();

    private HashMap<String, String> parameter = new HashMap<String, String>();

    private String requestUrl;

    private String requestType;

    private String httpType;

    private String data = "";

    private long contentLength=-1;

    private byte[] postData;

    public Request() {
    }

    protected void setData(String data) {
        this.data = data;
    }

    protected void setRequestUrl(String requestUrl) {
        this.requestUrl = requestUrl;
    }

    protected void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    protected void setHttpType(String httpType) {
        this.httpType = httpType;
    }

    protected void addHeader(String key, String value) {
        headers.put(key, value);
        if (key.equalsIgnoreCase("Content-Length")) {
            try{
            contentLength = Long.parseLong(value.trim());
            }catch(Exception e){
                
            }
        }
    }

    protected void setHeader(HashMap<String, String> headers) {
        this.headers = headers;
    }

    protected void addParameter(String key, String value) {
        parameter.put(key, value);
    }

    protected void setParameter(HashMap<String, String> parameter) {
        this.parameter = parameter;
    }

    public String getData() {
        return data;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public String getRequestType() {
        return requestType;
    }

    public String getHttpType() {
        return httpType;
    }

    public String getHeader(String key) {
        return headers.get(key);
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public String getParameter(String key) {
        return parameter.get(key);
    }

    public HashMap<String, String> getParameters() {
        return parameter;
    }

    public String toString() {
        return this.headers + "";
    }

    public long getContentLength() {
      return contentLength;
    }

    public void setPostData(byte[] buffer) {
      postData=buffer;
        
    }

    public byte[] getPostData() {
        return postData;
    }
}
