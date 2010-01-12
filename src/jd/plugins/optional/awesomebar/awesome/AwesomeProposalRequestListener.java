package jd.plugins.optional.awesomebar.awesome;

import java.util.*;


public interface AwesomeProposalRequestListener extends EventListener
{
	void performAction(AwesomeAction action);
	void requestProposal(AwesomeProposalRequest event);
	List<String> getKeywords();
	float getRankingMultiplier();
}