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

package jd.plugins.optional.jdunrar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import javax.swing.JOptionPane;

import jd.gui.skins.simple.components.JDFileChooser;
import jd.parser.Regex;
import jd.utils.JDUtilities;

public class FileSignatures {

    public static void main(String args[]) throws IOException {
        Signature sig = getFileSignature(new File("C:\\Users\\coalado\\.jd_home\\plugins\\jdunrar\\template.xml"));
        sig = sig;
        scan(null);

    }
/**
 * DUrchsucht verzeichnisse aufd er hd nach neuen signaturen
 * @param file
 */
    private static void scan(File file) {
        if (file == null) {
            JDFileChooser fc = new JDFileChooser();
            fc.setApproveButtonText("Folders to scan");
            fc.setFileSelectionMode(JDFileChooser.DIRECTORIES_ONLY);

            fc.showOpenDialog(null);
            file = fc.getSelectedFile();
            if (file == null) return;
        }
        if (file == null) return;
        for (File f : file.listFiles()) {
            if (f.isDirectory()) {
                if (JOptionPane.showConfirmDialog(null, "Scan " + f) == JOptionPane.OK_OPTION) scan(f);
            } else {
                try {
                    String out = "";
                    String sig = readFileSignature(f);
                    String ext = JDUtilities.getFileExtension(f);
                    out += (sig + ":::");
                    out += (":::");
                    out += (".+\\." + ext);
                    Signature sss;
                    if ((sss = getSignature(sig)) == null) System.out.println(out + "  " + f + "\r\n");

                } catch (IOException e) {

                }
            }

        }

    }
/**
 * Überprüft eine datei auf ihre signatur
 * @param f
 * @return
 * @throws IOException
 */
    public static Signature getFileSignature(File f) throws IOException {
        return getSignature(readFileSignature(f));

    }
/**
 * GIbt den signaturstring einer datei zurück
 * @param f
 * @return
 * @throws IOException
 */
    public static String readFileSignature(File f) throws IOException {
        FileInputStream reader = new FileInputStream(f);
        String sig = "";
        for (int i = 0; i < 10; i++) {
            int h = reader.read();
            String s = Integer.toHexString(h);
            sig += (s.length() < 2 ? "0" + s : s);
        }
        reader.close();
        return sig;
    }

    private static final Signature SIG_TXT = new Signature("TXTfile", null, "Plaintext", ".*\\.(txt|doc|nfo|html|htm|xml)");
    private static Signature[] SIGNATURES;
/**
 * Gibt alle verfügbaren signaturen zurück
 * @return
 */
    public static Signature[] getSignatureList() {
        if (SIGNATURES != null) return SIGNATURES;
        String[] m = Regex.getLines(JDUtilities.getLocalFile(JDUtilities.getResourceFile("jd/mime.type")));
        SIGNATURES = new Signature[m.length];
        int i = 0;
        for (String e : m) {
            String[] entry = e.split(":::");
            if(entry.length>=4){
                SIGNATURES[i++] = new Signature(entry[0], entry[1], entry[2], entry[3]);
            }else{
                System.err.println("Signature "+e+" invalid!");
            }
        }
        return SIGNATURES;
    }

    /**
     * GIbt die signatur zu einem signaturstring zurück.
     * @param sig
     * @return
     */
    public static Signature getSignature(String sig) {
        Signature[] db = getSignatureList();

        for (Signature entry : db) {
            if (entry.matches(sig)) return entry;
        }

        return checkTxt(sig);

    }
/**
 * Prüft ob eine Datei möglicheriwese eine TXT datei ist.
 * Dabei wird geprüft ob die signatur nur aus lesbaren zeichen besteht
 * @param sig
 * @return
 */
    public static Signature checkTxt(String sig) {
        for (int i = 0; i < sig.length(); i += 2) {
            if ((i + 2) > sig.length()) return null;
            String b = sig.substring(i, i + 2);
            int ch = Integer.parseInt(b, 16);

            if (ch < 32 || ch > 126) { return null; }

        }

        return SIG_TXT;
    }

}
