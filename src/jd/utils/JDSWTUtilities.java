package jd.utils;
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

import jd.gui.simpleSWT.GuiListeners;
import jd.gui.simpleSWT.ItemData;
import jd.gui.simpleSWT.MainGui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

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
     * Hier werden TreeItemImages reingeladen fals sie benoetigt werden
     */
    private static HashMap<String, ImageMap> programmImages = new HashMap<String, ImageMap>();
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

    public static void disposeItem(TreeItem item) {
        ItemData data = (ItemData) item.getData();
        data.disposeProgressBar();
        String extension=null;
        if (data.type == ItemData.TYPE_FILE && (item.getText(0) != null)) {
            int dot = item.getText(0).lastIndexOf('.');
            if (dot != -1)
                extension = item.getText(0).substring(dot + 1);
        }
        if (extension != null) {
            if (programmImages.containsKey(extension))
            {
                ImageMap im = programmImages.get(extension);
                if(im.usageCount<=1)
                {
                    im.image.dispose();
                    programmImages.remove(extension);
                }
                else
                {
                    im.usageCount--;
                    programmImages.put(extension, im);
                }
            }
        }
        TreeItem[] itm = item.getItems();
        for (int i = 0; i < itm.length; i++) {
            disposeItem(itm[i]);
        }
        item.dispose();
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
     * TODO Dies ist die Methode um den Itemstatus auf Downloading zu stellen es
     * wird eine Progressbar fuer dieses Item erzeugt. Diese Progressbar
     * benoetigt noch einen Listener.
     * 
     * @param treeItem
     * @return
     */
    public static void setTreeItemDownloading(final TreeItem treeItem, GuiListeners guiListeners) {
        ItemData itd = new ItemData();
        itd.setProgressBar(treeItem, guiListeners);
        itd.status=ItemData.STATUS_DOWNLOADING;
        itd.isLocked=true;
        treeItem.setData(itd);
    }
    
    /**
     * Diese Methode gibt das Bild fuer den Entsprechenden Typ aus
     * 
     * @param display
     * @param type
     * @return
     */
    public static Image getTreeImage(Display display, int type, String filename, boolean changeUsageCount) {
        Image img;
        String imageString = "";
        if (type > 1) {
            switch (type) {
                case ItemData.TYPE_FOLDER :
                    imageString = "folder";
                    break;
                case ItemData.TYPE_CONTAINER :
                    imageString = "container";
                    break;
                case ItemData.TYPE_LOCKEDCONTAINER :
                    imageString = "container";
                    break;
                case ItemData.TYPE_DEFAULT :
                    imageString = "default";
                    break;
                default :
                    imageString = "default";
                    break;
            }

            if (imagesSWT.containsKey(imageString))
                return imagesSWT.get(imageString);
        } else {
            String extension=null;
            if (type == ItemData.TYPE_FILE && (filename != null)) {
                int dot = filename.lastIndexOf('.');
                if (dot != -1)
                    extension = filename.substring(dot + 1);
            }
            if (filename != null && extension != null) {
                if (programmImages.containsKey(extension))
                {
                    ImageMap im = programmImages.get(extension);
                    if(changeUsageCount)
                    {
                        im.usageCount++;
                        programmImages.put(extension, im);
                    }
                    return im.image;
                }
                else {
                    Program program = Program.findProgram(extension);
                    ImageData imageData = (program == null ? null : program.getImageData());

                    if (imageData != null) {
                        imageData = imageData.scaledTo(24, 24);
                        img = new Image(display, imageData);
                        programmImages.put(extension, new ImageMap(img) );
                        return img;
                    }
                }
            }

        }
        return getTreeImage(display, ItemData.TYPE_DEFAULT, null, changeUsageCount);

    }
    public static void redrawTreeImage(TreeItem item, String newFilename) { 
        String oldFilename=item.getText(0);
        if(((ItemData)item.getData()).type != ItemData.TYPE_FILE)
            return;
        String oldExtension=null;
        if (oldFilename != null) {
            int dot = oldFilename.lastIndexOf('.');
            if (dot != -1)
                oldExtension = oldFilename.substring(dot + 1);
        }
        String newExtension=null;
        if (newFilename != null) {
            int dot = newFilename.lastIndexOf('.');
            if (dot != -1)
                newExtension = newFilename.substring(dot + 1);
        }
        if(oldExtension==newExtension)
        return;
        else
        {
            item.setImage(getTreeImage(item.getDisplay(),ItemData.TYPE_FILE,newFilename,true));
            if (oldExtension != null) {
                if (programmImages.containsKey(oldExtension))
                {
                    ImageMap im = programmImages.get(oldExtension);
                    if(im.usageCount<=1)
                    {
                        im.image.dispose();
                        programmImages.remove(oldExtension);
                    }
                    else
                    {
                        im.usageCount--;
                        programmImages.put(oldExtension, im);
                    }
                }
            }
            
        }
    }
    /**
     * Eine Die vielen Methoden ein neues TreeItem zu erstellen
     * 
     * @param parent
     * @param text
     * @param link
     * @param status
     * @param type
     * @param index
     * @return
     */
    public static TreeItem createTreeitem(Tree parent, String[] text, String link, int status, int type, int index) {
        TreeItem treeItem = null;
        treeItem = new TreeItem(parent, SWT.NONE, index);
        addItemData(treeItem, text, link, status, type);
        return treeItem;
    }

    public static TreeItem createTreeitem(TreeItem parent, String[] text, String link, int status, int type, int index) {
        TreeItem treeItem = null;
        treeItem = new TreeItem(parent, SWT.NONE, index);
        addItemData(treeItem, text, link, status, type);
        return treeItem;
    }

    public static TreeItem createTreeitem(Tree parent, String[] text, String link, int status, int type) {
        TreeItem treeItem = null;
        treeItem = new TreeItem(parent, SWT.NONE);
        addItemData(treeItem, text, link, status, type);
        return treeItem;
    }

    public static TreeItem createTreeitem(TreeItem parent, String[] text, String link, int status, int type) {
        TreeItem treeItem = null;
        treeItem = new TreeItem(parent, SWT.NONE);
        addItemData(treeItem, text, link, status, type);
        return treeItem;
    }

    /**
     * Mit dieser Methode werden die Daten dem TreeItem hinzugefuegt
     * 
     * @param item
     * @param text
     * @param link
     * @param status
     * @param type
     */
    private static void addItemData(TreeItem item, String[] text, String link, int status, int type) {
        ItemData itd = new ItemData();
        itd.link=link;
        itd.status=status;
        itd.type=type;
        item.setData(itd);
        item.setText(text);
        item.setImage(getTreeImage(item.getDisplay(), type, text[0], true));
    }
    /**
     * Checkt den Itemstatus und den seiner SubItems und gibt true aus wenn ein
     * Item den Status hat
     */
    @SuppressWarnings("unused")
    private static boolean checkItemStatus(TreeItem item, int status) {
        if (((ItemData) item.getData()).status == status)
            return true;
        TreeItem[] itm = item.getItems();
        for (int i = 0; i < itm.length; i++) {
            if (itm[i].getItemCount() > 0)
                return checkItemStatus(itm[i], status);
            else if (((ItemData) item.getData()).status == status) {
                return true;
            }

        }
        return false;
    }

    /**
     * Checkt den Itemtyp und den seiner SubItems und gibt true aus wenn es ein
     * Item dieses Typs ist
     */
    @SuppressWarnings("unused")
    private static boolean checkItemType(TreeItem item, int type) {
        if (((ItemData) item.getData()).type == type)
            return true;
        TreeItem[] itm = item.getItems();
        for (int i = 0; i < itm.length; i++) {
            if (itm[i].getItemCount() > 0)
                return checkItemType(itm[i], type);
            else if (((ItemData) item.getData()).type == type) {
                return true;
            }

        }
        return false;
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

class ImageMap {

    public int usageCount;
    public Image image;
    
    public ImageMap(Image image) {
        this.image=image;
        usageCount=1;
    }

}
