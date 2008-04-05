/*
 * $Id: MessageSource.java,v 1.2 2005/10/10 18:03:08 rbair Exp $
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

package org.jdesktop.swingx.event;

/**
 * Interface for MessageListener registrations methods and indicates that the
 * implementation class is a source of MessageEvents. 
 * MessageListeners which are interested in MessageEvents from this class can
 * register themselves as listeners. 
 * 
 * @see MessageEvent
 * @see MessageListener
 * @author Mark Davidson
 */
public interface MessageSource  {

    /**
     * Register the MessageListener. 
     * 
     * @param l the listener to register
     */
    void addMessageListener(MessageListener l);

    /**
     * Unregister the MessageListener from the MessageSource.
     * 
     * @param l the listener to unregister
     */
    void removeMessageListener(MessageListener l);

    /**
     * Returns an array of listeners.
     *
     * @return an non null array of MessageListeners.
     */
    MessageListener[] getMessageListeners();
}
