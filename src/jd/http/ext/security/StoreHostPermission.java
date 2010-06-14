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
 * Created on Jun 1, 2005
 */
package jd.http.ext.security;

import java.security.BasicPermission;

/**
 * Permission for restricted store access.
 */
public class StoreHostPermission extends BasicPermission {
	/**
	 * @param name
	 */
	private StoreHostPermission(String name) {
		super(name);
	}
		
	public static StoreHostPermission forURL(java.net.URL url) {
		if(LocalSecurityPolicy.isLocal(url)) {
			return new StoreHostPermission("*");
		}
		else {
			String hostName = url.getHost();
			if(hostName != null && hostName.indexOf('*') != -1) {
				throw new SecurityException("Invalid host: " + hostName);
			}
			return StoreHostPermission.forHost(hostName);
		}
	}
	
	public static StoreHostPermission forHost(String hostName) {
		//TODO What about a JAR URL or a VC URL?
		String h = hostName == null || "".equals(hostName) ? "<<local>>" : hostName;
		return new StoreHostPermission(h);
	}
}
