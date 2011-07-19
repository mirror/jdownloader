package org.jdownloader.extensions.omnibox.omni;

import java.awt.Component;


public class Proposal implements Comparable<Proposal> {
    private final float relevance;
    private final ProposalRequestListener source;
    private final ProposalRequest request;
    private final Component proposal;
    private final Component proposalListElement;
    private final Object actionid;

    /**
     * @return the original proposal request
     */
    public final ProposalRequest getRequest() {
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
    public final ProposalRequestListener getSource() {
        return source;
    }

    /**
     * @return the actionid
     */
    public Object getActionID() {
        return actionid;
    }

    public int compareTo(Proposal o) {
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

    public Proposal(ProposalRequestListener source, ProposalRequest request, Component proposal, Object actionid, float match) {
        this.source = source;
        this.request = request;
        this.proposal = proposal;
        this.actionid = actionid;
        this.proposalListElement = Utils.createProposalListElement(this.source, this.request);
        this.relevance = match;
        this.request.getSource().addProposal(this);
    }

    public Proposal(ProposalRequestListener source, ProposalRequest request, Component proposal, Object actionid, Component proposalListElement, float match) {
        this.source = source;
        this.request = request;
        this.proposal = proposal;
        this.actionid = actionid;
        this.proposalListElement = proposalListElement;
        this.relevance = match;
        this.request.getSource().addProposal(this);
    }
}