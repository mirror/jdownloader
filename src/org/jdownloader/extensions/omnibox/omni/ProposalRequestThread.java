package org.jdownloader.extensions.omnibox.omni;

public class ProposalRequestThread extends Thread
{
	private final ProposalRequest request;
	private final ProposalRequestListener listener;
	ProposalRequestThread(ThreadGroup threadgroup, ProposalRequestListener listener,ProposalRequest request)
	{
		super(threadgroup,listener.getClass().toString());
		this.request = request;
		this.listener = listener;
		this.start();
	}

	@Override
	public void run() {
			listener.requestProposal(request);
	}
	
	public ProposalRequestListener getListener()
	{
		return this.listener;
	}

}