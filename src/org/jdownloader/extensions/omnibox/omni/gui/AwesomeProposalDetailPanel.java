package org.jdownloader.extensions.omnibox.omni.gui;

import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdownloader.extensions.omnibox.omni.gui.jlist.AwesomeProposalJList;

import net.miginfocom.swing.MigLayout;


public class AwesomeProposalDetailPanel extends JPanel implements ListSelectionListener, ListDataListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3556373081543237304L;

	private final AwesomeProposalJList proposalList;

	public AwesomeProposalDetailPanel(AwesomeProposalJList proposalList) {
		super(new MigLayout("insets 20"));
		this.proposalList = proposalList;
		this.setBackground(new Color(200,200,200));
		//this.setBorder(BorderFactory.createLineBorder(new Color(200,200,200), 2));//BorderFactory.createEtchedBorder(new Color(200,200,200),new Color(200,200,200).brighter()));// .createLineBorder(Color.black));
		this.proposalList.getSelectionModel().addListSelectionListener(this);
		this.proposalList.getModel().addListDataListener(this);
	}


	public void valueChanged(ListSelectionEvent e) {
		updateDetailPanel();
	}
	

	public void contentsChanged(ListDataEvent arg0) {
		updateDetailPanel();
	}

	
	public void intervalAdded(ListDataEvent arg0) {
		updateDetailPanel();
	}


	public void intervalRemoved(ListDataEvent arg0) {
		updateDetailPanel();
	}
	
	public void updateDetailPanel() {
		this.removeAll();
		try {
			this.proposalList.getModel().getAwesome().returnedProposalsLock.readLock().lock();
			if (this.proposalList.getModel().getAwesome().getProposals().size() > 0) {
				this.add(proposalList.getSelectedProposal().getProposal());
			}
		} finally {
			this.proposalList.getModel().getAwesome().returnedProposalsLock.readLock().unlock();
		}

		this.validate();
		this.repaint();
	}
}