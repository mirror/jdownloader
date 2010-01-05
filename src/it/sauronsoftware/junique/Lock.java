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

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

/**
 * Lock structure. It is internally used by JUnique to put together the elements
 * of a JUnique taken lock.
 * 
 * @author Carlo Pelliccia
 */
class Lock {

	/**
	 * The lock id.
	 */
	private final String id;

	/**
	 * The lock file.
	 */
	private final File lockFile;

	/**
	 * The port file.
	 */
	private final File portFile;

	/**
	 * The channel associated to the lock file.
	 */
	private final FileChannel lockFileChannel;

	/**
	 * The file lock taken on the lock file.
	 */
	private final FileLock lockFileLock;

	/**
	 * The server handling message reception for this lock.
	 */
	private final Server server;

	/**
	 * It builds the lock representation.
	 * 
	 * @param id
	 *            The lock id.
	 * @param lockFile
	 *            The lock file.
	 * @param lockFileChannel
	 *            The channel associated to the lock file.
	 * @param lockFileLock
	 *            The file lock taken on the lock file.
	 * @param server
	 *            The server handling message reception for this lock.
	 */
	Lock(final String id, final File lockFile, final File portFile, final FileChannel lockFileChannel,
			final FileLock lockFileLock, final Server server) {
		super();
		this.id = id;
		this.lockFile = lockFile;
		this.portFile = portFile;
		this.lockFileChannel = lockFileChannel;
		this.lockFileLock = lockFileLock;
		this.server = server;
	}

	/**
	 * It returns the lock id.
	 * 
	 * @return The lock id.
	 */
	public String getId() {
		return id;
	}

	/**
	 * It returns the lock file.
	 * 
	 * @return The lock file.
	 */
	public File getLockFile() {
		return lockFile;
	}

	/**
	 * It returns the port file.
	 * 
	 * @return The port file.
	 */
	public File getPortFile() {
		return portFile;
	}

	/**
	 * It returns the channel associated to the lock file.
	 * 
	 * @return The channel associated to the lock file.
	 */
	public FileChannel getLockFileChannel() {
		return lockFileChannel;
	}

	/**
	 * It returns the file lock taken on the lock file.
	 * 
	 * @return The file lock taken on the lock file.
	 */
	public FileLock getLockFileLock() {
		return lockFileLock;
	}

	/**
	 * It returns the server handling message reception for this lock.
	 * 
	 * @return The server handling message reception for this lock.
	 */
	public Server getServer() {
		return server;
	}

}
