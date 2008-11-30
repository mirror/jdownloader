package jd.http.download;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import jd.http.Browser;
import jd.http.Request;
import jd.utils.jobber.JDRunnable;
import jd.utils.jobber.Jobber;
import jd.utils.jobber.Jobber.WorkerListener;

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
    private ArrayList<DownloadChunk> chunks;
    private int flags = 0;
    private long fileSize;

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public HTTPDownload(Request request, File file, int flags) {
        this.orgRequest = request;
        this.flags = flags;
    }

    public HTTPDownload(Request request, File file) {
        this.orgRequest = request;
        this.outputFile = file;
    }

    public static void main(String[] args) throws Exception {

        Browser br = new Browser();
        Request request = br.createGetRequest("http://services.jdownloader.net/ubuntu.iso");

        HTTPDownload dl = new HTTPDownload(request, new File("cg:/test.download"), HTTPDownload.FLAG_RESUME);

        dl.setChunkNum(20);
        dl.start();

    }

    private void setChunkNum(int i) {
        this.chunkNum = i;

    }

    private void start() throws IOException, BrowserException {

        // If resumeFlag is set and ResumInfo Import fails, initiate the Default
        // ChunkSetup
        if (hasStatus(FLAG_RESUME) || !importResumeInfos()) {

            this.initChunks();

        }

    }

    /**
     * Method creates the initial Chunks
     * 
     * @throws IOException
     * @throws BrowserException 
     */
    private void initChunks() throws IOException, BrowserException {
        this.chunks = new ArrayList<DownloadChunk>();

        DownloadChunk chunk = new DownloadChunk();
        chunk.setRequest(this.orgRequest);
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
        Jobber jobs = new Jobber(10);
        jobs.addWorkerListener(jobs.new WorkerListener(){

            @Override
            public void onJobException(Jobber jobber, JDRunnable job,Exception e) {
                e.printStackTrace();
                
            }

            @Override
            public void onJobFinished(Jobber jobber, JDRunnable job) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onJobListFinished(Jobber jobber) {
                // TODO Auto-generated method stub
                
            }

            @Override
            public void onJobStarted(Jobber jobber, JDRunnable job) {
                // TODO Auto-generated method stub
                
            }
            
        });
        for (int i = 1; i < chunkNum; i++) {
            chunk=new DownloadChunk(chunk.getChunkEnd()+1,fileSize*(i+1)/chunkNum);          
            chunk.setRequest(orgRequest);
            chunks.add(chunk);            
            jobs.add(chunk);
        }
        //TODO
        //Fehler müssen hier noch abgefangen werden, z.B. über listener
        jobs.start();
       
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
}
