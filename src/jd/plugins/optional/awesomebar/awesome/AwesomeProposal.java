package jd.plugins.optional.awesomebar.awesome;

import java.awt.Component;
import java.awt.Container;


public class AwesomeProposal implements Comparable<AwesomeProposal> {
	private final float relevance;
	private final AwesomeProposalRequestListener source;
	private final AwesomeProposalRequest request;
	private final Component proposal;
	private final Component proposalListElement;

	/**
	 * @return the original proposal request
	 */
	public final AwesomeProposalRequest getRequest() {
		return request;
	}
	/**
	 * @return the match relevance. 1 is 100%
	 */
	public final float getRelevance() {
		return relevance;
	}
	/**
	 * @return the proposal message
	 */
	public final Component getProposal() {
		return proposal;
	}

	/**
	 * @return the proposal message
	 */
	public final Component getProposalListElement() {
		return proposalListElement;
	}

	/**
	 * @return return the source of the proposal
	 */
	public final AwesomeProposalRequestListener getSource() {
		return source;
	}


	public int compareTo(AwesomeProposal o) {
		int ratingThis = 0;
		int ratingCompare = 0;
		final String command;
		try {
			this.request.getSource().commandLock.readLock().lock();
			command = this.request.getSource().getCommand();
		} finally {
			this.request.getSource().commandLock.readLock().unlock();
		}

		for (String keyword : this.source.getKeywords()) {
			while ((keyword.length() > ratingThis) && command.startsWith(keyword.substring(0, ratingThis + 1))) {
				ratingThis++;
			}
		}

		for (String keyword : o.source.getKeywords()) {
			while ((keyword.length() > ratingCompare) && (command.startsWith(keyword.substring(0, ratingCompare + 1)))) {
				ratingCompare++;
			}
		}

		if (ratingThis == ratingCompare) {
			return Float.compare(o.getRelevance(), this.getRelevance());
		} else {
			return Integer.valueOf(ratingCompare).compareTo(ratingThis);
		}
	}

	public AwesomeProposal(AwesomeProposalRequestListener source, AwesomeProposalRequest request, Component proposal, float match) {
		this.source = source;
		this.request = request;
		this.proposal = proposal;
		this.proposalListElement = AwesomeUtils.createProposalListElement(this.source, this.request);
		this.relevance = match;
		this.request.getSource().addProposal(this);
	}

	public AwesomeProposal(AwesomeProposalRequestListener source, AwesomeProposalRequest request, Component proposal, Container proposalListElement, float match) {
		this.source = source;
		this.request = request;
		this.proposal = proposal;
		this.proposalListElement = proposalListElement;
		this.relevance = match;
		this.request.getSource().addProposal(this);
	}
}