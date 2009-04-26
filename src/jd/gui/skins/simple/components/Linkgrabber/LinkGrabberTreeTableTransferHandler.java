package jd.gui.skins.simple.components.Linkgrabber;

import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Vector;

import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.TransferHandler;
import javax.swing.tree.TreePath;

import jd.controlling.DownloadController;
import jd.controlling.LinkGrabberController;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.utils.JDLocale;
import jd.utils.JDTheme;
import jd.utils.JDUtilities;

public class LinkGrabberTreeTableTransferHandler extends TransferHandler {
    private static final long serialVersionUID = 2560352681437669412L;
    private static final int DRAG_LINKS = 1;
    private static final int DRAG_PACKAGES = 2;

    private Object draggingObjects = null;
    private int draggingType = 0;

    public boolean isDragging = false;
    private LinkGrabberTreeTable treeTable;
    private LinkGrabberController LGINSTANCE;

    public LinkGrabberTreeTableTransferHandler(LinkGrabberTreeTable linkgrabberTreeTable) {
        treeTable = linkgrabberTreeTable;
        LGINSTANCE = LinkGrabberController.getInstance();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean canImport(TransferSupport info) {
        if (isDragging) {
            if (draggingObjects == null) return false;
            int row = ((JTable.DropLocation) info.getDropLocation()).getRow();
            TreePath current = treeTable.getPathForRow(row);
            if (current == null) return false;
            switch (draggingType) {
            case DRAG_LINKS:
                Vector<DownloadLink> downloadLinks = (Vector<DownloadLink>) draggingObjects;
                if (current.getLastPathComponent() instanceof DownloadLink && downloadLinks.contains(current.getLastPathComponent())) return false;
                break;
            case DRAG_PACKAGES:
                Vector<LinkGrabberFilePackage> packages = (Vector<LinkGrabberFilePackage>) draggingObjects;
                if (current.getLastPathComponent() instanceof LinkGrabberFilePackage && packages.contains(current.getLastPathComponent())) return false;
                if (current.getLastPathComponent() instanceof DownloadLink) {
                    LinkGrabberFilePackage fp = LGINSTANCE.getFPwithLink(((DownloadLink) current.getLastPathComponent()));
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
        Vector<LinkGrabberFilePackage> packages = treeTable.getSelectedFilePackages();
        Vector<DownloadLink> downloadLinks = treeTable.getSelectedDownloadLinks();
        int row = treeTable.rowAtPoint(treeTable.getMousePosition());
        TreePath current = treeTable.getPathForRow(row);
        if (current.getLastPathComponent() instanceof LinkGrabberFilePackage) {
            this.draggingObjects = packages;
            this.draggingType = DRAG_PACKAGES;
        } else {
            this.draggingObjects = downloadLinks;
            this.draggingType = DRAG_LINKS;
        }
        return new StringSelection("JDAFFE");
    }

    @Override
    protected void exportDone(JComponent source, Transferable data, int action) {
        isDragging = false;
    }

    @SuppressWarnings("unchecked")
    private boolean drop(int row, Point point) {
        if (!isDragging) return false;
        final TreePath current = treeTable.getPathForRow(row);
        if (current == null) return false;
        JPopupMenu popup = new JPopupMenu();
        JMenuItem m;
        synchronized (DownloadController.getInstance().getPackages()) {
            switch (draggingType) {
            case DRAG_LINKS:
                final Vector<DownloadLink> downloadLinks = (Vector<DownloadLink>) draggingObjects;

                if (current.getLastPathComponent() instanceof LinkGrabberFilePackage) {
                    /* Links in Package */
                    String Name = ((LinkGrabberFilePackage) current.getLastPathComponent()).getName();
                    popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.insertinpackagestart", "In Paket '%s' am Anfang einfügen"), Name)));
                    m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            synchronized (DownloadController.getInstance().getPackages()) {
                                LinkGrabberFilePackage fp = ((LinkGrabberFilePackage) current.getLastPathComponent());
                                fp.addAllAt(downloadLinks, 0);
                            }
                        }
                    });

                    popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.insertinpackageend", "In Paket '%s' am Ende einfügen"), Name)));
                    m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            synchronized (DownloadController.getInstance().getPackages()) {
                                LinkGrabberFilePackage fp = ((LinkGrabberFilePackage) current.getLastPathComponent());
                                fp.addAllAt(downloadLinks, fp.size());
                            }
                        }
                    });

                } else if (current.getLastPathComponent() instanceof DownloadLink) {
                    /* Links in Links */
                    String Name = ((DownloadLink) current.getLastPathComponent()).getName();
                    popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.before", "Vor '%s' ablegen"), Name)));
                    m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            synchronized (DownloadController.getInstance().getPackages()) {
                                LinkGrabberFilePackage fp = LGINSTANCE.getFPwithLink(((DownloadLink) current.getLastPathComponent()));
                                if (fp != null) fp.addAllAt(downloadLinks, fp.indexOf((DownloadLink) current.getLastPathComponent()) - 1);
                            }
                        }
                    });

                    popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.after", "Nach '%s' ablegen"), Name)));
                    m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                    m.addActionListener(new ActionListener() {
                        public void actionPerformed(ActionEvent e) {
                            synchronized (DownloadController.getInstance().getPackages()) {
                                LinkGrabberFilePackage fp = LGINSTANCE.getFPwithLink(((DownloadLink) current.getLastPathComponent()));
                                if (fp != null) fp.addAllAt(downloadLinks, fp.indexOf((DownloadLink) current.getLastPathComponent()) + 1);
                            }
                        }
                    });
                }
                break;
            case DRAG_PACKAGES:
                final Vector<LinkGrabberFilePackage> packages = (Vector<LinkGrabberFilePackage>) draggingObjects;
                final LinkGrabberFilePackage fp;
                final String Name;
                if (current.getLastPathComponent() instanceof LinkGrabberFilePackage) {
                    Name = ((LinkGrabberFilePackage) current.getLastPathComponent()).getName();
                    fp = ((LinkGrabberFilePackage) current.getLastPathComponent());
                } else if (current.getLastPathComponent() instanceof DownloadLink) {
                    Name = ((DownloadLink) current.getLastPathComponent()).getFilePackage().getName();
                    fp = LGINSTANCE.getFPwithLink(((DownloadLink) current.getLastPathComponent()));
                    if (fp == null) return false;
                } else
                    return false;

                popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.movepackagebefore", "Vor Paket '%s' einfügen"), Name)));
                m.setIcon(JDTheme.II("gui.images.go_top", 16, 16));
                m.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        LGINSTANCE.addAllAt(packages, LGINSTANCE.indexOf(fp));
                    }

                });

                popup.add(m = new JMenuItem(String.format(JDLocale.L("gui.table.draganddrop.movepackageend", "Nach Paket '%s' einfügen"), Name)));
                m.setIcon(JDTheme.II("gui.images.go_bottom", 16, 16));
                m.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        LGINSTANCE.addAllAt(packages, LGINSTANCE.indexOf(fp) + 1);
                    }
                });

                break;
            default:
                return false;
            }
        }
        popup.add(m = new JMenuItem(JDLocale.L("gui.btn_cancel", "Abbrechen")));
        m.setIcon(JDTheme.II("gui.images.unselected", 16, 16));
        popup.show(treeTable, point.x, point.y);
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
                List list = (List) tr.getTransferData(DataFlavor.javaFileListFlavor);
                for (int t = 0; t < list.size(); t++) {
                    JDUtilities.getController().loadContainerFile((File) list.get(t));
                }
            } else if (tr.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                String files = (String) tr.getTransferData(DataFlavor.stringFlavor);
                String linuxfiles[] = new Regex(files, "file://(.*?)\n").getColumn(0);
                if (linuxfiles != null && linuxfiles.length > 0) {
                    for (String file : linuxfiles) {
                        JDUtilities.getController().loadContainerFile(new File(file));
                    }
                } else
                    JDUtilities.getController().distributeLinks(files);
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
