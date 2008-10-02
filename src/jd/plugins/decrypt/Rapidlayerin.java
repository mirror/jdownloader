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

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class Rapidlayerin extends PluginForDecrypt {

    public Rapidlayerin(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String page = br.getPage(parameter);
        String fun_id = new Regex(page, Pattern.compile("function (.*?)\\(", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String all = "function " + new Regex(page, Pattern.compile("function (.*?)a=", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String dec = new Regex(page, Pattern.compile("a=(.*?);document.write", Pattern.CASE_INSENSITIVE)).getMatch(0);

        Context cx = Context.enter();
        Scriptable scope = cx.initStandardObjects();
        String fun = "function f(){ " + all + "\nreturn " + fun_id + "(" + dec + ")} f()";
        Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
        String link = Encoding.htmlDecode(Context.toString(result));
        if (link == null) {
            Context.exit();
            return null;
        }
        decryptedLinks.add(createDownloadlink(link));

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}
