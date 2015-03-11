package org.jdownloader.gui.donate;

import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.gui.translate._GUI;

public enum PayProvider implements LabelInterface {
    MOBILEPAY() {
        @Override
        public String getLabel() {
            return _GUI._.DonationDialog_popup_mobile();
        }
    },
    CALL2PAY() {
        @Override
        public String getLabel() {
            return _GUI._.DonationDialog_popup_call2pay();
        }
    },
    DIRECT_DEBIT() {
        @Override
        public String getLabel() {
            return _GUI._.DonationDialog_popup_directdebit();
        }
    },
    SOFORT_UEBERWEISUNG() {
        @Override
        public String getLabel() {
            return _GUI._.DonationDialog_popup_sofort_ueberweisung();
        }
    },
    WIRED() {
        @Override
        public String getLabel() {
            return _GUI._.DonationDialog_popup_wired();
        }
    },
    AMAZONPAY() {
        @Override
        public String getLabel() {
            return _GUI._.DonationDialog_popup_amazon();
        }
    },

    CREDIT_CARD() {
        @Override
        public String getLabel() {
            return _GUI._.DonationDialog_popup_creditcard();
        }
    },
    GIOROPAY() {
        @Override
        public String getLabel() {
            return _GUI._.DonationDialog_popup_giropay();
        }
    };

    public String getID() {
        return null;
    }
    // OTHER() {
    // @Override
    // public String getLabel() {
    // return _GUI._.DonationDialog_popup_other();
    // }
    // };

}
