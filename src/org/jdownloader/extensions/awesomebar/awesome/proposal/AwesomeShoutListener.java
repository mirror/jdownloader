package org.jdownloader.extensions.awesomebar.awesome.proposal;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import org.jdownloader.extensions.awesomebar.awesome.AwesomeAction;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposal;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposalRequest;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposalRequestListener;



public class AwesomeShoutListener implements AwesomeProposalRequestListener
{

	
	public void performAction(AwesomeAction action) {
		System.out.println("SHOUT: " + action.getProposal().getRequest().getParams().toUpperCase());
	}
	
	public void requestProposal(AwesomeProposalRequest request)
	{
		if(request.getParams().equals("")){
			new AwesomeProposal(this,request, new JLabel(
					"I'd like to shout something."),null,0.5f);			
		}
		else{
			new AwesomeProposal(this,request,new JLabel(
				"I'd like to shout " + request.getParams()),null, 1.0f);
		}
		System.out.println("Shout-Proposal submitted.");
	}


	public List<String> getKeywords() {
		return Arrays.asList( "alerter", "shout" );
	}

	
	public float getRankingMultiplier() {
		return 0.95f;
	}

}