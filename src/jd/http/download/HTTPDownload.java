package jd.http.download;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import jd.http.Browser;
import jd.http.Request;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;
import jd.utils.jobber.JDRunnable;

public class HTTPDownload {
    /**
     * Flag indicates, that the server allows resuming
     */
    private static final int FLAG_RESUME = 1 << 0;
    /**
     * indicates, that the stored filesize is correct.
     */
    private static final int FLAG_FILESIZE_CORRECT = 1 << 1;

    private Request orgRequest;

    private File outputFile;
    private int chunkNum;
    private Threader chunks;
    private int flags = 0;
    private long fileSize;
    private RandomAccessFile outputRAF;
    private FileChannel outputChannel;
    private long byteCounter;

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public HTTPDownload(Request request, File file, int flags) {
        this.orgRequest = request;
        this.flags = flags;
        this.outputFile = file;
    }

    public HTTPDownload(Request request, File file) {
        this.orgRequest = request;
        this.outputFile = file;
    }

    public static void main(String[] args) {
        try{

            String destPath = "c:/test.download";
        Browser br = new Browser();

        Request request = br.createGetRequest("http://services.jdownloader.net/testfiles/25bmtest.test");

        final HTTPDownload dl = new HTTPDownload(request, new File(destPath), HTTPDownload.FLAG_RESUME);

        dl.setChunkNum(2);
        try{
            new Thread(){
                public void run(){
                    try {
                        Thread.sleep(2000);
                        dl.setChunkNum(4);                        
                        Thread.sleep(6000);
                        dl.setChunkNum(10);
                        Thread.sleep(6000);
                        dl.setChunkNum(20);
                    } catch (Exception e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    
                }
            }.start();
        dl.start();
        long crc = JDUtilities.getCRC(new File(destPath));

        if ("862E7007".trim().endsWith(Long.toHexString(crc).toUpperCase())) {
            System.out.println("CRC OK");
        } else {
            System.out.println("CRC FAULT");
        }
        }catch(BrowserException e){
            if(e.getType()==BrowserException.TYPE_LOCAL_IO){
                new File(destPath).delete();
                e.printStackTrace();
            }
        }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void setChunkNum(int i) throws BrowserException {
        if (i == chunkNum) return;
        if (chunks != null && chunks.isHasStarted()) {
            if (i > chunkNum) addChunksDyn(i - chunkNum);
            if (i < chunkNum) removeChunksDyn(chunkNum - i);
        }
        this.chunkNum = i;

    }

    private void removeChunksDyn(int i) {
        // TODO Auto-generated method stub

    }

    private void addChunksDyn(int i) throws BrowserException {
        while (i-- > 0) {
            addChunk();
        }
    }

    private void addChunk() throws BrowserException {
        DownloadChunk biggestRemaining = null;
        for (int i = 0; i < chunks.size(); i++) {
            System.out.println(chunks.get(i) + ":" + ((DownloadChunk) chunks.get(i)).getRemainingChunkBytes());
            if (biggestRemaining == null || biggestRemaining.getRemainingChunkBytes() < ((DownloadChunk) chunks.get(i)).getRemainingChunkBytes()) {
                biggestRemaining = ((DownloadChunk) chunks.get(i));
            }
        }
        long newSize = biggestRemaining.getRemainingChunkBytes() / 2;
        System.out.println(biggestRemaining + " New size: " + newSize);
        long old = biggestRemaining.getChunkEnd();
        biggestRemaining.setChunkEnd(biggestRemaining.getChunkStart() + biggestRemaining.getChunkBytes() + newSize);

        DownloadChunk newChunk = new DownloadChunk(this, biggestRemaining.getChunkEnd() + 1, old);
        System.out.println("SPLIT: " + biggestRemaining + " + " + newChunk);
        chunks.add(newChunk);

    }

    private void start() throws IOException, BrowserException {

        // If resumeFlag is set and ResumInfo Import fails, initiate the Default
        // ChunkSetup
        this.initOutputChannel();
        if (hasStatus(FLAG_RESUME) || !importResumeInfos()) {

            this.initChunks();

        }
        this.byteCounter = 0l;
        this.download();
        this.closeFileDiscriptors();
        this.clean();

    }

    private void closeFileDiscriptors() {
        try {
            outputChannel.force(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            outputRAF.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            outputChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * Renames all files and deletes tmp files
     * 
     * @throws BrowserException
     */
    private void clean() throws BrowserException {
        if (!new File(outputFile.getAbsolutePath() + ".part").renameTo(outputFile)) { throw new BrowserException(JDLocale.L("exceptions.browserexception.couldnotrenam", "Could not rename outputfile"), BrowserException.TYPE_LOCAL_IO);

        }
        if (!new File(outputFile.getAbsolutePath() + ".jdp").delete()) {
            new File(outputFile.getAbsolutePath() + ".jdp").deleteOnExit();
        }
    }

    private void download() {
        chunks.startAndWait();

    }

    private void initOutputChannel() throws FileNotFoundException, BrowserException {
        if (outputFile.exists()) { throw new BrowserException(JDLocale.L("exceptions.browserexception.alreadyexists", "Outputfile already exists"), BrowserException.TYPE_LOCAL_IO);

        }

        if (new File(outputFile.getAbsolutePath() + ".part").exists() && !this.hasStatus(FLAG_RESUME)) {
            if (!new File(outputFile.getAbsolutePath() + ".part").delete()) { throw new BrowserException("Could not delete *.part file", BrowserException.TYPE_LOCAL_IO); }
        }
        if (!outputFile.getParentFile().exists()) {
            outputFile.getParentFile().mkdirs();
        }

        outputRAF = new RandomAccessFile(outputFile.getAbsolutePath() + ".part", "rw");
        outputChannel = outputRAF.getChannel();

    }

    /**
     * Method creates the initial Chunks
     * 
     * @throws IOException
     * @throws BrowserException
     */
    private void initChunks() throws IOException, BrowserException {
        this.chunks = new Threader();

        DownloadChunk chunk = new DownloadChunk(this);

        // 0-Chunk has to be Rangeless.
        orgRequest.getHeaders().remove("Range");
        chunk.connect();
        System.out.println(orgRequest.printHeaders());
        if (orgRequest.getContentLength() > 0) {
            this.fileSize = orgRequest.getContentLength();
            this.addStatus(FLAG_FILESIZE_CORRECT);
        }

        chunk.setRange(0l, fileSize / chunkNum);
        chunks.add(chunk);

        chunks.getBroadcaster().addListener(chunks.new WorkerListener() {

            @Override
            public void onThreadException(Threader th, JDRunnable job, Exception e) {
                e.printStackTrace();
            }

            @Override
            public void onThreadFinished(Threader th, JDRunnable job) {
                // TODO Auto-generated method stub
            }

        });
        for (int i = 1; i < chunkNum; i++) {
            if (i < chunkNum - 1) {
                chunk = new DownloadChunk(this, chunk.getChunkEnd() + 1, fileSize * (i + 1) / chunkNum);
            } else {
                chunk = new DownloadChunk(this, chunk.getChunkEnd() + 1, -1);

            }

            chunks.add(chunk);

        }
        // TODO
        // Fehler müssen hier noch abgefangen werden, z.B. über listener

    }

    /**
     * Liest die file.name.jdp (J_dD_ownloadP_rogress) Datei ein.
     */
    private boolean importResumeInfos() {
        // TODO Auto-generated method stub
        return false;

    }

    public void addStatus(int status) {
        this.flags |= status;

    }

    public boolean hasStatus(int status) {
        return (this.flags & status) > 0;
    }

    public void removeStatus(int status) {
        int mask = 0xffffffff;
        mask &= ~status;
        this.flags &= mask;
    }

    public synchronized void writeBytes(DownloadChunk chunk, ByteBuffer buffer) throws IOException {

        synchronized (outputChannel) {
            buffer.flip();
            this.outputRAF.seek(chunk.getWritePosition());
            byteCounter += buffer.limit();
            System.out.println(chunk + " - " + "Write " + buffer.limit() + " bytes at " + chunk.getWritePosition() + " total: " + byteCounter + " written until: " + (chunk.getWritePosition() + buffer.limit() - 1));

            outputChannel.write(buffer);
            // if (chunk.getID() >= 0) {
            // downloadLink.getChunksProgress()[chunk.getID()] =
            // chunk.getCurrentBytesPosition() - 1;
            // }

        }

    }

    public Request getRequest() {

        return this.orgRequest;
    }
}
