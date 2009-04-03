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

package jd.gui.skins.simple.components.DownloadView;

import java.awt.BorderLayout;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import jd.controlling.JDDownloadControllerEvent;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.JDEvent;
import jd.event.JDListener;
import jd.gui.skins.simple.JTabbedPanel;
import jd.gui.skins.simple.SimpleGUI;
import jd.plugins.FilePackage;
import jd.utils.JDUtilities;

public class DownloadLinksPanel extends JTabbedPanel implements ControlListener, JDListener {

    /**
     * 
     */
    private static final long serialVersionUID = -6029423913449902141L;

    public final static int REFRESH_ALL_DATA_CHANGED = 1;
    public final static int REFRESH_DATA_AND_STRUCTURE_CHANGED = 0;
    public static final int REFRESH_SPECIFIED_LINKS = 2;

    /**
     * Der Logger für Meldungen
     */
    protected Logger logger = JDUtilities.getLogger();

    private DownloadTreeTable internalTreeTable;

    private Vector<FilePackage> packages = new Vector<FilePackage>();

    public DownloadLinksPanel(SimpleGUI parent) {
        super(new BorderLayout());
        internalTreeTable = new DownloadTreeTable(new DownloadTreeTableModel(this), this);
        JScrollPane scrollPane = new JScrollPane(internalTreeTable);
        this.add(scrollPane);
        JDUtilities.getController().addControlListener(this);
        JDUtilities.getDownloadController().addJDListener(this);
    }

    public synchronized void fireTableChanged(final int id, final Object param) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                synchronized (JDUtilities.getController().getPackages()) {
                    internalTreeTable.fireTableChanged(id, param);
                }
            }
        });
    }

    @Override
    public void onDisplay() {
        internalTreeTable.removeKeyListener(internalTreeTable);
        internalTreeTable.addKeyListener(internalTreeTable);
    }

    @Override
    public void onHide() {
        internalTreeTable.removeKeyListener(internalTreeTable);
    }

    public void controlEvent(final ControlEvent event) {
        if (event == null) {
            logger.warning("event == null");
            return;
        }
        switch (event.getID()) {
        case ControlEvent.CONTROL_SPECIFIED_DOWNLOADLINKS_CHANGED:
            fireTableChanged(REFRESH_SPECIFIED_LINKS, event.getParameter());
            break;
        case ControlEvent.CONTROL_ALL_DOWNLOADLINKS_DATA_CHANGED:
            fireTableChanged(REFRESH_ALL_DATA_CHANGED, null);
            break;
        case ControlEvent.CONTROL_LINKLIST_STRUCTURE_CHANGED:
            fireTableChanged(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);
            break;
        }
    }

    public Vector<FilePackage> getPackages() {
        return packages;
    }

    public void receiveJDEvent(JDEvent event) {
        if (event instanceof JDDownloadControllerEvent) {
            if (event.getSource() == JDUtilities.getDownloadController()) {
                switch (event.getID()) {
                case JDDownloadControllerEvent.REFRESH:
                    fireTableChanged(REFRESH_DATA_AND_STRUCTURE_CHANGED, null);
                    break;
                case JDDownloadControllerEvent.UPDATE:
                    fireTableChanged(REFRESH_ALL_DATA_CHANGED, null);
                    break;
                }

            }
        }
    }
}
