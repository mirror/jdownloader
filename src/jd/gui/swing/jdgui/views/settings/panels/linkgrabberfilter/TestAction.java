package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class TestAction extends AbstractAction {
    private static final long serialVersionUID = 8659634943980480698L;

    public TestAction() {
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_test());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("run", 20));

    }

    public void actionPerformed(ActionEvent e) {
    }

}
