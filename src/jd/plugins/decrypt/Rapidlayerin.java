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
import jd.utils.JDUtilities;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Rapidlayerin extends PluginForDecrypt {

    static private final String HOST = "rapidlayer.in";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidlayer\\.in/go/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    private String CODER = "JD-Team";

    public Rapidlayerin() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        String cryptedLink = parameter;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            String link = null;
            URL url = new URL(cryptedLink);
            RequestInfo requestInfo = HTTP.getRequest(url);

            /* DownloadLink entschlüsseln */
            String fun_id = new Regex(requestInfo.getHtmlCode(), Pattern.compile("function (.*?)\\(", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            String all = "function " + new Regex(requestInfo.getHtmlCode(), Pattern.compile("function (.*?)a=", Pattern.CASE_INSENSITIVE)).getFirstMatch();
            String dec = new Regex(requestInfo.getHtmlCode(), Pattern.compile("a=(.*?);document.write", Pattern.CASE_INSENSITIVE)).getFirstMatch();

            Context cx = Context.enter();
            Scriptable scope = cx.initStandardObjects();
            String fun = "function f(){ " + all + "\nreturn " + fun_id + "(" + dec + ")} f()";
            Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
            if ((link = JDUtilities.htmlDecode(Context.toString(result))) != null) {
                decryptedLinks.add(createDownloadlink(link));
            } else {
                return null;
            }
            Context.exit();
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
        return CODER;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
