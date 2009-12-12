/*
 * JUnique - Helps in preventing multiple instances of the same application
 * 
 * Copyright (C) 2008 Carlo Pelliccia (www.sauronsoftware.it)
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.sauronsoftware.junique;

/**
 * This interface is used by {@link Connection} class to notify connection
 * related events.
 * 
 * @author Carlo Pelliccia
 */
interface ConnectionListener {

    /**
     * This method is called when an incoming message is received.
     * 
     * @param connection
     *            The source connection.
     * @param message
     *            The message received.
     * @return An optional response (may be null).
     */
    public String messageReceived(Connection connection, String message);

    /**
     * This method is called to notify that the connection with the remote side
     * has been closed.
     * 
     * @param connection
     *            The source connection.
     */
    public void connectionClosed(Connection connection);

}
