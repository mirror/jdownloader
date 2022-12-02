package org.jdownloader.extensions.eventscripter;

import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.jdownloader.gui.settings.AbstractConfigPanel;

public class TriggerSetupPanel extends AbstractConfigPanel {
    private final List<Runnable> onSaveList    = new ArrayList<Runnable>();
    private final List<Runnable> onTestRunList = new ArrayList<Runnable>();

    public TriggerSetupPanel(int insets) {
        super(insets);
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    @Override
    public String getLeftGap() {
        return "0";
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public void updateContents() {
    }

    @Override
    public void save() {
        for (final Runnable onSave : onSaveList) {
            onSave.run();
        }
    }

    public void executeOnSave(Runnable run) {
        if (run != null) {
            onSaveList.add(run);
        }
    }

    public void testRun() {
        for (final Runnable onTestRun : onTestRunList) {
            onTestRun.run();
        }
    }

    public void executeOnTestRun(Runnable run) {
        if (run != null) {
            onTestRunList.add(run);
        }
    }
}