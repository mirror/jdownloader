package org.jdownloader.extensions.awesomebar.awesome.proposal;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import org.jdownloader.extensions.awesomebar.awesome.AwesomeAction;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposal;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposalRequest;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposalRequestListener;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeUtils;

import jd.config.Configuration;
import jd.controlling.reconnect.Reconnecter;
import jd.utils.JDUtilities;

public class AwesomeReconnectListener implements AwesomeProposalRequestListener {
    private enum actionid {
        TURNOFF, TURNON, RCNOW
    }

    public List<String> getKeywords() {
        return Arrays.asList("reconnect");
    }

    public float getRankingMultiplier() {
        return 0.95f;
    }

    public void performAction(final AwesomeAction action) {
        switch ((actionid) action.getProposal().getActionID()) {
        case TURNOFF:
            Reconnecter.getInstance().setAutoReconnectEnabled(false);
            break;
        case TURNON:
            Reconnecter.getInstance().setAutoReconnectEnabled(true);
            break;
        case RCNOW:
            Reconnecter.getInstance().forceReconnect();
            break;
        }
    }

    public void requestProposal(final AwesomeProposalRequest request) {
        if (request.isParamsEmpty() || request.getParams().startsWith("n")) {
            new AwesomeProposal(this, request, new JLabel("Reconnect now."), actionid.RCNOW, AwesomeUtils.createProposalListElement(this, request.withParams("now")), 0.6f);

        }
        if (request.isParamsEmpty() || request.getParams().startsWith("o")) {
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_ALLOW_RECONNECT, true)) {
                new AwesomeProposal(this, request, new JLabel("Turn automatic reconnect off"), actionid.TURNOFF, AwesomeUtils.createProposalListElement(this, request.withParams("off")), 1.0f);
            } else {
                new AwesomeProposal(this, request, new JLabel("Turn automatic reconnect on"), actionid.TURNON, AwesomeUtils.createProposalListElement(this, request.withParams("on")), 1.0f);
            }
        }
    }

}