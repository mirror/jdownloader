//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui.skins.simple.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.PlainDocument;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class TextArea extends JScrollPane implements MouseListener,ClipboardOwner {

    /**
	 * 
	 */
	private static final long serialVersionUID = -3642394448923083114L;

	private static JTextArea txt;

    private int              maxHeight;
    protected Logger logger=JDUtilities.getLogger();
    private int              minHeight;

    protected  TextArea _this;

    public TextArea() {
        super(txt = new JTextArea());
        init();
        _this=this;


    }

    private void onChanged() {
        int height=this.getHeight();
        int lines = txt.getLineCount();
       // if (lines < minHeight) lines = minHeight;
       // if (lines > maxHeight) lines = maxHeight;
        txt.setPreferredSize(new Dimension(-1,1));
       
     txt.setRows(lines);
   
       // logger.info("Set size: "+lines+" - "+txt.getHeight());
        
        if(txt.getHeight()<maxHeight && txt.getHeight()>minHeight){     
   
            this.setPreferredSize(new Dimension(-1,txt.getHeight()));
        }
      
        
       
     
        this.firePropertyChange("HEIGHT", height, this.getHeight()+1);

    }

    @SuppressWarnings("static-access")
	public void setText(String text) {
        this.txt.setText(text);
        onChanged();
    }

    public String getText() {
        return txt.getText();
    }

    private void init() {
        txt.addMouseListener(this);
        this.maxHeight = 200;
        this.minHeight = 35;
    
        PlainDocument doc = (PlainDocument) txt.getDocument();
        doc.addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) {
                onChanged();
            }

            public void insertUpdate(DocumentEvent e) {
           
                onChanged();
            }

            public void removeUpdate(DocumentEvent e) {

                onChanged();
            }
        });
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void setMaxHeight(int maxLines) {
        this.maxHeight = maxLines;
        onChanged();
    }

    public int getMinHeight() {
        return minHeight;
    }

    public void setMinHeight(int minLines) {
        this.minHeight = minLines;
        onChanged();
    }


    public void mouseClicked(MouseEvent e) {
    // TODO Auto-generated method stub

    }

    public void mouseEntered(MouseEvent e) {
    // TODO Auto-generated method stub

    }

    public void mouseExited(MouseEvent e) {
    // TODO Auto-generated method stub

    }

    public void mousePressed(MouseEvent e) {
logger.info("PRESSED");
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {
           // Point point = e.getPoint();
            int x = e.getX();
            int y = e.getY();
            new InternalPopup(this, x, y);
        }

    }

    public void mouseReleased(MouseEvent e) {
    // TODO Auto-generated method stub

    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        // TODO Auto-generated method stub
        
    }

    private class InternalPopup extends JPopupMenu implements ActionListener {

        /**
		 * 
		 */
		private static final long serialVersionUID = 7718129427883204876L;

		private JMenuItem  copy;

        private JMenuItem  paste;

        private JPopupMenu popup;

        private JMenuItem  delete;

      //  private int[]      indeces;

        public InternalPopup(Component parent, int x, int y) {
            popup = new JPopupMenu();

            // Create and add a menu item
            copy = new JMenuItem(JDLocale.L("gui.component.textarea.context.copy", "Kopieren"));
            paste = new JMenuItem(JDLocale.L("gui.component.textarea.context.paste", "Einfügen"));
            delete = new JMenuItem(JDLocale.L("gui.component.textarea.context.delete", "Löschen"));
            copy.addActionListener(this);

            paste.addActionListener(this);

            delete.addActionListener(this);

            popup.add(copy);
            popup.add(paste);
            popup.add(delete);
            popup.add(new JSeparator());

            popup.show(parent, x, y);
        }

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() == copy) {
                Clipboard clip = getToolkit().getSystemClipboard();
                
              
                 
                   StringSelection cont = new StringSelection(txt.getSelectedText());
                   clip.setContents(cont, _this);
            }

            if (e.getSource() == paste) {
                Clipboard clip = getToolkit().getSystemClipboard();
          
                   Transferable cont = clip.getContents(_this);
                   if (cont != null) {                    
                      try {
                         String s = (String) cont.getTransferData( DataFlavor.stringFlavor);
                         String str= txt.getText();
                         String a=str.substring(0,txt.getSelectionStart());
                         String b = str.substring(txt.getSelectionEnd());
                         txt.setText(a+s+b);
                      } catch (Exception e2) {
                         
                      }
                   }
                }     
         
    
         
            if (e.getSource() == delete) {
                String str= txt.getText();
                String a=str.substring(0,txt.getSelectionStart());
                String b = str.substring(txt.getSelectionEnd());
                txt.setText(a+b);
            }

        }
    }
}
