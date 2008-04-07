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
//    along with this program.  If not, see <http://wnu.org/licenses/>.


package jd.gui.skins.simple;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import jd.plugins.FilePackage;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Dies Klasse ziegt informationen zu einem DownloadLink an
 */
public class PackageInfo extends JDialog {
    /**
     * 
     */
    private static final long serialVersionUID = -9146764850581039090L;
    @SuppressWarnings("unused")
    private static Logger     logger           = JDUtilities.getLogger();
    private jd.plugins.FilePackage      fp;
    private int               i                = 0;
    private JPanel            panel;
    /**
     * @param frame
     * @param dlink
     */
    public PackageInfo(JFrame frame, FilePackage fp) {
        super(frame);
        this.fp = fp;
        setModal(true);
        setLayout(new BorderLayout());
        this.setBackground(Color.WHITE);
        this.setTitle(JDLocale.L("gui.frame.packageinfo.title","Package Information"));
        panel = new JPanel(new GridBagLayout());
        this.add(panel, BorderLayout.CENTER);
        initDialog();
        pack();
        setLocation((int) (frame.getLocation().getX() + frame.getWidth() / 2 - this.getWidth() / 2), (int) (frame.getLocation().getY() + frame.getHeight() / 2 - this.getHeight() / 2));
        setVisible(true);
    }
    private void initDialog() {
         addEntry("name",fp.getName());
       addEntry(null,null);
       addEntry("comment",fp.getComment());
       addEntry("dldirectory",fp.getDownloadDirectory());
       addEntry("packagesize",JDUtilities.formatBytesToMB(fp.getEstimatedPackageSize()));
       addEntry("loaded",JDUtilities.formatBytesToMB(fp.getTotalLoadedPackageBytes()));
       addEntry(null,null);
       addEntry("properties",fp.getProperties()+"");
    }
    private void addEntry(String label, String data) {
        if (label == null && data == null) {
            JDUtilities.addToGridBag(panel, new JSeparator(), 0, i, 2, 1, 0, 0, null, GridBagConstraints.BOTH, GridBagConstraints.CENTER);
            return;
        }
        JDUtilities.addToGridBag(panel, new JLabel(JDLocale.L("gui.frame.packageinfo.entry"+label)), 0, i, 1, 1, 0, 1, null, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JLabel(data), 1, i, 1, 1, 1, 0, null, GridBagConstraints.NONE, GridBagConstraints.EAST);
        i++;
    }
}
