//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.captcha.easy;


 import jd.captcha.translate.*;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.ImageIcon;

import jd.captcha.JAntiCaptcha;
import jd.captcha.easy.load.LoadCaptchas;
import jd.captcha.pixelgrid.Captcha;
import jd.captcha.utils.Utilities;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.JDLabelContainer;
import jd.nutils.JDFlags;
import jd.nutils.JDImage;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

public class EasyMethodFile implements JDLabelContainer, Serializable {
    /**
     * erstellt eine Liste aller vorhandener Methoden
     */
    public static EasyMethodFile[] getMethodeList() {
        final File[] files = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory()).listFiles();
        final ArrayList<EasyMethodFile> ret = new ArrayList<EasyMethodFile>();
        for (int i = 0; i < files.length; i++) {
            final EasyMethodFile ef = new EasyMethodFile(files[i]);
            if (ef.getScriptJas().exists()) {
                ret.add(ef);
            }

        }
        return ret.toArray(new EasyMethodFile[] {});
    }

    public File               file             = null;

    private static final long serialVersionUID = 1L;

    public EasyMethodFile() {
    }

    public EasyMethodFile(final File file) {
        this.file = file;
    }

    public EasyMethodFile(final String methodename) {
        this.file = new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/" + JDUtilities.getJACMethodsDirectory(), methodename);
    }

    public boolean copyExampleImage() {
        final File exf = this.getExampleImage();
        if (exf == null || !exf.exists()) {
            final File[] listF = this.getCaptchaFolder().listFiles(new FilenameFilter() {

                public boolean accept(final File dir, final String name) {
                    return name.matches("(?is).*\\.(jpg|png|gif)");
                }
            });
            if (listF != null && listF.length > 1) {
                JDIO.copyFile(listF[0], new File(this.file, "example." + this.getCaptchaType(false)));
                return true;
            }
        }
        return false;
    }

    public File getCaptchaFolder() {
        return new File(JDUtilities.getJDHomeDirectoryFromEnvironment().getAbsolutePath() + "/captchas/" + this.file.getName());
    }

    /**
     * Ermittelt den Dateityp der Captchas im Captchaordner ist der Ordner leer
     * wird jpg ausgegeben bzw Bilder geladen wenn showLoadDialog true ist
     * 
     * @param showLoadDialog
     * @return captchatyp jpg | png | gif
     */
    public String getCaptchaType(final boolean showLoadDialog) {
        final File folder2 = this.getCaptchaFolder();
        if (showLoadDialog) {
            if (!folder2.exists() || folder2.list().length < 1) {
                final int res = UserIO.getInstance().requestConfirmDialog(0, T._.easycaptcha_loadcaptchas_title(), T._.easycaptcha_needCaptchas(), null, T._.easycaptcha_openCaptchaFolder(), T._.easycaptcha_loadcaptchas());
                if (JDFlags.hasSomeFlags(res, UserIO.RETURN_OK)) {
                    folder2.mkdir();
                    this.openCaptchaFolder();
                    return "jpg";
                } else {
                    return new GuiRunnable<String>() {
                        public String runSave() {
                            if (!new LoadCaptchas(EasyCaptchaTool.ownerFrame, EasyMethodFile.this.file.getName()).start()) {
                                EasyMethodFile.this.openCaptchaFolder();
                                return "jpg";
                            }
                            String filetype = "jpg";
                            final File[] fl = folder2.listFiles();
                            if (fl[fl.length - 1].getName().toLowerCase().contains("png")) {
                                filetype = "png";
                            } else if (fl[fl.length - 1].getName().toLowerCase().contains("gif")) {
                                filetype = "gif";
                            }
                            return filetype;
                        }
                    }.getReturnValue();
                }
            }
        }
        String filetype = "jpg";
        final File[] fl = folder2.listFiles();
        if (fl != null && fl.length > 0) {
            if (fl[fl.length - 1].getName().toLowerCase().contains("png")) {
                filetype = "png";
            } else if (fl[fl.length - 1].getName().toLowerCase().contains("gif")) {
                filetype = "gif";
            }
        }
        return filetype;
    }

    /**
     * gibt falls vorhanden das File zum example.jpg/png/gif im Methodenordner
     * aus
     * 
     * @return
     */
    public File getExampleImage() {
        final File[] files = this.file.listFiles(new FileFilter() {

            public boolean accept(final File pathname) {
                final String ln = pathname.getName().toLowerCase();
                if (ln.contains("example") && ln.matches(".*\\.(jpg|jpeg|gif|png|bmp)")) { return true; }
                return false;
            }
        });
        if (files != null && files.length > 0) { return files[0]; }
        return null;
    }

    public File getFile() {
        return this.file;
    }

    /**
     * läd das ExamleImage fals vorhanden und gibt ein ImageIcon mit
     * maxWidht=44px und maxHeight=24 aus
     */
    public ImageIcon getIcon() {
        try {
            final File image = this.getExampleImage();
            if (image != null) {
                final ImageIcon img = JDImage.getScaledImageIcon(JDImage.getImageIcon(image), 44, 24);
                return img;

            }
        } catch (final Exception e) {
        }
        return null;
    }

    /**
     * 
     * @return JAntiCaptcha Instanz für die Methode
     */
    public JAntiCaptcha getJac() {
        return new JAntiCaptcha(this.getName());
    }

    public File getJacinfoXml() {
        return new File(this.file, "jacinfo.xml");
    }

    public String getLabel() {
        return this.toString();
    }

    public String getName() {
        return this.toString();
    }

    /**
     * Erzeugt ein zufallsCaptcha aus dem Captchaordner
     * 
     * @return
     */
    public Captcha getRandomCaptcha() {
        Captcha captchaImage = this.getJac().createCaptcha(Utilities.loadImage(this.getRandomCaptchaFile()));
        if (captchaImage != null && captchaImage.getWidth() > 0 && captchaImage.getWidth() > 0) { return captchaImage; }
        captchaImage = this.getJac().createCaptcha(Utilities.loadImage(this.getRandomCaptchaFile()));
        if (captchaImage != null && captchaImage.getWidth() > 0 && captchaImage.getWidth() > 0) { return captchaImage; }
        return null;
    }

    public File getRandomCaptchaFile() {
        final File[] list = this.getCaptchaFolder().listFiles(new FileFilter() {

            public boolean accept(final File pathname) {
                final String ln = pathname.getName().toLowerCase();
                if (pathname.isFile() && ln.matches(".*\\.(jpg|jpeg|gif|png|bmp)") && pathname.length() > 1) { return true; }
                return false;
            }
        });
        if (list.length == 0) { return null; }
        final int id = (int) (Math.random() * (list.length - 1));
        return list[id];
    }

    public File getScriptJas() {
        return new File(this.file, "script.jas");

    }

    public boolean isEasyCaptchaMethode() {
        final File js = this.getScriptJas();
        return js.exists() && JDIO.readFileToString(js).contains("param.useSpecialGetLetters=EasyCaptcha");
    }

    public boolean isReadyToTrain() {
        if (!this.isEasyCaptchaMethode()) { return true; }
        final Vector<CPoint> list = ColorTrainer.load(new File(this.file, "CPoints.xml"));
        boolean hasForderGround = false;
        boolean hasBackGround = false;

        for (final CPoint point : list) {
            if (point.isForeground()) {
                hasForderGround = true;
            } else {
                hasBackGround = true;
            }
        }
        return hasForderGround && hasBackGround;
    }

    /**
     * öffnet den Captchaordner
     */
    public void openCaptchaFolder() {
        JDUtilities.openExplorer(this.getCaptchaFolder());
    }

    public void setFile(final File file) {
        this.file = file;
    }

    @Override
    public String toString() {
        final File infoxml = this.getJacinfoXml();
        String ret = null;
        try {
            if (infoxml.exists()) {
                ret = new Regex(JDIO.readFileToString(infoxml), "name=\"([^\"]*)").getMatch(0);
            }
        } catch (final Exception e) {
            // e.printStackTrace();
        }
        if (ret == null) {
            ret = this.file.getName();
        }
        return ret;
    }
}