package org.jdownloader.extensions.omnibox.awesome;

public class AwesomeProposalRequestThread extends Thread
{
	private final AwesomeProposalRequest request;
	private final AwesomeProposalRequestListener listener;
	AwesomeProposalRequestThread(ThreadGroup threadgroup, AwesomeProposalRequestListener listener,AwesomeProposalRequest request)
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
	
	public AwesomeProposalRequestListener getListener()
	{
		return this.listener;
	}

}