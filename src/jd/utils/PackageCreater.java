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
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import jd.nutils.zip.Zip;

public class PackageCreater {
    public static void main(String[] args) {
        Date dt = new Date();
        // Festlegung des Formats:
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
        frame.add(new JLabel("JDU Packer"));
        frame.pack();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        File srcDir = new File("D:/jd_jdu");

        String[] packages = srcDir.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                if (name.endsWith(".svn")) { return false; }
                if (new File(dir, name).isDirectory()) {
                    return true;
                } else {
                    return false;
                }

            }

        });
        // ArrayList<File> upload = new ArrayList<File>();

        StringBuilder sb = new StringBuilder();
        sb.append("<packages>");
        String uid = "3373035";
        String pw = JOptionPane.showInputDialog(frame, "PW für: " + 3373035);
        for (String p : packages) {
            File pDir = new File(srcDir, p);
            File[] files = pDir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    if (name.endsWith(".svn")) { return false; }
                    return true;

                }

            });
            int i = 1;
            String filename = null;
            String[] dat = p.split("__");
            String name = dat[1];
            String id = dat[0];
            do {
                filename = name + "_" + df.format(dt) + "_v" + i + ".jdu";
                i++;
            } while (filename == null || new File(srcDir, filename).exists());
            Zip zip = new Zip(files, new File(srcDir, filename));
            zip.setExcludeFilter(Pattern.compile("\\.svn", Pattern.CASE_INSENSITIVE));
            zip.fillSize = 3 * 1024 * 1024 + 30000 + (int) (Math.random() * 1024.0 * 150.0);
            try {
                zip.zip();
                String url;
                if (pw != null) {
                    System.out.println(url = Upload.toRapidshareComPremium(new File(srcDir, filename), uid, pw));
                    sb.append("<package>");
                    sb.append("<category>" + JOptionPane.showInputDialog(frame, "Kategorie für: " + name) + "</category>");
                    sb.append("<name>" + JOptionPane.showInputDialog(frame, "Name für: " + name) + "</name>");
                    sb.append("<version>" + JOptionPane.showInputDialog(frame, "Version für: " + name) + "</version>");
                    sb.append("<url>" + url + "</url>");
                    sb.append("<filename>" + name + ".jdu</filename>");
                    sb.append("<infourl>http://wiki.jdownloader.org/index.php?title=" + name + "</infourl>");
                    sb.append("<preselected>false</preselected>");
                    sb.append("<id>" + id + "</id>");
                    sb.append("</package>");
                }

            } catch (Exception e) {

                e.printStackTrace();
            }
        }
        sb.append("</packages>");

        System.out.println(sb + "");

    }
}
