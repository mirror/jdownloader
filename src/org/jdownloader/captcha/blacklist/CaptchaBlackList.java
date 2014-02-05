package org.jdownloader.captcha.blacklist;

import java.util.ArrayList;
import java.util.HashSet;

import jd.controlling.downloadcontroller.DownloadLinkCandidate;
import jd.controlling.downloadcontroller.DownloadLinkCandidateResult;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.downloadcontroller.DownloadWatchDogProperty;
import jd.controlling.downloadcontroller.PrePluginCheckDummyChallenge;
import jd.controlling.downloadcontroller.SingleDownloadController;
import jd.controlling.downloadcontroller.event.DownloadWatchdogListener;
import jd.plugins.DownloadLink;

import org.jdownloader.captcha.v2.Challenge;

public class CaptchaBlackList implements DownloadWatchdogListener {
    private static final CaptchaBlackList INSTANCE = new CaptchaBlackList();
    private ArrayList<BlacklistEntry>     entries;

    private CaptchaBlackList() {
        entries = new ArrayList<BlacklistEntry>();
        DownloadWatchDog.getInstance().getEventSender().addListener(this);
    }

    public static CaptchaBlackList getInstance() {
        return INSTANCE;
    }

    public void add(BlacklistEntry entry) {
        synchronized (entries) {
            entries.add(entry);
        }
        synchronized (whitelist) {
            ArrayList<DownloadLink> rem = new ArrayList<DownloadLink>();
            for (DownloadLink link : whitelist) {
                if (entry.matches(new PrePluginCheckDummyChallenge(link))) {
                    rem.add(link);
                }
            }
            whitelist.removeAll(rem);
        }
    }

    public boolean matches(Challenge<?> c) {
        return matches(c, false);
    }

    private boolean matches(Challenge<?> c, boolean bypasswhitelist) {
        if (!bypasswhitelist) {
            DownloadLink link = Challenge.getDownloadLink(c);
            if (link != null) {
                synchronized (whitelist) {
                    if (whitelist.contains(link)) { return false; }

                }
            }
        }
        synchronized (entries) {
            ArrayList<BlacklistEntry> cleanups = new ArrayList<BlacklistEntry>();
            try {
                for (BlacklistEntry e : entries) {
                    if (e.canCleanUp()) {
                        cleanups.add(e);
                        continue;
                    }
                    if (e.matches(c)) { return true; }
                }
            } finally {
                entries.removeAll(cleanups);
            }

        }
        return false;
    }

    @Override
    public void onDownloadWatchdogDataUpdate() {
    }

    @Override
    public void onDownloadWatchdogStateIsIdle() {
    }

    @Override
    public void onDownloadWatchdogStateIsPause() {
    }

    @Override
    public void onDownloadWatchdogStateIsRunning() {
    }

    @Override
    public void onDownloadWatchdogStateIsStopped() {
        synchronized (entries) {
            ArrayList<BlacklistEntry> cleanups = new ArrayList<BlacklistEntry>();
            try {
                for (BlacklistEntry e : entries) {
                    if (e.canCleanUp() || e instanceof SessionBlackListEntry) {
                        cleanups.add(e);
                        continue;
                    }
                }
            } finally {
                entries.removeAll(cleanups);
            }

        }
        synchronized (whitelist) {
            whitelist.clear();
        }
    }

    @Override
    public void onDownloadWatchdogStateIsStopping() {
    }

    @Override
    public void onDownloadControllerStart(SingleDownloadController downloadController, DownloadLinkCandidate candidate) {
    }

    @Override
    public void onDownloadControllerStopped(SingleDownloadController downloadController, DownloadLinkCandidate candidate, DownloadLinkCandidateResult result) {

    }

    private HashSet<DownloadLink> whitelist = new HashSet<DownloadLink>();

    public void addWhitelist(DownloadLink link) {
        if (!matches(new PrePluginCheckDummyChallenge(link), true)) return;
        synchronized (whitelist) {
            whitelist.add(link);
        }

        collectGarbage();
    }

    protected void collectGarbage() {
        synchronized (entries) {
            ArrayList<BlacklistEntry> cleanups = new ArrayList<BlacklistEntry>();
            try {
                for (BlacklistEntry e : entries) {
                    if (e.canCleanUp()) {
                        cleanups.add(e);
                        continue;
                    }
                }
            } finally {
                entries.removeAll(cleanups);
            }
        }
    }

    public boolean isWhitelisted(DownloadLink downloadLink) {
        synchronized (whitelist) {
            return whitelist.contains(downloadLink);
        }
    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
    }
}
