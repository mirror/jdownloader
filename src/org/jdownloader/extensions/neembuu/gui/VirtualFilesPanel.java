/*
 * Copyright (C) 2012 Shashank Tulsyan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jdownloader.extensions.neembuu.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;

import org.appwork.utils.logging.Log;
import org.jdownloader.extensions.neembuu.DownloadSession;
import org.jdownloader.extensions.neembuu.translate._NT;

/**
 * 
 * @author Shashank Tulsyan
 */
public class VirtualFilesPanel extends JPanel {

    private JPanel centre;
    private int    cnt;

    public VirtualFilesPanel(LayoutManager layout) {
        super(layout);
    }

    public static VirtualFilesPanel getOrCreate(DownloadSession jdds, final String mountLocation, JPanel httpFilePanel) {
        VirtualFilesPanel virtualFilesPanel = null;
        try {
            virtualFilesPanel = jdds.getWatchAsYouDownloadSession().getVirtualFileSystem().getVirtualFilesPanel();
            //virtualFilesPanel = NeembuuExtension.getInstance().getVirtualFileSystems().get(jdds.getDownloadLink().getFilePackage());//.getProperty("virtualFilesPanel");
        } catch (Exception e) {
            Log.L.log(Level.SEVERE, "Could not get virtual files panel",e);
        }
        if (virtualFilesPanel == null) {
            virtualFilesPanel = new VirtualFilesPanel(new BorderLayout());
            JPanel north = new JPanel(new FlowLayout());
            virtualFilesPanel.centre = new JPanel(new MigLayout());
            virtualFilesPanel.add(north, BorderLayout.NORTH);
            virtualFilesPanel.add(new JScrollPane(virtualFilesPanel.centre), BorderLayout.CENTER);

            JLabel mntLoc = new JLabel(_NT._.mountLocation());
            north.add(mntLoc, "span 2");
            JLabel mntLocVal = new JLabel(mountLocation);
            north.add(mntLocVal, "span 4");
            JButton openF = new JButton(_NT._.openMountLocation());
            north.add(openF);
            //jdds.getDownloadLink().getFilePackage().setProperty("virtualFilesPanel", virtualFilesPanel);
            //NeembuuExtension.getInstance().getVirtualFilesPanels().put(jdds.getDownloadLink().getFilePackage(),virtualFilesPanel);
            try{
                jdds.getWatchAsYouDownloadSession().getVirtualFileSystem().setVirtualFilesPanel(virtualFilesPanel);
            }catch(IllegalStateException a){
                virtualFilesPanel = jdds.getWatchAsYouDownloadSession().getVirtualFileSystem().getVirtualFilesPanel();
            }
            openF.addActionListener(new ActionListener() {
                // @Override

                public void actionPerformed(ActionEvent e) {
                    try {
                        java.awt.Desktop.getDesktop().open(new File(mountLocation));
                    } catch (Exception a) {
                        JOptionPane.showMessageDialog(null, _NT._.couldNotOpenMountLocation());
                    }
                }
            });
        }
        if (virtualFilesPanel.cnt % 2 == 0) {
            virtualFilesPanel.centre.add(httpFilePanel);
        } else {
            virtualFilesPanel.centre.add(httpFilePanel, "wrap");
        }
        virtualFilesPanel.cnt++;
        return virtualFilesPanel;
    }
}
