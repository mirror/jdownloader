package jd.gui.swing.jdgui.views.settings.sidebar;

import javax.swing.ImageIcon;

import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public interface CheckBoxedEntry {

    String getName();

    ImageIcon _getIcon(int size);

    boolean _isEnabled();

    String getDescription();

    void _setEnabled(boolean b) throws StartException, StopException;

}
