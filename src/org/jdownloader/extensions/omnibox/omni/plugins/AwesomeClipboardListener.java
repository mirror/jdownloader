package org.jdownloader.extensions.omnibox.omni.plugins;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;

import org.jdownloader.extensions.omnibox.omni.Action;
import org.jdownloader.extensions.omnibox.omni.Proposal;
import org.jdownloader.extensions.omnibox.omni.ProposalRequest;
import org.jdownloader.extensions.omnibox.omni.ProposalRequestListener;
import org.jdownloader.extensions.omnibox.omni.Utils;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class AwesomeClipboardListener implements ProposalRequestListener {
    private enum actionid {
        TURNOFF,
        TURNON,
        ADDLINKS
    }

    public void performAction(Action action) {
        switch ((actionid) action.getProposal().getActionID()) {
        case TURNOFF:
            GraphicalUserInterfaceSettings.CLIPBOARD_MONITORED.setValue(false);
            break;
        case TURNON:
            GraphicalUserInterfaceSettings.CLIPBOARD_MONITORED.setValue(true);
            break;
        case ADDLINKS:
            LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(ClipboardMonitoring.getINSTANCE().getCurrentContent()));
            /*
             * if (ClipboardHandler.getClipboard().isEnabled()) {
             * ClipboardHandler.getClipboard().copyTextToClipboard(data) } else
             * { ClipboardHandler.getClipboard().toggleActivation();
             * ClipboardHandler.getClipboard().toggleActivation(); }
             */
            break;
        }
    }

    public void requestProposal(ProposalRequest request) {
        if (request.isParamsEmpty() || (!request.getParams().startsWith("o"))) {
            new Proposal(this, request, new JLabel("Add all links from the clipboard."), actionid.ADDLINKS, Utils.createProposalListElement(this, request.withParams("now")), 0.6f);

        }
        if (request.isParamsEmpty() || request.getParams().startsWith("o")) {
            if (GraphicalUserInterfaceSettings.CLIPBOARD_MONITORED.getValue()) {
                new Proposal(this, request, new JLabel("Turn automatic clipboard detection off"), actionid.TURNOFF, Utils.createProposalListElement(this, request.withParams("off")), 1.0f);
            } else {
                new Proposal(this, request, new JLabel("Turn automatic clipboard detection on"), actionid.TURNON, Utils.createProposalListElement(this, request.withParams("on")), 1.0f);
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