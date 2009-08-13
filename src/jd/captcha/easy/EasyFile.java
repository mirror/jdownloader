package jd.captcha.easy;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.jdgui.userio.UserIOGui;
import jd.nutils.JDFlags;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import jd.config.container.JDLabelContainer;

import jd.nutils.JDImage;

public class EasyFile implements JDLabelContainer, Serializable {
    public File file = null;
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }


    private static final long serialVersionUID = 1L;

    public EasyFile(File file) {
        this.file = file;
    }

    public boolean existsScriptJas() {
        return new File(file, "script.jas").exists();
    }

    public EasyFile() {
    }

    public EasyFile(String file) {
        this(new File(file));
    }

    @Override
    public String toString() {
        return file.getName();
    }

    public EasyFile[] listFiles() {
        File[] files = file.listFiles();
        ArrayList<EasyFile> ret = new ArrayList<EasyFile>();
        for (int i = 0; i < files.length; i++) {
            EasyFile ef = new EasyFile(files[i]);
            if (ef.existsScriptJas()) ret.add(ef);

        }
        return ret.toArray(new EasyFile[] {});
    }

    public File getExampleImage() {
        File[] files = file.listFiles(new FileFilter() {

            public boolean accept(File pathname) {
                if (pathname.getName().toLowerCase().contains("example")) return true;
                return false;
            }
        });
        if (files != null && files.length > 0) return files[0];
        return null;
    }
    public boolean hasCaptchas() {
        String path = JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath();
        File folder2 = new File(path + "/captchas/" + getName());
        if (!folder2.exists() || folder2.list().length < 1) {
            int res = UserIOGui.getInstance().requestConfirmDialog(UserIOGui.DONT_SHOW_AGAIN, JDL.L("easycaptcha.loadcaptchas.title", "Load Captchas"), JDL.L("easycaptcha.loadcaptchas", "You need Captchas do you wanna load Captchas?"), null, JDL.L("gui.btn_yes", "yes"), JDL.L("gui.btn_no", "no"));
            if (JDFlags.hasSomeFlags(res, UserIO.RETURN_OK | UserIO.RETURN_COUNTDOWN_TIMEOUT | UserIO.RETURN_SKIPPED_BY_DONT_SHOW)) {
                return new GuiRunnable<Boolean>() {
                    public Boolean runSave() {
                        return LoadCaptchas.load(getName());
                    }
                }.getReturnValue();
            } else
                return false;
        }
        return true;
    }
    public ImageIcon getIcon() {
        try {
            File image = getExampleImage();
            if (image != null) {
                ImageIcon img = JDImage.getScaledImageIcon(JDImage.getImageIcon(image),44,24);
                return img;

            }
        } catch (Exception e) {
        }
        return null;
    }

    public String getName() {
        // TODO Auto-generated method stub
        return toString();
    }


    public String getLabel() {
        // TODO Auto-generated method stub
        return toString();
    }
}