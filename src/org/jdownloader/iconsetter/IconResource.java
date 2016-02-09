package org.jdownloader.iconsetter;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;

public class IconResource {

    private String path;
    private String standardMd5;

    public String getPath() {
        return path;
    }

    public IconResource(String rel, String md5) {
        this.path = rel;
        this.standardMd5 = md5;
    }

    public String getStandardMd5() {
        return standardMd5;
    }

    private HashSet<String> sets = new HashSet<String>();

    public void addSet(String name) {
        sets.add(name);
    }

    public String getTags() {
        String name = new File(path).getName();
        name = name.replaceAll("\\..+$", "");
        name = name.replaceAll("[^\\w]+", " ");
        return name;
    }

    public Icon getIcon(String name, int size) {
        File file = Application.getResource("themes/" + name + "/" + getPath());

        try {
            return new ImageIcon(IconIO.getScaledInstance(ImageIO.read(file), size, size));
        } catch (IOException e) {

            return null;
        }
    }

    public File getFile(ResourceSet resoureSet) {

        return Application.getResource("themes/" + resoureSet.getName() + "/" + getPath());

    }

}
