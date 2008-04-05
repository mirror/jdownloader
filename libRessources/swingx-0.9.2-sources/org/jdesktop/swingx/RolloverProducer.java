/*
 * $Id: RolloverProducer.java,v 1.10 2005/10/24 13:20:42 kleopatra Exp $
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

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import javax.swing.JComponent;

/**
 * Mouse/Motion/Listener which stores mouse location as 
 * client property in the target JComponent.
 * 
 * Note: assumes that the component it is listening to is 
 * of type JComponent!
 * 
 * @author Jeanette Winzenburg
 */
public class RolloverProducer implements MouseListener, MouseMotionListener {

    //----------------- mouseListener
        
        public static final String CLICKED_KEY = "swingx.clicked";
        public static final String ROLLOVER_KEY = "swingx.rollover";
//        public static final String PRESSED_KEY = "swingx.pressed";
        
        
        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            
        }

        public void mouseReleased(MouseEvent e) {
            updateRollover(e, CLICKED_KEY);
            
        }

        public void mouseEntered(MouseEvent e) {
            updateRollover(e, ROLLOVER_KEY);
        }


        public void mouseExited(MouseEvent e) {
            if (e.getSource() instanceof JComponent) {
                ((JComponent) e.getSource()).putClientProperty(ROLLOVER_KEY, null);
                ((JComponent) e.getSource()).putClientProperty(CLICKED_KEY, null);
//                ((JComponent) e.getSource()).putClientProperty(PRESSED_KEY, null);
            }
            
        }

//---------------- MouseMotionListener
        public void mouseDragged(MouseEvent e) {
            // TODO Auto-generated method stub
            
        }

        public void mouseMoved(MouseEvent e) {
            updateRollover(e, ROLLOVER_KEY);
        }

        protected void updateRollover(MouseEvent e, String property) {
            updateRolloverPoint((JComponent) e.getComponent(), e.getPoint());
            updateClientProperty((JComponent) e.getSource(), property);
        }

        protected Point rollover = new Point(-1, -1);
        
        protected void updateClientProperty(JComponent component, String property) {
            Point p = (Point) component.getClientProperty(property);
            if (p == null || (rollover.x != p.x) || (rollover.y != p.y)) {
                component.putClientProperty(property, new Point(rollover));
            }
        }

        /**
         * Subclasses must override to map the given mouse coordinates into
         * appropriate client coordinates. The result must be stored in the 
         * rollover field. 
         * 
         * Here: does nothing.
         * 
         * @param component
         * @param mousePoint
         */
        protected void updateRolloverPoint(JComponent component, Point mousePoint) {
            
        }
        
        
    }