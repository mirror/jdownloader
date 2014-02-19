package jd.gui.swing.jdgui;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsDevice.WindowTranslucency;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Transparency;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;

import org.appwork.swing.ExtJWindow;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtMergedIcon;
import org.appwork.utils.Application;
import org.appwork.utils.ColorUtils;
import org.appwork.utils.StringUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.SwingUtils;
import org.jdownloader.controlling.contextmenu.CustomizableAppAction;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.jdtrayicon.ScreenStack;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.statistics.StatsManager;
import org.jdownloader.updatev2.gui.LAFOptions;

public class VoteFinderWindow extends ExtJWindow implements AWTEventListener {

    private static final int BOTTOM_MARGIN = 5;
    private static final int TOP_MARGIN    = 20;
    private MigPanel         content;

    private ScreenStack      screenStack;
    private Point            endLocation;

    public Point getEndLocation() {
        return endLocation;
    }

    private Point          startLocation;

    private Rectangle      bounds;

    private JLabel         headerLbl;

    private int            round = 25;
    private JLabel         lbl;
    private Timer          timer;
    private MigPanel       actualContent;
    private boolean        positive;
    private DirectFeedback feedback;
    private JLabel         icon;

    public VoteFinderWindow(boolean positive) {
        super();

        bounds = new Rectangle();
        content = new MigPanel("ins 2 5 10 5,wrap 1", "[grow,fill]", "[][grow,fill]") {

            @Override
            public void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();

                int width = getWidth();
                int height = getHeight();

                // Create a soft clipped image for the background
                BufferedImage img = getSoftClipWorkaroundImage(g2d, width, height);

                g2d.drawImage(img, 0, 0, null);

                g2d.dispose();

            }

        };

        setContentPane(content);
        content.add(createHeader(_GUI._.VoteFinderWindow_VoteFinderWindow_title_()));
        MigPanel cp;
        content.add(cp = new MigPanel("ins 0,wrap 2", "10[]10[grow,fill]", "[]"));
        SwingUtils.setOpaque(cp, false);
        this.positive = positive;
        cp.add(icon = new JLabel(positive ? new AbstractIcon(IconKey.ICON_THUMBS_UP, 24) : new AbstractIcon(IconKey.ICON_THUMBS_DOWN, 24)), "hidemode 3,spany,aligny top");

        actualContent = new MigPanel("ins 0", "[]", "[]");
        SwingUtils.setOpaque(actualContent, false);
        cp.add(actualContent);
        pack();

        try {
            if (setWindowOpaque(this) == false) {

            }
        } catch (Throwable e) {
            e.printStackTrace();
        }

