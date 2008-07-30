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

import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class ftp2share extends PluginForDecrypt {
    static private final String host = "ftp2share.net";
    private String version = "1.0.0.0";
    private static final Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?ftp2share\\.net/folder/[a-zA-Z0-9\\-]+/(.*?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?ftp2share\\.net/file/[a-zA-Z0-9\\-]+/(.*?)", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternSupported_Folder.pattern() + "|" + patternSupported_File.pattern(), Pattern.CASE_INSENSITIVE);

    public ftp2share() {
        super();
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = (String) parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url;
            RequestInfo requestInfo;

            if (cryptedLink.matches(patternSupported_Folder.pattern())) {
                if (!cryptedLink.contains("?system")) cryptedLink = cryptedLink + "?system=*";
                url = new URL(cryptedLink);
                requestInfo = HTTP.getRequest(url);
                String links[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"javascript\\:go\\('(.*?)'\\)\">", Pattern.CASE_INSENSITIVE)).getMatches();
                for (int i = 0; i < links.length; i++) {
                    String link = JDUtilities.Base64Decode(JDUtilities.filterString(links[i][0], "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/"));
                    decryptedLinks.add(this.createDownloadlink(link));
                }
            } else if (cryptedLink.matches(patternSupported_File.pattern())) {
                url = new URL(cryptedLink);
                requestInfo = HTTP.getRequest(url);
                Form[] forms = requestInfo.getForms();
                if (forms.length > 1) requestInfo = forms[1].getRequestInfo();
                String links[][] = new Regex(requestInfo.getHtmlCode(), Pattern.compile("<a href=\"javascript\\:go\\('(.*?)'\\)\">", Pattern.CASE_INSENSITIVE)).getMatches();
                for (int i = 0; i < links.length; i++) {
                    String link = JDUtilities.Base64Decode(JDUtilities.filterString(links[i][0], "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/"));
                    decryptedLinks.add(this.createDownloadlink(link));
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

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return host;
    }

    
    
        
    

    
    public String getPluginName() {
        return host;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
        return new Regex("$Revision$","\\$Revision: ([\\d]*?)\\$").getFirstMatch();
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}
