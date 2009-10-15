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

import jd.http.Browser;
import jd.http.JDProxy;

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
        // load();
        // loadMegaUpload();
        loadknt();
    }

    private void loadknt() {
        final Browser br = new Browser();

        try {

            br.setCookie("kino.to", "_csoot", "1255453317840");
            br.setCookie("kino.to", "_csuid", "48c78b976f126fa6");
            br.setCookie("kino.to", "sitechrx", "1cb8625bb6a0e8a8125cb62ebf20d179");
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < 100; i++) {
            final int c = i;
            new Thread(new Runnable() {
                public void run() {
                    String dir = "/home/dwd/.jd_home/captchas/knt/";
                    File file = new File(dir + c + ".png");

                    Browser cln = br.cloneBrowser();

                    try {

                        cln.getPage("http://kino.to/Entry/6788/Die%20Simpsons%20-%20Der%20Film.html");
                        cln.getDownload(file, "http://kino.to" + cln.getRegex("src=\"(/res/gr/Img/Capture.php.*?)\" height").getMatch(0));
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        System.out.println(br);
    }

    private void load() {
        final Browser br = new Browser();
        br.setProxy(new JDProxy("www-proxy.t-online.de:80"));

        // System.out.println(br);
        for (int i = 0; i < 100; i++) {
            final int c = i;
            new Thread(new Runnable() {
                public void run() {
                    String dir = "/home/dwd/.jd_home/captchas/ppscnrg/";
                    File file = new File(dir + c + ".jpg");
                    try {
                        Browser cln = br.cloneBrowser();
                        try {
                            cln.getPage("http://www.appscene.org/download.php?id=289845362");
                        } catch (IOException e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();

                        }
                        String url = cln.getRegex("<img src=\"(http://www.appscene.org/captcha/[^\\.]*\\.jpg)\" />").getMatch(0);
                        cln.cloneBrowser().getDownload(file, url);
                    } catch (IOException e) {
                        file.delete();
                        e.printStackTrace();
                    }
                }
            }).start();
        }

    }

    // private void loadMegaUpload() {
    // final String dir = "/home/dwd/.jd_home/captchas/gtthbtcm/";
    // final Browser fbr = new Browser();
    // try {
    // fbr.setCookie("getthebit.com", "gb_guest_sid",
    // "c79803a2c3656a0a36ebed436a19348f");
    // //
    // fbr.getPage("http://narod.ru/disk/3192829000/Vol.55%20-%20Wilhelm%20Kempff%20I%20(Flac).rar.html");
    // } catch (IOException e1) {
    // // TODO Auto-generated catch block
    // e1.printStackTrace();
    // }
    // for (int i = 0; i < 500; i++) {
    // final int b = i;
    // new Thread(new Runnable() {
    // public void run() {
    // Browser br = fbr.cloneBrowser();
    // try {
    // br.setCookie("getthebit.com", "gb_guest_sid",
    // "c79803a2c3656a0a36ebed436a19348f");
    // // br.getPage();
    // // String cid =
    // // br.getRegex("url=\"([^\"]+)\"").getMatch(0);
    // // System.out.println(cid);
    // System.out.println(dir + b + ".jpg");
    // File file = new File(dir + b + ".jpg");
    // br.getDownload(file,
    // "http://st2.srv.getthebit.com/plain.php?s=files&ev=kcapcha");
    // file.renameTo(new File(dir, JDHash.getMD5(file) + ".jpg"));
    // } catch (IOException e) {
    // JDLogger.exception(e);
    // }
    // }
    // }).start();
    // }
    // }
}