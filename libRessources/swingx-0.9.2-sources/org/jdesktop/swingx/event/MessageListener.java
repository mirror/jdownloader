/*
 * $Id: MessageListener.java,v 1.3 2005/10/10 18:03:07 rbair Exp $
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
 * The listener interface for recieving message events.
 * The class interested in handling {@link MessageEvent}s should implement
 * this interface. The complementary interface would be {@link MessageSource}
 * 
 * @see MessageEvent
 * @see MessageSource
 * @author Mark Davidson
 */
public interface MessageListener extends java.util.EventListener {

    /**
     * Invoked to send a message to a listener. The {@link MessageEvent}
     * is qualified depending on context. It may represent a simple
     * transient messages to be passed to the ui or it could
     * represent a serious exception which has occured during 
     * processing. 
     * <p>
     * The implementation of this interface should come up 
     * with a consistent policy to reflect the business logic
     * of the application.
     * 
     * @param evt an object which describes the message
     */
    void message(MessageEvent evt);
}
