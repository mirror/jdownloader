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

package jd.unrar;

import java.io.File;
import java.util.Vector;

import javax.swing.JFileChooser;
import javax.swing.JFrame;

import jd.gui.skins.simple.components.JDFileChooser;

public class testZip {
    public static void main(String[] args) {
        // Zip zip = new Zip(new File[]{new
        // File("D:/jd_jdu/Xtract/JDownloader.exe"),new
        // File("D:/jd_jdu/Starter/readme.html")}, new
        // File("d:/jd_windows_starter.jdu"));

        while (true) {

            JDFileChooser fc2 = new JDFileChooser();

            fc2.setFileSelectionMode(JFileChooser.FILES_ONLY);

            fc2.showOpenDialog(new JFrame());

            // JOptionPane.showInputDialog("Dateiname")
            if (fc2.getSelectedFile() == null) {
                break;
            }
            // Zip zip = new Zip(fc.getSelectedFile(), fc2.getSelectedFile());
            Vector<File> files = new Vector<File>();
            while (true) {
                JDFileChooser fc = new JDFileChooser();

                fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);

                fc.showOpenDialog(new JFrame());
                fc.setMultiSelectionEnabled(true);
                File f = fc.getSelectedFile();
                if (f == null) {
                    break;
                }
                files.add(f);

            }
            if (files.size() == 0) {
                break;
            }
            Zip zip = new Zip(files.toArray(new File[] {}), fc2.getSelectedFile());
            zip.fillSize = 1048576 + (int) (Math.random() * 1024.0 * 150.0);
            try {
                zip.zip();
            } catch (Exception e) {

                e.printStackTrace();
            }
        }
    }
}
