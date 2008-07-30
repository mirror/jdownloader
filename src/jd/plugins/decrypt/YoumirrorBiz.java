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
import java.net.URL;
import java.util.ArrayList;
import java.util.MissingResourceException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class YoumirrorBiz extends PluginForDecrypt {

    final static String HOST = "youmirror.biz";
    private String VERSION = "4.0.2";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?youmirror\\.biz/(.*/)?(file|folder)/.+", Pattern.CASE_INSENSITIVE);

    private final static Pattern patternTableRowLink = Pattern.compile("<tr[^>]*>(.*?)</tr>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    private final static Pattern patternFileName = Pattern.compile("<div align=\"left\">(.*?) \\(.*\\) <br />");

    // <a href="#" id="contentlink_0"
    // rev="/links/76b5bb4380524456c61c1afb1638fbe7/" rel="linklayer"><img
    // src="/img/download.jpg" alt="Download" width="24" height="24"
    // border="0"></a>
    static Pattern patternLink = Pattern.compile("<a href=\"#\".*rev=\"([^\"].+)\" rel=\"linklayer\">", Pattern.CASE_INSENSITIVE);

    // <br /><a href="/" onclick="createWnd('/gateway/278450/5/', '', 1000,
    // 600);" id="dlok">share.gulli.com</a>
    static Pattern patternMirrorLink = Pattern.compile("<a href=\"[^\"]*\" onclick=\"createWnd\\('([^']*)[^>]*>([^<]+)</a>", Pattern.CASE_INSENSITIVE);

    static Pattern patternJSDESFile = Pattern.compile("<script type=\"text/javascript\" src=\"/([^\"]+)\">");

    static Pattern patternJsScript = Pattern.compile("<script[^>].*>(.*)\\n[^\\n]*=\\s*(des.*).\\n[^\\n]*document.write\\(.*?</script>", Pattern.DOTALL);

    static Pattern patternHosterIframe = Pattern.compile("src\\s*=\\s*\"([^\"]+)\"");

    public YoumirrorBiz() {
        super();
    }

    
    public String getCoder() {
        return "JD-Team";
    }

    
    public String getHost() {
        return HOST;
    }

    
    
        
   

    
    public String getPluginName() {
        return HOST;
    }

    
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    
    public String getVersion() {
       String ret=new Regex("$Revision$","\\$Revision: ([\\d]*?) \\$").getFirstMatch();return ret==null?"0.0":ret;
    }

    
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Context cx = null;
        try {
            Scriptable scope = null;
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url, null, null, true);
            // just to fetch the link count
            String[] links = new Regex(reqinfo.getHtmlCode(), patternLink).getMatches(1);
            progress.setRange(links.length);
            String[] rowCandidates = new Regex(reqinfo.getHtmlCode(), patternTableRowLink).getMatches(1);
            for (String rowCandiate : rowCandidates) {
                // check if there is a link in rowCandidate
                String link = new Regex(rowCandiate, patternLink).getFirstMatch(1);
                if (null == link) {
                    continue;
                }
                // check if there is a filename in row Candidate
                String fileName = new Regex(rowCandiate, patternFileName).getFirstMatch();

                URL mirrorUrl = new URL("http://" + (getHost() + link));
                RequestInfo mirrorInfo = HTTP.getRequest(mirrorUrl, null, null, true);

                String groups[][] = new Regex(mirrorInfo.getHtmlCode(), patternMirrorLink).getMatches();

                for (String[] pair : groups) {

                    URL fileURL = new URL("http://" + getHost() + pair[0]);

                    // System.out.println(fileURL);
                    RequestInfo fileInfo = HTTP.getRequest(fileURL, null, null, true);

                    if (null == cx) {
                        // setup the JavaScrip interpreter context
                        cx = Context.enter();
                        scope = cx.initStandardObjects();

                        // fetch the file that contains the JavaScript
                        // Implementation of DES
                        String jsDESLink = new Regex(fileInfo.getHtmlCode(), patternJSDESFile).getFirstMatch();
                        URL jsDESURL = new URL("http://" + getHost() + "/" + jsDESLink);
                        RequestInfo desInfo = HTTP.getRequest(jsDESURL);

                        // compile the script and load it into context and
                        // scope
                        cx.compileString(desInfo.getHtmlCode(), "<des>", 1, null).exec(cx, scope);
                    }

                    // get the script that contains the link and the
                    // decipher recipe
                    Matcher matcher = patternJsScript.matcher(fileInfo.getHtmlCode());

                    if (!matcher.find()) {
                        logger.severe("Unable to find decypher recipe - step to next link");
                        continue;
                    }

                    // put the script together and run it
                    String decypherScript = matcher.group(1) + matcher.group(2);
                    Object result = cx.evaluateString(scope, decypherScript, "<cmd>", 1, null);

                    // fetch the result of the javascript interpreter and
                    // finally find the link :)
                    String iframe = Context.toString(result);
                    String hosterURL = new Regex(iframe, patternHosterIframe).getFirstMatch();

                    if (null == hosterURL) {
                        logger.severe("Unable to determin hosterURL - adapt patternHosterIframe");
                        continue;
                    }
                    DownloadLink downloadLink = createDownloadlink(hosterURL);
                    downloadLink.setName(fileName);
                    decryptedLinks.add(downloadLink);
                }
                progress.increase(1);
            }
        } catch (MissingResourceException e) {
            logger.severe("MissingResourceException class name: " + e.getClassName() + " key: " + e.getKey());
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            // Exit from the context.
            if (null != cx) {
                Context.exit();
            }
        }
        return decryptedLinks;
    }

    
    public boolean doBotCheck(File file) {
        return false;
    }
}