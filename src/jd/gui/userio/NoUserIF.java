package jd.gui.userio;

import jd.gui.UserIF;

public class NoUserIF extends UserIF {

    public NoUserIF() {
        super();
    }

    @Override
    public void requestPanel(Panels panelID, Object parameter) {
        System.out.println("NoUserIF set!");
    }

    @Override
    public void displayMiniWarning(String shortWarn, String longWarn) {
        System.out.println("NoUserIF set!");
    }

    @Override
    public void setFrameStatus(int id) {
        System.out.println("NoUserIF set!");
    }

}
