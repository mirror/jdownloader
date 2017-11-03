//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.gui.swing.jdgui.views.settings.panels.packagizer;

import javax.swing.Icon;

import jd.controlling.TaskQueue;
import jd.gui.swing.jdgui.views.settings.sidebar.CheckBoxedEntry;

import org.appwork.utils.event.queue.QueueAction;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.extensions.Header;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class Packagizer extends AbstractConfigPanel implements CheckBoxedEntry {
    private static final long serialVersionUID = 2L;
    private PackagizerFilter  packagizer;

    public String getTitle() {
        return _GUI.T.gui_config_linkgrabber_packagizer();
    }

    public Packagizer() {
        super();
        add(new Header(getTitle(), new AbstractIcon(IconKey.ICON_PACKAGIZER, 32), org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED), "spanx,growx,pushx");
        this.addDescriptionPlain(_JDT.T.gui_settings_linkgrabber_packagizer_description());
        packagizer = new PackagizerFilter();
        add(packagizer);
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_PACKAGIZER, 32);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {
            @Override
            protected Void run() throws RuntimeException {
                packagizer.getTable().getModel()._fireTableStructureChanged(PackagizerController.getInstance().list(), false);
                return null;
            }
        });
    }

    public String getName() {
        return getTitle();
    }

    public Icon _getIcon(int size) {
        return getIcon();
    }

    public boolean _isEnabled() {
        return org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.isEnabled();
    }

    public String getDescription() {
        return _JDT.T.gui_settings_linkgrabber_packagizer_description();
    }

    public void _setEnabled(boolean b) throws StartException, StopException {
        org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.setValue(b);
    }
}