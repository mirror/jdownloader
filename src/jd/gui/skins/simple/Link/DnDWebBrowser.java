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
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class DnDWebBrowser extends JFrame {

  /**
     * 
     */
    private static final long serialVersionUID = 2215347106537659141L;

private WebToolBar toolBar;

  private WebBrowserPane browserPane = new WebBrowserPane();

  public DnDWebBrowser() {
    super("Web Browser");

    toolBar = new WebToolBar(browserPane);

    browserPane.setDropTarget(new DropTarget(browserPane, DnDConstants.ACTION_COPY,
        new DropTargetHandler()));

    Container contentPane = getContentPane();
    contentPane.add(toolBar, BorderLayout.NORTH);
    contentPane.add(new JScrollPane(browserPane), BorderLayout.CENTER);
  }
  
  public void goTo(URL url)
  {
      try {
        browserPane.setPage(url);
        toolBar.urlTextField.setText(url.toString());
    } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
    }
  }

  private class DropTargetHandler implements DropTargetListener {
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

    public void dropActionChanged(DropTargetDragEvent event) {
    }

  }
}

class WebBrowserPane extends JEditorPane {

/**
     * 
     */
    private static final long serialVersionUID = 7910030213439231615L;

/**
     * 
     */

private List<URL> history = new ArrayList<URL>();

  private int historyIndex;

  public WebBrowserPane() {
    setEditable(false);
  }

  public void goToURL(URL url) {
    displayPage(url);
    history.add(url);
    historyIndex = history.size() - 1;
  }

  public URL forward() {
    historyIndex++;
    if (historyIndex >= history.size())
      historyIndex = history.size() - 1;

    URL url = (URL) history.get(historyIndex);
    displayPage(url);

    return url;
  }

  public URL back() {
    historyIndex--;
    if (historyIndex < 0)
      historyIndex = 0;
    URL url = (URL) history.get(historyIndex);
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
}

class WebToolBar extends JToolBar implements HyperlinkListener {

  /**
     * 
     */
    private static final long serialVersionUID = -3472298374837088136L;

private WebBrowserPane webBrowserPane;

  private JButton backButton;

  private JButton forwardButton;

  public JTextField urlTextField;

  public WebToolBar(WebBrowserPane browser) {
    super("Web Navigation");
    webBrowserPane = browser;
    webBrowserPane.addHyperlinkListener(this);
    urlTextField = new JTextField(25);
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