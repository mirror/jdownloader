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

import java.awt.Toolkit;
import java.util.logging.Logger;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.events.EDTEventQueue;
import jd.gui.swing.laf.LookAndFeelController;

import jd.captcha.utils.Utilities;
import jd.utils.JDUtilities;

/**
 * Jac Training
 * 
 * @author JD-Team
 */
public class JACTrain {
    /**
     * @param args
     */
    public static void main(String args[]) {

        JACTrain main = new JACTrain();

        main.go();
    }

    private Logger logger = Utilities.getLogger();

    private void go() {

        // String hoster="rscat.com";
        final String hoster = "canna.to";
        final JAntiCaptcha jac = new JAntiCaptcha(Utilities.getMethodDir(), hoster);
        // jac.runTestMode(new File("1186941165349_captcha.jpg"));
        jac.displayLibrary();

        // jac.setShowDebugGui(true);
        // jac.showPreparedCaptcha(new
        // File("/home/dwd/.jd_home/captchas/datenklo.net/08.01.2008_18.08.52.gif"));

        // jac.trainCaptcha(new
        // File("C:/Users/coalado/.jd_home/jd/captcha/methods/"+hoster+"/captchas/"+"captcha08_05_2008_22_20_01"+".jpg"),
        // 4);
        new GuiRunnable<Object>() {
            // @Override
            public Object runSave() {
                LookAndFeelController.setUIManager();
                Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EDTEventQueue());

                return null;
            }
        }.waitForEDT();
        jac.trainAllCaptchas(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + hoster);


        // jac.saveMTHFile();
        logger.info("Training Ende");
        // jac.addLetterMap();
        // jac.saveMTHFile();
    }
    // private static class FilterJAR implements FileFilter{
    // public boolean accept(File f) {
    // if(f.getName().endsWith(".jar"))
    // return true;
    // else
    // return false;
    // }
    // }
}