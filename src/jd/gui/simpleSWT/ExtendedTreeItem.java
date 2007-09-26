package jd.gui.simpleSWT;

import java.util.HashMap;

import jd.plugins.DownloadLink;
import jd.utils.JDSWTUtilities;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ExtendedTreeItem {
    /**
     * Hier werden TreeItemImages reingeladen fals sie benoetigt werden
     */
    private static HashMap<String, ImageMap> programmImages = new HashMap<String, ImageMap>();
    /*
     * Achtung wenn ein neuer TYPE gesetzt wird muss dafuer die Methode
     * ProgrammImage() gegebenenfalls angepasst werden und neue Bilder in der
     * MainGui.java Methode loadImages() geladen werden
     */
    public final static int STATUS_NOTHING = 1;
    public final static int STATUS_ERROR = 2;
    public final static int STATUS_QUEUE = 3;
    public final static int STATUS_DOWNLOADING = 4;
    public final static int TYPE_FILE = 1;
    public final static int TYPE_FOLDER = 2;
    public final static int TYPE_CONTAINER = 3;
    public final static int TYPE_LOCKEDCONTAINER = 4;
    public final static int TYPE_DEFAULT = 5;
    public final static int LOCKSTATE_UNLOCKED = 1;
    public final static int LOCKSTATE_LOCKED = 2;
    public final static int LOCKSTATE_FOREVERLOCKED = 3;
    public boolean drawProgrammImage = true;
    public TreeItem item;
    private int status = STATUS_NOTHING;
    private int type = TYPE_FILE;
    private int isLocked = LOCKSTATE_UNLOCKED;
    private DownloadLink downloadLink = null;
    private TreeEditor editor = null;
    private ProgressBar progressBar = null;
    /**
     * Konstruktor
     * 
     * @param parent
     */
    public ExtendedTreeItem(ExtendedTree parent) {
        item = new TreeItem(parent.tree, SWT.NONE);
        item.setData(this);
    }
    /**
     * Konstruktor
     * 
     * @param parent
     */
    public ExtendedTreeItem(ExtendedTreeItem parent) {
        item = new TreeItem(parent.item, SWT.NONE);
        item.setData(this);
    }
    /**
     * Konstruktor
     * 
     * @param parent
     * @param index
     */
    public ExtendedTreeItem(ExtendedTree parent, int index) {
        item = new TreeItem(parent.tree, SWT.NONE, index);
        item.setData(this);
    }
    /**
     * Konstruktor
     * 
     * @param parent
     * @param index
     */
    public ExtendedTreeItem(ExtendedTreeItem parent, int index) {
        item = new TreeItem(parent.item, SWT.NONE, index);
        item.setData(this);
    }
    /**
     * 
     * @return ElternItem
     */
    public ExtendedTreeItem getParentItem() {
        return ((item.getParentItem() != null) ? ((ExtendedTreeItem) item.getParentItem().getData()) : null);
    }
    /**
     * 
     * @return subitem
     */
    public ExtendedTreeItem getItem(int index) {
        return (ExtendedTreeItem) item.getItem(index).getData();
    }
    /**
     * 
     * @return all subitems
     */
    public ExtendedTreeItem[] getItems() {
        ExtendedTreeItem[] returnExtendedTreeItems = new ExtendedTreeItem[item.getItemCount()];
        for (int i = 0; i < item.getItemCount(); i++) {
            returnExtendedTreeItems[i] = (ExtendedTreeItem) item.getData();
        }
        return returnExtendedTreeItems;
    }

    public void dispose() {
        for (int i = 0; i < getItemCount(); i++) {
            getItem(i).dispose();
        }
        disposeProgressBar();
        disposeProgrammImage();
        item.dispose();
    }

    public void disposeProgrammImage() {
        String extension = null;
        if (getType() == ExtendedTreeItem.TYPE_FILE && (getText(0) != null)) {
            int dot = getText(0).lastIndexOf('.');
            if (dot != -1)
                extension = getText(0).substring(dot + 1);
        }
        if (extension != null) {
            if (programmImages.containsKey(extension)) {
                ImageMap im = programmImages.get(extension);
                if (im.usageCount <= 1) {
                    im.image.dispose();
                    programmImages.remove(extension);
                } else {
                    im.usageCount--;
                    programmImages.put(extension, im);
                }
            }
        }
    }

    public String getText(int i) {
        return item.getText(i);
    }

    public void setText(int column, String text) {

        if (column == 0) {
            if (downloadLink != null)
                downloadLink.setName(text);
            if (drawProgrammImage)
                redrawProgrammImage(text);
        }
        item.setText(column, text);
        setData(this);
    }
    public void setText(String text) {
        if (drawProgrammImage)
            redrawProgrammImage(text);
        item.setText(text);
        if (downloadLink != null)
            downloadLink.setName(text);
        setData(this);
    }
    public void setText(String[] text) {
        if (drawProgrammImage)
            redrawProgrammImage(text[0]);
        item.setText(text);
        if (downloadLink != null)
            downloadLink.setName(text[0]);
        setData(this);

    }

    /**
     * Diese Methode gibt das Bild fuer den Entsprechenden Typ aus
     * 
     * @param filename
     * @return
     */
    private Image ProgrammImage(String filename) {
        Image img;
        String imageString = null;
        int type = getType();
        if (type != TYPE_FILE) {

            switch (type) {
                case TYPE_FOLDER :
                    imageString = "folder";
                    break;
                case TYPE_CONTAINER :
                    imageString = "container";
                    break;
                case TYPE_LOCKEDCONTAINER :
                    imageString = "container";
                    break;
                case TYPE_DEFAULT :
                    imageString = "default";
                    break;
                default :
                    imageString = "default";
                    break;
            }
            return JDSWTUtilities.getImageSwt(imageString);
        } else {
            String extension = null;
            if (type == ExtendedTreeItem.TYPE_FILE && (filename != null)) {
                int dot = filename.lastIndexOf('.');
                if (dot != -1)
                    extension = filename.substring(dot + 1);
            }
            if (filename != null && extension != null) {
                if (programmImages.containsKey(extension)) {
                    ImageMap im = programmImages.get(extension);
                    im.usageCount++;
                    programmImages.put(extension, im);
                    return im.image;
                } else {
                    Program program = Program.findProgram(extension);
                    ImageData imageData = (program == null ? null : program.getImageData());

                    if (imageData != null) {
                        imageData = imageData.scaledTo(24, 24);
                        img = new Image(item.getDisplay(), imageData);
                        programmImages.put(extension, new ImageMap(img));
                        return img;
                    }
                }
            }

        }
        return JDSWTUtilities.getImageSwt("default");

    }
    /**
     * Zeichnet das Progammimage fals noetig neu
     * 
     * @param newFilename
     */
    private void redrawProgrammImage(String newFilename) {

        String oldFilename = getText(0);
        if (item.getImage() == null) {
            item.setImage(ProgrammImage(newFilename));
            return;
        }
        if (getType() != ExtendedTreeItem.TYPE_FILE)
            return;
        String oldExtension = null;
        if (oldFilename != null) {
            int dot = oldFilename.lastIndexOf('.');
            if (dot != -1)
                oldExtension = oldFilename.substring(dot + 1);
        }
        String newExtension = null;
        if (newFilename != null) {
            int dot = newFilename.lastIndexOf('.');
            if (dot != -1)
                newExtension = newFilename.substring(dot + 1);
        }
        if (oldExtension == newExtension)
            return;
        else {
            item.setImage(ProgrammImage(newFilename));
            if (oldExtension != null) {
                if (programmImages.containsKey(oldExtension)) {
                    ImageMap im = programmImages.get(oldExtension);
                    if (im.usageCount <= 1) {
                        im.image.dispose();
                        programmImages.remove(oldExtension);
                    } else {
                        im.usageCount--;
                        programmImages.put(oldExtension, im);
                    }
                }
            }

        }
    }
    /**
     * Gibt den text von jedem column als Array aus
     */
    public String[] getTextArray() {
        String[] returnStrings = new String[item.getParent().getColumnCount()];
        for (int i = 0; i < returnStrings.length; i++) {
            returnStrings[i] = item.getText(i);
        }
        return returnStrings;
    }
    /**
     * Gibt das Bild von jedem column als Array aus
     */
    public Image[] getImages() {
        Image[] returnImages = new Image[item.getParent().getColumnCount()];
        for (int i = 0; i < returnImages.length; i++) {
            returnImages[i] = item.getImage(i);
        }
        return returnImages;
    }
    /**
     * Gibt die Progressbar aus
     * 
     * @return
     */
    public ProgressBar getProgressBar() {
        return getdata().progressBar;
    }
    /**
     * initialisiert die Progressbar
     * 
     * @param column
     */
    public void setProgressBar(final int column) {
        final Tree tree = item.getParent();
        final TreeItem treeItem = item;
        progressBar = new ProgressBar(tree, SWT.SMOOTH);
        final Listener listener = new Listener() {

            public void handleEvent(Event e) {
                if (e.stateMask == SWT.CTRL && tree.getSelectionCount() > 0) {
                    Event event = new Event();
                    event.widget = tree;
                    int x = 0;
                    int[] c = tree.getColumnOrder();
                    for (int i = 0; i < c.length; i++) {
                        if (c[i] == column)
                            break;
                        x += tree.getColumn(c[i]).getWidth();

                    }

                    event.x = x + e.x;
                    event.y = treeItem.getBounds().y + 5;
                    event.button = 1;
                    TreeItem[] items = tree.getSelection();
                    TreeItem[] items2;
                    boolean isSelected = false;
                    for (int i = 0; i < items.length; i++) {
                        if (items[i] == treeItem) {
                            isSelected = true;
                            break;
                        }
                    }

                    if (isSelected) {
                        if (e.button != 1 && tree.getSelectionCount() > 1)
                            return;
                        items2 = new TreeItem[items.length - 1];
                        int d = 0;
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] != treeItem)
                                items2[d++] = items[i];
                        }
                    } else {
                        items2 = new TreeItem[items.length + 1];
                        items2[0] = treeItem;
                        for (int i = 1; i < items2.length; i++) {
                            items2[i] = items[i - 1];
                        }
                    }
                    // tree.setSelection(items);
                    tree.setSelection(items2);
                    tree.notifyListeners(SWT.MouseDown, event);
                    tree.notifyListeners(SWT.Selection, event);
                } else {
                    if (e.button != 1 && tree.getSelectionCount() > 1) {
                        TreeItem[] items = tree.getSelection();
                        boolean isSelected = false;
                        for (int i = 0; i < items.length; i++) {
                            if (items[i] == treeItem) {
                                isSelected = true;
                                break;
                            }
                        }
                        if (isSelected)
                            return;
                    }
                    Event event = new Event();
                    event.widget = tree;
                    int x = 0;
                    int[] c = tree.getColumnOrder();
                    for (int i = 0; i < c.length; i++) {
                        if (c[i] == column)
                            break;
                        x += tree.getColumn(c[i]).getWidth();

                    }
                    event.x = x + e.x;
                    event.y = treeItem.getBounds().y + 5;
                    event.button = 1;
                    tree.notifyListeners(SWT.MouseDown, event);
                    tree.setSelection(treeItem);
                    tree.notifyListeners(SWT.Selection, event);
                }

            }

        };

        progressBar.addListener(SWT.MouseDown, listener);

        progressBar.setMenu(tree.getMenu());
        editor = new TreeEditor(tree);
        editor.grabHorizontal = editor.grabVertical = true;
        editor.setEditor(progressBar, treeItem, column);
        setData(this);
    }

    public void disposeProgressBar() {
        if (editor != null)
            editor.dispose();
        if (progressBar != null)
            progressBar.dispose();
        progressBar = null;
        editor = null;
        setData(this);
    }
    /**
     * Dies ist die Methode um den Itemstatus auf Downloading zu stellen es wird
     * eine Progressbar fuer dieses Item erzeugt.
     */
    public void setTreeItemDownloading() {
        setProgressBar(2);
        setStatus(ExtendedTreeItem.STATUS_DOWNLOADING);
        setlockState(3);
    }
    /**
     * 
     * @return ExtendedTreeItem
     */
    public ExtendedTreeItem getdata() {
        return (ExtendedTreeItem) item.getData();

    }
    /**
     * 
     * @return status
     */
    public int getStatus() {
        return getdata().status;
    }
    /**
     * 
     * @return type
     */
    public int getType() {
        return getdata().type;
    }
    /**
     * 
     * @return DownloadLink
     */
    public DownloadLink getDownloadLink() {
        return getdata().downloadLink;
    }
    /**
     * 
     * @param status
     */
    public void setStatus(int status) {
        this.status = status;
        setData(this);
    }
    /**
     * 
     * @param type
     */
    public void setType(int type) {
        this.type = type;
        setData(this);
    }
    /**
     * 
     * @param downloadLink
     */
    public void setDownloadLink(DownloadLink downloadLink) {
        this.downloadLink = downloadLink;
        setData(this);
    }
    /**
     * Interne Methode zum setzen der neuen Position
     * 
     * @param ex
     * @return
     */
    private ExtendedTreeItem setex(ExtendedTreeItem ex) {
        for (int i = 0; i < item.getItemCount(); i++) {
            System.out.println(i);
            getItem(i).setex(new ExtendedTreeItem(ex));
        }

        ex.setStatus(getStatus());
        ex.setType(getType());
        ex.setDownloadLink(getDownloadLink());
        ex.setText(getTextArray());
        disposeProgrammImage();
        return ex;
    }
    /**
     * 
     * @param ExtendedTreeItem
     *            parent
     */
    public void setPosition(ExtendedTreeItem parent) {
        ExtendedTreeItem ex = new ExtendedTreeItem(parent);
        ex = setex(ex);
        item.dispose();
        item = ex.item;

    }
    /**
     * 
     * @param ExtendedTree
     *            parent
     */
    public void setPosition(ExtendedTree parent) {
        ExtendedTreeItem ex = new ExtendedTreeItem(parent);
        ex = setex(ex);
        item.dispose();
        item = ex.item;
    }
    /**
     * 
     * @param ExtendedTreeItem
     *            parent
     * @param index
     */
    public void setPosition(ExtendedTreeItem parent, int index) {
        ExtendedTreeItem ex = new ExtendedTreeItem(parent, index);
        ex = setex(ex);
        item.dispose();
        item = ex.item;
    }
    /**
     * 
     * @param ExtendedTree
     *            parent
     * @param index
     */
    public void setPosition(ExtendedTree parent, int index) {
        ExtendedTreeItem ex = new ExtendedTreeItem(parent, index);
        ex = setex(ex);
        item.dispose();
        item = ex.item;
    }
    /**
     * 
     * @return ItemCount
     */
    private int getItemCount() {
        return item.getItemCount();
    }
    /**
     * 
     * @param itemData
     */
    public void setData(Object itemData) {
        item.setData(itemData);
    }
    /**
     * setzt des Sperrstatus des TreeItems
     * 
     */
    public void setlockState(int state) {
        isLocked = state;
    }

    /**
     * 
     * @return Sperrstatus des TreeItems
     */
    public int getlockState() {
        return isLocked;
    }
    /**
     * 
     * @return Ob das TreeItem locked ist
     */
    public boolean isLocked() {
        return getdata().isLocked > LOCKSTATE_UNLOCKED;

    }
    /**
     * Checkt ob ein ElternItem selektiert ist
     * 
     * @param item
     * @return
     */
    public boolean isParentSelected() {
        TreeItem[] selection = item.getParent().getSelection();
        TreeItem parent = item.getParentItem();
        while (parent != null) {
            for (int i = 0; i < selection.length; i++) {
                if (parent == selection[i])
                    return true;
            }
            parent = parent.getParentItem();
        }
        return false;
    }

    public String getText() {
        return item.getText();
    }

    public Rectangle getBounds() {
        return item.getBounds();
    }

    private class ImageMap {

        private int usageCount;
        private Image image;
        private ImageMap(Image image) {
            this.image = image;
            usageCount = 1;
        }
    }

}
