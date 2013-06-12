//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.nutils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.logging.Logger;

import org.appwork.utils.processes.ProcessBuilderFactory;

public class Executer extends Thread implements Runnable {
    class StreamObserver extends Thread implements Runnable {

        private final BufferedInputStream reader;

        private final DynByteBuffer       dynbuf;
        private boolean                   started;
        /**
         * is set to true if the observer is waiting for data
         */
        private boolean                   idle              = true;
        /**
         * is set to true of the reader returned -1
         */
        private boolean                   endOfFileReceived = false;

        private final InputStream         stream;
        /** flag to signal if underlying stream got already closed */
        private boolean                   isClosed          = false;

        private final Object              LOCK              = new Object();

        public StreamObserver(final InputStream stream, final DynByteBuffer buffer) {
            this.stream = stream;
            this.reader = new BufferedInputStream(stream);
            this.dynbuf = buffer;
        }

        /**
         * @return the {@link Executer.StreamObserver#idle}
         * @see Executer.StreamObserver#idle
         */
        public boolean isIdle() {
            return this.idle;
        }

        public boolean isStarted() {
            return this.started;
        }

        private int readLine() throws IOException, InterruptedException {
            int i = 0;
            final byte[] buffer = new byte[1];
            // some processes to not return an errorstream which leads
            // reader.read to lock. This lock cannot be released unter windows
            // so we start reading as soon as available() marks some bytes as

            // lock until bytes are available
            // if the stream does not provide data, the observer is set to idle.
            // if the concerned process has finished, and the stream does not
            // provide data, we can interrupt the observer
            // use isIdle() to check observer status
            this.idle = false;
            for (;;) {
                int read;
                if (this.isInterrupted()) { throw new InterruptedException(); }
                if ((read = this.reader.read(buffer)) < 0) {
                    this.endOfFileReceived = true;
                    return i;
                }
                if (read > 0) {
                    i += read;
                    this.dynbuf.put(buffer, read);
                    if (buffer[0] == '\b' || buffer[0] == '\r' || buffer[0] == '\n') { return i; }
                    Executer.this.fireEvent(this.dynbuf, read, this == Executer.this.sbeObserver ? Executer.LISTENER_ERRORSTREAM : Executer.LISTENER_STDSTREAM);
                } else {
                    Thread.sleep(100);
                }
            }
        }

        public void requestInterrupt() {
            try {
                /*
                 * if there is data available to read and we never read any data from stream yet, let the streamobserver read it, because it can be important
                 * data (eg. it breaks unrar pw finding for fast computers if we dont do this)
                 */
                /* must be synchronized */
                synchronized (this.LOCK) {
                    /*
                     * we never read any data but there is some available, so abort interrupt this time(its the only possible time to abort an interrupt
                     * request) and read it
                     */
                    if (!this.isClosed && this.idle == true && this.reader.available() > 0) { return; }
                }
                /* close the stream and interrupt the observer */
                this.isClosed = true;
                super.interrupt();
                this.stream.close();
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            this.started = true;
            int num;
            try {
                Executer.this.fireEvent(this.dynbuf, 0, this == Executer.this.sbeObserver ? Executer.LISTENER_ERRORSTREAM : Executer.LISTENER_STDSTREAM);
                /* waitloop until we got interrupt request or data to read */
                while (this.isInterrupted() || this.reader.available() <= 0) {
                    if (this.isInterrupted()) { return; }
                    Thread.sleep(150);
                }
                /*
                 * this must be synchronized for correct working requestInterrupt()
                 */
                synchronized (this.LOCK) {
                    this.idle = false;
                }
                while (!this.endOfFileReceived) {
                    num = this.readLine();
                    String line;
                    try {
                        line = new String(this.dynbuf.getLast(num), Executer.this.codepage).trim();
                    } catch (final UnsupportedEncodingException e) {
                        e.printStackTrace();
                        /* CHECK: we should always use new String (bytes,charset) to avoid issues with system charset and utf-8 */
                        line = new String(this.dynbuf.getLast(num)).trim();
                    }
                    if (line.length() > 0) {
                        if (Executer.this.isDebug()) {
                            System.out.println(this + ": " + line + "");
                        }
                        Executer.this.fireEvent(line, this.dynbuf, this == Executer.this.sbeObserver ? Executer.LISTENER_ERRORSTREAM : Executer.LISTENER_STDSTREAM);

                    }
                }
            } catch (final IOException e) {
                e.printStackTrace();
            } catch (final InterruptedException e) {
                // e.printStackTrace();
            } finally {
                // System.out.println("END");
                /* close streams for good */
                try {
                    this.reader.close();
                } catch (Throwable e) {
                }
                try {
                    this.stream.close();
                } catch (Throwable e) {
                }
            }
        }

    }

    public static final String CODEPAGE = Executer.isWindows() ? "ISO-8859-1" : "UTF-8";

