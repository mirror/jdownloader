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

/**
 * Static methods for encoding/decoding messages.
 * 
 * @author Carlo Pelliccia
 */
final class Message {
    private static final String UTF8 = "UTF-8";

	/**
	 * It reads a JUnique formatted message from an InputStream.
	 * 
	 * @param inputStream
	 *            The source stream.
	 * @return The message decoded from the stream.
	 * @throws IOException
	 *             In an I/O error occurs.
	 */
	public static String read(final InputStream inputStream) throws IOException {
		// Message length (4 bytes)
		final byte[] b = new byte[4];
		if (inputStream.read(b) != 4) {
			throw new IOException("Unexpected end of stream");
		}
		final int length = (b[0] << 24) | (b[1] << 16) | (b[2] << 8) | b[3];
		// Length validation.
		if (length < 0) {
			throw new IOException("Invalid length block");
		} else if (length == 0) {
			return "";
		} else {
			// The message in bytes.
			final byte[] message = new byte[length];
			if (inputStream.read(message) != length) {
				throw new IOException("Unexpected end of stream");
			}
			// From bytes to string (utf-8).
			return new String(message, UTF8);
		}
	}

	/**
	 * It writes a JUnique formatted message in an OutputStream.
	 * 
	 * @param message
	 *            The message.
	 * @param outputStream
	 *            The OutputStream.
	 * @throws IOException
	 *             In an I/O error occurs.
	 */
	public static void write(final String message, final OutputStream outputStream)
			throws IOException {
		// Is this message null?
		if (message == null) {
			// Writes a 0 length block.
			outputStream.write(0);
			outputStream.write(0);
			outputStream.write(0);
			outputStream.write(0);
			outputStream.flush();
		} else {
			// Message length.
			final int length = message.length();
			// The length block.
			final byte[] l = new byte[4];
			l[0] = (byte) ((length >> 24) & 0xff);
			l[1] = (byte) ((length >> 16) & 0xff);
			l[2] = (byte) ((length >> 8) & 0xff);
			l[3] = (byte) (length & 0xff);
			outputStream.write(l);
			outputStream.flush();
			// Message block.
			final byte[] b = message.getBytes(UTF8);
			outputStream.write(b);
			outputStream.flush();
		}
	}

}
