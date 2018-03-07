package org.jdownloader.donate;

import org.appwork.utils.Application;
import org.jdownloader.settings.GraphicalUserInterfaceSettings.DonateButtonState;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class DonationManager {
    private static final DonationManager INSTANCE = new DonationManager();

    private DonationManager() {
        final boolean hasDonated = Application.getResource("cfg/donation_0.json").exists();
        if (hasDonated) {
            switch (CFG_GUI.CFG.getDonateButtonState()) {
            case AUTO_VISIBLE:
                CFG_GUI.CFG.setDonateButtonLatestAutoChange(System.currentTimeMillis());
                CFG_GUI.CFG.setDonateButtonState(DonateButtonState.AUTO_HIDDEN);
            default:
                break;
            }
        } else {
            switch (CFG_GUI.CFG.getDonateButtonState()) {
            case CUSTOM_HIDDEN:
            case AUTO_HIDDEN:
                if ((System.currentTimeMillis() - CFG_GUI.CFG.getDonateButtonLatestAutoChange()) > 4 * 30 * 24 * 60 * 60 * 1000l) {
                    CFG_GUI.CFG.setDonateButtonLatestAutoChange(System.currentTimeMillis());
                    CFG_GUI.CFG.setDonateButtonState(DonateButtonState.AUTO_VISIBLE);
                }
                break;
            default:
                break;
            }
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
