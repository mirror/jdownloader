package org.jdownloader.extensions.omnibox.omni.gui.jlist;

import javax.swing.JList;
import javax.swing.ListSelectionModel;

import org.jdownloader.extensions.omnibox.omni.Proposal;



/*
 * Just typecasting and a shortcut ( getSelectedProposal() ) - that's it.
 */

public class AwesomeProposalJList extends JList {
	/**
	 * 
	 */
	private static final long serialVersionUID = -9216993116208607535L;

	/**
	 * This does not work since typecasting getModel() will fail.
	 * Create ListModel before and pass it as argument.
	 */
	@SuppressWarnings("unused")
	private AwesomeProposalJList(){}
	
	public AwesomeProposalJList(AwesomeProposalListModel model){
		super(model);
	}

	public AwesomeProposalJList(AwesomeProposalListModel listModel, ListSelectionModel selectionModel) {
		super(listModel);
		this.setSelectionModel(selectionModel);
		this.setCellRenderer(new AwesomeProposalListCellRenderer());
	}

	public void setModel(AwesomeProposalListModel model){
		super.setModel(model);
	}
	
	public AwesomeProposalListModel getModel(){
		return (AwesomeProposalListModel) super.getModel();
	}
	
	public Proposal getSelectedProposal(){
		return this.getModel().getSelectedProposal();
	}
}