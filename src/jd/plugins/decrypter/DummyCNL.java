//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.HashMap;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.formatter.HexFormatter;
import org.appwork.utils.logging.Log;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dummycnl.jdownloader.org" }, urls = { "http://dummycnl\\.jdownloader\\.org/[a-f0-9A-F]+" }, flags = { 0 })
public class DummyCNL extends PluginForDecrypt {
    
    public DummyCNL(final PluginWrapper wrapper) {
        super(wrapper);
    }
    
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String hex = new Regex(parameter, "http://dummycnl\\.jdownloader\\.org/([a-f0-9A-F]+)").getMatch(0);
        
        HashMap<String, String> params = JSonStorage.restoreFromString(new String(HexFormatter.hexToByteArray(hex), "UTF-8"), new TypeRef<HashMap<String, String>>() {
        }, null);
        
        String decrypted = decrypt(params.get("crypted"), params.get("jk"), params.get("k"));
        
        String source = params.get("source");
        
        String packageName = params.get("package");
        FilePackage fp = null;
        
        if (packageName != null) {
            fp = FilePackage.getInstance();
            fp.setName(packageName);
        }
        
        for (String s : Regex.getLines(decrypted)) {
            final DownloadLink dl = createDownloadlink(s);
            if (source != null) dl.setBrowserUrl(source);
            if (fp != null) {
                fp.add(dl);
            }
            try {
                distribute(dl);
            } catch (final Throwable e) {
                /* does not exist in 09581 */
            }
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }
    
    /* decrypt given crypted string with js encrypted aes key */
    public static String decrypt(String crypted, final String jk, String k) {
        byte[] key = null;
        if (jk != null) {
            Context cx = null;
            try {
                try {
                    cx = ContextFactory.getGlobal().enterContext();
                    cx.setClassShutter(new ClassShutter() {
                        public boolean visibleToScripts(String className) {
                            if (className.startsWith("adapter")) {
                                return true;
                            } else {
                                throw new RuntimeException("Security Violation");
                            }
                        }
                    });
                } catch (java.lang.SecurityException e) {
                    /* in case classshutter already set */
                }
                Scriptable scope = cx.initStandardObjects();
                String fun = jk + "  f()";
                Object result = cx.evaluateString(scope, fun, "<cmd>", 1, null);
                key = HexFormatter.hexToByteArray(Context.toString(result));
            } finally {
                try {
                    Context.exit();
                } catch (final Throwable e) {
                }
            }
        } else {
            key = HexFormatter.hexToByteArray(k);
        }
        /* workaround for wrong relink post encoding! */
        crypted = crypted.trim().replaceAll("\\s", "+");
        byte[] baseDecoded = Base64.decode(crypted);
        return decrypt(baseDecoded, key).trim();
    }
    
    public static String decrypt(byte[] b, byte[] key) {
        Cipher cipher;
        try {
            IvParameterSpec ivSpec = new IvParameterSpec(key);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return new String(cipher.doFinal(b), "UTF-8");
        } catch (Throwable e) {
            Log.exception(e);
        }
        return null;
    }
    
    /* NOTE: no override to keep compatible to old stable */
    public int getMaxConcurrentProcessingInstances() {
        return 10;
    }
    
    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
    
}