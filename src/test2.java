import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.ListIterator;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

public class test2 extends JPanel implements DropTargetListener {

    private static final long serialVersionUID = 3583068430484359388L;
    DropTarget dropTarget;
    JLabel dropHereLabel;
    static DataFlavor urlFlavor, uriListFlavor, macPictStreamFlavor;

    static {
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
            uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String");
            macPictStreamFlavor = new DataFlavor("image/x-pict; class=java.io.InputStream");
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        }
    }

    public test2() {
        super(new BorderLayout());
        dropHereLabel = new JLabel("   Drop here   ", SwingConstants.CENTER);
        dropHereLabel.setFont(getFont().deriveFont(Font.BOLD, 24.0f));
        add(dropHereLabel, BorderLayout.CENTER);
        // set up drop target stuff
        dropTarget = new DropTarget(dropHereLabel, this);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Image DropTarget Demo");
        test2 demoPanel = new test2();
        frame.getContentPane().add(demoPanel);
        frame.pack();
        frame.setVisible(true);
    }

    // drop target listener events

    public void dragEnter(DropTargetDragEvent dtde) {
        System.out.println("dragEnter");
    }

    public void dragExit(DropTargetEvent dte) {
        System.out.println("dragExit");
    }

    public void dragOver(DropTargetDragEvent dtde) {
        System.out.println("dragOver");

    }

    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent dtde) {
        System.out.println("drop");
        dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
        Transferable trans = dtde.getTransferable();
        System.out.println("Flavors:");
        dumpDataFlavors(trans);
        boolean gotData = false;
        try {
            // try to get an image
            if (trans.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                System.out.println("image flavor is supported");
                Image img = (Image) trans.getTransferData(DataFlavor.imageFlavor);
                showImageInNewFrame(img);
                gotData = true;
            } else if (trans.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                System.out.println("javaFileList is supported");
                List<File> list = (List<File>) trans.getTransferData(DataFlavor.javaFileListFlavor);
                ListIterator<File> it = list.listIterator();
                while (it.hasNext()) {
                    File f = it.next();
                    ImageIcon icon = new ImageIcon(f.getAbsolutePath());
                    showImageInNewFrame(icon);
                }
                gotData = true;

            } else if (trans.isDataFlavorSupported(uriListFlavor)) {
                System.out.println("uri-list flavor is supported");
                String uris = (String) trans.getTransferData(uriListFlavor);
                // url-lists are defined by rfc 2483 as crlf-delimited
                StringTokenizer izer = new StringTokenizer(uris, "\r\n");
                while (izer.hasMoreTokens()) {
                    String uri = izer.nextToken();
                    System.out.println(uri);
                    ImageIcon icon = new ImageIcon(uri);
                    showImageInNewFrame(icon);
                }
                gotData = true;
            } else if (trans.isDataFlavorSupported(urlFlavor)) {
                System.out.println("url flavor is supported");
                URL url = (URL) trans.getTransferData(urlFlavor);
                System.out.println(url.toString());
                ImageIcon icon = new ImageIcon(url);
                showImageInNewFrame(icon);
                gotData = true;
            } else if (trans.isDataFlavorSupported(macPictStreamFlavor)) {
                System.out.println("mac pict stream flavor is supported");
                InputStream in = (InputStream) trans.getTransferData(macPictStreamFlavor);
                // for the benefit of the non-mac crowd, this is
                // done with reflection. directly, it would be:
                // Image img = QTJPictHelper.pictStreamToJavaImage (in);
                Class<?> qtjphClass = Class.forName("QTJPictHelper");
                Class<?>[] methodParamTypes = { InputStream.class };
                Method method = qtjphClass.getDeclaredMethod("pictStreamToJavaImage", methodParamTypes);
                Object[] methodParams = { in };
                Image img = (Image) method.invoke(null, methodParams);
                showImageInNewFrame(img);
                gotData = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("gotData is " + gotData);
            dtde.dropComplete(gotData);
        }
    }

    /** Liest den kompletten Reader in einen String und schließt ihn danach */
    public static String readAll(Reader in) throws IOException {
        if (in == null) throw new NullPointerException("in == null");
        try {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int charsRead;
            while ((charsRead = in.read(buf)) != -1) {
                sb.append(buf, 0, charsRead);
            }
            return sb.toString();
        } finally {
            in.close();
        }
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        System.out.println("dropActionChanged");

    }

    public void showImageInNewFrame(ImageIcon icon) {
        JFrame frame = new JFrame();
        frame.getContentPane().add(new JLabel(icon));
        frame.pack();
        frame.setVisible(true);
    }

    public void showImageInNewFrame(Image image) {
        showImageInNewFrame(new ImageIcon(image));
    }

    private void dumpDataFlavors(Transferable trans) {
        System.out.println("Flavors:");
        DataFlavor[] flavors = trans.getTransferDataFlavors();

        for (int i = 0; i < flavors.length; i++) {
            System.out.println("*** " + i + ": " + flavors[i]);
        }
        System.out.println("*** BEST " + DataFlavor.selectBestTextFlavor(flavors));
    }

}

/*
 * Swing Hacks Tips and Tools for Killer GUIs By Joshua Marinacci, Chris Adamson
 * First Edition June 2005 Series: Hacks ISBN: 0-596-00907-0
 * http://www.oreilly.com/catalog/swinghks/
 */
