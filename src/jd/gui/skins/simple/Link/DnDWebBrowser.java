//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui.skins.simple.Link;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class DnDWebBrowser extends JDialog {

    private class DropTargetHandler implements DropTargetListener {
        public void dragEnter(DropTargetDragEvent event) {
            if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor))
                event.acceptDrag(DnDConstants.ACTION_COPY);
            else {
                event.rejectDrag();
            }
        }

        public void dragExit(DropTargetEvent event) {
        }

        public void dragOver(DropTargetDragEvent event) {
        }

        @SuppressWarnings({ "unchecked", "deprecation" })
        public void drop(DropTargetDropEvent event) {
            Transferable transferable = event.getTransferable();
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    List fileList = (List) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    Iterator iterator = fileList.iterator();

                    while (iterator.hasNext()) {
                        File file = (File) iterator.next();
                        browserPane.goToURL(file.toURL());
                    }
                    event.dropComplete(true);
                } catch (UnsupportedFlavorException flavorException) {
                    flavorException.printStackTrace();
                    event.dropComplete(false);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                    event.dropComplete(false);
                }
            } else {
                event.rejectDrop();
            }
        }

        public void dropActionChanged(DropTargetDragEvent event) {
        }

    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private WebBrowserPane browserPane = new WebBrowserPane();

    /**
     * 
     */

    private WebToolBar toolBar;

    public DnDWebBrowser(JFrame owner) {
        super(owner);
        setModal(true);
        toolBar = new WebToolBar(browserPane);

        browserPane.setDropTarget(new DropTarget(browserPane, DnDConstants.ACTION_COPY, new DropTargetHandler()));

        Container contentPane = getContentPane();
        contentPane.add(toolBar, BorderLayout.NORTH);
        contentPane.add(new JScrollPane(browserPane), BorderLayout.CENTER);
    }

    public void goTo(URL url) {
        try {
            browserPane.setPage(url);
            toolBar.urlTextField.setText(url.toString());
        } catch (IOException e) {
            
            e.printStackTrace();
        }
    }
}

class WebBrowserPane extends JEditorPane {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */

    /**
     * 
     */

    private List<URL> history = new ArrayList<URL>();

    private int historyIndex;

    public WebBrowserPane() {
        setEditable(false);
    }

    public URL back() {
        historyIndex--;
        if (historyIndex < 0) historyIndex = 0;
        URL url = history.get(historyIndex);
        displayPage(url);

        return url;
    }

    private void displayPage(URL pageURL) {
        try {
            setPage(pageURL);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    public URL forward() {
        historyIndex++;
        if (historyIndex >= history.size()) historyIndex = history.size() - 1;

        URL url = history.get(historyIndex);
        displayPage(url);

        return url;
    }

    public void goToURL(URL url) {
        displayPage(url);
        history.add(url);
        historyIndex = history.size() - 1;
    }
}

class WebToolBar extends JToolBar implements HyperlinkListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private JButton backButton;

    private JButton forwardButton;

    public JTextField urlTextField;

    /**
     * 
     */

    private WebBrowserPane webBrowserPane;

    public WebToolBar(WebBrowserPane browser) {
        super("Web Navigation");
        webBrowserPane = browser;
        webBrowserPane.addHyperlinkListener(this);
        urlTextField = new JTextField(25);
        urlTextField.setEditable(true);
        urlTextField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                try {
                    URL url = new URL(urlTextField.getText());
                    webBrowserPane.goToURL(url);
                } catch (MalformedURLException urlException) {
                    urlException.printStackTrace();
                }
            }
        });
        backButton = new JButton("back");

        backButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                URL url = webBrowserPane.back();
                urlTextField.setText(url.toString());
            }
        });
        forwardButton = new JButton("forward");

        forwardButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                URL url = webBrowserPane.forward();
                urlTextField.setText(url.toString());
            }
        });
        add(backButton);
        add(forwardButton);
        add(urlTextField);

    }

    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            URL url = event.getURL();
            webBrowserPane.goToURL(url);
            urlTextField.setText(url.toString());
        }
    }
}