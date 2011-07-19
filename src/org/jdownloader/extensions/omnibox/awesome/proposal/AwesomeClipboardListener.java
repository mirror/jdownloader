package org.jdownloader.extensions.omnibox.awesome.proposal;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import org.jdownloader.extensions.omnibox.awesome.AwesomeAction;
import org.jdownloader.extensions.omnibox.awesome.AwesomeProposal;
import org.jdownloader.extensions.omnibox.awesome.AwesomeProposalRequest;
import org.jdownloader.extensions.omnibox.awesome.AwesomeProposalRequestListener;
import org.jdownloader.extensions.omnibox.awesome.AwesomeUtils;

import jd.config.Configuration;
import jd.controlling.ClipboardHandler;
import jd.controlling.DistributeData;
import jd.utils.JDUtilities;

public class AwesomeClipboardListener implements AwesomeProposalRequestListener {
    private enum actionid {
        TURNOFF, TURNON, ADDLINKS
    }

    public void performAction(AwesomeAction action) {
        switch ((actionid) action.getProposal().getActionID()) {
        case TURNOFF:
            final Configuration configuration = JDUtilities.getConfiguration();
            if (configuration.getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true) == true) {
                configuration.setProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, false);
                configuration.save();
            }
            break;
        case TURNON:
            final Configuration configuration2 = JDUtilities.getConfiguration();
            if (configuration2.getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true) == false) {
                configuration2.setProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true);
                configuration2.save();
            }
            break;
        case ADDLINKS:
            new DistributeData(ClipboardHandler.getClipboard().getCurrentClipboardLinks()).start();
            /*
             * if (ClipboardHandler.getClipboard().isEnabled()) {
             * ClipboardHandler.getClipboard().copyTextToClipboard(data) } else
             * { ClipboardHandler.getClipboard().toggleActivation();
             * ClipboardHandler.getClipboard().toggleActivation(); }
             */
            break;
        }
    }

    public void requestProposal(AwesomeProposalRequest request) {
        if (request.isParamsEmpty() || (!request.getParams().startsWith("o"))) {
            new AwesomeProposal(this, request, new JLabel("Add all links from the clipboard."), actionid.ADDLINKS, AwesomeUtils.createProposalListElement(this, request.withParams("now")), 0.6f);

        }
        if (request.isParamsEmpty() || request.getParams().startsWith("o")) {
            if (JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true)) {
                new AwesomeProposal(this, request, new JLabel("Turn automatic clipboard detection off"), actionid.TURNOFF, AwesomeUtils.createProposalListElement(this, request.withParams("off")), 1.0f);
            } else {
                new AwesomeProposal(this, request, new JLabel("Turn automatic clipboard detection on"), actionid.TURNON, AwesomeUtils.createProposalListElement(this, request.withParams("on")), 1.0f);
            }
        }
    }

    public List<String> getKeywords() {
        return Arrays.asList("clipboard");
    }

    public float getRankingMultiplier() {
        return 0.95f;
    }

}