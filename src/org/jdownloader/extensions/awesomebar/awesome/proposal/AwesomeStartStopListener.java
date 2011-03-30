package org.jdownloader.extensions.awesomebar.awesome.proposal;

import java.util.Arrays;
import java.util.List;

import javax.swing.JLabel;

import jd.controlling.DownloadWatchDog;
import jd.gui.swing.SwingGui;

import org.jdownloader.extensions.awesomebar.awesome.AwesomeAction;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposal;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposalRequest;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeProposalRequestListener;
import org.jdownloader.extensions.awesomebar.awesome.AwesomeUtils;

public class AwesomeStartStopListener implements AwesomeProposalRequestListener {
    private enum actionid {
        STARTDL,
        STOPDL,
        STOPJD
    }

    public void performAction(AwesomeAction action) {
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

    public void requestProposal(AwesomeProposalRequest request) {
        new AwesomeProposal(this, request, new JLabel("Stop JDownloader."), actionid.STOPJD, AwesomeUtils.createProposalListElement(request, "stop"), 0.1f);
        if (DownloadWatchDog.getInstance().getStateMonitor().isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.STOPPED_STATE)) {
            new AwesomeProposal(this, request, new JLabel("Start your downloads."), actionid.STARTDL, (request.getCommand().startsWith("sta")) ? 2.0f : 1.0f);
        } else {
            new AwesomeProposal(this, request, new JLabel("Stop your downloads."), actionid.STOPDL, (request.getCommand().startsWith("sto")) ? 2.0f : 1.0f);
        }
    }

    public List<String> getKeywords() {
        return Arrays.asList("start", "stop");
    }

    public float getRankingMultiplier() {
        return 1.1f;
    }

}