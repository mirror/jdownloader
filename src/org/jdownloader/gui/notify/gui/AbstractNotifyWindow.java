package org.jdownloader.gui.notify.gui;

import java.awt.AWTEvent;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsDevice.WindowTranslucency;
import java.awt.GraphicsEnvironment;
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
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.ExtJWindow;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.Application;
import org.appwork.utils.ColorUtils;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.windowmanager.WindowManager.FrameState;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.gui.ExtPopupMenu;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.jdtrayicon.ScreenStack;
import org.jdownloader.gui.notify.AbstractBubbleContentPanel;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.ExtRealCheckBoxMenuItem;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;
import org.jdownloader.updatev2.gui.LAFOptions;

public abstract class AbstractNotifyWindow<T extends AbstractBubbleContentPanel> extends ExtJWindow implements ActionListener, AWTEventListener, GenericConfigEventListener<Boolean> {
    
    private static final int BOTTOM_MARGIN = 5;
    private static final int TOP_MARGIN    = 20;
    private MigPanel         content;
    private Timer            timer;
    private Fader            fader;
    private ScreenStack      screenStack;
    private Point            endLocation;
    
    public Point getEndLocation() {
        return endLocation;
    }
    
    private int                   timeout = 15000;
    
    private Point                 startLocation;
    private int                   round   = 10;
    private Balloner              controller;
    private T                     contentComponent;
    private long                  endTime = 0;
    private boolean               mouseOver;
    private Rectangle             bounds;
    private boolean               disposed;
    private boolean               closed;
    private JLabel                headerLbl;
    private AbstractBubbleSupport bubbleSupport;
    private Color                 highlightColor;
    
    public AbstractNotifyWindow(String caption, T comp) {
        this(null, caption, comp);
    };
    
