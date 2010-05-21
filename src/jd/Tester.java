package jd;

import javax.swing.ImageIcon;

import jd.controlling.CaptchaController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.nutils.JDImage;

public class Tester {

    public static void main(String[] args) throws Exception {
        UserIO.setInstance(UserIOGui.getInstance());

        testIcon("uploaded.to");
        testIcon("uploadfloor.com");
    }

    private static void testIcon(String hoster) {
        ImageIcon icon = JDImage.getImageIcon("favicons/" + hoster);

        CaptchaController cc = new CaptchaController(hoster, icon, null, null, null, null);
        System.out.println(cc.getCode(0));
    }

}