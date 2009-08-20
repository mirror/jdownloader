package jd.captcha.easy;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;
import java.util.ArrayList;
import javax.swing.ImageIcon;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;
import jd.captcha.JAntiCaptcha;
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
    /**
    *
    * @param file
    */
   public EasyFile(String methodename) {
       this.file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory(), methodename);
   }
   public EasyFile(File file) {
       this.file = file;
   }
   public EasyFile() {
   }
    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        this.file = file;
    }

    private static final long serialVersionUID = 1L;

    public File getScriptJas() {
        return new File(file, "script.jas");

    }

    public File getJacinfoXml() {
        return new File(file, "jacinfo.xml");
    }


    @Override
    public String toString() {
        return file.getName();
    }

    public JAntiCaptcha getJac() {
        return new JAntiCaptcha(Utilities.getMethodDir(), getName());
    }

    public static EasyFile[] getMethodeList() {
        File[] files = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory()).listFiles();
        ArrayList<EasyFile> ret = new ArrayList<EasyFile>();
        for (int i = 0; i < files.length; i++) {
            EasyFile ef = new EasyFile(files[i]);
            if (ef.getScriptJas().exists()) ret.add(ef);

        }
        return ret.toArray(new EasyFile[] {});
    }

    public File getCaptchaFolder() {
        return new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + getName());
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

    public String getCaptchaType(boolean showLoadDialog) {
        final File folder2 = getCaptchaFolder();
        if (showLoadDialog) {
            if (!folder2.exists() || folder2.list().length < 1) {
                int res = UserIOGui.getInstance().requestConfirmDialog(0, JDL.L("easycaptcha.loadcaptchas.title", "Load Captchas"), JDL.L("easycaptcha.needCaptchas", "You need Captchas first!"), null, JDL.L("easycaptcha.openCaptchaFolder", "Open Captcha Folder"), JDL.L("easycaptcha.loadcaptchas", "Load Captchas"));
                if (JDFlags.hasSomeFlags(res, UserIO.RETURN_OK)) {
                    folder2.mkdir();
                    openCaptchaFolder();
                    return "jpg";
                } else {
                    return new GuiRunnable<String>() {
                        public String runSave() {
                            if (!new LoadCaptchas(getName()).start()) {
                                openCaptchaFolder();
                                return "jpg";
                            }
                            String filetype = "jpg";
                            File[] fl = folder2.listFiles();
                            if (fl[fl.length - 1].getName().toLowerCase().contains("png"))
                                filetype = "png";
                            else if (fl[fl.length - 1].getName().toLowerCase().contains("gif")) filetype = "gif";
                            return filetype;
                        }
                    }.getReturnValue();
                }
            }
        }
        String filetype = "jpg";
        File[] fl = folder2.listFiles();
        if (fl != null && fl.length > 0) {
            if (fl[fl.length - 1].getName().toLowerCase().contains("png"))
                filetype = "png";
            else if (fl[fl.length - 1].getName().toLowerCase().contains("gif")) filetype = "gif";
        }
        return filetype;
    }

    public void openCaptchaFolder() {
        JDUtilities.openExplorer(getCaptchaFolder());
    }

    public ImageIcon getIcon() {
        try {
            File image = getExampleImage();
            if (image != null) {
                ImageIcon img = JDImage.getScaledImageIcon(JDImage.getImageIcon(image), 44, 24);
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

    public Captcha getRandomCaptcha() {
        File[] list = getCaptchaFolder().listFiles();
        if (list.length == 0) return null;
        int id = (int) (Math.random() * (list.length - 1));
        Captcha captchaImage = getJac().createCaptcha(Utilities.loadImage(list[id]));
        return captchaImage;
    }

    public String getLabel() {
        // TODO Auto-generated method stub
        return toString();
    }
}