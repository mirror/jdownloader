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
import jd.utils.JDUtilities;

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
        loadAppScence();
    }
    private void loadAppScence()
    {
        final String destination = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + "ppscnrg" + "/";
       System.out.println(destination);
        for (int i = 0; i < 200; i++) {

            new Thread(new Runnable() {
                public void run() {
                 Browser br = new Browser();
                 try {
                    br.getPage("http://www.appscene.org/download/YrsmOt0FmJ1a7c95b941");
                    br.getDownload(new File(destination+Math.random()*100+""+System.currentTimeMillis()+".jpg"), br.getRegex("<img src=\"(http://www.appscene.org/captcha/.*?)\" />").getMatch(0));

                 } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                }
            }).start();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
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
                        e.printStackTrace();
                    }
                }
            }).start();
        }
        System.out.println(br);
    }
}