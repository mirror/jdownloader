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


package jd.gui.skins.simple;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;

import jd.gui.skins.simple.Link.JLinkButton;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Dieser Dialog wird angezeigt, wenn ein Download mit einem Plugin getätigt wird,
 * dessen Agbs noch nicht akzeptiert wurden
 * 
 * @author eXecuTe
 */

public class AgbDialog extends JDialog implements ActionListener {
	
	private static final long serialVersionUID = 1L;
	
    private JLabel        	  labelInfo1;
    private JLabel        	  labelInfo2;
    private JLinkButton       linkAgb;
    private JCheckBox         checkAgbAccepted;
    private JButton           btnOK;
    private JButton           btnCancel;
    
	/**
     * betroffenes Plugin
     */
    
    private PluginForHost     plugin;
    
	/**
     * abzuarbeitender Link
     */
    
    private DownloadLink     downloadLink;

    /**
     * zeigt einen Dialog in dem man die Hoster AGB akzeptieren kann
     * 
     * @param downloadLink abzuarbeitender Link
     */
    
    public AgbDialog(DownloadLink downloadLink) {
        
    	super();

        this.plugin = (PluginForHost) downloadLink.getPlugin();
        this.downloadLink = downloadLink;
        
        setModal(true);
        setLayout(new GridBagLayout());
        getRootPane().setDefaultButton(btnOK);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setTitle(JDLocale.L("gui.dialogs.agb_tos.title",
        					"Allgemeine Geschäftsbedingungen nicht aktzeptiert"));
	    
	    labelInfo1 = new JLabel(JDLocale.L("gui.dialogs.agb_tos.description1", String.format(
	    		"Die Allgemeinen Geschäftsbedingungen (AGB) von %s",
	    		plugin.getHost())));
	    
	    labelInfo2 = new JLabel(JDLocale.L("gui.dialogs.agb_tos.description2",
	    		"wurden nicht gelesen und akzeptiert."));
	    
	    linkAgb = new JLinkButton(JDLocale.L("gui.dialogs.agb_tos.readAgb",
	    		String.format("%s AGB lesen", plugin.getHost())),
	    		plugin.getAGBLink());
	    linkAgb.setFocusable(false);
	    
	    checkAgbAccepted = new JCheckBox(JDLocale.L("gui.dialogs.agb_tos.agbAccepted",
	    		"Ich bin mit den Allgemeinen Geschäftsbedingungen einverstanden"));
	    checkAgbAccepted.setFocusable(false);
	    
        btnOK = new JButton(JDLocale.L("gui.btn_ok","OK"));
        btnOK.addActionListener(this);
        
        btnCancel = new JButton(JDLocale.L("gui.btn_cancel","Abbrechen"));
        btnCancel.addActionListener(this);
        btnCancel.setFocusable(false);
        
        JDUtilities.addToGridBag(this, labelInfo1, 		1, 1, 2, 1, 1, 1, new Insets(10,5,0,5), GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, labelInfo2, 		1, 2, 2, 1, 1, 1, new Insets(5,5,10,5), GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, linkAgb, 		1, 3, 2, 1, 1, 1, new Insets(5,5,10,5), GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, checkAgbAccepted,1, 4, 2, 1, 1, 1, new Insets(5,5,15,5), GridBagConstraints.NONE, GridBagConstraints.CENTER);
        JDUtilities.addToGridBag(this, btnOK,     		2, 5, 1, 1, 1, 1, new Insets(5,5,5,5), GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(this, btnCancel, 		1, 5, 1, 1, 1, 1, new Insets(5,5,5,5), GridBagConstraints.NONE, GridBagConstraints.EAST);
        
        pack();
        setLocation(JDUtilities.getCenterOfComponent(null, this));
        setVisible(true);

    }
    
    /**
     * wird bei Actionen ausgeführt
     */
    
    public void actionPerformed(ActionEvent e) {
    	
        if (e.getSource() == btnOK) {
        	
        	plugin.setAGBChecked(checkAgbAccepted.isSelected());
        	if ( checkAgbAccepted.isSelected() ) downloadLink.setStatus(DownloadLink.STATUS_TODO);
        	dispose();
        	
        } else if (e.getSource() == btnCancel) {
        	
        	dispose();
        	
        }
        
    }
    
}
