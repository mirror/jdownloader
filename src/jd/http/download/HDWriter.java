package jd.http.download;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Diese Klasse übernimmt für alle Downloads das Schreiben auf die festplatte.
 * Zugriffe auf die Festplatte. Es wird in einem eigenen Thread geschrieben. So
 * können Downloads und Chunks interrupted werden, ohne dass der Schreibeprozess
 * unterbrochen wird. Die public SChreibfunktion ist blockierend. und
 * synchronized.Es ist also sichergestellt, dass immer nur ein Chunk
 * gleichzeitig schreibt.
 * 
 * @author coalado
 * 
 */
public class HDWriter extends Thread {
    public static enum STATUS {
        FREE, WRITING_IN_PROGRESS
    }

    private static HDWriter WRITER = null;

    private HDWriter() {

    }

    /**
     * gibt den aktuellen writer zurück.
     * 
     * @return
     */

    public synchronized static HDWriter getWriter() {
        if (WRITER == null || !WRITER.isAlive()) {
            WRITER = new HDWriter();
            WRITER.start();
        }
        return WRITER;
    }

    private STATUS currentStatus = HDWriter.STATUS.FREE;
    private ByteBuffer buffer;
    private FileChannel channel;
    private long position;
    private Exception exception;

    public void run() {
        while (!this.isInterrupted()) {
            try {
                write();
            } catch (Exception e) {
                // TODO fehlerbehanldung bei lokalen schreibfehlern. Momentan
                // werden fehler an den Chunk weitergeleitet
                this.exception = e;

            }
        }
    }

    private synchronized void write() throws IOException, InterruptedException {
        while (currentStatus != STATUS.WRITING_IN_PROGRESS) {
            // System.out.println(position + " Wait for writejob");

            this.wait();

        }
        // System.out.println(position + " START WRITING NOW");
        // if (buffer.position() != 0) {
        //
        // throw new IllegalStateException("Buffer has position 0"); }
        buffer.flip();
        channel.position(position);

        // System.out.println("Write " + position + "-" + (position +
        // buffer.limit()) + " :  " + buffer.limit());

        channel.write(buffer);
        
        // benachrichtige gib chunkwarteplatz wieder frei
        currentStatus = STATUS.FREE;
        // debug("STATUS = " + STATUS);
        // System.out.println(position + " WRITING FINISHED");
        notifyAll();

    }

    public synchronized void writeAndWait(ByteBuffer buffer, FileChannel outputChannel, long writePosition) throws Exception {

        // System.out.println(writePosition + "want to write ");
        while (currentStatus != STATUS.FREE) {
            // System.out.println(writePosition + "have to wait");
            this.wait();

        }

        this.buffer = buffer;
        channel = outputChannel;
        position = writePosition;

        currentStatus = STATUS.WRITING_IN_PROGRESS;
        // System.out.println(writePosition + " request write");
        notifyAll();
        // System.out.println(writePosition + " wait to return");
        waitForWriting();
        // System.out.println(writePosition + "return");
    }

    private synchronized void waitForWriting() throws Exception {
        while (currentStatus != STATUS.FREE) {
            this.wait();
        }
        if (this.exception != null) {
            try {
                throw this.exception;
            } finally {
                exception = null;
            }
        }
    }

}
