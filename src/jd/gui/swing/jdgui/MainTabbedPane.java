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

import java.awt.AWTKeyStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.donate.DonationManager;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.event.GUIEvent;
import org.jdownloader.gui.event.GUIEventSender;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.DownloadsView;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberView;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.gui.swing.jdgui.interfaces.SwitchPanelEvent;
import jd.gui.swing.jdgui.interfaces.View;
import jd.gui.swing.jdgui.maintab.ClosableTabHeader;
import jd.gui.swing.jdgui.maintab.CustomTabHeader;
import jd.gui.swing.jdgui.maintab.TabHeader;
import jd.gui.swing.jdgui.views.ClosableView;

public class MainTabbedPane extends JTabbedPane implements MouseMotionListener, MouseListener {
    private static final long     serialVersionUID               = -1531827591735215594L;
    private static MainTabbedPane INSTANCE;
    protected View                latestSelection;
    protected Component           latestSelectionTabHeader;
    public static AtomicBoolean   SPECIAL_DEALS_ENABLED          = new AtomicBoolean(false);
    public static AtomicBoolean   SPECIAL_DEALS_REMINDER_ENABLED = new AtomicBoolean(false);
    //
    // private AbstractIcon specialDealIcon;
    //
    // private Font specialDealFont;
    // private Color specialDealColor;
    private Rectangle             specialDealBounds              = null;
    private boolean               specialDealMouseOver           = false;
    private View                  donatePanel;
    private DonateTabHeader       donateHeader;
    protected int                 rightest                       = -1;