    public AbstractNotifyWindow(AbstractBubbleSupport bubbleSupport, String caption, T comp) {
        super();
        this.bubbleSupport = bubbleSupport;
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
        comp.setWindow(this);
        setContentPane(content);
        content.add(createHeader(caption));
        contentComponent = comp;
        content.add(comp);
        pack();
        round = 0;
        try {
            if (setWindowOpaque(this) == false) round = 10;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        
        setWindowOpacity(this, 0f);
        
        fader = new Fader(this);
        timeout = CFG_BUBBLE.DEFAULT_TIMEOUT.getValue();
        if (timeout > 0) {
        }
        
        if (bubbleSupport != null) {
            List<Element> elements = bubbleSupport.getElements();
            if (elements != null) {
                for (Element e : elements) {
                    e.getKeyhandler().getEventSender().addListener(this, true);
                }
            }
        }
        
    }
    
    public AbstractBubbleSupport getBubbleSupport() {
        return bubbleSupport;
    }
    
    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }
    
    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        new EDTRunner() {
            
            @Override
            protected void runInEDT() {
                updateLayout();
            }
        };
    }
    
    protected void updateLayout() {
        
        getContentComponent().updateLayout();
        pack();
        BubbleNotify.getInstance().relayout();
        
    }
    
    public void dispose() {
        try {
            disposed = true;
            closed = true;
            if (bubbleSupport != null) {
                List<Element> elements = bubbleSupport.getElements();
                if (elements != null) {
                    for (Element e : elements) {
                        e.getKeyhandler().getEventSender().removeListener(this);
                    }
                }
            }
        } finally {
            super.dispose();
        }
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
    
    public boolean isClosed() {
        return closed;
    }
    
    public boolean isDisposed() {
        return disposed;
    }
    
    public T getContentComponent() {
        return contentComponent;
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
    
    @Override
    public void eventDispatched(AWTEvent e) {
        if (e instanceof MouseEvent) {
            MouseEvent m = (MouseEvent) e;
            
            if (!mouseOver && m.getID() == MouseEvent.MOUSE_ENTERED) {
                if (isMouseOver(m.getLocationOnScreen(), this)) {
                    mouseOver = true;
                    
                    onMouseEntered(m);
                    return;
                }
                
            } else if (mouseOver && m.getID() == MouseEvent.MOUSE_EXITED) {
                
                if (!isMouseOver(m.getLocationOnScreen(), this)) {
                    
                    mouseOver = false;
                    
                    onMouseExited(m);
                    return;
                }
                
            } else if (m.getID() == MouseEvent.MOUSE_CLICKED) {
                
                if (isMouseOver(m.getLocationOnScreen(), contentComponent)) {
                    
                    onMouseClicked(m);
                    return;
                }
            }
        }
        
    }
    
    protected void onMouseClicked(MouseEvent m) {
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
    protected void onMouseExited(MouseEvent m) {
        System.out.println("Exit");
        if (endTime > 0 && timer == null) {
            timer = new Timer((int) Math.max(2000l, endTime - System.currentTimeMillis()), this);
            timer.setRepeats(false);
            timer.start();
        }
    }
    
    private void onMouseEntered(MouseEvent e) {
        System.out.println("Enter");
        if (timer != null) {
            timer.stop();
            timer = null;
        }
    }
    
    public void setVisible(boolean b) {
        if (b == isVisible()) return;
        super.setVisible(b);
        Toolkit.getDefaultToolkit().removeAWTEventListener(this);
        if (b) {
            
            Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.MOUSE_EVENT_MASK);
            fader.fadeIn(getFadeSpeed());
            
        } else {
            if (timer != null) {
                timer.stop();
                timer = null;
            }
            dispose();
        }
        if (b && getTimeout() > 0) {
            int to = getTimeout();
            startTimeout(to);
        }
    }
    
    protected void startTimeout(int to) {
        if (timer != null) {
            timer.stop();
        }
        endTime = System.currentTimeMillis() + to;
        if (to <= 0) {
            onClose();
            return;
        }
        timer = new Timer(to, this);
        timer.setRepeats(false);
        timer.start();
    }
    
    protected int getFadeSpeed() {
        
        return CFG_BUBBLE.FADE_ANIMATION_DURATION.getValue();
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        onClose();
    }
    
    protected int getTimeout() {
        
        return timeout;
    }
    
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public void setHeaderText(String txt) {
        headerLbl.setText(txt);
    }
    
    private Component createHeader(String caption) {
        MigPanel ret = new MigPanel("ins 0", "[grow,fill][][]", "[]");
        headerLbl = new JLabel(caption);
        headerLbl.setForeground(ColorUtils.getAlphaInstance(headerLbl.getForeground(), 160));
        headerLbl.setHorizontalAlignment(SwingConstants.LEFT);
        ret.add(SwingUtils.toBold(headerLbl));
        SwingUtils.setOpaque(ret, false);
        SwingUtils.setOpaque(headerLbl, false);
        ExtButton settings = new ExtButton() {
            {
                setToolTipText(_GUI._.Notify_createHeader_settings_tt());
                setRolloverEffectEnabled(true);
                addActionListener(new ActionListener() {
                    
                    public void actionPerformed(ActionEvent e) {
                        onSettings(e);
                        onRollOut();
                    }
                    
                });
            }
            
            /**
             * 
             */
            private static final long serialVersionUID = 1L;
            
            protected void onRollOut() {
                setContentAreaFilled(false);
                
                setIcon(IconIO.getTransparentIcon(NewTheme.I().getImage("brightmix/wrench", 16), 0.5f));
                // setIcon(NewTheme.I().getIcon("brightmix/w2", 18));
            }
            
            /**
             * 
             */
            protected void onRollOver() {
                setIcon(NewTheme.I().getIcon("brightmix/wrench", 16));
            }
            
        };
        ret.add(settings, "width 16!,height 16!");
        ExtButton closeButton = new ExtButton() {
            {
                setToolTipText(_GUI._.Notify_createHeader_close_tt());
                setRolloverEffectEnabled(true);
                addActionListener(new ActionListener() {
                    
                    public void actionPerformed(ActionEvent e) {
                        onClose();
                        onRollOut();
                    }
                    
                });
            }
            
            /**
             * 
             */
            private static final long serialVersionUID = 1L;
            
            protected void onRollOut() {
                setContentAreaFilled(false);
                setIcon(IconIO.getTransparentIcon(NewTheme.I().getImage("16/mimiglyphs/close", 10), 0.5f));
                
            }
            
            /**
             * 
             */
            protected void onRollOver() {
                setIcon(NewTheme.I().getIcon("16/mimiglyphs/close", 10));
            }
            
        };
        ret.add(closeButton, "width 16!,height 16!");
        SwingUtils.setOpaque(ret, false);
        SwingUtils.setOpaque(headerLbl, false);
        SwingUtils.setOpaque(closeButton, false);
        return ret;
    }
    
    protected void onSettings(ActionEvent action) {
        
        final AbstractBubbleSupport support = getBubbleSupport();
        
        if (support != null) {
            ExtRealCheckBoxMenuItem item;
            ExtPopupMenu popup = new ExtPopupMenu();
            List<Element> elements = support.getElements();
            if (elements != null) {
                
                for (final Element e : elements) {
                    
                    popup.add(item = new ExtRealCheckBoxMenuItem(new AppAction() {
                        {
                            
                            setName(e.getLabel());
                            setIconKey(e.getIcon());
                            setSelected(e.getKeyhandler().isEnabled());
                        }
                        
                        @Override
                        public void actionPerformed(ActionEvent e1) {
                            e.getKeyhandler().toggle();
                        }
                    }));
                    item.setHideOnClick(false);
                }
                
            }
            popup.add(new JSeparator());
            popup.add(new AppAction() {
                {
                    setName(_GUI._.bubble_hide_permanent());
                    setIconKey(IconKey.ICON_FALSE);
                }
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    try {
                        Dialog.getInstance().showConfirmDialog(0, _GUI._.lit_are_you_sure(), _GUI._.bubble_disable_rly_msg(support.getLabel()), NewTheme.I().getIcon(IconKey.ICON_QUESTION, 32), null, null);
                        support.getKeyHandler().setValue(false);
                        hideBubble(getTimeout());
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                    
                }
            });
            popup.add(new JSeparator());
            popup.add(new AppAction() {
                {
                    setName(_GUI._.bubblepopup_open_settings());
                    setIconKey(IconKey.ICON_SETTINGS);
                }
                
                @Override
                public void actionPerformed(ActionEvent e) {
                    JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
                    JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
                    JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
                    ConfigurationView.getInstance().setSelectedSubPanel(BubbleNotifyConfigPanel.class);
                }
            });
            Component source = (Component) action.getSource();
            popup.show(source, 0, -popup.getPreferredSize().height);
            return;
        }
        
        JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);
        JsonConfig.create(GraphicalUserInterfaceSettings.class).setConfigViewVisible(true);
        JDGui.getInstance().setContent(ConfigurationView.getInstance(), true);
        ConfigurationView.getInstance().setSelectedSubPanel(BubbleNotifyConfigPanel.class);
        
    }
    
    public void hideBubble(int timeout) {
        startTimeout(timeout);
        getContentComponent().stop();
        
    }
    
    protected void onClose() {
        closed = true;
        if (timer != null) {
            timer.stop();
            timer = null;
        }
        fader.fadeOut(getFadeSpeed());
        if (endLocation != null) fader.moveTo(endLocation.x, endLocation.y, getFadeSpeed());
        if (controller != null) controller.remove(this);
        // dispose();
        
    }
    
    public void setHighlightColor(Color highlightColor) {
        this.highlightColor = highlightColor;
        repaint();
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
        if (highlightColor == null) {
            p = new GradientPaint(0, 0, LAFOptions.getInstance().getColorForPanelHeaderBackground(), 0, TOP_MARGIN, LAFOptions.getInstance().getColorForScrollbarsMouseOverState());
            g2.setPaint(p);
        } else {
            p = new GradientPaint(0, 0, highlightColor, 0, TOP_MARGIN, highlightColor.brighter());
            g2.setPaint(p);
        }
        
        g2.fillRoundRect(0, 0, width, height, round, round);
        
        p = new GradientPaint(0, 0, LAFOptions.getInstance().getColorForPanelBackground(), 0, TOP_MARGIN, LAFOptions.getInstance().getColorForPanelBackground().brighter());
        g2.setPaint(p);
        
        g2.fillRect(0, TOP_MARGIN, width, height - TOP_MARGIN - BOTTOM_MARGIN);
        
        g2.setColor(LAFOptions.getInstance().getColorForPanelHeaderLine());
        
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, round, round);
        g2.dispose();
        return img;
    }
    
    public void setPreferedLocation(int x, int y) {
        fader.moveTo(x, y, getFadeSpeed());
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
    
    public void setController(Balloner balloner) {
        this.controller = balloner;
    }
    
    public float getFinalTransparency() {
        return CFG_BUBBLE.CFG.getTransparency() / 100f;
    }
    
    public void setHighlightColor(Object object) {
    }
    
}
