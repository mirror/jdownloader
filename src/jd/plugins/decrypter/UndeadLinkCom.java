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

package jd.plugins.decrypter;

import java.util.ArrayList;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.ext.BasicBrowserEnviroment;
import jd.http.ext.ExtBrowser;
import jd.http.ext.ExtBrowserException;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 13393 $", interfaceVersion = 2, names = { "undeadlink.com" }, urls = { "http://undeadlink\\.com/lien\\.php\\?id=[0-9A-Z]+" }, flags = { 0 })
public class UndeadLinkCom extends PluginForDecrypt {

    private static String DLLINK;

    public UndeadLinkCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (!br.containsHTML("eval\\(")) { return null; }

        hardAndStatic();
        if (DLLINK == null) {
            veryEasy();
        }
        if (DLLINK == null) { return null; }
        decryptedLinks.add(createDownloadlink(DLLINK));
        return decryptedLinks;
    }

    private String hardAndStatic() throws Exception {
        // Init rhino
        Object result = new Object();
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("javascript");
        final Invocable inv = (Invocable) engine;
        // Load function
        final Browser br2 = br.cloneBrowser();
        String unk = br.getRegex("<script src=\"(.*?)\"").getMatch(0);
        if (unk == null) { return null; }
        unk = br2.getPage("http://" + br.getHost() + unk).toString();

        String beautifier = new Regex(br.toString().replaceAll("\n|\\\\|\\+|\\s", ""), "\\*/(eval\\('.*\\);)</script>").getMatch(0);
        beautifier = beautifier.replaceAll("''", "");
        beautifier = beautifier.replaceAll("/\\*.*?\\*/", "");
        beautifier = beautifier.replaceAll("'\\);|eval\\('|thelink=|'", "");
        String fn = beautifier.substring(0, beautifier.indexOf("("));
        final String[] value = new Regex(beautifier, fn + "\\((.*?)\\)\\.").getMatch(0).split(",");
        if (value == null || value.length == 0) { return null; }

        // Execute subroutines
        for (final String p : value) {
            if (p.indexOf("(") == -1) {
                continue;
            }
            final String fn1 = p.substring(0, p.indexOf("("));
            final String value1 = new Regex(p, fn1 + "\\((.*?)\\)").getMatch(0);
            try {
                engine.eval(unk);
                result = inv.invokeFunction(fn1, value1);
            } catch (final Throwable e) {
                DLLINK = null;
                fn = "undefined";
            }
        }
        // Execute final function
        try {
            engine.eval(unk);
            result = inv.invokeFunction(fn, value[0], result, 0, 0, 0, 0);
        } catch (final Throwable e) {
            DLLINK = "undefined";
        }

        if (DLLINK == "undefined") {
            DLLINK = null;
            return null;
        }
        return DLLINK = result.toString();
    }

    private String veryEasy() throws Exception {
        try {
            final ExtBrowser eb = new ExtBrowser();
            eb.setBrowserEnviroment(new BasicBrowserEnviroment(new String[] { ".*" }, new String[] { ".*undeadlink.com.*" }) {
                @Override
                public boolean isAutoProcessSubFrames() {
                    return false;
                }
            });
            eb.eval(br);
            DLLINK = eb.getRegex("FRAME SRC=\"(http.*?)\"").getMatch(0);
        } catch (final ExtBrowserException e) {
            DLLINK = null;
            e.printStackTrace();
        }
        return DLLINK;
    }
}
