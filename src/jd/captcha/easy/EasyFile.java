package jd.captcha.easy;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

import javax.swing.ImageIcon;

import jd.config.container.JDLabelContainer;

import jd.nutils.JDImage;

public class EasyFile implements JDLabelContainer {
    public File file = null;
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