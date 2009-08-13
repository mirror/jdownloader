package jd.captcha.easy;

import java.io.File;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.utils.locale.JDL;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

public class CreateHoster {


    public static boolean create(EasyFile ef, String user, int lettersize) {
        if(!ef.hasCaptchas())return false;
        if (ef.file.exists()) {
            EasyFile ef2 = new EasyFile(new File(ef.file.getParentFile(), ef.toString() + System.currentTimeMillis()));
            UserIOGui.getInstance().requestConfirmDialog(UserIOGui.NO_CANCEL_OPTION | UserIOGui.ICON_WARNING, JDL.LF("easycaptcha.methodedirmove", "Methode dir %s will be moved to %s", ef, ef2));
            new File(ef.file.getAbsolutePath()).renameTo(ef2.file);
        }
        String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
        File folder2 = new File(path + "/captchas/" + ef.toString());
        String type = "jpg";
        String[] files = folder2.list();
        for (String string : files) {
            if (string.endsWith(".jpg"))
                break;
            else if (string.endsWith(".png")) {
                type = "png";
                break;
            } else if (string.endsWith(".gif")) {
                type = "gif";
                break;
            }
        }

        ef.file.mkdir();
        String jacInfoXml = "<jDownloader>\r\n" + "<method name=\"" + ef + "\" author=\"" + user + "\" />\r\n" + "<format type=\"" + type + "\" letterNum=\"" + lettersize + "\" />\r\n" + "</jDownloader>";
        File in = new File(ef.file.getParent(), "easycaptcha/script.jas");
        File out = new File(ef.file, "script.jas");
        if (out.exists()) out.renameTo(new File(ef.file, "script_bak" + System.currentTimeMillis() + ".jas"));
        JDIO.copyFile(in, out);
        JDIO.writeLocalFile(new File(ef.file, "jacinfo.xml"), jacInfoXml, false);
        return true;
    }

}
