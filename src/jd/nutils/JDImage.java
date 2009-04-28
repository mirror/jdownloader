package jd.nutils;

import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;

import jd.utils.JDUtilities;

public class JDImage {

    /**
     * Alle verfügbaren Bilder werden hier gespeichert
     */
    public static HashMap<String, BufferedImage> BUFFERED_IMAGE_CACHE = new HashMap<String, BufferedImage>();
    public static HashMap<String, ImageIcon> IMAGE_ICON_CACHE = new HashMap<String, ImageIcon>();
    public static HashMap<String, Image> SCALED_IMAGE_CACHE = new HashMap<String, Image>();

    public static ImageIcon iconToImage(Icon icon) {
        if (icon == null) return null;
        if (icon instanceof ImageIcon && false) {
            return ((ImageIcon) icon);
        } else {
            int w = icon.getIconWidth();
            int h = icon.getIconHeight();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = ge.getDefaultScreenDevice();
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);
            Graphics2D g = image.createGraphics();
            icon.paintIcon(null, g, 0, 0);
            g.dispose();
            return new ImageIcon(image);
        }
    }

    public static ImageIcon getFileIcon(String ext) {
        String id;
        ImageIcon ret;
        if ((ret = IMAGE_ICON_CACHE.get(id = "ext_" + ext + "")) != null) return ret;
        File file = null;
        try {
            file = File.createTempFile("icon", "." + ext);

            sun.awt.shell.ShellFolder shellFolder = sun.awt.shell.ShellFolder.getShellFolder(file);
            ret = new ImageIcon(shellFolder.getIcon(true));
            IMAGE_ICON_CACHE.put(id, ret);
            return ret;

        } catch (Throwable e) {

            // FileSystemView view = FileSystemView.getFileSystemView();
            // return iconToImage(view.getSystemIcon(file));
            return iconToImage(new JFileChooser().getIcon(file));

        } finally {
            if (file != null) file.delete();
        }

    }

    public static ImageIcon getScaledImageIcon(BufferedImage img, int width, int height) {
        return new ImageIcon(getScaledImage(img, width, height));

    }

    public static ImageIcon getScaledImageIcon(ImageIcon img, int width, int height) {
        ImageIcon ret;
        String id;
        long start = System.currentTimeMillis();
        if ((ret = IMAGE_ICON_CACHE.get(id = img.hashCode() + "_" + width + "x" + height)) != null) {
            // System.out.println("Return cached image: " + id + "(" +
            // (System.currentTimeMillis() - start) + ")");
            return ret;
        }
        ret = new ImageIcon(getScaledImage((BufferedImage) img.getImage(), width, height));
        IMAGE_ICON_CACHE.put(id, ret);
        // System.out.println("Return new scaled image: " + id + "(" +
        // (System.currentTimeMillis() - start) + ")");

        return ret;
    }

    public static Image getScaledImage(ImageIcon img, int width, int height) {
        Image ret;
        String id;
        long start = System.currentTimeMillis();

        if ((ret = SCALED_IMAGE_CACHE.get(id = img.hashCode() + "_" + width + "x" + height)) != null) {
            // System.out.println("Return cached image: " + id + "(" +
            // (System.currentTimeMillis() - start) + ")");
            return ret;
        }
        ret = getScaledImage((BufferedImage) img.getImage(), width, height);
        SCALED_IMAGE_CACHE.put(id, ret);
        // System.out.println("Return new scaled image: " + id + "(" +
        // (System.currentTimeMillis() - start) + ")");
        return ret;

    }

    public static Image getScaledImage(BufferedImage img, int width, int height) {
        Image ret;
        String id;
        long start = System.currentTimeMillis();

        if ((ret = SCALED_IMAGE_CACHE.get(id = img.hashCode() + "_" + width + "x" + height)) != null) {
            // System.out.println("Return cached image: " + id + "(" +
            // (System.currentTimeMillis() - start) + ")");
            return ret;
        }
        if (img == null) return null;
        if ((ret = SCALED_IMAGE_CACHE.get(id = img.hashCode() + "_" + width + "x" + height)) != null) {
            // System.out.println("Return cached image: " + id + "(" +
            // (System.currentTimeMillis() - start) + ")");
            return ret;
        }
        double faktor = Math.min((double) img.getWidth() / width, (double) img.getHeight() / height);
        width = (int) (img.getWidth() / faktor);
        height = (int) (img.getHeight() / faktor);
        if (faktor == 1.0) return img;
        ret = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        SCALED_IMAGE_CACHE.put(id, ret);
        // System.out.println("Return new scaled image: " + id + "(" +
        // (System.currentTimeMillis() - start) + ")");
        return ret;

    }

    /**
     * gibt ein bild zu dem übergebenem pfad zurück. nutzt einen cache
     * 
     * @param imageName
     *            Name des Bildes das zurückgeliefert werden soll
     * @return Das gewünschte Bild oder null, falls es nicht gefunden werden
     *         kann
     */
    public static BufferedImage getImage(String imageName) {
        File file;
        long i = System.currentTimeMillis();
        if (!(file = JDUtilities.getResourceFile("jd/img/" + imageName + ".png")).exists()) return null;
        BufferedImage ret;
        if ((ret = BUFFERED_IMAGE_CACHE.get(imageName)) != null) {
            // System.out.println("loaded cached image " + imageName + "(" +
            // (System.currentTimeMillis() - i) + ")");
            return ret;
        }

        try {
            ret = ImageIO.read(file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE, "Exception occured", e);
            return null;
        }
        BUFFERED_IMAGE_CACHE.put(imageName, ret);
        // System.out.println("loaded new image " + imageName + "(" +
        // (System.currentTimeMillis() - i) + ")");
        return ret;

    }

    public static ImageIcon getImageIcon(String string) {
        ImageIcon ret;
        if ((ret = IMAGE_ICON_CACHE.get(string)) != null) { return ret; }
        ret = new ImageIcon(getImage(string));
        IMAGE_ICON_CACHE.put(string, ret);
        return ret;
    }
}
