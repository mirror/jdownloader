package org.jdownloader.extensions.awesomebar.awesome.proposal;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import org.jdownloader.extensions.awesomebar.awesome.AwesomeAction;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposal;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposalRequest;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposalRequestListener;



public class AwesomeAlertListener implements AwesomeProposalRequestListener {

	
	public void performAction(AwesomeAction action) {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} //Simulate time expense
		
		System.out.println( ((String)action.getProposal().getActionID()) +": " + action.getProposal().getRequest().getParams());
	}


	public void requestProposal(AwesomeProposalRequest request) {
		
		try {
			Thread.sleep(200); //Simulate time expense
		} catch (InterruptedException e) {
			System.out.println("Thread " + this.getClass() + " interrupted.");
			Thread.currentThread().interrupt();
		}
		
		String command = (request.getCommand().startsWith("a")) ? "alert" : "print";
		if (request.getParams().equals("")) {
			new AwesomeProposal(
					this, request,new JLabel("I'd like to "+command+" something after 2 seconds."),command, 0.55f);
		} else {
			new AwesomeProposal(
					this, request, new JLabel("I'd like to "+command+" "
							+ request.getParams()+" after 2 seconds."),command, 1.1f);
		}
		System.out.println("Alert-Proposal submitted.");

	}


	public List<String> getKeywords() {
		return Arrays.asList("alert", "print","noimage");
	}


	public float getRankingMultiplier() {
		return 0.95f;
	}

}