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

package jd.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.regex.Pattern;

import javax.swing.JFileChooser;

import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JDFileChooser;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.nutils.DiffMatchPatch;
import jd.nutils.JDFlags;
import jd.nutils.DiffMatchPatch.Diff;
import jd.nutils.DiffMatchPatch.Operation;
import jd.nutils.io.JDIO;
import jd.parser.Regex;
import tests.utils.TestUtils;

public class ReplaceInFiles {

    // private static final String SVN_SRC =
    // "svn://svn.jdownloader.org/jdownloader/trunk/src";

    public static void main(String[] args) {
        TestUtils.mainInit();
        // TestUtils.initGUI();
        TestUtils.initDecrypter();
        TestUtils.initContainer();
        TestUtils.initHosts();
        TestUtils.finishInit();

        new GuiRunnable<Object>() {

            @Override
            public Object runSave() {
                final JDFileChooser fc = new JDFileChooser();
                UserIO.setInstance(UserIOGui.getInstance());
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                if (fc.showOpenDialog(null) == JDFileChooser.APPROVE_OPTION) {
                    final File ret = fc.getSelectedFile();
                    if (ret != null) {
                        String pat = UserIO.getInstance().requestInputDialog("Define filepattern");
                        String find = UserIO.getInstance().requestInputDialog("Find pattern");
                        String replace = UserIO.getInstance().requestInputDialog("Replace with pattern");
                        replaceInFiles(scanDir(ret, pat), find, replace);
                    }
                }
                return null;
            }

        }.waitForEDT();
    }

    private static void replaceInFiles(final ArrayList<File> scanDir, final String find, final String replace) {
        final long id = System.currentTimeMillis();
        boolean ok = false;
        for (final File f : scanDir) {
            final String l = JDIO.readFileToString(f);

            final String newL = Pattern.compile(find, Pattern.DOTALL | Pattern.CASE_INSENSITIVE).matcher(l).replaceAll(replace);

            final DiffMatchPatch diff = new DiffMatchPatch();
            final LinkedList<Diff> diffs = diff.diffMain(l, newL, true);
            final String html = diffPrettyHtml(diffs);
            if (!ok) {
                final int ret = UserIO.getInstance().requestConfirmDialog(UserIO.DONT_SHOW_AGAIN | UserIO.STYLE_HTML | UserIO.STYLE_LARGE, "Diffs found in  " + id, html, null, null, null);
                if (JDFlags.hasSomeFlags(ret, UserIO.RETURN_CANCEL)) { return; }
                if (JDFlags.hasSomeFlags(ret, UserIO.RETURN_DONT_SHOW_AGAIN, UserIO.RETURN_OK)) {
                    ok = true;
                }
            }
            System.out.println(html);
            JDIO.writeLocalFile(f, newL);
        }
    }

    public static String diffPrettyHtml(final LinkedList<Diff> diffs) {
        final StringBuilder html = new StringBuilder();
        int i = 0;
        for (final Diff aDiff : diffs) {
            final String text = aDiff.text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<BR>");
            switch (aDiff.operation) {
            case INSERT:
                html.append("<span STYLE=\"color:#000000;background:#00FF00;border=1\" TITLE=\"i=").append(i).append("\">").append(text).append("</span>");
                break;
            case DELETE:
                html.append("<span STYLE=\"color:#000000;background:#FF0000;border=1\" TITLE=\"i=").append(i).append("\">").append(text).append("</span>");
                break;
            case EQUAL:
                String[] text2 = aDiff.text.split("[\r\n]{1,2}");
                if (text2.length > 2) {
                    html.append("<SPAN TITLE=\"i=").append(i).append("\">").append(text2[0].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<BR>")).append("</SPAN>");
                    html.append("<br>[.....]<br>");
                    html.append("<SPAN TITLE=\"i=").append(i).append("\">").append(text2[text2.length - 1].replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\n", "<BR>")).append("</SPAN>");

                } else {
                    html.append("<SPAN TITLE=\"i=").append(i).append("\">").append(text).append("</SPAN>");
                }
                break;
            }
            if (aDiff.operation != Operation.DELETE) {
                i += aDiff.text.length();
            }
        }
        return html.toString();
    }

    private static ArrayList<File> scanDir(File dir, String pat) {
        final ArrayList<File> ret = new ArrayList<File>();

        for (final File f : dir.listFiles()) {
            if (f.isDirectory()) {
                ret.addAll(scanDir(f, pat));
            } else {
                final String absolutePath = f.getAbsolutePath();
                if (!absolutePath.contains(".svn") && new Regex(absolutePath, pat).matches()) {
                    System.out.println("Find in file: " + f);
                    ret.add(f);
                }
            }
        }
        return ret;
    }
}
