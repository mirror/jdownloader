package jd.captcha.easy;

import java.io.File;
import javax.swing.JOptionPane;

import jd.gui.swing.GuiRunnable;

import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;

public class CreateHoster {

    public static void create(final EasyFile ef, String user, int lettersize) {
        String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
        File folder2 = new File(path + "/captchas/" + ef.toString());
        if (!folder2.exists() || folder2.list().length < 1) {
            if (JOptionPane.showConfirmDialog(null, "You need Captchas do you wanna load Captchas?", "Load Captchas", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_NO_OPTION) {

                new GuiRunnable<Boolean>() {
                    public Boolean runSave() {
                        return LoadCaptchas.load(ef.toString());
                    }
                }.getReturnValue();
            }
            return;
        }
        EasyFile ef2 = new EasyFile(new File(ef.file.getParentFile(), ef.toString() + System.currentTimeMillis()));
        JOptionPane.showConfirmDialog(null, "Methode dir " + ef + " will be moved to " + ef2, "Old Methode will be moved", JOptionPane.WARNING_MESSAGE);
        new File(ef.file.getAbsolutePath()).renameTo(ef2.file);
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

    }

}
