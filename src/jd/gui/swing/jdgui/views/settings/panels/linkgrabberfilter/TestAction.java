package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.LinkFilterController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class TestAction extends AbstractAction {
    private static final long serialVersionUID = 8659634943980480698L;
    private LinkgrabberFilter linkgrabberFilter;

    public TestAction(LinkgrabberFilter linkgrabberFilter) {
        this.linkgrabberFilter = linkgrabberFilter;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_test());
        this.putValue(AbstractAction.SMALL_ICON, NewTheme.I().getIcon("run", 20));

    }

    @Override
    public boolean isEnabled() {
        return linkgrabberFilter.getTestText() != null;
    }

    public void actionPerformed(ActionEvent e) {

        String filter = linkgrabberFilter.getTestText();

        String result = LinkFilterController.getInstance().test(filter);

        Dialog.getInstance().showMessageDialog(0, _GUI._.settings_linkgrabber_filter_action_test_title(filter), result);
    }

}
