package jd.captcha.easy;

import java.io.File;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.utils.locale.JDL;
import jd.nutils.JDFlags;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

public class CreateHoster {

    public static boolean create(final EasyFile ef, String user, int lettersize) {
        String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
        File folder2 = new File(path + "/captchas/" + ef.toString());
        if (!folder2.exists() || folder2.list().length < 1) {
            int res = UserIOGui.getInstance().requestConfirmDialog(UserIOGui.DONT_SHOW_AGAIN, JDL.L("easycaptcha.loadcaptchas.title", "Load Captchas"), JDL.L("easycaptcha.loadcaptchas", "You need Captchas do you wanna load Captchas?"), null, JDL.L("gui.btn_yes", "yes"), JDL.L("gui.btn_no", "no"));
            if (JDFlags.hasAllFlags(res, UserIO.RETURN_OK)) {
                if (!new GuiRunnable<Boolean>() {
                    public Boolean runSave() {
                        return LoadCaptchas.load(ef.toString());
                    }
                }.getReturnValue()) return false;
            }
        }
        if (ef.file.exists()) {
            EasyFile ef2 = new EasyFile(new File(ef.file.getParentFile(), ef.toString() + System.currentTimeMillis()));
            UserIOGui.getInstance().requestConfirmDialog(UserIOGui.NO_CANCEL_OPTION|UserIOGui.ICON_WARNING, JDL.LF("easycaptcha.methodedirmove", "Methode dir %s will be moved to %s", ef,ef2));
            new File(ef.file.getAbsolutePath()).renameTo(ef2.file);
        }
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