    /*
     * We use our own method, to avoid usage of apwork utils. we need to keep restarter.jar free from wau
     */
    private static boolean isWindows() {
        final String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")) {
            return true;
        } else if (os.contains("nt")) { return true; }
        return false;
    }

    private boolean                               debug                = true;
    private Logger                                logger;

    private String                                codepage             = Executer.CODEPAGE;

    public static int                             LISTENER_ERRORSTREAM = 1;

    public static int                             LISTENER_STDSTREAM   = 1 << 1;

    private String                                command;

    private java.util.List<String>                parameter;

    private String                                runIn;

    private final DynByteBuffer                   inputStreamBuffer;

    private final DynByteBuffer                   errorStreamBuffer;
    private final java.util.List<ProcessListener> listener             = new ArrayList<ProcessListener>();

    private final java.util.List<ProcessListener> elistener            = new ArrayList<ProcessListener>();

    private int                                   waitTimeout          = 60;
    private int                                   exitValue            = -1;
    private boolean                               gotInterrupted       = false;
    private Process                               process;
    private StreamObserver                        sbeObserver;
    private StreamObserver                        sboObserver;

    private OutputStream                          outputStream         = null;
    private Exception                             exception            = null;

    public Executer(final String command) {
        super("Executer: " + command);
        this.command = command;
        this.parameter = new ArrayList<String>();
        this.inputStreamBuffer = new DynByteBuffer(1024 * 4);
        this.errorStreamBuffer = new DynByteBuffer(1024 * 4);
    }

    public void addParameter(final String par) {
        this.parameter.add(par);
    }

    public void addParameters(final String[] par) {
        if (par == null) { return; }
        for (final String p : par) {
            this.parameter.add(p);
        }
    }

