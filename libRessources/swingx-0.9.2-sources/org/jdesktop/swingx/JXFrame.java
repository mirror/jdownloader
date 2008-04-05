/*
 * $Id: JXFrame.java,v 1.18 2008/02/15 15:08:20 kleopatra Exp $
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

import org.jdesktop.swingx.util.WindowUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;


/**
 * A smarter JFrame specifically used for top level frames for Applications.
 * This frame uses a JXRootPane.
 */
public class JXFrame extends JFrame {
    public enum StartPosition {CenterInScreen, CenterInParent, Manual}
    
    private Component waitPane = null;
    private Component glassPane = null;
    private boolean waitPaneVisible = false;
    private Cursor realCursor = null;
    private boolean waitCursorVisible = false;
    private boolean waiting = false;
    private StartPosition startPosition;
    private boolean hasBeenVisible = false; //startPosition is only used the first time the window is shown
    private AWTEventListener keyEventListener; //for listening to KeyPreview events
    private boolean keyPreview = false;
    private AWTEventListener idleListener; //for listening to events. If no events happen for a specific amount of time, mark as idle
    private Timer idleTimer;
    private long idleThreshold = 0;
    private boolean idle;
    
    public JXFrame() {
        this(null, false);
    }
    
    public JXFrame(String title, boolean exitOnClose) {
        super(title);
        if (exitOnClose) {
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
        
        //create the event handler for key preview functionality
        keyEventListener = new AWTEventListener() {
            public void eventDispatched(AWTEvent aWTEvent) {
                if (aWTEvent instanceof KeyEvent) {
                    KeyEvent evt = (KeyEvent)aWTEvent;
                    for (KeyListener kl : getKeyListeners()) {
                        int id = aWTEvent.getID();
                        switch (id) {
                            case KeyEvent.KEY_PRESSED:
                                kl.keyPressed(evt);
                                break;
                            case KeyEvent.KEY_RELEASED:
                                kl.keyReleased(evt);
                                break;
                            case KeyEvent.KEY_TYPED:
                                kl.keyTyped(evt);
                                break;
                            default:
                                System.err.println("Unhandled Key ID: " + id);    
                        }
                    }
                }
            }
        };
        
        idleTimer = new Timer(100, new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                setIdle(true);
            }
        });
        
