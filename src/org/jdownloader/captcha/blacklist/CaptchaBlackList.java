package org.jdownloader.captcha.blacklist;

import java.util.ArrayList;
import java.util.WeakHashMap;

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
            final ArrayList<DownloadLink> rem = new ArrayList<DownloadLink>();
            for (DownloadLink link : whitelist.keySet()) {
                if (entry.matches(new PrePluginCheckDummyChallenge(link))) {
                    rem.add(link);
                }
            }
            whitelist.keySet().removeAll(rem);
        }
    }

    public BlacklistEntry matches(Challenge<?> c) {
        return matches(c, false);
    }

    private BlacklistEntry matches(Challenge<?> c, boolean bypasswhitelist) {
        if (!bypasswhitelist) {
            DownloadLink link = Challenge.getDownloadLink(c);
            if (link != null) {
                synchronized (whitelist) {
                    if (whitelist.containsKey(link)) {
                        return null;
                    }
                }
            }
        }
        synchronized (entries) {
            ArrayList<BlacklistEntry> cleanups = new ArrayList<BlacklistEntry>();
            try {
                for (BlacklistEntry e : entries) {
                    if (e.canCleanUp()) {
                        cleanups.add(e);
                    } else if (e.matches(c)) {
                        return e;
                    }
                }
            } finally {
                entries.removeAll(cleanups);
            }
        }
        return null;
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
            final ArrayList<BlacklistEntry> cleanups = new ArrayList<BlacklistEntry>();
            try {
                for (BlacklistEntry e : entries) {
                    if (e.canCleanUp() || e instanceof SessionBlackListEntry) {
                        cleanups.add(e);
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

    private final WeakHashMap<DownloadLink, Object> whitelist = new WeakHashMap<DownloadLink, Object>();

    public void addWhitelist(DownloadLink link) {
        if (matches(new PrePluginCheckDummyChallenge(link), true) != null) {
            synchronized (whitelist) {
                whitelist.put(link, this);
            }
            collectGarbage();
        }
    }

    protected void collectGarbage() {
        synchronized (entries) {
            final ArrayList<BlacklistEntry> cleanups = new ArrayList<BlacklistEntry>();
            try {
                for (BlacklistEntry e : entries) {
                    if (e.canCleanUp()) {
                        cleanups.add(e);
                    }
                }
            } finally {
                entries.removeAll(cleanups);
            }
        }
    }

    public boolean isWhitelisted(DownloadLink downloadLink) {
        synchronized (whitelist) {
            return whitelist.containsKey(downloadLink);
        }
    }

    @Override
    public void onDownloadWatchDogPropertyChange(DownloadWatchDogProperty propertyChange) {
    }
}