    public void addProcessListener(final ProcessListener listener, final int flag) {
        this.removeProcessListener(listener, flag);

        if ((flag & Executer.LISTENER_STDSTREAM) > 0) {
            this.listener.add(listener);
        }
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) {
            this.elistener.add(listener);
        }

    }

    private void fireEvent(final DynByteBuffer buffer, final int read, final int flag) {
        if (this.isInterrupted()) { return; }
        if ((flag & Executer.LISTENER_STDSTREAM) > 0) {
            for (final ProcessListener listener : this.listener) {
                listener.onBufferChanged(this, buffer, read);
            }
        }
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) {
            for (final ProcessListener elistener : this.elistener) {
                elistener.onBufferChanged(this, buffer, read);
            }
        }
    }

    private void fireEvent(final String line, final DynByteBuffer sb, final int flag) {
        if ((flag & Executer.LISTENER_STDSTREAM) > 0) {
            for (final ProcessListener listener : this.listener) {
                listener.onProcess(this, line, sb);
            }
        }
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) {
            for (final ProcessListener elistener : this.elistener) {
                elistener.onProcess(this, line, sb);
            }
        }
    }

    public String getCodepage() {
        return this.codepage;
    }

    public String getCommand() {
        return this.command;
    }

    public String getErrorStream() {
        return this.errorStreamBuffer.toString(this.codepage);
    }

    public DynByteBuffer getErrorStreamBuffer() {
        return this.errorStreamBuffer;
    }

    public Exception getException() {
        return this.exception;
    }

    public int getExitValue() {
        return this.exitValue;
    }

    public DynByteBuffer getInputStreamBuffer() {
        return this.inputStreamBuffer;
    }

    public Logger getLogger() {
        return this.logger;
    }

    public String getOutputStream() {
        return this.inputStreamBuffer.toString(this.codepage);
    }

    public java.util.List<String> getParameter() {
        return this.parameter;
    }

    public Process getProcess() {
        return this.process;
    }

    public String getRunin() {
        return this.runIn;
    }

    public int getWaitTimeout() {
        return this.waitTimeout;
    }

    public boolean gotInterrupted() {
        return this.gotInterrupted;
    }

    @Override
    public void interrupt() {
        try {
            this.gotInterrupted = true;
            super.interrupt();
            if (this.sbeObserver != null) {
                this.sbeObserver.requestInterrupt();
            }
            if (this.sboObserver != null) {
                this.sboObserver.requestInterrupt();
            }
        } finally {
            this.process.destroy();
        }
    }

    public boolean isDebug() {
        return this.debug;
    }

    public void removeProcessListener(final ProcessListener listener, final int flag) {
        if ((flag & Executer.LISTENER_STDSTREAM) > 0) {
            this.listener.remove(listener);
        }
        if ((flag & Executer.LISTENER_ERRORSTREAM) > 0) {
            this.elistener.remove(listener);
        }
    }

    @Override
    public void run() {
        if (this.command == null || this.command.trim().length() == 0) {
            if (this.logger != null) {
                this.logger.severe("Execute Parameter error: No Command");
            }
            return;
        }

        final java.util.List<String> params = new ArrayList<String>();
        params.add(this.command);
        params.addAll(this.parameter);
        if (this.isDebug()) {
            final StringBuilder out = new StringBuilder();
            for (final String p : params) {
                out.append(p);
                out.append(' ');
            }
            if (this.logger != null) {
                this.logger.info("Execute: " + out + " in " + this.runIn);
            }
        }
        final ProcessBuilder pb = ProcessBuilderFactory.create(params.toArray(new String[] {}));
        if (this.runIn != null && this.runIn.length() > 0) {
            if (new File(this.runIn).exists()) {
                pb.directory(new File(this.runIn));
            } else {
                if (new File(params.get(0)).getParentFile().exists()) {
                    // logger.info("Run in: " + new
                    // File(params.get(0)).getParentFile());
                    pb.directory(new File(params.get(0)).getParentFile());
                } else {
                    if (this.logger != null) {
                        this.logger.severe("Working directory " + this.runIn + " does not exist!");
                    }
                }
            }
        }

        try {

            this.process = pb.start();

            if (this.waitTimeout == 0) { return; }
            this.outputStream = this.process.getOutputStream();
            this.sbeObserver = new StreamObserver(this.process.getErrorStream(), this.errorStreamBuffer);
            this.sbeObserver.setName(this.getName() + " ERRstreamobserver");
            this.sboObserver = new StreamObserver(this.process.getInputStream(), this.inputStreamBuffer);
            this.sboObserver.setName(this.getName() + " STDstreamobserver");
            this.sbeObserver.start();
            this.sboObserver.start();

            Thread timeoutThread = null;
            if (this.waitTimeout > 0) {
                timeoutThread = new Thread() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(Executer.this.waitTimeout * 1000);
                        } catch (final InterruptedException e) {
                        }
                        // interrupt on timeout. this handles and timeout like
                        // an external interrupt
                        Executer.this.interrupt();

                    }
                };
                timeoutThread.start();
            }

            try {
                this.process.waitFor();
                this.exitValue = this.process.exitValue();
            } catch (final InterruptedException e1) {
                this.process.destroy();
                this.gotInterrupted = true;
            } catch (final Exception e) {
                e.printStackTrace();
            } finally {
                /* close outputstream for good */
                try {
                    this.outputStream.close();
                } catch (Throwable e) {
                }
            }

            if (this.logger != null) {
                this.logger.finer("Process returned");
            }
            // stream did not return -1 yet, and so the observer sill still
            // waiting for data. we interrupt him
            if (this.sboObserver != null && this.sboObserver.isIdle()) {
                if (this.logger != null) {
                    this.logger.finer("sbo idle - interrupt");
                }
                this.sboObserver.requestInterrupt();

            }
            if (this.sbeObserver != null && this.sbeObserver.isIdle()) {
                if (this.logger != null) {
                    this.logger.finer("sbe idle - interrupt");
                }
                this.sbeObserver.requestInterrupt();
            }
            final long returnTime = System.currentTimeMillis();

            // must be called to clear interrupt flag
            Thread.interrupted();
            while (this.sbeObserver != null && this.sbeObserver.isAlive() || this.sboObserver != null && this.sboObserver.isAlive()) {
                Thread.sleep(50);
                if (System.currentTimeMillis() - returnTime > 60000) {
                    if (this.logger != null) {
                        this.logger.severe("Executer Error. REPORT THIS BUG INCL. THIS LOG to jd support");
                    }
                    this.sboObserver.requestInterrupt();
                    this.sbeObserver.requestInterrupt();
                    break;

                }
            }
            if (timeoutThread != null) timeoutThread.interrupt();
            if (this.logger != null) {
                this.logger.finer("Stream observer closed");
            }
        } catch (final IOException e1) {
            this.exception = e1;
            return;
        } catch (final InterruptedException e) {
            this.exception = e;
        }
    }

    public void setCodepage(final String codepage) {
        this.codepage = codepage;
    }

    public void setCommand(final String command) {
        this.command = command;
    }

    public void setDebug(final boolean debug) {
        this.debug = debug;
    }

    public void setLogger(final Logger logger) {
        this.logger = logger;
    }

    public void setParameter(final java.util.List<String> parameter) {
        this.parameter = parameter;
    }

    public void setRunin(final String runin) {
        this.runIn = runin;
    }

    public void setWaitTimeout(final int waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    public void waitTimeout() {
        while (this.isAlive()) {
            try {
                Thread.sleep(50);
            } catch (final InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void writetoOutputStream(String data) {
        if (data == null || data.length() == 0) {
            data = "";
        }
        try {
            this.outputStream.write(data.getBytes());
            this.outputStream.write("\n".getBytes());
            if (this.isDebug()) {
                System.out.println("Out>" + data);
            }
            this.outputStream.flush();
        } catch (final IOException e) {
        }
    }

}
