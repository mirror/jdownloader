package org.jdownloader.iconsetter;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Properties;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.utils.images.IconIO;
import org.jdownloader.images.IdentifierImageIcon;

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

    public String getTags(ResourceSet resourceSet) {
        String name = new File(path).getName();
        // name = name.replaceAll("\\..+$", "");
        name = name.replaceAll("\\W+", " ");
        name = name.replaceAll("([a-z])\\s*([A-Z])", "$1 $2");
        name = name.toLowerCase(Locale.ENGLISH);
        File svg = getFile(resourceSet, "svg.icons8");
        if (svg.exists()) {
            String str;
            try {

                Properties props = new Properties();
                FileInputStream fis = new FileInputStream(svg);
                try {
                    props.load(fis);

                } finally {
                    fis.close();
                }
                if (props.getProperty("name") != null) {
                    name = name.replace(props.getProperty("name").toLowerCase(Locale.ENGLISH), "");
                    name += " " + props.getProperty("name");

                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return name;
    }

    public Icon getIcon(String name, int size) {
        File file = new File(IconSetMaker.THEMES, "/themes/" + name + "/" + getPath() + ".svg");

        if (file.exists()) {

            try {

                Image image = IconIO.getImageFromSVG(file.toURI().toURL(), size, size);
                Graphics g = image.getGraphics();
                g.setColor(Color.RED);
                g.drawRect(0, 0, image.getWidth(null) - 1, image.getHeight(null) - 1);
                g.dispose();
                return new IdentifierImageIcon(image, getPath());
            } catch (IOException e) {

                return null;
            }

        } else {
            file = new File(IconSetMaker.THEMES, "themes/" + name + "/" + getPath() + ".png");

            try {
                BufferedImage image = size > 0 ? IconIO.getScaledInstance(ImageIO.read(file), size, size) : ImageIO.read(file);
                Graphics g = image.getGraphics();
                g.setColor(Color.RED);
                g.drawRect(0, 0, image.getWidth(null) - 1, image.getHeight(null) - 1);
                g.dispose();
                return new ImageIcon(image);
            } catch (IOException e) {

                return null;
            }
        }
    }

    public File getFile(ResourceSet resoureSet, String ext) {

        return new File(IconSetMaker.THEMES, "themes/" + resoureSet.getName() + "/" + getPath() + "." + ext);

    }

}
