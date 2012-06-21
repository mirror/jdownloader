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

package jd.nutils.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.appwork.utils.Application;
import org.appwork.utils.Regex;
import org.jdownloader.logging.LogController;

public class FileSignatures {

    private final Signature SIG_TXT = new Signature("TXTfile", null, "Plaintext", ".*\\.(txt|doc|nfo|html|htm|xml)");
    private Signature[]     SIGNATURES;

    public static String readFileSignature(File f) throws IOException {
        FileInputStream reader = null;
        try {
            reader = new FileInputStream(f);
            StringBuilder sig = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                int h = reader.read();
                String s = Integer.toHexString(h);
                if (s.length() < 2) sig.append('0');
                sig.append(s);
            }
            return sig.toString();
        } finally {
            try {
                reader.close();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * Gibt alle verfügbaren signaturen zurück
     * 
     * @return
     */
    private Signature[] getSignatureList() {
        if (SIGNATURES != null) return SIGNATURES;
        synchronized (this) {
            if (SIGNATURES != null) return SIGNATURES;
            String[] m = Regex.getLines(JDIO.readFileToString(Application.getResource("jd/mime.type")));
            SIGNATURES = new Signature[m.length];
            int i = 0;
            for (String e : m) {
                String[] entry = e.split(":::");
                if (entry.length >= 5) {
                    SIGNATURES[i++] = new Signature(entry[0], entry[1], entry[2], entry[3], entry[4]);
                } else if (entry.length >= 4) {
                    SIGNATURES[i++] = new Signature(entry[0], entry[1], entry[2], entry[3]);
                } else {
                    LogController.CL().warning("Signature " + e + " invalid!");
                }
            }
        }
        return SIGNATURES;
    }

    /**
     * GIbt die signatur zu einem signaturstring zurück.
     * 
     * @param sig
     * @return
     */
    public Signature getSignature(String sig) {
        Signature[] db = getSignatureList();
        for (Signature entry : db) {
            if (entry != null && entry.matches(sig)) return entry;
        }
        return checkTxt(sig);
    }

    /**
     * Prüft ob eine Datei möglicheriwese eine TXT datei ist. Dabei wird geprüft ob die signatur nur aus lesbaren zeichen besteht
     * 
     * @param sig
     * @return
     */
    private Signature checkTxt(String sig) {
        for (int i = 0; i < sig.length(); i += 2) {
            if ((i + 2) > sig.length()) return null;
            String b = sig.substring(i, i + 2);
            int ch = Integer.parseInt(b, 16);

            if (ch < 32 || ch > 126) { return null; }

        }

        return SIG_TXT;
    }
}
