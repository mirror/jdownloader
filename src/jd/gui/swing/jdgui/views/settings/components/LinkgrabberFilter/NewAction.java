package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.Theme;

public class NewAction extends AbstractAction {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private FilterTable       table;

    public NewAction(FilterTable table) {
        this.table = table;
        this.putValue(NAME, _GUI._.settings_linkgrabber_filter_action_add());
        this.putValue(AbstractAction.SMALL_ICON, Theme.getIcon("add", 20));
    }

    public void actionPerformed(ActionEvent e) {

    }

}
