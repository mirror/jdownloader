package jd.plugins.optional.awesomebar.awesome.jlist;
import java.awt.Component;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.AbstractListModel;
import javax.swing.ListSelectionModel;

import jd.plugins.optional.awesomebar.awesome.Awesome;
import jd.plugins.optional.awesomebar.awesome.AwesomeProposal;


public class AwesomeProposalListModel extends AbstractListModel implements Observer {
	private static final long serialVersionUID = 245192547176430473L;

	private final Awesome awesome;
	private final ListSelectionModel selectionmodel;
	private int count;

	public ReentrantReadWriteLock countLock = new ReentrantReadWriteLock();

	/**
	 * @return the awesome object
	 */
	public Awesome getAwesome() {
		return awesome;
	}

	public AwesomeProposalListModel(Awesome awesome, ListSelectionModel selectionmodel) {
		this.awesome = awesome;
		this.awesome.addObserver(this);
		this.selectionmodel = selectionmodel;
		try {
			this.awesome.returnedProposalsLock.readLock().lock();
			count = this.awesome.getProposals().size();
		} finally {
			this.awesome.returnedProposalsLock.readLock().unlock();
		}
	}

	@Override
	public Component getElementAt(int index) {
		try {
			this.awesome.returnedProposalsLock.readLock().lock();
			return this.awesome.getProposals().get(index).getProposalListElement();
		} finally {
			this.awesome.returnedProposalsLock.readLock().unlock();
		}
	}

	@Override
	public int getSize() {
		try {
			countLock.readLock().lock();
			return count;
		} finally {
			countLock.readLock().unlock();
		}
	}

	@Override
	public void update(Observable arg0, Object e) {
		Awesome.EVENTTYPE event = (Awesome.EVENTTYPE) e;

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

	public AwesomeProposal getSelectedProposal() {
		try {
			this.awesome.returnedProposalsLock.readLock().lock();
			return awesome.getProposals().get(selectionmodel.getLeadSelectionIndex());
		} finally {
			this.awesome.returnedProposalsLock.readLock().unlock();
		}
	}
	
	public AwesomeProposal getSelectedProposal(int selectedIndex) {
		try {
			this.awesome.returnedProposalsLock.readLock().lock();
			return awesome.getProposals().get(selectedIndex);
		} finally {
			this.awesome.returnedProposalsLock.readLock().unlock();
		}
	}
}
