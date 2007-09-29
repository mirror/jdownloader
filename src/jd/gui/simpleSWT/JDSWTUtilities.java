package jd.gui.simpleSWT;
/**
 * Allemoeglichen Methoden um etwas mehr uebersicht in die MainGui zu bringen
 * @author DwD
 *
 */
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

public class JDSWTUtilities {
    /**
     * RessourceBundle für Texte
     */
    private static ResourceBundle resourceBundle = null;
    /**
     * Angaben über Spracheinstellungen
     */
    private static Locale locale = null;
    /**
     * Alle verfügbaren SWT Bilder werden hier gespeichert
     */
    private static HashMap<String, Image> imagesSWT = new HashMap<String, Image>();

    /**
     * Fuegt die SWT Bilder zur HashMap hinzu
     */
    public static void addImageSwt(String imageName, InputStream inputStream, Display display) {
        imagesSWT.put(imageName, new Image(display, inputStream));
    }
    /**
     * Holt die SWT Bilder aus der HashMap heraus
     */
    public static Image getImageSwt(String imageName) {
        return imagesSWT.get(imageName);
    }
    /**
     * Erstellt ein neues ToolBarItem, das sind diese Bunten Icons ganz oben im
     * GUI
     * 
     * @param display
     * @param toolBar
     * @param imageName
     * @return
     */
    public static ToolItem toolBarItem(Display display, ToolBar toolBar, String imageName) {
        return toolBarItem(display, toolBar, imageName, SWT.NONE);
    }

    public static ToolItem toolBarItem(Display display, ToolBar toolBar, String imageName, int style) {
        ToolItem TI = new ToolItem(toolBar, style);
        Image img = getImageSwt(imageName);
        TI.setImage(img);
        if (!SWT.getPlatform().equals("gtk"))
            TI.setDisabledImage(new Image(display, img, SWT.IMAGE_GRAY));
        return TI;
    }


    /**
     * Hier kann man neue TreeColumns erstellen.
     */
    public static TreeColumn treeCol(String text, Tree tree, int width) {
        TreeColumn treeColumn = new TreeColumn(tree, SWT.LEFT);
        treeColumn.setText(text);
        treeColumn.setWidth(width);
        return treeColumn;
    }
    /**
     * Liefert eine Zeichenkette aus dem aktuellen ResourceBundle zurück
     * 
     * @param key
     *            Identifier der gewünschten Zeichenkette
     * @return Die gewünschte Zeichnenkette
     */
    public static String getSWTResourceString(String key) {
        if (resourceBundle == null) {
            if (locale == null) {
                locale = Locale.getDefault();
            }
            resourceBundle = ResourceBundle.getBundle("SWTLanguagePack", locale);
        }
        String result = key;
        try {
            result = resourceBundle.getString(key);
        } catch (MissingResourceException e) {
            // logger.warning("resource missing." + e.getKey());
        }
        return result;
    }
    /**
     * Liefert einer char aus dem aktuellen ResourceBundle zurück
     * 
     * @param key
     *            Identifier des gewünschten chars
     * @return der gewünschte char
     */
    public static int getSWTResourceMnemChar(String ResourceString) {
        int i = ResourceString.indexOf("&");
        ResourceString = ResourceString.toLowerCase();
        if (i != -1 && ResourceString.length() > i)
            return ResourceString.charAt(i + 1);
        return -1;
    }
    
    

}
