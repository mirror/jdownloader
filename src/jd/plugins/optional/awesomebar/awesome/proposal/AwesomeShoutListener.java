package jd.plugins.optional.awesomebar.awesome.proposal;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import jd.plugins.optional.awesomebar.awesome.AwesomeAction;
import jd.plugins.optional.awesomebar.awesome.AwesomeProposal;
import jd.plugins.optional.awesomebar.awesome.AwesomeProposalRequest;
import jd.plugins.optional.awesomebar.awesome.AwesomeProposalRequestListener;


public class AwesomeShoutListener implements AwesomeProposalRequestListener
{

	
	public void performAction(AwesomeAction action) {
		System.out.println("SHOUT: " + action.getProposal().getRequest().getParams().toUpperCase());
	}
	
	public void requestProposal(AwesomeProposalRequest request)
	{
		if(request.getParams().equals("")){
			new AwesomeProposal(this,request, new JLabel(
					"I'd like to shout something."),0.5f);			
		}
		else{
			new AwesomeProposal(this,request,new JLabel(
				"I'd like to shout " + request.getParams()), 1.0f);
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