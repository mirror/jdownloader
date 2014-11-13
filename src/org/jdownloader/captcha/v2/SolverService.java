package org.jdownloader.captcha.v2;

import java.util.HashMap;

import javax.swing.Icon;

import jd.gui.swing.jdgui.views.settings.panels.anticaptcha.AbstractCaptchaSolverConfigPanel;

public interface SolverService {
    public abstract String getName();

    public abstract AbstractCaptchaSolverConfigPanel getConfigPanel();

    public abstract boolean hasConfigPanel();

    public abstract Icon getIcon(int size);

    public abstract String getType();

    public int getWaitForByID(String solverID);

    public abstract HashMap<String, Integer> getWaitForOthersDefaultMap();

    public HashMap<String, Integer> getWaitForMap();

    public abstract ChallengeSolverConfig getConfig();

    public abstract String getID();

    public abstract boolean isEnabled();

    public abstract void setEnabled(boolean b);
}
