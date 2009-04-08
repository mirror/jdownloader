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
    public static HashMap<String, BufferedImage> imagesCache = new HashMap<String, BufferedImage>();

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

        File file = null;
        try {
            file = File.createTempFile("icon", "." + ext);

            sun.awt.shell.ShellFolder shellFolder = sun.awt.shell.ShellFolder.getShellFolder(file);

            return new ImageIcon(shellFolder.getIcon(true));

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
        return new ImageIcon(getScaledImage((BufferedImage) img.getImage(), width, height));

    }

    public static Image getScaledImage(ImageIcon img, int width, int height) {
        return getScaledImage((BufferedImage) img.getImage(), width, height);

    }

    public static Image getScaledImage(BufferedImage img, int width, int height) {
        // corrects the viewport
        if (img == null) return null;
        double faktor = Math.min((double) img.getWidth() / width, (double) img.getHeight() / height);
        width = (int) (img.getWidth() / faktor);
        height = (int) (img.getHeight() / faktor);
        if (faktor == 1.0) return img;
        return img.getScaledInstance(width, height, Image.SCALE_SMOOTH);

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
        if (!(file = JDUtilities.getResourceFile("jd/img/" + imageName + ".png")).exists()) return null;
        if (!imagesCache.containsKey(imageName)) {
            BufferedImage newImage;
            try {
                newImage = ImageIO.read(file);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                jd.controlling.JDLogger.getLogger().log(java.util.logging.Level.SEVERE,"Exception occured",e);
                return null;
            }
            imagesCache.put(imageName, newImage);

        }
        if (new ImageIcon(imagesCache.get(imageName)).getIconWidth() < 4) {
            System.out.println(imageName);
        }
        return imagesCache.get(imageName);
    }

    public static ImageIcon getImageIcon(String string) {
        // TODO Auto-generated method stub
        return new ImageIcon(getImage(string));
    }
}
