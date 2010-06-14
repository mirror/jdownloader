/*
    GNU GENERAL PUBLIC LICENSE
    Copyright (C) 2006 The Lobo Project

    This program is free software; you can redistribute it and/or
    modify it under the terms of the GNU General Public
    License as published by the Free Software Foundation; either
    verion 2 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    General Public License for more details.

    You should have received a copy of the GNU General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA

    Contact info: lobochief@users.sourceforge.net
*/
/*
 * Created on Jun 6, 2005
 */
package jd.http.ext.security;

import java.security.Permission;

/**
 * @author J. H. S.
 */
public class HistoryPermission extends Permission {
	/**
	 * @param name
	 */
	public HistoryPermission() {
		super("");
	}

	/* (non-Javadoc)
	 * @see java.security.Permission#implies(java.security.Permission)
	 */
	public boolean implies(Permission permission) {
		return permission instanceof HistoryPermission;
	}

	/* (non-Javadoc)
	 * @see java.security.Permission#getActions()
	 */
	public String getActions() {
		return null;
	}

	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return obj instanceof HistoryPermission; 
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		return 1000;
	}
}