        // setWindowOpacity(this, 0.85f);
        setAlwaysOnTop(true);
        if (timer != null) timer.stop();
        timer = new Timer(1000 / 25, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateLocation();
            }
        });
        timer.start();
        setVisible(false);
        Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);

        JDGui.getInstance().getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    }

    // @Override
    // public void mouseClicked(MouseEvent e) {
    // System.out.println(e);
    // }
    //
    // @Override
    // public void mousePressed(MouseEvent e) {
    // System.out.println(e);
    // }
    //
    // @Override
    // public void mouseReleased(MouseEvent e) {
    // System.out.println(e);
    // }
    //

    public boolean isPositive() {
        return positive;
    }

    private static Boolean setWindowOpaqueSupported = null;

    public static boolean setWindowOpaque(Window owner) {
        if (Boolean.FALSE.equals(setWindowOpaqueSupported)) return false;
        try {

            com.sun.awt.AWTUtilities.setWindowOpaque(owner, false);
            setWindowOpaqueSupported = Boolean.TRUE;
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        setWindowOpaqueSupported = Boolean.FALSE;
        return false;
    }

    @Override
    public void setVisible(boolean b) {
        super.setVisible(b);

    }

    protected void updateLocation() {
        Point mouse = MouseInfo.getPointerInfo().getLocation();

        DirectFeedback feedback = null;
        ArrayList<Component> allComponents = new ArrayList<Component>();
        Window[] windows = Window.getWindows();
        main: for (int i = windows.length - 1; i >= 0; i--) {
            Window w = windows[i];
            try {

                if (w.isVisible() && !(w instanceof VoteFinderWindow)) {
                    if (w.isActive()) {
                        Point local = new Point(mouse.x, mouse.y);
                        SwingUtilities.convertPointFromScreen(local, w);

                        ArrayList<Component> ret = find((Container) w, local);
                        Collections.reverse(ret);
                        if (ret.size() == 0) {
                            setVisible(false);
                        } else {
                            setVisible(true);

                        }
                        allComponents.addAll(ret);
                        boolean found = false;
                        for (Component c : ret) {

                            if (c instanceof JMenu) {

                                feedback = null;
                                break main;
                            } else if (c instanceof AbstractButton) {
                                Action action = ((AbstractButton) c).getAction();
                                if (action != null && action instanceof CustomizableAppAction) {

                                    String text = ((CustomizableAppAction) action).getName();
                                    if (StringUtils.isEmpty(text)) text = ((CustomizableAppAction) action).getTooltipText();
                                    String iconKey = ((CustomizableAppAction) action).getIconKey();
                                    if (StringUtils.isNotEmpty(text)) {

                                        MenuItemData menuItemData = ((CustomizableAppAction) action).getMenuItemData();
                                        feedback = new MenuItemFeedback(isPositive(), menuItemData);

                                        actualContent.removeAll();
                                        actualContent.setLayout(new MigLayout("ins 0", "[]", "[][]"));
                                        // if (positive) {
                                        // actualContent.add(new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_msg_positive()));
                                        // } else {
                                        // actualContent.add(new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_msg_negative()));
                                        // }
                                        icon.setVisible(false);
                                        if (isPositive()) {
                                            JLabel lbl = new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_action_positive(text));
                                            if (iconKey != null) {
                                                lbl.setIcon(new ExtMergedIcon(new AbstractIcon(IconKey.ICON_THUMBS_UP, 24), 0, 0).add(new AbstractIcon(iconKey, 24), 10, 10));
                                            } else {
                                                lbl.setIcon(new ExtMergedIcon(new AbstractIcon(IconKey.ICON_THUMBS_UP, 24), 0, 0).add(IconIO.getScaledInstance(((CustomizableAppAction) action).getSmallIcon(), 22, 22), 10, 10));
                                            }

                                            actualContent.add(lbl, "");

                                        } else {
                                            JLabel lbl = new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_action_negative(text));
                                            if (iconKey != null) {
                                                lbl.setIcon(new ExtMergedIcon(new AbstractIcon(IconKey.ICON_THUMBS_DOWN, 24), 0, 0).add(new AbstractIcon(iconKey, 24), 10, 10));
                                            } else {
                                                lbl.setIcon(new ExtMergedIcon(new AbstractIcon(IconKey.ICON_THUMBS_DOWN, 24), 0, 0).add(IconIO.getScaledInstance(((CustomizableAppAction) action).getSmallIcon(), 22, 22), 10, 10));
                                            }
                                            actualContent.add(lbl, "");
                                        }
                                    }
                                }

                            }

                            if (c instanceof DirectFeedbackInterface) {
                                Point p = new Point(mouse.x, mouse.y);
                                SwingUtilities.convertPointFromScreen(p, c);
                                feedback = ((DirectFeedbackInterface) c).layoutDirectFeedback(p, isPositive(), actualContent, this);
                                if (feedback != null) {

                                    break main;
                                }

                            }
                        }

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.feedback = feedback;
        if (feedback == null) {
            fallback: if (allComponents.size() > 0) {

                for (Component c : allComponents) {

                }
            }
        }
        if (feedback == null) {
            actualContent.removeAll();
            icon.setVisible(true);
            actualContent.setLayout(new MigLayout("ins 0", "[]", "[]"));
            // if (positive) {
            // actualContent.add(new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_msg_positive()));
            // } else {
            // actualContent.add(new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_msg_negative()));
            // }
            actualContent.add(new JLabel(_GUI._.VoteFinderWindow_VoteFinderWindow_explain()), "");
        }
        pack();
        Point loc = correct(mouse, this.getPreferredSize());
        setLocation(loc.x, loc.y);
    }

    private ArrayList<Component> find(Container container, Point point) {
        ArrayList<Component> ret = new ArrayList<Component>();
        for (Component c : container.getComponents()) {
            if (c.getBounds().contains(point) && c.isVisible() && c.isShowing()) {
                ret.add(c);
                Point newPoint = SwingUtilities.convertPoint(container, new Point(point.x, point.y), c);
                if (c instanceof Container) {
                    ret.addAll(find((Container) c, newPoint));

                } else {

                }
            }
        }
        return ret;

    }

    @Override
    public void dispose() {
        super.dispose();
        JDGui.getInstance().getMainFrame().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        MainTabbedPane.getInstance().onDisposedVoteWindow(this);
    }

    private static Boolean getWindowOpacitySupported = null;

    public static Float getWindowOpacity(Window owner) {
        if (Boolean.FALSE.equals(getWindowOpacitySupported)) return null;
        try {
            Float ret = null;
            if (Application.getJavaVersion() >= Application.JAVA17) {
                ret = owner.getOpacity();
            } else {
                ret = com.sun.awt.AWTUtilities.getWindowOpacity(owner);
            }
            getWindowOpacitySupported = Boolean.TRUE;
            return ret;
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        getWindowOpacitySupported = Boolean.FALSE;
        return null;
    }

    private static Boolean setWindowOpacitySupported = null;

    private Component createHeader(String caption) {
        MigPanel ret = new MigPanel("ins 0", "[grow,fill]", "[]");
        headerLbl = new JLabel(caption);
        headerLbl.setForeground(ColorUtils.getAlphaInstance(headerLbl.getForeground(), 160));
        headerLbl.setHorizontalAlignment(SwingConstants.LEFT);
        ret.add(SwingUtils.toBold(headerLbl));

        SwingUtils.setOpaque(ret, false);
        SwingUtils.setOpaque(headerLbl, false);

        SwingUtils.setOpaque(ret, false);
        SwingUtils.setOpaque(headerLbl, false);

        return ret;
    }

    public static void setWindowOpacity(Window window, float f) {
        if (Boolean.FALSE.equals(setWindowOpacitySupported)) return;
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        try {
            if (gd.isWindowTranslucencySupported(WindowTranslucency.TRANSLUCENT)) {
                if (Application.getJavaVersion() >= Application.JAVA17) {
                    window.setOpacity(f);
                } else {
                    com.sun.awt.AWTUtilities.setWindowOpacity(window, f);
                }
                setWindowOpacitySupported = Boolean.TRUE;
                return;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        setWindowOpacitySupported = Boolean.FALSE;
    }

    protected boolean isMouseOver(Point loc, Component comp) {
        comp.getBounds(bounds);
        Point los = comp.getLocationOnScreen();
        bounds.x = los.x;
        bounds.y = los.y;
        // System.out.println(bounds + " - " + getBounds() + " - " + loc + "  " + bounds.contains(loc) + " - " + getBounds().contains(loc));

        if (loc.x >= bounds.x && loc.x <= bounds.x + bounds.width) {

            if (loc.y >= bounds.y && loc.y <= bounds.y + bounds.height) {
                //
                return true;
            }
        }
        return false;
    }

    // @Override
    // public void mouseEntered(MouseEvent e) {

    // }

    // @Override
    // public void mouseExited(MouseEvent e) {
    //

    // }

    public void setHeaderText(String txt) {
        headerLbl.setText(txt);
    }

    protected BufferedImage getSoftClipWorkaroundImage(Graphics2D g2d, int width, int height) {
        GraphicsConfiguration gc = g2d.getDeviceConfiguration();
        BufferedImage img = gc.createCompatibleImage(width, height, Transparency.TRANSLUCENT);
        Graphics2D g2 = img.createGraphics();
        // clear the full image
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, width, height);

        g2.setComposite(AlphaComposite.Src);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.WHITE);
        // rendering the shape
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), round, round);
        g2.setComposite(AlphaComposite.SrcAtop);
        // rendering the actuall contents
        Paint p = null;

        p = new GradientPaint(0, 0, LAFOptions.getInstance().getColorForPanelHeaderBackground(), 0, TOP_MARGIN, LAFOptions.getInstance().getColorForScrollbarsMouseOverState());
        g2.setPaint(p);

        g2.fillRoundRect(0, 0, width, height, round, round);

        p = new GradientPaint(0, 0, LAFOptions.getInstance().getColorForPanelBackground(), 0, TOP_MARGIN, LAFOptions.getInstance().getColorForPanelBackground().brighter());
        g2.setPaint(p);

        g2.fillRect(0, TOP_MARGIN, width, height - TOP_MARGIN - BOTTOM_MARGIN);

        g2.setColor(LAFOptions.getInstance().getColorForPanelHeaderLine());

        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, round, round);
        g2.dispose();
        return img;
    }

    @Override
    public void eventDispatched(AWTEvent event) {

        if (feedback != null) {
            if (event instanceof MouseEvent) {
                if (((MouseEvent) event).getButton() == MouseEvent.BUTTON1) {
                    switch (((MouseEvent) event).getID()) {
                    case MouseEvent.MOUSE_PRESSED:
                    case MouseEvent.MOUSE_RELEASED:
                        ((MouseEvent) event).consume();
                        return;
                    case MouseEvent.MOUSE_CLICKED:
                        StatsManager.I().feedback(feedback);
                        setVisible(false);
                        dispose();
                    }
                }
            }
        }
    }

    public void setScreenStack(ScreenStack screenStack) {
        this.screenStack = screenStack;
    }

    public ScreenStack getScreenStack() {
        return screenStack;
    }

    public void setEndLocation(Point p) {

        endLocation = p;
    }

    public void setStartLocation(Point point) {
        if (startLocation == null) {
            setLocation(point);
            startLocation = point;
        }

    }

    public Point getStartLocation() {
        return startLocation;
    }

    public void setHighlightColor(Object object) {
    }

    public static Point correct(final Point point, final Dimension prefSize) {
        final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        final GraphicsDevice[] screens = ge.getScreenDevices();

        final Rectangle preferedRect = new Rectangle(point.x, point.y, prefSize.width, prefSize.height);

        GraphicsDevice myScreen = null;
        Rectangle myBounds = null;
        for (final GraphicsDevice screen : screens) {
            final Rectangle bounds = screen.getDefaultConfiguration().getBounds();

            // bounds.x += insets.left;
            // bounds.y += insets.top;
            // bounds.width -= insets.left + insets.right;
            // bounds.height -= insets.top + insets.bottom;
            if (bounds.contains(point)) {
                myScreen = screen;
                myBounds = bounds;
                break;
            }
        }
        Point ret = new Point(point.x, point.y);
        if (myBounds == null) return ret;

        boolean rightOK = true;
        boolean bottomOK = true;
        final Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(myScreen.getDefaultConfiguration());

        if (myBounds.x + myBounds.width - (point.x + prefSize.width + 24 + insets.right) < 0) {
            rightOK = false;

        }
        if (myBounds.y + myBounds.height - (point.y + prefSize.height + 24 + insets.bottom) < 0) {
            bottomOK = false;

        }

        if (rightOK) {
            ret.x += 24;
        } else {
            ret.x -= prefSize.width;
            ret.x -= 24;
        }

        if (bottomOK) {
            ret.y += 24;
        } else {
            ret.y -= prefSize.height;
            ret.y -= 24;
        }

        return ret;
    }

    public void setIconVisible(boolean b) {
        icon.setVisible(b);
    }

}
