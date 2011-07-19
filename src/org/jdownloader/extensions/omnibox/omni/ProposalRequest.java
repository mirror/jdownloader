package org.jdownloader.extensions.omnibox.omni;

import java.util.EventObject;


public class ProposalRequest extends EventObject {
    /**
	 * 
	 */
    private static final long serialVersionUID = 4142238983507559780L;

    private final String command;
    private final String params;

    public ProposalRequest(Omni source, String command, String params) {
        super(source);
        this.command = command;
        this.params = params;
    }

    /**
     * @return the event command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return the event prams
     */
    public String getParams() {
        return params;
    }

    public boolean isParamsEmpty() {
        return params.length() == 0;
    }

    public Omni getSource() {
        return (Omni) super.getSource();
    }

    /**
     * @return a cloned AwesomeProposalRequest object, but with changed params.
     */
    public ProposalRequest withParams(String params2) {
        return new ProposalRequest(this.getSource(), this.getCommand(), params2);
    }

}