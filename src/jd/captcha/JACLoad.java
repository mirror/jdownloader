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
import jd.nutils.JDHash;

/**
 * JAC Tester
 * 
 * @author JD-Team
 */
public class JACLoad {

    public static void main(String args[]) {
        JACLoad main = new JACLoad();
        main.go();
    }

    private void go() {
            //http://duckload.com/design/Captcha2.php?wmid=1338&Sec=aDC54808130775Cb&nob=true
//        System.out.println("aDC54808130775Cb".length());
//        System.out.println();
        for (int i = 0; i < 100; i++) {
            String hash = JDHash.getMD5(""+System.currentTimeMillis()).substring(16);
            try {
                Browser.download(new File("/home/dwd/.jd_home/captchas/dckld/"+hash+".png"), "http://duckload.com/design/Captcha2.php?wmid=1338&Sec="+hash+"&nob=true");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}