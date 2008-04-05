/*
 * $Id: JXRootPane.java,v 1.15 2008/02/15 15:08:20 kleopatra Exp $
 *
 * Copyright 2004 Sun Microsystems, Inc., 4150 Network Circle,
 * Santa Clara, California 95054, U.S.A. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package org.jdesktop.swingx;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuBar;
import javax.swing.JRootPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;

import org.jdesktop.swingx.event.MessageSource;
import org.jdesktop.swingx.event.ProgressSource;

/**
 * Extends the JRootPane by supporting specific placements for a toolbar and a
 * status bar. If a status bar exists, then toolbars, menus and any
 * MessageSource components will be registered with the status bar.
 * 
 * @see JXStatusBar
 * @see org.jdesktop.swingx.event.MessageEvent
 * @see org.jdesktop.swingx.event.MessageSource
 * @see org.jdesktop.swingx.event.ProgressSource
 * @author Mark Davidson
 */
public class JXRootPane extends JRootPane {
    protected class XRootLayout extends RootLayout {
        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension preferredLayoutSize(Container parent) {
            Dimension rd, mbd, sbd;
            Insets i = getInsets();
        
            if(contentPane != null) {
                rd = contentPane.getPreferredSize();
            } else {
                rd = parent.getSize();
            }
            if(menuBar != null && menuBar.isVisible()) {
                mbd = menuBar.getPreferredSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            if(statusBar != null && statusBar.isVisible()) {
                sbd = statusBar.getPreferredSize();
            } else {
                sbd = new Dimension(0, 0);
            }
            
            return new Dimension(Math.max(rd.width, Math.max(mbd.width, sbd.width))
                    + i.left + i.right, rd.height + mbd.height + sbd.height
                    + i.top + i.bottom);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension minimumLayoutSize(Container parent) {
            Dimension rd, mbd, sbd;
            Insets i = getInsets();
            
            if(contentPane != null) {
                rd = contentPane.getMinimumSize();
            } else {
                rd = parent.getSize();
            }
            if(menuBar != null && menuBar.isVisible()) {
                mbd = menuBar.getMinimumSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            if(statusBar != null && statusBar.isVisible()) {
                sbd = statusBar.getMinimumSize();
            } else {
                sbd = new Dimension(0, 0);
            }
            
            return new Dimension(Math.max(rd.width, Math.max(mbd.width, sbd.width))
                    + i.left + i.right, rd.height + mbd.height + sbd.height
                    + i.top + i.bottom);
        }
        
        /**
         * {@inheritDoc}
         */
        @Override
        public Dimension maximumLayoutSize(Container target) {
            Dimension rd, mbd, sbd;
            Insets i = getInsets();
            if(menuBar != null && menuBar.isVisible()) {
                mbd = menuBar.getMaximumSize();
            } else {
                mbd = new Dimension(0, 0);
            }
            if(statusBar != null && statusBar.isVisible()) {
                sbd = statusBar.getMaximumSize();
            } else {
                sbd = new Dimension(0, 0);
            }
            if(contentPane != null) {
                rd = contentPane.getMaximumSize();
            } else {
                // This is silly, but should stop an overflow error
                rd = new Dimension(Integer.MAX_VALUE, 
                        Integer.MAX_VALUE - i.top - i.bottom - mbd.height - sbd.height - 1);
            }
            
            return new Dimension(Math.min(rd.width, Math.min(mbd.width, sbd.width)) + i.left + i.right,
                                         rd.height + mbd.height + sbd.height + i.top + i.bottom);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void layoutContainer(Container parent) {
            Rectangle b = parent.getBounds();
            Insets i = getInsets();
            int contentY = 0;
            int adjustH = 0;
            int w = b.width - i.right - i.left;
            int h = b.height - i.top - i.bottom;
        
            if(layeredPane != null) {
                layeredPane.setBounds(i.left, i.top, w, h);
            }
            if(glassPane != null) {
                glassPane.setBounds(i.left, i.top, w, h);
            }
            // Note: This is laying out the children in the layeredPane,
            // technically, these are not our children.
            if(menuBar != null && menuBar.isVisible()) {
                Dimension mbd = menuBar.getPreferredSize();
                menuBar.setBounds(0, 0, w, mbd.height);
                contentY += mbd.height;
            }
            if(statusBar != null && statusBar.isVisible()) {
                Dimension sbd = statusBar.getPreferredSize();
                statusBar.setBounds(0, h - sbd.height, w, sbd.height);
                adjustH += sbd.height;
            }
            if(contentPane != null) {
                contentPane.setBounds(0, contentY, w, h - contentY - adjustH);
            }
        }
    }
    
    protected JXStatusBar statusBar;

    private JToolBar toolBar;

    /** 
     * The button that gets activated when the pane has the focus and
     * a UI-specific action like pressing the <b>ESC</b> key occurs.
     */
    private JButton cancelButton;

    public JXRootPane() {
        installKeyboardActions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Container createContentPane() {
        JComponent c = new JXPanel() {
            /**
             * {@inheritDoc}
             */
            @Override
            protected void addImpl(Component comp, Object constraints, int index) {
                synchronized (getTreeLock()) {
                    super.addImpl(comp, constraints, index);
                    registerStatusBar(comp);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void remove(int index) {
                synchronized (getTreeLock()) {
                    unregisterStatusBar(getComponent(index));
                    super.remove(index);
                }
            }

            /**
             * {@inheritDoc}
             */
            @Override
            public void removeAll() {
                synchronized (getTreeLock()) {
                    for (Component c : getComponents()) {
                        unregisterStatusBar(c);
                    }
                    
                    super.removeAll();
                }
            }
        };
        c.setName(this.getName()+".contentPane");
        c.setLayout(new BorderLayout() {
            /* This BorderLayout subclass maps a null constraint to CENTER.
             * Although the reference BorderLayout also does this, some VMs
             * throw an IllegalArgumentException.
             */
            @Override
            public void addLayoutComponent(Component comp, Object constraints) {
                if (constraints == null) {
                    constraints = BorderLayout.CENTER;
                }
                super.addLayoutComponent(comp, constraints);
            }
        });
        return c;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LayoutManager createRootLayout() {
        return new XRootLayout();
    } 

    /**
     * PENDING: move to UI
     * 
     */
    private void installKeyboardActions() {
        Action escAction = new AbstractAction() {
            public void actionPerformed(ActionEvent evt) {
                JButton cancelButton = getCancelButton();
                if (cancelButton != null) {
                    cancelButton.doClick(20);
                }
            }
            
            /**
             * Overridden to hack around #566-swing: 
             * JXRootPane eats escape keystrokes from datepicker popup.
             * Disable action if there is no cancel button.<p>
             * 
             * That's basically what RootPaneUI does - only not in 
             * the parameterless isEnabled, but in the one that passes
             * in the sender (available in UIAction only). We can't test 
             * nor compare against core behaviour, UIAction has
             * sun package scope. <p>
             * 
             * 
             */
            @Override
            public boolean isEnabled() {
                return (cancelButton != null) && (cancelButton.isEnabled());
            }
        };
        getActionMap().put("esc-action", escAction);
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        KeyStroke key = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
        im.put(key, "esc-action");
    }
    
    private void registerStatusBar(Component comp) {
        if (statusBar == null || comp == null) {
            return;
        }
        if (comp instanceof MessageSource) {
            MessageSource source = (MessageSource) comp;
//            source.addMessageListener(statusBar);
        }
        if (comp instanceof ProgressSource) {
            ProgressSource source = (ProgressSource) comp;
//            source.addProgressListener(statusBar);
        }
        if (comp instanceof Container) {
            Component[] comps = ((Container) comp).getComponents();
            for (int i = 0; i < comps.length; i++) {
                registerStatusBar(comps[i]);
            }
        }
    }

    private void unregisterStatusBar(Component comp) {
        if (statusBar == null || comp == null) {
            return;
        }
        if (comp instanceof MessageSource) {
            MessageSource source = (MessageSource) comp;
//            source.removeMessageListener(statusBar);
        }
        if (comp instanceof ProgressSource) {
            ProgressSource source = (ProgressSource) comp;
//            source.removeProgressListener(statusBar);
        }
        if (comp instanceof Container) {
            Component[] comps = ((Container) comp).getComponents();
            for (int i = 0; i < comps.length; i++) {
                unregisterStatusBar(comps[i]);
            }
        }
    }

    /**
     * Set the status bar for this root pane. Any components held by this root
     * pane will be registered. If this is replacing an existing status bar then
     * the existing component will be unregistered from the old status bar.
     * 
     * @param statusBar
     *            the status bar to use
     */
    public void setStatusBar(JXStatusBar statusBar) {
        JXStatusBar oldStatusBar = this.statusBar;
        this.statusBar = statusBar;

        if (statusBar != null) {
            if (handler == null) {
                // Create the new mouse handler and register the toolbar
                // and menu components.
//                handler = new MouseMessagingHandler(this, statusBar);
                if (toolBar != null) {
//                    handler.registerListeners(toolBar.getComponents());
                }
                if (menuBar != null) {
//                    handler.registerListeners(menuBar.getSubElements());
                }
            } else {
//                handler.setMessageListener(statusBar);
            }
        }

        Component[] comps = getContentPane().getComponents();
        for (int i = 0; i < comps.length; i++) {
            // Unregister the old status bar.
            unregisterStatusBar(comps[i]);

            // register the new status bar.
            registerStatusBar(comps[i]);
        }
        if (oldStatusBar != null) {
            remove(oldStatusBar);
        }
        if (statusBar != null) {
            add(statusBar);
        }
        firePropertyChange("statusBar", oldStatusBar, getStatusBar());
    }

    public JXStatusBar getStatusBar() {
        return statusBar;
    }

    private MouseMessagingHandler handler;

    /**
     * Set the toolbar bar for this root pane. If the status bar exists, then
     * all components will be registered with a
     * <code>MouseMessagingHandler</code> so that mouse over messages will be
     * sent to the status bar. If a tool bar is currently registered with this
     * {@code JXRootPane}, then it is removed prior to setting the new tool
     * bar. If an implementation needs to handle more than one tool bar, a
     * subclass will need to override the singleton logic used here or manually
     * add toolbars with {@code getContentPane().add}.
     * 
     * @param toolBar
     *            the toolbar to register
     * @see MouseMessagingHandler
     */
    public void setToolBar(JToolBar toolBar) {
        JToolBar oldToolBar = getToolBar();
        this.toolBar = toolBar;

        if (oldToolBar != null) {
            getContentPane().remove(oldToolBar);
            
            if (handler != null) {
                handler.unregisterListeners(oldToolBar.getComponents());
            }
        }
        
        if (handler != null && this.toolBar != null) {
            handler.registerListeners(this.toolBar.getComponents());
        }

        getContentPane().add(BorderLayout.NORTH, this.toolBar);
        
        //ensure the new toolbar is correctly sized and displayed
        getContentPane().validate();
        
        firePropertyChange("toolBar", oldToolBar, getToolBar());
    }

    public JToolBar getToolBar() {
        return toolBar;
    }

    /**
     * Set the menu bar for this root pane. If the status bar exists, then all
     * components will be registered with a <code>MouseMessagingHandler</code>
     * so that mouse over messages will be sent to the status bar.
     * 
     * @param menuBar
     *            the menu bar to register
     * @see MouseMessagingHandler
     */
    @Override
    public void setJMenuBar(JMenuBar menuBar) {
        JMenuBar oldMenuBar = this.menuBar;

        super.setJMenuBar(menuBar);

        if (handler != null && oldMenuBar != null) {
            handler.unregisterListeners(oldMenuBar.getSubElements());
        }

        if (handler != null && menuBar != null) {
            handler.registerListeners(menuBar.getSubElements());
        }
    }

    /**
     * Sets the <code>cancelButton</code> property,
     * which determines the current default cancel button for this <code>JRootPane</code>.
     * The cancel button is the button which will be activated 
     * when a UI-defined activation event (typically the <b>ESC</b> key) 
     * occurs in the root pane regardless of whether or not the button 
     * has keyboard focus (unless there is another component within 
     * the root pane which consumes the activation event,
     * such as a <code>JTextPane</code>).
     * For default activation to work, the button must be an enabled
     * descendent of the root pane when activation occurs.
     * To remove a cancel button from this root pane, set this
     * property to <code>null</code>.
     *
     * @param cancelButton the <code>JButton</code> which is to be the cancel button
     * @see #getCancelButton() 
     *
     * @beaninfo
     *  description: The button activated by default for cancel actions in this root pane
     */
    public void setCancelButton(JButton cancelButton) { 
        JButton old = this.cancelButton;

        if (old != cancelButton) {
            this.cancelButton = cancelButton;

            if (old != null) {
                old.repaint();
            }
            if (cancelButton != null) {
                cancelButton.repaint();
            } 
        }

        firePropertyChange("cancelButton", old, cancelButton);        
    }

    /**
     * Returns the value of the <code>cancelButton</code> property. 
     * @return the <code>JButton</code> which is currently the default cancel button
     * @see #setCancelButton
     */
    public JButton getCancelButton() { 
        return cancelButton;
    }

}
