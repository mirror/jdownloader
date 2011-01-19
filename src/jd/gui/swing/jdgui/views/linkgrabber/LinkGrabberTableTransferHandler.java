//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.jdgui.views.linkgrabber;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;

import jd.controlling.DownloadController;
import jd.controlling.JDController;
import jd.controlling.LinkGrabberController;
import jd.gui.swing.SwingGui;
import jd.plugins.DownloadLink;
import jd.plugins.LinkGrabberFilePackage;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;

import org.appwork.utils.AwReg;

public class LinkGrabberTableTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 2560352681437669412L;
    private static final int DRAG_LINKS = 1;
    private static final int DRAG_PACKAGES = 2;

    private Object draggingObjects = null;
    private int draggingType = 0;

    public boolean isDragging = false;
    private LinkGrabberTable table;
    private LinkGrabberController LGINSTANCE;

    public LinkGrabberTableTransferHandler(LinkGrabberTable linkgrabberTreeTable) {
        table = linkgrabberTreeTable;
        LGINSTANCE = LinkGrabberController.getInstance();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean canImport(TransferSupport info) {
        if (isDragging) {
            if (draggingObjects == null) return false;
            int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
            if (row == -1) return false;
            Object current = table.getModel().getValueAt(row, 0);
            if (current == null) return false;
            switch (draggingType) {
            case DRAG_LINKS:
                ArrayList<DownloadLink> downloadLinks = (ArrayList<DownloadLink>) draggingObjects;
                if (current instanceof DownloadLink && downloadLinks.contains(current)) return false;
                break;
            case DRAG_PACKAGES:
                ArrayList<LinkGrabberFilePackage> packages = (ArrayList<LinkGrabberFilePackage>) draggingObjects;
                if (current instanceof LinkGrabberFilePackage && packages.contains(current)) return false;
                if (current instanceof DownloadLink) {
                    LinkGrabberFilePackage fp = LGINSTANCE.getFPwithLink(((DownloadLink) current));
                    if (fp != null && packages.contains(fp)) return false;
                }
                break;
            default:
                return false;
            }
            return true;
        } else {
            if (info.isDataFlavorSupported(DataFlavor.stringFlavor)) return true;
            if (info.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return true;
            return false;
        }
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        isDragging = true;
        String url = "http://www.jdownloader.org";
        ArrayList<LinkGrabberFilePackage> packages = table.getSelectedFilePackages();
        ArrayList<DownloadLink> downloadLinks = table.getSelectedDownloadLinks();
        Point p = SwingGui.getInstance().getMainFrame().getMousePosition();
        p = SwingUtilities.convertPoint(SwingGui.getInstance().getMainFrame(), p, table);
        int row = table.rowAtPoint(p);
        if (row == -1) {
            isDragging = false;
            return null;
        }
        Object current = table.getModel().getValueAt(row, 0);
        if (current == null) return new StringSelection(url);
        if (current instanceof LinkGrabberFilePackage) {
            this.draggingObjects = packages;
            this.draggingType = DRAG_PACKAGES;
        } else {
            this.draggingObjects = downloadLinks;
            if (downloadLinks.size() != 0 && downloadLinks.get(0).getLinkType() == DownloadLink.LINKTYPE_NORMAL) url = downloadLinks.get(0).getBrowserUrl();
            this.draggingType = DRAG_LINKS;
        }
        return new StringSelection(url);
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        isDragging = false;
    }

    private boolean drop(int row, Point point) {
        if (!isDragging) return false;
        final Object current = table.getModel().getValueAt(row, 0);
        if (current == null) return false;
        JPopupMenu popup = new JPopupMenu();

        JMenuItem m;
        synchronized (DownloadController.getInstance().getPackages()) {
            switch (draggingType) {
            case DRAG_LINKS:
                if (current instanceof LinkGrabberFilePackage) {
                    /* Links in Package */
                    String Name = ((LinkGrabberFilePackage) current).getName();
                    popup.add(m = new JMenuItem(JDL.LF("gui.table.draganddrop.insertinpackagestart", "Insert at the beginning of Package '%s'", Name)));
                    m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            LinkGrabberController.getInstance().move(draggingObjects, current, LinkGrabberController.MOVE_BEGIN);
                        }
                    });

                    popup.add(m = new JMenuItem(JDL.LF("gui.table.draganddrop.insertinpackageend", "Insert at the end of Package '%s'", Name)));
                    m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            LinkGrabberController.getInstance().move(draggingObjects, current, LinkGrabberController.MOVE_END);
                        }
                    });

                } else if (current instanceof DownloadLink) {
                    /* Links in Links */
                    String name = ((DownloadLink) current).getName();
                    popup.add(m = new JMenuItem(JDL.LF("gui.table.draganddrop.before", "Drop before '%s'", name)));
                    m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            LinkGrabberController.getInstance().move(draggingObjects, current, LinkGrabberController.MOVE_BEFORE);
                        }
                    });

                    popup.add(m = new JMenuItem(JDL.LF("gui.table.draganddrop.after", "Drop after '%s'", name)));
                    m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            LinkGrabberController.getInstance().move(draggingObjects, current, LinkGrabberController.MOVE_AFTER);
                        }
                    });
                }
                break;
            case DRAG_PACKAGES:
                final String name;
                if (current instanceof LinkGrabberFilePackage) {
                    name = ((LinkGrabberFilePackage) current).getName();
                } else if (current instanceof DownloadLink) {
                    name = ((DownloadLink) current).getFilePackage().getName();
                } else
                    return false;

                popup.add(m = new JMenuItem(JDL.LF("gui.table.draganddrop.movepackagebefore", "Insert before '%s'", name)));
                m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                m.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        LinkGrabberController.getInstance().move(draggingObjects, current, LinkGrabberController.MOVE_BEFORE);
                    }

                });

                popup.add(m = new JMenuItem(JDL.LF("gui.table.draganddrop.movepackageend", "Insert after '%s'", name)));
                m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                m.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        LinkGrabberController.getInstance().move(draggingObjects, current, LinkGrabberController.MOVE_AFTER);
                    }
                });

                break;
            default:
                return false;
            }
        }
        popup.add(m = new JMenuItem(JDL.L("gui.btn_cancel", "Cancel")));
        m.setIcon(JDTheme.II("gui.images.unselected", 16, 16));
        popup.show(table, point.x, point.y);
        return true;
    }

    @Override
    public int getSourceActions(JComponent c) {
        return MOVE;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean importData(TransferSupport info) {
        try {
            Transferable tr = info.getTransferable();
            if (isDragging) {
                Point p = ((JTable.DropLocation) info.getDropLocation()).getDropPoint();
                int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
                return drop(row, p);
            } else if (tr.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                List<File> list = (List<File>) tr.getTransferData(DataFlavor.javaFileListFlavor);
                for (File file : list) {
                    JDController.loadContainerFile(file);
                }
            } else if (tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String files = (String) tr.getTransferData(DataFlavor.stringFlavor);
                String linuxfiles[] = new AwReg(files, "file://(.*?)(\r\n|\r|\n)").getColumn(0);
                if (linuxfiles != null && linuxfiles.length > 0) {
                    for (String file : linuxfiles) {
                        JDController.loadContainerFile(new File(file.trim()));
                    }
                } else {
                    JDController.distributeLinks(files);
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