    public synchronized static MainTabbedPane getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new MainTabbedPane();
        }
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
        if (getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(ks) != null) {
            return false;
        }
        if (getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(ks) != null) {
            return false;
        }
        return ret;
    }

    public void remove(View view) {
        if (!this.contains(view)) {
            return;
        }
        boolean selected = (getSelectedView() == view);
        int index = -1;
        for (int i = 0; i < getTabCount(); i++) {
            if (getComponentAt(i) == view) {
                index = i;
                break;
            }
        }
        removeTabAt(index);
        if (view != null) {
            view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_REMOVE));
        }
        if (selected && getTabCount() > 0) {
            setSelectedComponent(getComponentAt(0));
        }
    }

    public void addTab(View view) {
        if (this.contains(view)) {
            return;
        }
        if (view instanceof ClosableView) {
            addClosableTab((ClosableView) view);
        } else {
            int index = getTabCount();
            while (index > 0 && getTabComponentAt(index - 1) instanceof PromotionTabHeader) {
                index--;
            }
            super.insertTab(view.getTitle(), view.getIcon(), view, view.getTooltip(), index);
            this.setFocusable(false);
            TabHeader header;
            this.setTabComponentAt(index, header = new TabHeader(view));
            if (getTabCount() == 1) {
                this.latestSelectionTabHeader = header;
                header.setShown();
            }
            view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));
        }
    }

    private void setupTabTraversalKeys() {
        KeyStroke ctrlTab = KeyStroke.getKeyStroke("ctrl TAB");
        KeyStroke ctrlShiftTab = KeyStroke.getKeyStroke("ctrl shift TAB");
        // Remove ctrl-tab from normal focus traversal
        Set<AWTKeyStroke> forwardKeys = new HashSet<AWTKeyStroke>(getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS));
        forwardKeys.remove(ctrlTab);
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, forwardKeys);
        // Remove ctrl-shift-tab from normal focus traversal
        Set<AWTKeyStroke> backwardKeys = new HashSet<AWTKeyStroke>(getFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS));
        backwardKeys.remove(ctrlShiftTab);
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, backwardKeys);
        // Add keys to the tab's input map
        InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        inputMap.put(ctrlTab, "navigateNext");
        inputMap.put(ctrlShiftTab, "navigatePrevious");
        // add new actions to skip the donatetab
        getActionMap().put("navigatePrevious", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = getSelectedIndex();
                index--;
                if (index < 0) {
                    index = getTabCount() - 1;
                }
                while (index > 0 && getTabComponentAt(index) instanceof PromotionTabHeader) {
                    index--;
                }
                setSelectedIndex(index);
            }
        });
        getActionMap().put("navigateNext", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = getSelectedIndex();
                index++;
                if (index > getTabCount() - 1 || getTabComponentAt(index) instanceof PromotionTabHeader) {
                    index = 0;
                }
                setSelectedIndex(index);
            }
        });
    }

    private void addClosableTab(ClosableView view) {
        // super.addTab(view.getTitle(), view.getIcon(), view, view.getTooltip());
        int index = getTabCount();
        while (index > 0 && getTabComponentAt(index - 1) instanceof PromotionTabHeader) {
            index--;
        }
        super.insertTab(view.getTitle(), view.getIcon(), view, view.getTooltip(), index);
        ClosableTabHeader header;
        this.setTabComponentAt(index, header = new ClosableTabHeader(view));
        this.setFocusable(false);
        view.getBroadcaster().fireEvent(new SwitchPanelEvent(view, SwitchPanelEvent.ON_ADD));
    }

    private void updateDonateButton() {
        final boolean isVisible = DonationManager.getInstance().isButtonVisible();
        if (isVisible) {
            if (donatePanel == null) {
                donatePanel = new View() {
                    @Override
                    protected void onShow() {
                    }

                    @Override
                    protected void onHide() {
                    }

                    @Override
                    public String getTooltip() {
                        return null;
                    }

                    @Override
                    public String getTitle() {
                        return _GUI.T.DonateAction();
                    }

                    @Override
                    public Icon getIcon() {
                        return new AbstractIcon(IconKey.ICON_HEART, 16);
                    }

                    @Override
                    public String getID() {
                        return "DONATE";
                    }
                };
            }
            super.addTab("DONATE", null, donatePanel, null);
            setTabComponentAt(this.getTabCount() - 1, donateHeader = new DonateTabHeader(donatePanel));
        } else if (donatePanel != null) {
            remove(donatePanel);
            donatePanel = null;
        }
    }

    private MainTabbedPane() {
        this.setMinimumSize(new Dimension(300, 100));
        this.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        this.setOpaque(false);
        // specialDealFont = dummyLbl.getFont();
        // Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute, Integer>();
        // fontAttributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        // specialDealFont = (specialDealFont.deriveFont(specialDealFont.getStyle() ^
        // Font.BOLD).deriveFont(fontAttributes)).deriveFont(16f);
        setupTabTraversalKeys();
        addMouseMotionListener(this);
        addMouseListener(this);
        // we need this init BEFORE the event listener below.Else we would get a init-loop problem resulting in a nullpointer
        DonationManager.getInstance();
        CFG_GUI.DONATE_BUTTON_STATE.getEventSender().addListener(new GenericConfigEventListener<Enum>() {
            @Override
            public void onConfigValueModified(KeyHandler<Enum> keyHandler, Enum newValue) {
                new EDTRunner() {
                    @Override
                    protected void runInEDT() {
                        updateDonateButton();
                    }
                };
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Enum> keyHandler, Enum invalidValue, ValidationException validateException) {
            }
        });
        updateDonateButton();
        this.setFocusable(false);
        TabHeader header;
        this.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                if (JDGui.getInstance() != null) {
                    JDGui.getInstance().setWaiting(true);
                }
                try {
                    View comp = (View) getSelectedComponent();
                    if (comp == donatePanel) {
                        if (latestSelection != null) {
                            // Do not select. this should act as a button
                            setSelectedComponent(latestSelection);
                            return;
                        }
                    }
                    Component tabComp = getTabComponentAt(getSelectedIndex());
                    if (comp == latestSelection) {
                        return;
                    }
                    if (latestSelection != null) {
                        latestSelection.setHidden();
                    }
                    if (latestSelectionTabHeader != null && latestSelectionTabHeader instanceof CustomTabHeader) {
                        ((CustomTabHeader) latestSelectionTabHeader).setHidden();
                    }
                    GUIEventSender.getInstance().fireEvent(new GUIEvent(MainTabbedPane.this, GUIEvent.Type.TAB_SWITCH, latestSelection, comp));
                    latestSelection = comp;
                    latestSelectionTabHeader = tabComp;
                    if (tabComp != null && tabComp instanceof CustomTabHeader) {
                        ((CustomTabHeader) tabComp).setShown();
                    }
                    comp.setShown();
                    revalidate();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        });
        addComponentListener(new ComponentListener() {
            @Override
            public void componentShown(ComponentEvent e) {
                update();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                update();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                update();
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });
        this.addContainerListener(new ContainerListener() {
            @Override
            public void componentRemoved(ContainerEvent e) {
                update();
            }

            @Override
            public void componentAdded(ContainerEvent e) {
                update();
            }
        });
        LAFOptions.getInstance().getExtension().customizeMainTabbedPane(this);
    }

    private void update() {
        // invokeLater is needed because componentAdded gets called before actual added
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                int rightest = Integer.MIN_VALUE;
                for (int t = 0; t < getTabCount(); t++) {
                    Rectangle bounds = getUI().getTabBounds(MainTabbedPane.this, t);
                    int right = bounds.x + bounds.width;
                    if (right > rightest) {
                        rightest = right;
                    }
                }
                MainTabbedPane.this.rightest = rightest;
            }
        });
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

    private TopRightPainter topRightPainter = null;

    public TopRightPainter getTopRightPainter() {
        return topRightPainter;
    }

    public void setTopRightPainter(TopRightPainter topRightPainter) {
        this.topRightPainter = topRightPainter;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if (JDGui.getInstance() != null) {
            JDGui.getInstance().setWaiting(false);
        }
        final TopRightPainter topRightPainter = getTopRightPainter();
        // System.out.println(getPreferredSize().width - p.x);
        if (topRightPainter != null && rightest > 0) {
            final Shape clip = g.getClip();
            try {
                g.setClip(rightest, 0, getWidth() - rightest, 32);
                specialDealBounds = topRightPainter.paint((Graphics2D) g);
            } finally {
                g.setClip(clip);
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
        View c = getComponentEquals((View) e);
        int index = indexOfComponent(c);
        if (index < 0) {
            setSelectedIndex(0);
            return;
        }
        super.setSelectedIndex(index);
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
            if (c.equals(view)) {
                return (View) c;
            }
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
            if (c.equals(view)) {
                return true;
            }
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
        final TopRightPainter topRightPainter = getTopRightPainter();
        final Rectangle specialDealBounds = this.specialDealBounds;
        if (topRightPainter != null && topRightPainter.isVisible()) {
            if (specialDealBounds != null && specialDealBounds.contains(e.getPoint()) && !specialDealMouseOver) {
                specialDealMouseOver = true;
                topRightPainter.onMouseOver(e);
                repaint(specialDealBounds.x - 4, specialDealBounds.y, specialDealBounds.width + 6, specialDealBounds.height);
            } else if (specialDealMouseOver && (specialDealBounds == null || !specialDealBounds.contains(e.getPoint()))) {
                specialDealMouseOver = false;
                topRightPainter.onMouseOut(e);
                // setCursor(null);
                repaint(specialDealBounds.x - 4, specialDealBounds.y, specialDealBounds.width + 6, specialDealBounds.height);
            }
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        final TopRightPainter topRightPainter = getTopRightPainter();
        if (specialDealMouseOver && topRightPainter != null && topRightPainter.isVisible()) {
            topRightPainter.onClicked(e);
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
