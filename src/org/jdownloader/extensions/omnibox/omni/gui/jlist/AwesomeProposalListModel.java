package org.jdownloader.extensions.omnibox.omni.gui.jlist;
import java.awt.Component;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.AbstractListModel;
import javax.swing.ListSelectionModel;

import org.jdownloader.extensions.omnibox.omni.Omni;
import org.jdownloader.extensions.omnibox.omni.Proposal;



public class AwesomeProposalListModel extends AbstractListModel implements Observer {
	private static final long serialVersionUID = 245192547176430473L;

	private final Omni omni;
	private final ListSelectionModel selectionmodel;
	private int count;

	public ReentrantReadWriteLock countLock = new ReentrantReadWriteLock();

	/**
	 * @return the awesome object
	 */
	public Omni getAwesome() {
		return omni;
	}

	public AwesomeProposalListModel(Omni omni, ListSelectionModel selectionmodel) {
		this.omni = omni;
		this.omni.addObserver(this);
		this.selectionmodel = selectionmodel;
		try {
			this.omni.returnedProposalsLock.readLock().lock();
			count = this.omni.getProposals().size();
		} finally {
			this.omni.returnedProposalsLock.readLock().unlock();
		}
	}

	
	public Component getElementAt(int index) {
		try {
			this.omni.returnedProposalsLock.readLock().lock();
			return this.omni.getProposals().get(index).getProposalListElement();
		} finally {
			this.omni.returnedProposalsLock.readLock().unlock();
		}
	}


	public int getSize() {
		try {
			countLock.readLock().lock();
			return count;
		} finally {
			countLock.readLock().unlock();
		}
	}

	
	public void update(Observable arg0, Object e) {
		Omni.EVENTTYPE event = (Omni.EVENTTYPE) e;

		switch (event) {
			case PROPOSALSCLEARED :
				int temp;
				try {
					countLock.writeLock().lock();
					temp = count;
					count = 0;
				} finally {
					countLock.writeLock().unlock();
				}
				this.fireIntervalRemoved(this, 0, temp);
				break;

			case PROPOSALSUPDATE :
				this.fireContentsChanged(this, 0, count);
				break;

			case PROPOSALADDED :
				try {
					countLock.writeLock().lock();

					count++;
					if (count == 1) {
						countLock.writeLock().unlock();
						selectionmodel.setSelectionInterval(0, 0);
					}
				} finally {
					if(countLock.writeLock().isHeldByCurrentThread())
					{
						countLock.writeLock().unlock();
					}
				}
				this.fireIntervalAdded(this, count, count);
				break;
		}
	}

	public Proposal getSelectedProposal() {
		try {
			this.omni.returnedProposalsLock.readLock().lock();
			return omni.getProposals().get(selectionmodel.getLeadSelectionIndex());
		} finally {
			this.omni.returnedProposalsLock.readLock().unlock();
		}
	}
	
	public Proposal getSelectedProposal(int selectedIndex) {
		try {
			this.omni.returnedProposalsLock.readLock().lock();
			return omni.getProposals().get(selectedIndex);
		} finally {
			this.omni.returnedProposalsLock.readLock().unlock();
		}
	}
}
