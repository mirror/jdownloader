package org.jdownloader.extensions.omnibox.omni.plugins;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.SwingGui;

import org.jdownloader.extensions.omnibox.omni.Action;
import org.jdownloader.extensions.omnibox.omni.Proposal;
import org.jdownloader.extensions.omnibox.omni.ProposalRequest;
import org.jdownloader.extensions.omnibox.omni.ProposalRequestListener;
import org.jdownloader.extensions.omnibox.omni.Utils;

public class AwesomeStartStopListener implements ProposalRequestListener {
    private enum actionid {
        STARTDL,
        STOPDL,
        STOPJD
    }

    public void performAction(Action action) {
        switch ((actionid) action.getProposal().getActionID()) {
        case STARTDL:
            DownloadWatchDog.getInstance().startDownloads();
            break;
        case STOPDL:
            DownloadWatchDog.getInstance().stopDownloads();
            break;
        case STOPJD:
            DownloadWatchDog.getInstance().stopDownloads();
            SwingGui.getInstance().closeWindow();
            break;
        }
    }

    public void requestProposal(ProposalRequest request) {
        new Proposal(this, request, new JLabel("Stop JDownloader."), actionid.STOPJD, Utils.createProposalListElement(request, "stop"), 0.1f);
        if (DownloadWatchDog.getInstance().getStateMonitor().isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.STOPPED_STATE)) {
            new Proposal(this, request, new JLabel("Start your downloads."), actionid.STARTDL, (request.getCommand().startsWith("sta")) ? 2.0f : 1.0f);
        } else {
            new Proposal(this, request, new JLabel("Stop your downloads."), actionid.STOPDL, (request.getCommand().startsWith("sto")) ? 2.0f : 1.0f);
        }
    }

    public List<String> getKeywords() {
        return Arrays.asList("start", "stop");
    }

    public float getRankingMultiplier() {
        return 1.1f;
    }

}