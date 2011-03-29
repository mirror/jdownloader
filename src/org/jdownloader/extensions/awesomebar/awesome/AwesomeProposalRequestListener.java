package org.jdownloader.extensions.awesomebar.awesome;

import java.util.EventListener;
import java.util.List;


public interface AwesomeProposalRequestListener extends EventListener
{
	void performAction(AwesomeAction action);
	void requestProposal(AwesomeProposalRequest event);
	List<String> getKeywords();
	float getRankingMultiplier();
}