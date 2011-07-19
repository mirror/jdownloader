package org.jdownloader.extensions.omnibox.awesome;

public class AwesomeAction extends Thread
{	
	private final Awesome source;
	private final AwesomeProposal proposal;

	public AwesomeAction (Awesome source, AwesomeProposal proposal)
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
	 * @return the original {@link AwesomeProposal AwesomeProposal} the user wants to execute
	 */
	public AwesomeProposal getProposal() {
		return proposal;
	}
	/**
	 * @return the {@link Awesome#Awesome() Awesome} object which is the source of this action.
	 */
	public Awesome getSource() {
		return source;
	}
}