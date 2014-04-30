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

package jd.gui.swing.jdgui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.font.TextAttribute;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.ClosableTabHeader;
import jd.gui.swing.jdgui.oboom.OboomDialog;
import jd.gui.swing.jdgui.views.ClosableView;
import jd.http.Browser;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.event.GUIEvent;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class MainTabbedPane extends JTabbedPane implements MouseMotionListener, MouseListener {

    private static final long     serialVersionUID      = -1531827591735215594L;

    private static MainTabbedPane INSTANCE;
    protected View                latestSelection;
    public static boolean         SPECIAL_DEALS_ENABLED = false;

    private AbstractIcon          specialDealIcon;

    private Font                  specialDealFont;
    private Color                 specialDealColor;
    private Rectangle             specialDealBounds;
    private boolean               specialDealMouseOver  = false;

    public synchronized static MainTabbedPane getInstance() {
        if (INSTANCE == null) INSTANCE = new MainTabbedPane();
        return INSTANCE;
    }

    /**
     * Use {@link MainTabbedPane#remove(View)}!
     */
    @Override
    public void remove(Component component) {
        throw new RuntimeException("This method is not allowed!");
    }

    @Override
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition, boolean pressed) {
        boolean ret = super.processKeyBinding(ks, e, condition, pressed);
        if (getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ks) != null) return false;
        if (getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ks) != null) return false;
        return ret;
    }

    public void remove(View view) {
        if (!this.contains(view)) return;
        boolean selected = (getSelectedView() == view);
        super.remove(view);
        if (view != null) view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_REMOVE));
        if (selected && getTabCount() > 0) setSelectedComponent(getComponentAt(0));
    }

    public void addTab(View view) {
        if (this.contains(view)) return;
        if (view instanceof ClosableView) {
            addClosableTab((ClosableView) view);
        } else {
            super.addTab(view.getTitle(), view.getIcon(), view, view.getTooltip());
            view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));
            this.setFocusable(false);
        }
    }

    private void addClosableTab(ClosableView view) {

        super.addTab(view.getTitle(), view.getIcon(), view, view.getTooltip());
        view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));
        this.setTabComponentAt(this.getTabCount() - 1, new ClosableTabHeader(view));

        this.setFocusable(false);

    }

    private MainTabbedPane() {
        this.setMinimumSize(new Dimension(300, 100));
        this.setTabLayoutPolicy(JTabbedPane.WRAP_TAB_LAYOUT);
        this.setOpaque(false);

        specialDealIcon = new AbstractIcon("osrlogo", 18);

        JLabel dummyLbl = new JLabel();
        specialDealFont = dummyLbl.getFont();
        CFG_GUI.SPECIAL_DEALS_ENABLED.getEventSender().addListener(new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                repaint();
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        });
        Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
        // fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        specialDealFont = (specialDealFont.deriveFont(specialDealFont.getStyle() ^ Font.BOLD).deriveFont(fontAttributes)).deriveFont(16f);
        addMouseMotionListener(this);
        addMouseListener(this);
        specialDealColor = dummyLbl.getForeground();

        this.addChangeListener(new ChangeListener() {

            public void stateChanged(ChangeEvent e) {
                if (JDGui.getInstance() != null) JDGui.getInstance().setWaiting(true);
                try {
                    View comp = (View) getSelectedComponent();
                    if (comp == latestSelection) return;
                    if (latestSelection != null) {
                        latestSelection.setHidden();
                    }
                    GUIEventSender.getInstance().fireEvent(new GUIEvent(MainTabbedPane.this, GUIEvent.Type.TAB_SWITCH, latestSelection, comp));
                    latestSelection = comp;
                    comp.setShown();
                    revalidate();

                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }

        });

        new Thread("Ask StatServ") {
            public void run() {
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Browser br = new Browser();
                try {
                    br.getPage("http://stats.appwork.org/data/db/getDealStatus");

                    if (br.containsHTML("true")) {
                        SPECIAL_DEALS_ENABLED = true;
                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                repaint();
                            }
                        };
                        if (CFG_GUI.CFG.isSpecialDealOboomDialogVisibleOnStartup()) {
                            Thread.sleep(10000);

                            OboomDialog d = new OboomDialog();

                            UIOManager.I().show(null, d);
                            CFG_GUI.CFG.setSpecialDealOboomDialogVisibleOnStartup(false);

                        }
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }

            };
        }.start();
    }

    public void notifyCurrentTab() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                View comp = (View) getSelectedComponent();
                GUIEventSender.getInstance().fireEvent(new GUIEvent(MainTabbedPane.this, GUIEvent.Type.TAB_SWITCH, latestSelection, comp));
            }
        };
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (JDGui.getInstance() != null) JDGui.getInstance().setWaiting(false);

        if (CFG_GUI.CFG.isSpecialDealsEnabled() && SPECIAL_DEALS_ENABLED && OboomDialog.isOfferActive()) {
            int height = 22;
            specialDealIcon = new AbstractIcon("logo_oboom", 65);
            Graphics2D g2 = (Graphics2D) g;
            g2.setFont(specialDealFont);
            g2.setColor(Color.GRAY);
            //
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            String[] parts = _GUI._.special_deal_oboom().split("(?i)oboom(\\.com)?");

            int totalWidth = 0;
            for (int i = 0; i < parts.length; i++) {
                if (StringUtils.isEmpty(parts[i])) parts[i] = "";
                String s = parts[i];
                Rectangle2D bounds = g2.getFontMetrics().getStringBounds(s, g2);
                totalWidth += bounds.getWidth();
                if (i < parts.length - 1) {
                    // totalWidth += 5;
                    totalWidth += specialDealIcon.getIconWidth();
                }
            }

            int x = getWidth() - totalWidth - 2;
            for (int i = 0; i < parts.length; i++) {
                String s = parts[i];
                Rectangle2D bounds = g2.getFontMetrics().getStringBounds(s, g2);
                g2.drawString(s, x, (int) ((height - bounds.getHeight()) / 2) + (int) bounds.getHeight());
                x += bounds.getWidth();
                // x += 5;
                // g2.setColor(Color.RED);
                // g2.drawRect(getWidth() - width, 2, width, height);

                if (i < parts.length - 1) {
                    specialDealIcon.paintIcon(this, g2, x, 4 + (height - specialDealIcon.getIconHeight()) / 2);
                    x += specialDealIcon.getIconWidth();
                }
            }
            specialDealBounds = new Rectangle(getWidth() - totalWidth, 2, totalWidth, height);

            if (specialDealMouseOver) {

                g2.drawLine(getWidth() - totalWidth - 4, height, getWidth() - 2, height);
            } else {
                // g2.drawLine(getWidth() - totalWidth - 3, height - 2, getWidth() - 2, height - 2);
            }

        }

    }

    /**
     * gets called form the main frame if it gets closed
     */
    public void onClose() {
        getSelectedView().setHidden();
    }

    /**
     * returns the currently selected View
     */
    public View getSelectedView() {
        return (View) super.getSelectedComponent();
    }

    @Override
    public void setSelectedComponent(Component e) {
        super.setSelectedComponent(getComponentEquals((View) e));
    }

    /**
     * returns the component in this tab that equals view
     * 
     * @param view
     * @return
     */
    public View getComponentEquals(View view) {
        for (int i = 0; i < this.getTabCount(); i++) {
            Component c = this.getComponentAt(i);
            if (c.equals(view)) return (View) c;
        }
        return null;
    }

    /**
     * CHecks if there is already a tabbepanel of this type in this pane.
     * 
     * @param view
     * @return
     */
    public boolean contains(View view) {
        for (int i = 0; i < this.getTabCount(); i++) {
            Component c = this.getComponentAt(i);
            if (c.equals(view)) return true;
        }
        return false;
    }

    public boolean isLinkgrabberView() {
        return getSelectedView() instanceof LinkGrabberView;
    }

    public boolean isDownloadView() {
        return getSelectedView() instanceof DownloadsView;
    }

    // public void mouseClicked(MouseEvent e) {
    // try {
    // int tabNumber = getUI().tabForCoordinate(this, e.getX(), e.getY());
    // if (tabNumber < 0) return;
    // Rectangle rect = ((CloseTabIcon) getIconAt(tabNumber)).getBounds();
    // if (rect.contains(e.getX(), e.getY())) {
    // // the tab is being closed
    // ((ClosableView) this.getComponentAt(tabNumber)).close();
    // }
    // } catch (ClassCastException e2) {
    // }
    // }
    //
    // public void mouseEntered(MouseEvent e) {
    // }
    //
    // public void mouseExited(MouseEvent e) {
    // }
    //
    // public void mousePressed(MouseEvent e) {
    // }
    //
    // public void mouseReleased(MouseEvent e) {
    // }

    @Override
    public void mouseDragged(MouseEvent e) {
    }

    @Override
    public void mouseMoved(MouseEvent e) {

        if (specialDealBounds != null && specialDealBounds.contains(e.getPoint()) && !specialDealMouseOver && CFG_GUI.CFG.isSpecialDealsEnabled() && SPECIAL_DEALS_ENABLED && OboomDialog.isOfferActive()) {
            specialDealMouseOver = true;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            repaint(specialDealBounds.x - 4, specialDealBounds.y, specialDealBounds.width + 6, specialDealBounds.height);

        } else if (specialDealMouseOver && (specialDealBounds == null || !specialDealBounds.contains(e.getPoint()))) {
            specialDealMouseOver = false;
            setCursor(null);
            repaint(specialDealBounds.x - 4, specialDealBounds.y, specialDealBounds.width + 6, specialDealBounds.height);

        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (specialDealMouseOver && CFG_GUI.CFG.isSpecialDealsEnabled() && SPECIAL_DEALS_ENABLED && OboomDialog.isOfferActive()) {
            new Thread("OSR") {
                public void run() {

                    OboomDialog d = new OboomDialog();

                    UIOManager.I().show(null, d);
                    OboomDialog.track("TabbedClick");
                }
            }.start();

        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

}
