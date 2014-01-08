//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;
import java.util.logging.Level;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "binbox.io" }, urls = { "http://(www\\.)?binbox\\.io/\\w+#\\w+" }, flags = { 0 })
public class BinBoxIo extends PluginForDecrypt {

    private static String sjcl;

    public BinBoxIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">Page Not Found<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final String fpName = br.getRegex("<title>([^<>\"]*?)\\- Binbox</title>").getMatch(0);
        final String salt = parameter.substring(parameter.lastIndexOf("#") + 1);
        String paste = br.getRegex("<div id=\"paste\\-json\" style=\"[^\"]+\">([^<]+)</div>").getMatch(0);

        if (salt != null && paste != null) {
            paste = paste.replace("&quot;", "\"");
            paste = Encoding.Base64Decode(paste);
            if (isEmpty(sjcl)) sjcl = br.getPage("/public/js/sjcl.js");
            final String[] links = decryptLinks(salt, paste);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                if (!singleLink.startsWith("http")) continue;
                decryptedLinks.add(createDownloadlink(singleLink));
            }
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

    private String[] decryptLinks(final String salt, final String paste) throws Exception {
        String result = null;
        final ScriptEngineManager manager = new ScriptEngineManager();
        final ScriptEngine engine = manager.getEngineByName("ECMAScript");
        final Invocable inv = (Invocable) engine;
        try {
            engine.eval(sjcl);
            engine.eval("function sjclDecrypt(salt, paste){return sjcl.decrypt(salt, paste)}");
            result = (String) inv.invokeFunction("sjclDecrypt", salt, paste);
        } catch (final Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
            return null;
        }
        if (isEmpty(result)) return null;
        return result.split("[\r\n]+");
    }

    private boolean isEmpty(String ip) {
        return ip == null || ip.trim().length() == 0;
    }

}