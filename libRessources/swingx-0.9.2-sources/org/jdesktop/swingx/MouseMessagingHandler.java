/*
 * $Id: MouseMessagingHandler.java,v 1.3 2005/10/26 14:29:53 kleopatra Exp $
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

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.logging.Level;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.MenuElement;

import org.jdesktop.swingx.event.MessageEvent;
import org.jdesktop.swingx.event.MessageListener;
import org.jdesktop.swingx.event.MessageSource;

/**
 * Mouse handler which listens to mouse entered events on action based components 
 * and send the value of the LONG_DESCRIPTION as a transient message
 * to the MessageListener and the listeners registered to the MessageSource.
 * <p>
 * Components can be registered using the register methods. For example, to 
 * register all the components of a toolbar and all menu items in a menu bar
 * to send mouse over messages to a status bar:
 * <pre>
 *    handler = new MouseMessagingHandler(this, statusBar);
 *    if (toolBar != null) {
 *        handler.registerListeners(toolBar.getComponents());
 *    }
 *    if (menuBar != null) {
 *	  handler.registerListeners(menuBar.getSubElements());
 *    }
 * </pre>
 * 
 * @author Mark Davidson
 */
public class MouseMessagingHandler extends MouseAdapter {

    private Object source;
    private MessageSource messageSource;
    private MessageListener messageListener;

    /**
     * @param source the source of the MesageEvents
     * @param messageSource messages will be sent to these listeners
     */
    public MouseMessagingHandler(Object source, MessageSource messageSource) {
	setSource(source);
	setMessageSource(messageSource);
    }
    
    /**
     * @param source the source of the MesageEvents
     * @param messageListener messages will be sent to this listener
     */
    public MouseMessagingHandler(Object source, MessageListener messageListener) {
	setSource(source);
	setMessageListener(messageListener);
    }

    /**
     * Set the source object of the MessageEvents.
     */
    public void setSource(Object source) {
	this.source = source;
    }

    public void setMessageSource(MessageSource source) {
	this.messageSource = source;
    }

    public void setMessageListener(MessageListener listener) {
	this.messageListener = listener;
    }

    public void mouseExited(MouseEvent evt) {
	fireMessage("");
    }

    /**
     * Takes the LONG_DESCRIPTION of the Action based components
     * and sends them to the Status bar
     */
    public void mouseEntered(MouseEvent evt) {
	if (evt.getSource()instanceof AbstractButton) {
	    AbstractButton button = (AbstractButton) evt.getSource();
	    Action action = button.getAction();
	    if (action != null) {
		fireMessage((String)action.getValue(Action.LONG_DESCRIPTION));
	    }
	}
    }

    /**
     * Helper method to recursively register all MenuElements with
     * this messaging handler.
     */
    public void registerListeners(MenuElement[] elements) {
	for (int i = 0; i < elements.length; i++) {
	    if (elements[i] instanceof AbstractButton) {
		((AbstractButton)elements[i]).addMouseListener(this);
	    }
	    registerListeners(elements[i].getSubElements());
	}
    }

    public void unregisterListeners(MenuElement[] elements) {
	for (int i = 0; i < elements.length; i++) {
	    if (elements[i] instanceof AbstractButton) {
		((AbstractButton)elements[i]).removeMouseListener(this);
	    }
	    unregisterListeners(elements[i].getSubElements());
	}
    }

    /**
     * Helper method to register all components with this message handler
     */
    public void registerListeners(Component[] components) {
	for (int i = 0; i < components.length; i++) {
	    if (components[i] instanceof AbstractButton) {
		components[i].addMouseListener(this);
	    }
	}
    }

    public void unregisterListeners(Component[] components) {
	for (int i = 0; i < components.length; i++) {
	    if (components[i] instanceof AbstractButton) {
		components[i].removeMouseListener(this);
	    }
	}
    }

    private void fireMessage(String message) {
	MessageEvent evt = new MessageEvent(source, message, Level.FINE);
	    
	if (messageListener != null) {
	    messageListener.message(evt);
	}
		
	if (messageSource != null) {
	    MessageListener[] ls = messageSource.getMessageListeners();
	    for (int i = 0; i < ls.length; i++) {
		ls[i].message(evt);
	    }
	}
    }
}


