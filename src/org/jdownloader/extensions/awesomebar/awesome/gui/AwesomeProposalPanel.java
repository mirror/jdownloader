package org.jdownloader.extensions.awesomebar.awesome.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JPanel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import org.jdownloader.extensions.awesomebar.AwesomebarExtension;
import org.jdownloader.extensions.awesomebar.awesome.gui.jlist.AwesomeProposalJList;
import org.jdownloader.extensions.awesomebar.awesome.gui.jlist.AwesomeProposalListModel;
import org.jdownloader.extensions.awesomebar.awesome.gui.jlist.AwesomeProposalListSelectionModel;

import jd.gui.swing.jdgui.JDGui;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

public class AwesomeProposalPanel extends JPanel{
    private static final long serialVersionUID = 7051440058679012728L;
    
    private final AwesomebarExtension awesomebar;
    private final AwesomeProposalJList proposalList;
    private final AwesomeProposalDetailPanel detailPanel;
    private final String detailPanelHeight = "100";
    private final String width = "400";
    private final Dimension panelDimension = new Dimension(Integer.valueOf(width),300);

    public AwesomebarExtension getAwesomebar() {
        return awesomebar;
    }
    
    public AwesomeProposalJList getProposalList() {
        return proposalList;
    }

    public AwesomeProposalDetailPanel getDetailPanel() {
        return detailPanel;
    }
    
    public AwesomeProposalPanel(AwesomebarExtension awesomebar){        
    super(new MigLayout(new LC().insets("0").fill()));
    
    this.awesomebar = awesomebar;
    

    ListSelectionModel selectionModel = new AwesomeProposalListSelectionModel();
    AwesomeProposalListModel listModel = new AwesomeProposalListModel(this.awesomebar.getAwesome(), selectionModel);
    proposalList = new AwesomeProposalJList(listModel, selectionModel);
    detailPanel = new AwesomeProposalDetailPanel(proposalList);
        
    /* Debug
    this.setOpaque(true);
    this.setBackground(Color.blue);
    proposalList.setBackground(Color.red);
    proposalList.setOpaque(true);
    detailPanel.setBackground(Color.green);
    detailPanel.setOpaque(true);*/
    
    this.setOpaque(false);
    proposalList.setBackground(new Color(200,200,200));
    
    System.out.println(new CC().wrap().width(width).growY().dockNorth().minHeight("20").toString());
    this.setSize(panelDimension);
    this.add(proposalList, new CC().wrap().width(width).growY().dockNorth().minHeight("20"));
    this.add(detailPanel, new CC().width(width).minHeight(detailPanelHeight).dockNorth().growPrioY(200));
    
    Point position = SwingUtilities.convertPoint(this.getAwesomebar().getToolbarPanel(), this.getAwesomebar().getToolbarPanel().getAwesomeTextField().getLocation(), JDGui.getInstance().getMainFrame().getLayeredPane());
    position.y = position.y + this.getAwesomebar().getToolbarPanel().getAwesomeTextField().getSize().height;
    
    this.setLocation(position);

    JDGui.getInstance().getMainFrame().getLayeredPane().add(this, 0);
    
    this.setVisible(false);
    this.validateTree();
    
    JDGui.getInstance().getMainFrame().getLayeredPane().validate();
    JDGui.getInstance().getMainFrame().validate();
    }
}










