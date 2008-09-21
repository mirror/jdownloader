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

package jd.utils;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import jd.gui.skins.simple.components.JDFileChooser;
import jd.parser.Regex;
import jd.unrar.zip.Zip;

public class PackageCreaterRS {
    public static void main(String[] args) {
        Date dt = new Date();
        // Festlegung des Formats:
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setVisible(true);
        frame.add(new JLabel("JDU Packer"));
        frame.pack();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");

        File srcDir = new File("G:/pluginressourcen");

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

        StringBuffer sb = new StringBuffer();
        sb.append("<packages>");
        String uid = "jdown4";
        String pw = JOptionPane.showInputDialog(frame, "PW für: " + uid);
        JDFileChooser fc = new JDFileChooser();
        fc.setApproveButtonText("Select list.php");
        fc.setFileSelectionMode(JDFileChooser.FILES_ONLY);
        fc.showOpenDialog(null);
        File listphp = fc.getSelectedFile();
        String list = null;
        if (listphp != null) {

            list = JDUtilities.getLocalFile(listphp);
        }
        String[][] matches = new Regex(list, "\\<package\\>.+?\\<\\/package\\>").getMatches();
        for (String p : packages) {

            if (false && JOptionPane.showConfirmDialog(frame, "Upload " + p) != JOptionPane.OK_OPTION) continue;
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
            do {
                filename = name + "_" + df.format(dt) + "_v" + i + ".jdu";
                i++;
            } while (filename == null || new File(srcDir, filename).exists());

            Zip zip = new Zip(files, new File(srcDir, filename));
            zip.setExcludeFilter(Pattern.compile("\\.svn", Pattern.CASE_INSENSITIVE));
            zip.fillSize = 5 * 1024 * 1024 + 30000 + (int) (Math.random() * 1024.0 * 150.0);
            try {
                zip.zip();
                if (true || JOptionPane.showConfirmDialog(frame, "Upload " + filename) == JOptionPane.OK_OPTION) {
                    if (pw != null) {
                        String url = null;
                        System.out.println(url = Upload.toRapidshareComPremium(new File(srcDir, filename), uid, pw));

                        String tot = "<url>(.*?" + name + ".*?v\\d+?\\.jdu)</url>";
                        String all = "<packages>\r\n";
                  
                        for (int ii = 0; ii < matches.length; ii++) {
                            boolean entries = new Regex(matches[ii][0], tot).matches();
                            if (entries) {
                              
                                String v = new Regex(matches[ii][0], "<version>(.*?)</version>").getMatch(0).trim();

                                int versionid = (true||JOptionPane.showConfirmDialog(frame, "increase version of " + name + "?") == JOptionPane.OK_OPTION) ? (Integer.parseInt(v) + 1) : (Integer.parseInt(v) + 0);

                                matches[ii][0] = matches[ii][0].replaceAll("<version>.*?</version>", "<version>" + versionid + "</version>");
                                matches[ii][0] = matches[ii][0].replaceAll("<url>.*?</url>", "<url>" + url.replace(".html", "") + "</url>");
                                System.out.println(matches[ii][0]);
                            }
                            all += matches[ii][0] + "\r\n";

                        }
                      
                        all += "</packages>";
                        list = all;
                        // sb.append("<package>");
                        // sb.append("<category>"+c.showInputDialog(frame,
                        // "Kategorie für: " + name)+"</category>");
                        // sb.append("<name>"+JOptionPane.showInputDialog(frame,
                        // "Name für: " + name)+"</name>");
                        //sb.append("<version>"+JOptionPane.showInputDialog(frame
                        // ,
                        // "Version für: " + name)+"</version>");
                        // sb.append("<url>"+url+"</url>");
                        // sb.append("<filename>"+name+".jdu</filename>");
                        // sb.append(
                        // "<infourl>http://wiki.jdownloader.org/index.php?title="
                        // +name+"</infourl>");
                        // sb.append("<preselected>false</preselected>");
                        // sb.append("<id>"+id+"</id>");
                        // sb.append("</package>");
                    }
                }
            } catch (Exception e) {

                e.printStackTrace();
            }
            i = 1;
            filename = null;

            name = dat[1];
            do {
                filename = name + "_" + df.format(dt) + "_v" + i + " (LIGHT).jdu";
                i++;
            } while (filename == null || new File(srcDir, filename).exists());
            zip = new Zip(files, new File(srcDir, filename));
            zip.setExcludeFilter(Pattern.compile("\\.svn", Pattern.CASE_INSENSITIVE));

            try {
                zip.zip();
                if (true || JOptionPane.showConfirmDialog(frame, "Upload " + filename) == JOptionPane.OK_OPTION) {
                    if (pw != null) {

                        String url = null;
                        System.out.println(url = Upload.toRapidshareComPremium(new File(srcDir, filename), uid, pw));

                        String tot = "<url>(.*?" + name + ".*?LIGHT\\_\\.jdu)</url>";
                        String all = "<packages>\r\n";
                  
                        for (int ii = 0; ii < matches.length; ii++) {
                            boolean entries = new Regex(matches[ii][0], tot).matches();
                            if (entries) {
                                String v = new Regex(matches[ii][0], "<version>(.*?)</version>").getMatch(0).trim();

                                int versionid = (true||JOptionPane.showConfirmDialog(frame, "increase version of " + name + "?") == JOptionPane.OK_OPTION)  ? (Integer.parseInt(v) + 1) : (Integer.parseInt(v) + 0);
                                matches[ii][0] = matches[ii][0].replaceAll("<version>.*?</version>", "<version>" + versionid + "</version>");
                                matches[ii][0] = matches[ii][0].replaceAll("<url>.*?</url>", "<url>" + url.replace(".html", "") + "</url>");
                                System.out.println(matches[ii][0]);
                            }
                            all += matches[ii][0] + "\r\n";

                        }
                     
                        all += "</packages>";
                        list = all;

                    }
                }
            } catch (Exception e) {

                e.printStackTrace();
            }

        }
        sb.append("</packages>");

        System.out.println(list + "");
        JDUtilities.writeLocalFile(new File(listphp.getParentFile(), "list2.php"), list + "");

    }
}
