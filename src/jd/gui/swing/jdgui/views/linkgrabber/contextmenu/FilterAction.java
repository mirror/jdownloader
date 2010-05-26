package jd.gui.swing.jdgui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import javax.swing.JCheckBoxMenuItem;

import jd.controlling.LinkGrabberController;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;

public class FilterAction extends ContextMenuAction {

    private static final long serialVersionUID = -7285951677831827223L;

    private final String extension;

    public FilterAction(String extension) {
        this.extension = extension;

        init();
    }

    @Override
    protected String getIcon() {
        return null;
    }

    @Override
    protected String getName() {
        return "*." + extension;
    }

    public void actionPerformed(ActionEvent e) {
        boolean selected = ((JCheckBoxMenuItem) e.getSource()).isSelected();
        LinkGrabberController.getInstance().filterExtension(extension, selected);
    }

}
