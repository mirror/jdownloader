/*
 * $Id: ErrorReporter.java,v 1.2 2007/11/25 15:52:59 kschaefe Exp $
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

package org.jdesktop.swingx.error;

/**
 * <p>ErrorReporter is used by {@link org.jdesktop.swingx.JXErrorPane} to
 * implement a pluggable error reporting API. For example, a
 * <code>JXErrorPane</code> may use an {@link EmailErrorReporter}, or a 
 * {@link LogErrorReporter}, or perhaps even an
 * <code>RSSErrorReporter</code>.</p>
 *
 * @status REVIEWED
 * @author Alexander Zuev
 * @author rbair
 */
public interface ErrorReporter {
    /**
     * <p>Reports an error based on the given {@link ErrorInfo}. This
     * method may be a long running method, and so should not block the EDT in
     * any way. If an error occurs while reporting the error, it <strong>must not</strong>
     * throw an exception from this method. If an error dialog causes another error,
     * it should be silently swallowed. If proper heuristics can be used, an attempt
     * can be made some time later to re-report failed error reports, but such attempts
     * should be transparent to the user.</p>
     *
     * @param info encapsulates all information to report using this facility. Must not be null.
     * @exception thrown if the info param is null
     */
    public void reportError(ErrorInfo info) throws NullPointerException;
}