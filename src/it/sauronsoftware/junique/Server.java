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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

/**
 * A LockServer is started every time a lock has been taken by a caller. A
 * LockServer listens on a local port for messages sent on the lock channel.
 * Once a message is received the server delivers it to the
 * {@link MessageHandler} supplied by the caller.
 * 
 * @author Carlo Pelliccia
 */
class Server implements Runnable, ConnectionListener {

    /**
     * The lock id.
     */
    private final String id;

    /**
     * The message handler used by this server. It could be null if who has
     * taken the lock is not interested in message handling.
     */
    private final MessageHandler messageHandler;

    /**
     * A synchronization lock, used internally.
     */
    private final Object synchLock = new Object();

    /**
     * The underlying server socket listened by this server.
     */
    private ServerSocket serverSocket = null;

    /**
     * The thread used to run the connections handling routine.
     */
    private Thread thread = null;

    /**
     * Established connections.
     */
    private final ArrayList<Connection> connections = new ArrayList<Connection>();

    /**
     * It builds a ready-to-start lock server.
     * 
     * @param id
     *            The lock id.
     * @param messageHandler
     *            The message handler used by this server. It could be null if
     *            who has taken the lock is not interested in message handling.
     */
    public Server(final String id, final MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
        this.id = id;
    }

    /**
     * It starts the server.
     * 
     * @throws IllegalStateException
     *             If this server is already started.
     */
    public void start() throws IllegalStateException {
        synchronized (synchLock) {
            // Tests the server status by checking the serverSocket property
            // value.
            if (serverSocket != null) { throw new IllegalStateException("JUnique/Server for id \"" + id + "\" already started"); }
            // Opens the server socket.
            try {
                serverSocket = new ServerSocket();
                final InetAddress localhost = InetAddress.getByName(null);
                final SocketAddress socketAddress = new InetSocketAddress(localhost, 0);
                serverSocket.bind(socketAddress);
            } catch (IOException e) {
                // Release the server socket.
                if (serverSocket != null) {
                    try {
                        serverSocket.close();
                    } catch (Throwable t) {
                    }
                }
                // Throws a runtime exception.
                throw new RuntimeException("Unexpected IOException while starting " + "JUnique/Server for id \"" + id + "\"", e);
            }
            // Starts the connections handling routine.
            thread = new Thread(this, "JUnique/Server/" + id);
            thread.setDaemon(true);
            thread.start();
            // Waits for start signal.
            do {
                try {
                    synchLock.wait();
                    break;
                } catch (InterruptedException e) {
                }
            } while (true);
        }
    }

    /**
     * It stops the server.
     */
    public void stop() {
        synchronized (synchLock) {
            // Tests the server status by checking the serverSocket property
            // value.
            if (serverSocket == null) { throw new IllegalStateException("JUnique/Server for id \"" + id + "\" not started"); }
            // Stops any ongoing communication.
            while (connections.size() > 0) {
                Connection c = connections.remove(0);
                c.stop();
            }
            // Issues an interrupt signal to the secondary thread.
            thread.interrupt();
            // Close the serverSocket.
            try {
                serverSocket.close();
            } catch (IOException e) {
            }
            // Waiting for server exiting.
            do {
                try {
                    thread.join();
                    break;
                } catch (InterruptedException e) {
                }
            } while (true);
            // Discards references.
            serverSocket = null;
        }
    }

    /**
     * Connections handling routine, running on a separate thread.
     */
    public void run() {
        // Sends start signal.
        synchronized (synchLock) {
            synchLock.notify();
        }
        // Multiple connections handled with multi-threading.
        while (!Thread.interrupted()) {
            try {
                final Socket incoming = serverSocket.accept();
                final Connection c = new Connection(id, incoming, this);
                synchronized (synchLock) {
                    connections.add(c);
                    c.start();
                }
            } catch (Throwable t) {
            }
        }
    }

    /**
     * It returns the port on which by this server is listening.
     * 
     * @return The port on which by this server is listening.
     * @throws IllegalStateException
     *             If the server is not started.
     */
    public int getListenedPort() throws IllegalStateException {
        synchronized (synchLock) {
            // Tests the server status by checking the serverSocket property
            // value.
            if (serverSocket == null) { throw new IllegalStateException("JUnique/Server for id \"" + id + "\" not started"); }
            // Returns the value.
            return serverSocket.getLocalPort();
        }
    }

    /**
     * Called to notify a connection close.
     */
    public void connectionClosed(Connection connection) {
        synchronized (connections) {
            connections.remove(connection);
        }
    }

    /**
     * Handle a message reception.
     */
    public String messageReceived(Connection connection, String message) {
        if (messageHandler != null) {
            synchronized (messageHandler) { // Thread-safe
                return messageHandler.handle(message);
            }
        }
        return null;
    }

}
