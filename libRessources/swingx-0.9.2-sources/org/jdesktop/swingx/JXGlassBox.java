/*
 * $Id: JXGlassBox.java,v 1.5 2007/12/13 11:23:42 stolis Exp $
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

import java.applet.Applet;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;




/**
 * Component used to display transluscent user-interface content.
 * This component and all of its content will be displayed with the specified
 * &quot;alpha&quot; transluscency property value.  When this component is made visible,
 * it's content will fade in until the alpha transluscency level is reached.
 * <p>
 * If the glassbox's &quot;dismissOnClick&quot; property is <code>true</code>
 * (the default) then the glassbox will be made invisible when the user
 * clicks on it.</p>
 * <p>
 * This component is particularly useful for displaying transient messages
 * on the glasspane.</p>
 *
 * @author Amy Fowler
 * @version 1.0
 */

public class JXGlassBox extends JXPanel {
    private static final int SHOW_DELAY = 30; // ms
    private static final int TIMER_INCREMENT = 10; // ms

    private float alphaStart = 0.01f;
    private float alphaEnd = 0.8f;

    private Timer animateTimer;
    private float alphaIncrement = 0.02f;

    private boolean dismissOnClick = false;
    private MouseAdapter dismissListener = null;

    public JXGlassBox() {
        setOpaque(false);
        setAlpha(alphaStart);
        setBackground(Color.white);
        setDismissOnClick(true);

        animateTimer = new Timer(TIMER_INCREMENT, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setAlpha(Math.min(alphaEnd, getAlpha() + alphaIncrement));
            }
        });
    }

    public JXGlassBox(float alpha) {
        this();
        setAlpha(alpha);
    }

    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        this.alphaIncrement = (alphaEnd - alphaStart)/(SHOW_DELAY/TIMER_INCREMENT);
    }

    public void setDismissOnClick(boolean dismissOnClick) {
        boolean oldDismissOnClick = this.dismissOnClick;
        this.dismissOnClick = dismissOnClick;
        if (dismissOnClick && !oldDismissOnClick) {
            if (dismissListener == null) {
                dismissListener = new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        JComponent glassBox = JXGlassBox.this;
                        JComponent parent = (JComponent) glassBox.getParent();
                        Container toplevel = parent.getTopLevelAncestor();
                        parent.remove(glassBox);
                        toplevel.validate();
                        toplevel.repaint();
                    }
                };
            }
            addMouseListener(dismissListener);
        }
        else if (!dismissOnClick && oldDismissOnClick) {
            removeMouseListener(dismissListener);
        }
    }

    public void paint(Graphics g) {
        super.paint(g);
        if (!animateTimer.isRunning() && getAlpha() < alphaEnd ) {
            animateTimer.start();
        }
        if (animateTimer.isRunning() && getAlpha() >= alphaEnd) {
            animateTimer.stop();
        }
    }

    public void setVisible(boolean visible) {
        setAlpha(alphaStart);
        super.setVisible(visible);
    }

    private Container getTopLevel() {
        Container p = getParent();
        while (p != null && !(p instanceof Window || p instanceof Applet)) {
            p = p.getParent();
        }
        return p;
    }

    public void showOnGlassPane(Container glassPane, Component component,
                                int componentX, int componentY, int positionHint) {
        Dimension boxPrefSize = getPreferredSize();
        Dimension glassSize = glassPane.getSize();
        Rectangle compRect = component.getBounds();
        int boxX = 0;
        int boxY = 0;
        int boxWidth = Math.min(boxPrefSize.width, glassSize.width);
        int boxHeight = Math.min(boxPrefSize.height, glassSize.height);

        Point compLocation = SwingUtilities.convertPoint(component.getParent(),
                                                compRect.x, compRect.y,
                                                glassPane);

        if (positionHint == SwingConstants.TOP) {
            if (compLocation.x + componentX + boxWidth <= glassSize.width) {
                boxX = compLocation.x + componentX;
            } else {
                boxX = glassSize.width - boxWidth;
            }
            boxY = compLocation.y - boxHeight;
            if (boxY < 0) {
                if (compLocation.y + compRect.height <= glassSize.height) {
                    boxY = compLocation.y + compRect.height;
                }
                else {
                    boxY = 0;
                }
            }
        }

        glassPane.setLayout(null);
        setBounds(boxX, boxY, boxWidth, boxHeight);
        glassPane.add(this);
        glassPane.setVisible(true);

        Container topLevel = getTopLevel();
        topLevel.validate();
        topLevel.repaint();

    }

    public void showOnGlassPane(Container glassPane, int originX, int originY) {
        Dimension boxPrefSize = getPreferredSize();
        Dimension glassSize = glassPane.getSize();
        int boxX = 0;
        int boxY = 0;
        int boxWidth = 0;
        int boxHeight = 0;

        boxWidth = Math.min(boxPrefSize.width, glassSize.width);
        boxHeight = Math.min(boxPrefSize.height, glassSize.height);

        if (originY - boxHeight >= 0) {
            boxY = originY - boxHeight;
        } else if (originY + boxHeight <= glassSize.height) {
            boxY = originY;
        } else {
            boxY = glassSize.height - boxHeight;
        }

        if (originX + boxWidth <= glassSize.width) {
            boxX = originX;
        } else if (originX >= boxWidth) {
            boxX = originX - boxWidth;
        } else {
            boxX = glassSize.width - boxWidth;
        }

        glassPane.setLayout(null);
        setBounds(boxX, boxY, boxWidth, boxHeight);
        glassPane.add(this);
        glassPane.setVisible(true);

        Container topLevel = getTopLevel();
        topLevel.validate();
        topLevel.repaint();
    }

}