package jd.plugins.optional.awesomebar.awesome.proposal;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import jd.config.Configuration;
import jd.controlling.reconnect.Reconnecter;
import jd.plugins.optional.awesomebar.awesome.AwesomeAction;
import jd.plugins.optional.awesomebar.awesome.AwesomeProposal;
import jd.plugins.optional.awesomebar.awesome.AwesomeProposalRequest;
import jd.plugins.optional.awesomebar.awesome.AwesomeProposalRequestListener;
import jd.plugins.optional.awesomebar.awesome.AwesomeUtils;
import jd.utils.JDUtilities;

public class AwesomeReconnectListener implements AwesomeProposalRequestListener {
    private enum actionid {
        TURNOFF, TURNON, RCNOW
    }

    public void performAction(AwesomeAction action) {
        switch ((actionid) action.getProposal().getActionID()) {
        case TURNOFF:
            if (Reconnecter.isReconnectAllowed()) {
                Reconnecter.toggleReconnect();
            }
            break;
        case TURNON:
            if (!Reconnecter.isReconnectAllowed()) {
                Reconnecter.toggleReconnect();
            }
            break;
        case RCNOW:
            Reconnecter.doManualReconnect();
            break;
        }
    }

    public void requestProposal(AwesomeProposalRequest request) {
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

    public List<String> getKeywords() {
        return Arrays.asList("reconnect");
    }

    public float getRankingMultiplier() {
        return 0.95f;
    }

}