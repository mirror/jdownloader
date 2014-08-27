package jd.gui.swing.jdgui.views.settings.sidebar;

import javax.swing.Icon;

import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

public interface CheckBoxedEntry {

    String getName();

    Icon _getIcon(int size);

    boolean _isEnabled();

    String getDescription();

    void _setEnabled(boolean b) throws StartException, StopException;

}
