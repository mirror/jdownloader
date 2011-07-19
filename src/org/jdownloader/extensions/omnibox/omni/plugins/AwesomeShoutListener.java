package org.jdownloader.extensions.omnibox.omni.plugins;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import org.jdownloader.extensions.omnibox.omni.Action;
import org.jdownloader.extensions.omnibox.omni.Proposal;
import org.jdownloader.extensions.omnibox.omni.ProposalRequest;
import org.jdownloader.extensions.omnibox.omni.ProposalRequestListener;



public class AwesomeShoutListener implements ProposalRequestListener
{

	
	public void performAction(Action action) {
		System.out.println("SHOUT: " + action.getProposal().getRequest().getParams().toUpperCase());
	}
	
	public void requestProposal(ProposalRequest request)
	{
		if(request.getParams().equals("")){
			new Proposal(this,request, new JLabel(
					"I'd like to shout something."),null,0.5f);			
		}
		else{
			new Proposal(this,request,new JLabel(
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