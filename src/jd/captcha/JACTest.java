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

import jd.utils.JDUtilities;

/**
 * JAC Tester
 * 
 * 
 * @author JD-Team
 */
public class JACTest {
    /**
     * @param args
     */
    public static void main(String args[]) {

        JACTest main = new JACTest();
        main.go();
    }

    private void go() {
        String hoster = "xfileseek.com";

        JAntiCaptcha jac = new JAntiCaptcha(hoster);

        // jac.setShowDebugGui(true);
        // LetterComperator.CREATEINTERSECTIONLETTER = true;
        //
        // jac.exportDB();
        // Utilities.getLogger().info("has method:
        // "+JAntiCaptcha.hasMethod(methodsPath, hoster));

        //
        // Megaupload2.writeDB();
        // jac.importDB(JDUtilities.getResourceFile("caps/mu/explain/letters"));
        // // jac.importDB();
        // // MegaUpload.main(null);
        // //
        // //jac.displayLibrary();
        // System.out.println("LETTERDB: "+jac.letterDB.size());
        // jac.cleanLibrary(10.0);
        // System.out.println("LETTERDB after filter: "+jac.letterDB.size());
        // jac.displayLibrary();

        // jac.getJas().set("preScanFilter", 0);
        // jac.trainCaptcha(new
        // File(JDUtilities.getJDHomeDirectoryFromEnvironment
        // ().getAbsolutePath()+"/jd/captcha/methods"+"/"+hoster+"/captchas/"+
        // "securedin1730080724541.jpg"),4);
        File[] list = JDUtilities.getResourceFile("/captchas/" + jac.getMethodDirName()).listFiles();
        int id = (int) (Math.random() * (list.length - 1));
        // id=21;
        System.out.println("ID: " + id);
        // File f = new
        // File("/home/dwd/.jd_home/captchas/linkbase.biz/400.png");
        File f = list[2];
        System.out.println(f + "");
        // System.out.println(jac.checkCaptcha(f));
        jac.showPreparedCaptcha(f);
        // jac.displayLibrary();
        // if (JOptionPane.showConfirmDialog(null, "train") ==
        // JOptionPane.OK_OPTION) {
        // f.renameTo(new File("C:\\Users\\coalado\\Desktop\\caps\\" +
        // f.getName()));
        // }
        // Utilities.getLogger().info(JAntiCaptcha.getCaptchaCode(Utilities.
        // loadImage(new
        // File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath()
        // +"/jd/captcha/methods"+
        // "/rapidshare.com/captchas/rapidsharecom24190807214810.jpg")),
        // null, "rapidshare.com"));
        // jac.removeBadLetters(); 01801
        // jac.addLetterMap();
        // jac.saveMTHFile();

    }
}