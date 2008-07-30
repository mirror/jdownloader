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

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class Hideurlbiz extends PluginForDecrypt {

    static private final String host = "hideurl.biz";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?hideurl\\.biz/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);
    // private String version = "1.0.0.0";

    public Hideurlbiz() {
        super();
    }

    
    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(cryptedLink);
            RequestInfo requestInfo = HTTP.getRequest(url);
            String links[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("value=\"(Download|Free Download)\" onclick=\"openlink\\('(.*?)','.*?'\\);\"", Pattern.CASE_INSENSITIVE)).getMatches();
            for (int i = 0; i < links.length; i++) {
                requestInfo = HTTP.getRequest(new URL(links[i][1]));
                if (requestInfo.getLocation() != null) {
                    /* direkt aus dem locationheader */
                    decryptedLinks.add(this.createDownloadlink(requestInfo.getLocation()));
                } else {
                    /* aus dem htmlcode den link finden */
                    String link = new Regex(requestInfo.getHtmlCode(), Pattern.compile("action=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                    if (link != null) decryptedLinks.add(this.createDownloadlink(link));
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }

    
    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

   

    
    @Override
    public String getCoder() {
        return "JD-Team";
    }

    
    @Override
    public String getHost() {
        return host;
    }

    
    @Override
    public String getPluginName() {
        return host;
    }

    
    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    @Override
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }
}
