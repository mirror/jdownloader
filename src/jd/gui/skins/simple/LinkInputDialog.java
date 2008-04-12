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


package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Mit dieser Klasse werden in einem Dialog Links entgegengenommen
 * 
 * @author eXecuTe
 */
public class LinkInputDialog extends JDialog implements ActionListener {
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
     * In dieses Textfeld werden die Links eingegeben
     */
    private JTextArea        textArea;
    
    private JButton           btnOK;
    
    private JButton           btnCancel;
    
	/**
     * Scrollpane für das Textfeld
     */
    private JScrollPane       textScrollPane;
    
	/**
     * Elternfenster
     */
    private JFrame            owner;

    /**
     * eingegebene Links
     */
    private String            linksString      = null;

    @SuppressWarnings("unused")
    private static Logger     logger           = JDUtilities.getLogger();

    /**
     * Erstellt einen neuen Dialog.
     * 
     * @param owner Das übergeordnete Fenster
     * @param clipboard Text in der Zwischenablage
     */
    public LinkInputDialog(JFrame owner, String clipboard) {
        
    	super(owner);
        
        this.owner = owner;
        setModal(true);
        setLayout(new GridBagLayout());
        setTitle(JDLocale.L("gui.menu.action.add.name","Links hinzufügen"));
        getRootPane().setDefaultButton(btnOK);
        // funzt nicht
        //setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
	    addWindowListener(new MyWindowListener());

        btnOK = new JButton(JDLocale.L("gui.btn_add","Hinzufügen"));
        btnOK.addActionListener(this);
        btnCancel = new JButton(JDLocale.L("gui.btn_cancel","Abbrechen"));
        btnCancel.addActionListener(this);

        textArea = new JTextArea(10, 30);
        textScrollPane = new JScrollPane(textArea);
        textArea.setEditable(true);
        textScrollPane.setAutoscrolls(true);
        textArea.setText(clipboard);
        
        JDUtilities.addToGridBag(this, textScrollPane, 0, 0, 5, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(this, btnOK,          1, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnCancel,      0, 1, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);

        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        setVisible(true);

    }
    
    /**
     * wird bei Actionen ausgeführt
     */
    public void actionPerformed(ActionEvent e) {
    	
        if (e.getSource() == btnOK) {
        	
        	linksString = textArea.getText();
        	dispose();
		    owner.setVisible(true);
        	
        } else if (e.getSource() == btnCancel) {
        	
        	linksString = "";
        	dispose();
		    owner.setVisible(true);
        	
        }
        
    }

    /**
     * @return die eingegebenen Links
     */
    public String getLinksString() {

        while (linksString == null) {
            try {
                Thread.sleep(500);
            }
            catch (InterruptedException e) {

                 e.printStackTrace();
            }
        }
        return linksString;
    }


    /**
     * schließt das Fenster bei Klick auf x
     */
    class MyWindowListener extends WindowAdapter {
		
		public void windowClosing(WindowEvent e) {
			
			linksString = "";
		    owner.setVisible(true);
		    
		}           
	}
    
}
