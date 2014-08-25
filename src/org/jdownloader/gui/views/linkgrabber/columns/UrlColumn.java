package org.jdownloader.gui.views.linkgrabber.columns;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.MouseEvent;

import javax.swing.JPopupMenu;

import jd.controlling.ClipboardMonitoring;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.exttable.columns.ExtTextColumn;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.columns.FileColumn;

public class UrlColumn extends ExtTextColumn<AbstractNode> {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Override
    public boolean isEnabled(final AbstractNode obj) {
        if (obj instanceof CrawledPackage) {
            return ((CrawledPackage) obj).getView().isEnabled();
        }
        return obj.isEnabled();
    }

    protected boolean isEditable(final AbstractNode obj, final boolean enabled) {

        return false;
    }

    @Override
    public boolean isDefaultVisible() {
        return false;
    }

    public JPopupMenu createHeaderPopup() {

        return FileColumn.createColumnPopup(this, getMinWidth() == getMaxWidth() && getMaxWidth() > 0);

    }

    @Override
    public boolean onSingleClick(MouseEvent e, AbstractNode obj) {
        return super.onSingleClick(e, obj);
    }

    public UrlColumn() {
        super(_GUI._.LinkGrabberTableModel_initColumns_url());
        this.setClickcount(2);

    }

    @Override
    public void focusGained(final FocusEvent e) {
    }

    @Override
    public boolean isEditable(AbstractNode obj) {
        return false;
    }

    private boolean isOpenURLAllowed(AbstractNode value) {
        DownloadLink dlLink = getLink(value);

        if (dlLink != null) {
            return true;
        }
        return false;
    }

    public DownloadLink getLink(AbstractNode value) {
        DownloadLink dlLink = null;
        if (value instanceof CrawledPackage && ((CrawledPackage) value).getChildren().size() == 1) {
            value = ((CrawledPackage) value).getChildren().get(0);
        } else if (value instanceof FilePackage && ((FilePackage) value).getChildren().size() == 1) {
            value = ((FilePackage) value).getChildren().get(0);
        }
        if (value instanceof CrawledLink) {
            dlLink = ((CrawledLink) value).getDownloadLink();
        } else if (value instanceof DownloadLink) {
            dlLink = (DownloadLink) value;
        }
        return dlLink;
    }

    private long         lastHide = 0;
    private AbstractNode editing  = null;
    private JPopupMenu   popup;

    @Override
    public boolean onDoubleClick(MouseEvent e, final AbstractNode value) {
        final int row = getModel().getTable().rowAtPoint(new Point(e.getX(), e.getY()));

        final long timeSinceLastHide = System.currentTimeMillis() - lastHide;
        if (timeSinceLastHide < 250 && editing == value) {
            //

            editing = null;
            repaint();
            return true;

        }
        if (!isOpenURLAllowed(value)) {
            Toolkit.getDefaultToolkit().beep();
            return false;
        }
        editing = value;
        popup = new JPopupMenu() {
            public void setVisible(final boolean b) {

                super.setVisible(b);
                if (!b) {
                    lastHide = System.currentTimeMillis();
                    // editing = null;
                    // updateIcon(true);
                } else {
                    // updateIcon(false);
                }

            };

        };

        try {
            final DownloadLink dlLink = getLink(editing);
            popup.add(new AppAction() {
                {
                    setName(_GUI._.UrlColumn_onDoubleClick_object_copy(shorten(dlLink.getDownloadURL())));
                    setIconKey(IconKey.ICON_COPY);

                }

                @Override
                public void actionPerformed(ActionEvent e) {
                    ClipboardMonitoring.getINSTANCE().setCurrentContent(dlLink.getDownloadURL());
                }

            });
            popup.add(new AppAction() {
                {
                    setName(_GUI._.UrlColumn_onDoubleClick_object_open(shorten(dlLink.getDownloadURL())));
                    setIconKey(IconKey.ICON_BROWSE);

                }

                @Override
                public void actionPerformed(ActionEvent e) {

                    CrossSystem.openURLOrShowMessage(dlLink.getDownloadURL());
                }

            });
            if (!StringUtils.equals(dlLink.getDownloadURL(), dlLink.getBrowserUrl())) {
                popup.add(new AppAction() {
                    {
                        setName(_GUI._.UrlColumn_onDoubleClick_object_copy_origin(shorten(dlLink.getBrowserUrl())));
                        setIconKey(IconKey.ICON_COPY);

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        ClipboardMonitoring.getINSTANCE().setCurrentContent(dlLink.getBrowserUrl());
                    }

                });
                popup.add(new AppAction() {
                    {
                        setName(_GUI._.UrlColumn_onDoubleClick_object_open_origin(shorten(dlLink.getBrowserUrl())));
                        setIconKey(IconKey.ICON_BROWSE);

                    }

                    @Override
                    public void actionPerformed(ActionEvent e) {

                        CrossSystem.openURLOrShowMessage(dlLink.getBrowserUrl());
                    }

                });
            }

            final Rectangle bounds = getModel().getTable().getCellRect(row, getIndex(), true);
            final Dimension pref = popup.getPreferredSize();
            popup.setPreferredSize(new Dimension(Math.max(pref.width, bounds.width), pref.height));
            popup.show(getModel().getTable(), bounds.x, bounds.y + bounds.height);
            return true;
        } catch (final Exception e1) {
            e1.printStackTrace();
        }

        return true;
    }

    protected String shorten(String browserUrl) {
        if (browserUrl.length() < 45) {
            return browserUrl;
        }
        return browserUrl.substring(0, 45) + "[...]";
    }

    @Override
    public boolean isSortable(AbstractNode obj) {
        return true;
    }

    @Override
    protected void setStringValue(String value, AbstractNode object) {
    }

    @Override
    public String getStringValue(AbstractNode value) {
        if (value instanceof CrawledPackage) {
            return ((CrawledPackage) value).getView().getCommonSourceUrl();
        } else if (value instanceof FilePackage) {
            return ((FilePackage) value).getView().getCommonSourceUrl();
        } else if (value instanceof CrawledLink) {
            return ((CrawledLink) value).getDownloadLink().getView().getDownloadUrl();

        } else if (value instanceof DownloadLink) {

            return ((DownloadLink) value).getView().getDownloadUrl();
        }
        return null;
    }

}
