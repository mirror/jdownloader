/*
 * $Id: MailTransportProxy.java,v 1.3 2006/04/20 00:15:20 gfx Exp $
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
package org.jdesktop.swingx.util;

import java.util.List;

/**
 * This is a proxy interface to allow usage of the JDIC mail transport for error logging
 * without adding dependensies on the JDIC itaelf.
 *
 * @author Alexander Zuev
 * @version 1.0
 */
public interface MailTransportProxy {
    /**
     * Compose and send message
     * @param toAddr List of addresses to whom to send this mesage
     * @param ccAddr List of addresses to whom to carbon-copy this message
     * @param subject Message subject
     * @param body Message main text
     * @param attach Pathis to files that needs to be send in attachment with this message
     */
    public void mailMessage(List<String> toAddr, List<String> ccAddr,
                            String subject, String body, List<String> attach) throws Error;
}
