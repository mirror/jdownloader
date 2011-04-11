package org.jdownloader.images;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;

import jd.gui.swing.jdgui.GuiConfig;

import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.images.IconIO;

/**
 * New JDownloader Icon Theme Support
 * 
 * @author thomas
 * 
 */
public class Theme {

    private static String THEME_PATH = JsonConfig.create(GuiConfig.class).getThemeID();

    public static void setTheme(String themePath) {
        THEME_PATH = themePath;
    }

    public static ImageIcon getIcon(String relativePath, int size) {

        return IconIO.getImageIcon(getURL("images/", relativePath, ".png"), size);

    }

    /**
     * returns a valid resourceurl or null if no resource is available.
     * 
     * @param pre
     *            subfolder. for exmaple "images/"
     * @param relativePath
     *            relative resourcepath
     * @param ext
     *            resource extension
     * @return
     */
    private static URL getURL(String pre, String relativePath, String ext) {
        try {
            // first lookup in jds home dir. .jd_home or installdirectory
            File file = Application.getResource(getPath(pre, relativePath, ext));
            if (file.exists()) { return file.toURI().toURL(); }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        // afterwards, we lookup in classpath. jar or bin folders
        URL url = Theme.class.getResource(getPath(pre, relativePath, ext));
        if (url == null) url = Theme.class.getResource(getDefaultPath(pre, relativePath, ext));
        return url;
    }

    private static String getDefaultPath(String pre, String path, String ext) {
        StringBuilder sb = new StringBuilder();
        sb.append("/org/jdownloader/themes/standard/");
        sb.append(pre);
        sb.append(path);
        sb.append(ext);
        return sb.toString();
    }

    private static String getPath(String pre, String path, String ext) {
        StringBuilder sb = new StringBuilder();
        sb.append("/org/jdownloader/themes/");
        sb.append(THEME_PATH);
        sb.append("/");
        sb.append(pre);
        sb.append(path);
        sb.append(ext);
        return sb.toString();
    }

}
