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
import java.util.Arrays;
import java.util.logging.Level;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.controlling.DistributeData;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDHexUtils;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "Click n Load", "Click n Load" }, urls = { "cnl://.*?\\..*?/.*?/", "http://jdownloader\\.org/cnl/.*?/" }, flags = { 0, 0 })
public class CNL extends PluginForDecrypt {

    public CNL(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String code = new Regex(param, "cnl://jdownloader.org/(.*?)/").getMatch(0);
        if (code == null) {
            code = new Regex(param, "http://jdownloader.org/cnl/(.*?)/").getMatch(0);
        }
        String[] params = Regex.getLines(Encoding.Base64Decode(code));
        String passwords = null;
        String source = null;
        String jk = null;
        String crypted = null;
        for (String p : params) {
            int i = p.indexOf("=");
            String key = p.substring(0, i);
            if (key.equalsIgnoreCase("passwords")) {
                // passwords = Encoding.Base64Decode(p.substring(i + 1));
                continue;
            }
            if (key.equalsIgnoreCase("source")) {
                source = Encoding.Base64Decode(p.substring(i + 1));
                continue;
            }
            if (key.equalsIgnoreCase("jk")) {
                jk = Encoding.Base64Decode(p.substring(i + 1));
                continue;
            }
            if (key.equalsIgnoreCase("crypted")) {
                crypted = Encoding.Base64Decode(p.substring(i + 1));
                continue;
            }

        }
        decryptedLinks.addAll(decrypt(crypted, jk, null, passwords, source));
        return decryptedLinks;
    }

    public String decrypt(final byte[] b, final byte[] key) {
        final Cipher cipher;
        try {
            final IvParameterSpec ivSpec = new IvParameterSpec(key);
            final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return new String(cipher.doFinal(b), "UTF-8");
        } catch (Exception e) {
            logger.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    public ArrayList<DownloadLink> decrypt(final String crypted, final String jk, final String k, final String password, final String source) {
        final byte[] key;

        if (jk != null) {
            Context cx = null;
            try {
                cx = ContextFactory.getGlobal().enterContext();
                final Scriptable scope = cx.initStandardObjects();
                final String fun = jk + "  f()";
                final Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);

                key = JDHexUtils.getByteArray(Context.toString(result));
            } finally {
                if (cx != null) Context.exit();
            }
        } else {
            key = JDHexUtils.getByteArray(k);
        }
        final byte[] baseDecoded = Base64.decode(crypted);
        final String decryted = decrypt(baseDecoded, key).trim();

        final ArrayList<String> passwords = new ArrayList<String>(Arrays.asList(Regex.getLines(password)));

        final ArrayList<DownloadLink> links = new DistributeData(Encoding.htmlDecode(decryted)).findLinks();
        for (final DownloadLink link : links) {
            if (passwords != null && passwords.size() > 0) link.setSourcePluginPasswordList(passwords);
        }
        for (final DownloadLink l : links) {
            if (source != null) {
                l.setBrowserUrl(source);
            }
        }
        return links;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}