        //create the event handler for key preview functionality
        idleListener = new AWTEventListener() {
            public void eventDispatched(AWTEvent aWTEvent) {
                //reset the timer
                idleTimer.stop();
                //if the user is idle, then change to not idle
                if (isIdle()) {
                    setIdle(false);
                }
                //start the timer
                idleTimer.restart();
            }
        };
    }

    public JXFrame(String title) {
        this(title, false);
    }
    
    public void setCancelButton(JButton button) {
        getRootPaneExt().setCancelButton(button);
    }
    
    public JButton getCancelButton() {
        return getRootPaneExt().getCancelButton();
    }
    
    public void setDefaultButton(JButton button) {
        JButton old = getDefaultButton();
        getRootPane().setDefaultButton(button);
        firePropertyChange("defaultButton", old, getDefaultButton());
    }
    
    public JButton getDefaultButton() {
        return getRootPane().getDefaultButton();
    }
    
    public void setKeyPreview(boolean flag) {
        Toolkit.getDefaultToolkit().removeAWTEventListener(keyEventListener);
        if (flag) {
            Toolkit.getDefaultToolkit().addAWTEventListener(keyEventListener, AWTEvent.KEY_EVENT_MASK);
        }
        boolean old = keyPreview;
        keyPreview = flag;
        firePropertyChange("keyPreview", old, keyPreview);
    }
    
    public final boolean getKeyPreview() {
        return keyPreview;
    }
    
    public void setStartPosition(StartPosition position) {
        StartPosition old = getStartPosition();
        this.startPosition = position;
        firePropertyChange("startPosition", old, getStartPosition());
    }
    
    public StartPosition getStartPosition() {
        return startPosition == null ? StartPosition.Manual : startPosition;
    }
    
    public void setWaitCursorVisible(boolean flag) {
        boolean old = isWaitCursorVisible();
        if (flag != old) {
            waitCursorVisible = flag;
            if (isWaitCursorVisible()) {
                realCursor = getCursor();
                super.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            } else {
                super.setCursor(realCursor);
            }
            firePropertyChange("waitCursorVisible", old, isWaitCursorVisible());
        }
    }
    
    public boolean isWaitCursorVisible() {
        return waitCursorVisible;
    }
    
    @Override
    public void setCursor(Cursor c) {
        if (!isWaitCursorVisible()) {
            super.setCursor(c);
        } else {
            this.realCursor = c;
        }
    }
    
    public void setWaitPane(Component c) {
        Component old = getWaitPane();
        this.waitPane = c;
        firePropertyChange("waitPane", old, getWaitPane());
    }
    
    public Component getWaitPane() {
        return waitPane;
    }
    
    public void setWaitPaneVisible(boolean flag) {
        boolean old = isWaitPaneVisible();
        if (flag != old) {
            this.waitPaneVisible = flag;
            Component wp = getWaitPane();
            if (isWaitPaneVisible()) {
                glassPane = getRootPane().getGlassPane();
                if (wp != null) {
                    getRootPane().setGlassPane(wp);
                    wp.setVisible(true);
                }
            } else {
                if (wp != null) {
                    wp.setVisible(false);
                }
                getRootPane().setGlassPane(glassPane);
            }
            firePropertyChange("waitPaneVisible", old, isWaitPaneVisible());
        }
    }
    
    public boolean isWaitPaneVisible() {
        return waitPaneVisible;
    }
    
    public void setWaiting(boolean waiting) {
        boolean old = isWaiting();
        this.waiting = waiting;
        firePropertyChange("waiting", old, isWaiting());
        setWaitPaneVisible(waiting);
        setWaitCursorVisible(waiting);
    }
    
    public boolean isWaiting() {
        return waiting;
    }
    
    @Override
    public void setVisible(boolean visible) {
        if (!hasBeenVisible && visible) {
            //move to the proper start position
            StartPosition pos = getStartPosition();
            switch (pos) {
                case CenterInParent:
                    setLocationRelativeTo(getParent());
                    break;
                case CenterInScreen:
                    setLocation(WindowUtils.getPointForCentering(this));
                    break;
                case Manual:
                default:
                    //nothing to do!
            }
        }
        super.setVisible(visible);
    }
    
    public boolean isIdle() {
        return idle;
    }
    
    public void setIdle(boolean idle) {
        boolean old = isIdle();
        this.idle = idle;
        firePropertyChange("idle", old, isIdle());
    }
    
    public void setIdleThreshold(long threshold) {
        long old = getIdleThreshold();
        this.idleThreshold = threshold;
        firePropertyChange("idleThreshold", old, getIdleThreshold());
        
        threshold = getIdleThreshold(); // in case the getIdleThreshold method has been overridden
        
        Toolkit.getDefaultToolkit().removeAWTEventListener(idleListener);
        if (threshold > 0) {
            Toolkit.getDefaultToolkit().addAWTEventListener(idleListener, AWTEvent.KEY_EVENT_MASK | AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.MOUSE_WHEEL_EVENT_MASK);
        }
        idleTimer.stop();
        idleTimer.setInitialDelay((int)threshold);
        idleTimer.restart();
    }
    
    public long getIdleThreshold() {
        return idleThreshold;
    }
    
    public void setStatusBar(JXStatusBar statusBar) {
        getRootPaneExt().setStatusBar(statusBar);
    }
    
    public JXStatusBar getStatusBar() {
        return getRootPaneExt().getStatusBar();
    }
    
    public void setToolBar(JToolBar toolBar) {
        getRootPaneExt().setToolBar(toolBar);
    }
    
    public JToolBar getToolBar() {
        return getRootPaneExt().getToolBar();
    }
    
    //---------------------------------------------------- Root Pane Methods
    /**
     * Overloaded to create a JXRootPane.
     */
    @Override
    protected JRootPane createRootPane() {
        return new JXRootPane();
    }

    /**
     * Overloaded to make this public.
     */
    @Override
    public void setRootPane(JRootPane root) {
        super.setRootPane(root);
    }

    /**
     * Return the extended root pane. If this frame doesn't contain
     * an extended root pane the root pane should be accessed with
     * getRootPane().
     *
     * @return the extended root pane or null.
     */
    public JXRootPane getRootPaneExt() {
        if (rootPane instanceof JXRootPane) {
            return (JXRootPane)rootPane;
        }
        return null;
    }
}

