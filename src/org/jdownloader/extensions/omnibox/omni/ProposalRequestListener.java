package org.jdownloader.extensions.omnibox.omni;

import java.util.EventListener;
import java.util.List;



public interface ProposalRequestListener extends EventListener
{
	void performAction(Action action);
	void requestProposal(ProposalRequest event);
	List<String> getKeywords();
	float getRankingMultiplier();
}