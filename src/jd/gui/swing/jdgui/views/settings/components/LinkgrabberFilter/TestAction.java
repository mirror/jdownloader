package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate.T;
import org.jdownloader.images.Theme;

public class TestAction extends AbstractAction {
    private FilterTable table;

    public TestAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, T._.settings_linkgrabber_filter_action_test());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("run", 20));
    }

    public void actionPerformed(ActionEvent e) {
    }

}
