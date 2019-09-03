package org.jdownloader.donate;

import java.io.File;

import org.appwork.utils.Application;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DonateButtonState;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DonationManager {
    private static final DonationManager INSTANCE           = new DonationManager();
    private final long                   autoVisibleTimeout = 3 * 30 * 24 * 60 * 60 * 1000l;
    private final long                   donationThankYou   = 3 * 30 * 24 * 60 * 60 * 1000l;

    private DonationManager() {
        final File donation = Application.getResource("cfg/donation_0.json");
        switch (CFG_GUI.CFG.getDonateButtonState()) {
        case AUTO_VISIBLE:
            if (donation.isFile()) {
                final long waitUntil = donation.lastModified() + donationThankYou;
                if (System.currentTimeMillis() < waitUntil) {
                    CFG_GUI.CFG.setDonateButtonLatestAutoChange(donation.lastModified());
                    CFG_GUI.CFG.setDonateButtonState(DonateButtonState.AUTO_HIDDEN);
                }
            }
            break;
        case CUSTOM_HIDDEN:
        case AUTO_HIDDEN:
            final long lastChange = System.currentTimeMillis() - CFG_GUI.CFG.getDonateButtonLatestAutoChange();
            if (lastChange > autoVisibleTimeout) {
                CFG_GUI.CFG.setDonateButtonLatestAutoChange(System.currentTimeMillis());
                CFG_GUI.CFG.setDonateButtonState(DonateButtonState.AUTO_VISIBLE);
            }
            break;
        default:
            break;
        }
    }

    public static DonationManager getInstance() {
        return INSTANCE;
    }

    public boolean isButtonVisible() {
        switch (CFG_GUI.CFG.getDonateButtonState()) {
        case CUSTOM_VISIBLE:
        case AUTO_VISIBLE:
            return true;
        default:
            return false;
        }
    }
}
