//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.gui.swing.dialog;

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
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.swing.dialog.AbstractDialog;
import net.miginfocom.swing.MigLayout;

public class DnDWebBrowser extends AbstractDialog {

    private class DropTargetHandler implements DropTargetListener {
        public void dragEnter(DropTargetDragEvent event) {
            if (event.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.acceptDrag(DnDConstants.ACTION_COPY);
            } else {
                event.rejectDrag();
            }
        }

        public void dragExit(DropTargetEvent event) {
        }

        public void dragOver(DropTargetDragEvent event) {
        }

        @SuppressWarnings("unchecked")
        public void drop(DropTargetDropEvent event) {
            Transferable transferable = event.getTransferable();
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                try {
                    List<File> fileList = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);

                    for (File file : fileList) {
                        browserPane.goToURL(file.toURI().toURL());
                    }
                    event.dropComplete(true);
                } catch (UnsupportedFlavorException flavorException) {
                    JDLogger.exception(flavorException);
                    event.dropComplete(false);
                } catch (IOException ioException) {
                    JDLogger.exception(ioException);
                    event.dropComplete(false);
                }
            } else {
                event.rejectDrop();
            }
        }

        public void dropActionChanged(DropTargetDragEvent event) {
        }

    }

    private static final long serialVersionUID = 1L;

    private URL url;

    private WebBrowserPane browserPane;

    private WebToolBar toolBar;

    public DnDWebBrowser(URL url) {
        super(UserIO.NO_COUNTDOWN | UserIO.NO_ICON | UserIO.NO_CANCEL_OPTION | UserIO.NO_OK_OPTION, "Java WebBrowser", null, null, null);

        this.url = url;

        init();
    }

    @Override
    protected void packed() {
        goTo(url);
        setDefaultCloseOperation(DnDWebBrowser.DISPOSE_ON_CLOSE);
        setSize(800, 600);
    }

    @Override
    public JComponent contentInit() {
        browserPane = new WebBrowserPane();
        toolBar = new WebToolBar(browserPane);
        browserPane.setDropTarget(new DropTarget(browserPane, DnDConstants.ACTION_COPY, new DropTargetHandler()));

        JPanel panel = new JPanel(new MigLayout("ins 0, wrap 1", "[grow,fill]"));
        panel.add(toolBar);
        panel.add(new JScrollPane(browserPane), "spany");
        return panel;
    }

    public void goTo(URL url) {
        try {
            browserPane.setPage(url);
            toolBar.urlTextField.setText(url.toString());
        } catch (IOException e) {

            JDLogger.exception(e);
        }
    }
}

class WebBrowserPane extends JEditorPane {

    private static final long serialVersionUID = 1L;

    private ArrayList<URL> history = new ArrayList<URL>();

    private int historyIndex;

    public WebBrowserPane() {
        setEditable(false);
    }

    public URL back() {
        historyIndex--;
        if (historyIndex < 0) {
            historyIndex = 0;
        }
        URL url = history.get(historyIndex);
        displayPage(url);

        return url;
    }

    private void displayPage(URL pageURL) {
        try {
            setPage(pageURL);
        } catch (IOException ioException) {
            JDLogger.exception(ioException);
        }
    }

    public URL forward() {
        historyIndex++;
        if (historyIndex >= history.size()) {
            historyIndex = history.size() - 1;
        }

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

    private static final long serialVersionUID = 1L;

    private JButton backButton;

    private JButton forwardButton;

    public JTextField urlTextField;

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
                    JDLogger.exception(urlException);
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