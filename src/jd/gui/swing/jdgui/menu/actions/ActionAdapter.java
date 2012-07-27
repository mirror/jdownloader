package jd.gui.swing.jdgui.menu.actions;

import java.awt.event.ActionEvent;

import org.jdownloader.actions.AppAction;

@Deprecated
public abstract class ActionAdapter extends AppAction {

    public ActionAdapter(String name, String deprec, String iconKey) {
        super();
        setName(name);
        setIconKey(iconKey);
        setIconSizes(20);

        setAccelerator(createAccelerator());
        setTooltipText(createTooltip());
        initDefaults();
    }

    public ActionAdapter(String name, String deprec, int id) {
        super();
        setName(name);

        setAccelerator(createAccelerator());
        setTooltipText(createTooltip());
        initDefaults();
    }

    public void actionPerformed(ActionEvent e) {
        onAction(e);
    }

    public abstract void onAction(ActionEvent e);

    @Deprecated
    public abstract void initDefaults();

    public abstract String createAccelerator();

    public abstract String createTooltip();
}
