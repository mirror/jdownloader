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

package jd.captcha;

import java.io.File;
import java.io.IOException;

import jd.controlling.JDLogger;
import jd.http.Browser;
import jd.nutils.JDHash;

/**
 * JAC Tester
 * 
 * 
 * @author JD-Team
 */
public class JACLoad {
    /**
     * @param args
     */
    public static void main(String args[]) {

        JACLoad main = new JACLoad();
        main.go();
    }

    private void go() {
        // String methodsPath=Utilities.getFullPath(new String[] {
        // JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath(),
        // "jd", "captcha", "methods"});

        loadMegaUpload();
    }

    private void loadMegaUpload() {
        final String dir = "/home/dwd/.jd_home/captchas/nrdr/";
        final Browser fbr = new Browser();
        try {
            fbr.getPage("http://narod.ru/disk/3192829000/Vol.55%20-%20Wilhelm%20Kempff%20I%20(Flac).rar.html");
        } catch (IOException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        for (int i = 0; i < 500; i++) {
        	final int b = i;
            new Thread(new Runnable() {
                public void run() {
                    Browser br = fbr.cloneBrowser();
                    try {
                    	br.getPage("http://narod.ru/disk/getcapchaxml/?rnd=423");
                        String cid = br.getRegex("url=\"([^\"]+)\"").getMatch(0);
                        System.out.println(cid);
                        System.out.println(dir + b + ".gif");
                        File file = new File(dir + b + ".gif");
                        br.getDownload(file, cid);
                        file.renameTo(new File(dir, JDHash.getMD5(file)+".gif"));
                    } catch (IOException e) {
                        JDLogger.exception(e);
                    }
                }
            }).start();
        }
    }

}