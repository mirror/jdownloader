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

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Rapidlayerin extends PluginForDecrypt {

    static private final String HOST = "rapidlayer.in";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?rapidlayer\\.in/go/[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public Rapidlayerin() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {

        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        String page = br.getPage(parameter);
        String fun_id = new Regex(page, Pattern.compile("function (.*?)\\(", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String all = "function " + new Regex(page, Pattern.compile("function (.*?)a=", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String dec = new Regex(page, Pattern.compile("a=(.*?);document.write", Pattern.CASE_INSENSITIVE)).getMatch(0);

        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ " + all + "\nreturn " + fun_id + "(" + dec + ")} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        String link = Encoding.htmlDecode(Context.toString(result));
        decryptedLinks.add(createDownloadlink(link));
        Context.exit();

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
