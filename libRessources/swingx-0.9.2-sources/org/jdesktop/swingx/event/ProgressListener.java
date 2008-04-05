/*
 * $Id: ProgressListener.java,v 1.3 2005/10/10 18:03:09 rbair Exp $
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
 * The listener interface for recieving progress events.
 * The class interested in handling {@link ProgressEvent}s should implement
 * this interface. The complementary interface would be {@link MessageSource}
 *
 * @see ProgressEvent
 * @see MessageSource
 * @author Mark Davidson
 */
public interface ProgressListener extends java.util.EventListener {

    /**
     * Indicates the start of a long operation. The <code>ProgressEvent</code>
     * will indicate if this is a determinate or indeterminate operation.
     *
     * @param evt an object which describes the event
     */
    void progressStarted(ProgressEvent evt);


    /**
     * Indicates that the operation has stopped.
     */
    void progressEnded(ProgressEvent evt);

    /**
     * Invoked when an increment of progress is sent. This may not be
     * sent if an indeterminate progress has started.
     *
     * @param evt an object which describes the event
     */
    void progressIncremented(ProgressEvent evt);
}
