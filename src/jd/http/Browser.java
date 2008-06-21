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
import java.net.URL;
import java.util.HashMap;

public class Browser {
    private URL currentURL;
    private Request request;
    private int limit=-1;
    private boolean doRedirects=true;
    public static HashMap<String, HashMap<String, String>> COOKIES = new HashMap<String, HashMap<String, String>>();

    public Browser() {

    }

    public String getPage(String string) {
        try {
           if(currentURL==null) this.currentURL = new URL(string);
            GetRequest request = new GetRequest(string);
            request.setFollowRedirects(doRedirects);
            forwardCookies(request);
            request.getHeaders().put("Referer", currentURL.toString());
            request.connect();
            String ret =null;
            if(request.getHttpConnection().getHeaderField("Content-Length")==null||Integer.parseInt(request.getHttpConnection().getHeaderField("Content-Length"))<=limit){
                ret = request.read();
            }
         
            updateCookies(request);
            this.request=request;
            this.currentURL = new URL(string);
            return ret;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }
        return null;
    }

    public static void forwardCookies(Request request) {
        if (request == null) return;
        String host = request.getUrl().getHost();
        HashMap<String, String> cookies = COOKIES.get(host);
        if (cookies == null) return;
        request.getCookies().putAll(cookies);

    }

    public static  void updateCookies(Request request) {
        if (request == null) return;
        String host = request.getUrl().getHost();
        HashMap<String, String> cookies = COOKIES.get(host);
        if (cookies == null) {
            cookies = new HashMap<String, String>();
            COOKIES.put(host, cookies);
        }
        cookies.putAll(request.getCookies());

    }

    public Request getRequest() {
        // TODO Auto-generated method stub
        return request;
    }

    public void setCurrentURL(String string) {
        try {
            currentURL= new URL(string);
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
    }

    public void setLoadLimit(int i) {
        this.limit=i;
        
    }

    public void setFollowRedirects(boolean b) {
      this.doRedirects=b;
        
    }
}
