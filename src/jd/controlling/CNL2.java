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

package jd.controlling;

import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import jd.nutils.encoding.Base64;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.utils.JDHexUtils;

import org.appwork.utils.Regex;
import org.jdownloader.extensions.ExtensionController;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

public final class CNL2 {

    /**
     * Don't let anyone instantiate this class.
     */
    private CNL2() {
    }

    private static boolean isExternInterfaceActive() {

        return ExtensionController.getInstance().isExtensionActive(org.jdownloader.extensions.interfaces.ExternInterfaceExtension.class);
    }

    public static String decrypt(final byte[] b, final byte[] key) {
        final Cipher cipher;
        try {
            final IvParameterSpec ivSpec = new IvParameterSpec(key);
            final SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return new String(cipher.doFinal(b));
        } catch (final Exception e) {
            JDLogger.exception(e);
        }
        return null;
    }

    /**
     * @param crypted
     * @param jk
     * @param k
     * @param passwords
     * @param source
     */
    public static void decrypt(final String crypted, final String jk, final String k, final String password, final String source) {
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

        final String passwords[] = Regex.getLines(password);

        final ArrayList<DownloadLink> links = new DistributeData(Encoding.htmlDecode(decryted)).findLinks();
        for (final DownloadLink link : links) {
            link.addSourcePluginPasswords(passwords);
        }
        for (final DownloadLink l : links) {
            if (source != null) {
                l.setBrowserUrl(source);
            }
        }
        LinkGrabberController.getInstance().addLinks(links, false, false);
    }
}