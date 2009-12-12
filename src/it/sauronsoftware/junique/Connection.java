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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * This class encapsulates any connection received by the server.
 * 
 * @author Carlo Pelliccia
 */
class Connection implements Runnable {

    /**
     * A plain internal synchronization object.
     */
    private final Object synchLock = new Object();

    /**
     * The lock ID.
     */
    private final String id;

    /**
     * The connection listener.
     */
    private final ConnectionListener listener;

    /**
     * The underlying socket connection.
     */
    private Socket socket;

    /**
     * The stream for reading what client sent.
     */
    private InputStream inputStream;

    /**
     * The stream for sending bytes to the client.
     */
    private OutputStream outputStream;

    /**
     * A running flag. When true the connection handling routine is started.
     */
    private boolean running = false;

    /**
     * Secondary thread executing the reading routine.
     */
    private Thread thread;

    /**
     * It builds the connection.
     * 
     * @param id
     *            The associated lock id.
     * @param socket
     *            The underlying socket connection.
     * @param listener
     *            A connection listener (required).
     */
    public Connection(String id, Socket socket, ConnectionListener listener) {
        this.id = id;
        this.socket = socket;
        this.listener = listener;
    }

    /**
     * It starts the connection handling routine.
     * 
     * @throws IllegalStateException
     *             If the connection routine has already been started.
     */
    public void start() throws IllegalStateException {
        synchronized (synchLock) {
            // Status check.
            if (running) { throw new IllegalStateException("JUnique/Server/" + id + "/Connection already started"); }
            // Running flag update.
            running = true;
            // Starts the secondary thread.
            thread = new Thread(this, "JUnique/Server/" + id + "/Connection");
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
     * It stops the connection handling routine.
     * 
     * @throws IllegalStateException
     *             If the connection routine is not started.
     */
    public void stop() throws IllegalStateException {
        synchronized (synchLock) {
            // Status check.
            if (!running) { throw new IllegalStateException("JUnique/Server/" + id + "/Connection not started"); }
            // Running flag update.
            running = false;
            // Issues an interrupt signal to the secondary thread.
            thread.interrupt();
            // Closes any underlying stream.
            try {
                inputStream.close();
            } catch (IOException e) {
            }
            try {
                outputStream.close();
            } catch (IOException e) {
            }
            try {
                socket.close();
            } catch (IOException e) {
            }
            // Waiting for server exiting.
            if (Thread.currentThread() != thread) {
                do {
                    try {
                        thread.join();
                        break;
                    } catch (InterruptedException e) {
                    }
                } while (true);
            }
            // Discards references.
            socket = null;
            inputStream = null;
            outputStream = null;
        }
    }

    /**
     * The connection handling routine.
     */
    public void run() {
        // Sends start signal.
        synchronized (synchLock) {
            synchLock.notify();
        }
        // Streams retrieval.
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            stop();
        }
        // Loop.
        while (!Thread.interrupted()) {
            try {
                String message = Message.read(inputStream);
                String response = listener.messageReceived(this, message);
                if (response == null) {
                    response = "";
                }
                Message.write(response, outputStream);
            } catch (IOException e) {
                stop();
            }
        }
        // Listener notification.
        listener.connectionClosed(this);
    }

}
