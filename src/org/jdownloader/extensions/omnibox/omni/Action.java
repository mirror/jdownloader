package org.jdownloader.extensions.omnibox.omni;


public class Action extends Thread
{	
	private final Omni source;
	private final Proposal proposal;

	public Action (Omni source, Proposal proposal)
	{
		this.source = source;
		this.proposal = proposal;
		this.start();
	}
	
	@Override
	public void run()
	{
		this.proposal.getSource().performAction(this);
	}
	
	/**
	 * @return the original {@link Proposal AwesomeProposal} the user wants to execute
	 */
	public Proposal getProposal() {
		return proposal;
	}
	/**
	 * @return the {@link Omni#Awesome() Awesome} object which is the source of this action.
	 */
	public Omni getSource() {
		return source;
	}
}