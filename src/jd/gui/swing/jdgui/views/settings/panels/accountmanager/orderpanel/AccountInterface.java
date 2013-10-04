package jd.gui.swing.jdgui.views.settings.panels.accountmanager.orderpanel;

import org.appwork.swing.exttable.tree.TreeNodeInterface;

public interface AccountInterface extends TreeNodeInterface {

    boolean isEnabled();

    void setEnabled(boolean value);

    String getHost();

    String getUser();

}
