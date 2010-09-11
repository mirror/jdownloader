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

/**
 * JAC Tester
 * 
 * 
 * @author JD-Team
 */
public class JACTest {

    public static void main(String args[]) {
        JACTest main = new JACTest();
        main.go();
    }

    private void go() {
        String hoster = "externtest";

        File f = new File("/home/dwd/.jd_home/captchas/serienjunkies.org_02.09.2010_10.31.44.277.png");
        System.out.println(f + "");

        JAntiCaptcha jac = new JAntiCaptcha(hoster);
        System.out.println(jac.checkCaptcha(f));
        System.exit(0);
    }

}