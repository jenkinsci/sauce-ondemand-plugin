/*
 * The MIT License
 *
 * Copyright (c) 2010, InfraDNA, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package hudson.plugins.sauce_ondemand;

import hudson.model.AbstractBuild;
import hudson.util.IOUtils;
import sun.misc.BASE64Encoder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

/**
 * @author Kohsuke Kawaguchi
 */
public class DownloadQueue {
    private final List<Map<String,Task>> queues = new ArrayList<Map<String,Task>>();

    private Thread workerThread;

    public final class Task {
        public final String id;
        private final AbstractBuild<?,?> owner;
        private volatile boolean finished;

        /**
         * If non-zero, set the timestamp at which point this task can start.
         * Note that since the queue maintains FIFO semantics, this value
         * isn't meant to be arbitrary value per task.
         */
        private long startAt;

        public Task(String id, AbstractBuild<?, ?> owner) {
            this.id = id;
            this.owner = owner;
        }

        public boolean isFinished() {
            return finished;
        }

        public synchronized void join() throws InterruptedException {
            while (!finished)
                wait();
        }

        /**
         * Actually download the video and server log.
         */
        void download() {
            PluginImpl p = PluginImpl.get();
            for (String file : FILES) {
                try {
                    File dst = toLocalFile(owner,id,file);
                    if (dst.exists())   continue;   // already downloaded

                    File dir = dst.getParentFile();
                    dir.mkdirs();

                    File tmp = File.createTempFile("sod",file,dir);
                    URL url = new URL("https://saucelabs.com/rest/"+p.getUsername()+"/jobs/"+id+"/results/"+file);
                    LOGGER.fine("Attempting to download "+url);
                    URLConnection con = url.openConnection();
                    String encodedAuthorization = new BASE64Encoder().encode(
                            (p.getUsername() + ":" + p.getApiKey()).getBytes());
                    con.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
                    InputStream in = con.getInputStream();
                    try {
                        IOUtils.copy(in,tmp);
                    } finally {
                        in.close();
                    }

                    // rename upon a successful retrieval
                    tmp.renameTo(dst);
                } catch (IOException e) {
                    // if we fail to retrieve, we'll defer retry until the on-demand retrieval happens
                    LOGGER.log(WARNING, "Failed to retrieve "+id+"/"+file+" from SauceLabs",e);
                }
            }

            synchronized (this) {
                finished = true;
                notifyAll();
            }
        }
    }


    public DownloadQueue() {
        queues.add(new LinkedHashMap<String,Task>());
        queues.add(new LinkedHashMap<String,Task>());
        startWorkerThread();
    }

    private void startWorkerThread() {
        workerThread = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    try {
                        Task t = pop();
                        t.download();
                    } catch (Throwable t) {
                        LOGGER.log(WARNING, "Something went wrong",t);
                    }
                }
            }
        });
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Picks up the next task from the queue. The 'high priority' queue
     * takes precedence.
     */
    private synchronized Task pop() throws InterruptedException {
        while (true) {
            long now = System.currentTimeMillis();
            long wait = Long.MAX_VALUE;
            for (Map<String, Task> queue : queues) {
                if (!queue.isEmpty()) {
                    Iterator<Task> itr = queue.values().iterator();
                    Task t = itr.next();
                    if (t.startAt<=now) {// can start now
                        itr.remove();
                        return t;
                    } else {// needs more time
                        wait = Math.min(wait, t.startAt-now);
                    }
                }
            }
            if (wait == Long.MAX_VALUE)
                wait();
            else
                wait(wait);
        }
    }

    public Task requestLowPriority(String id, AbstractBuild<?,?> owner) {
        return request(id,owner,System.currentTimeMillis()+ RETRIEVAL_DELAY,queues.get(1));
    }

    public Task requestHighPriority(String id, AbstractBuild<?,?> owner) {
        return request(id,owner,0,queues.get(0));
    }

    public synchronized Task request(String id, AbstractBuild<?,?> owner, long start, Map<String,Task> queue) {
        if (!workerThread.isAlive())
            startWorkerThread();

        Task t = queue.get(id);
        if (t==null) {
            queue.put(id,t=new Task(id,owner));
            notifyAll();
        }
        return t;
    }

    /**
     * Where to store the downloaded file locally?
     */
    public File toLocalFile(AbstractBuild<?,?> owner, String id, String file) {
        return new File(owner.getRootDir(),"sauce-ondemand/"+id+"/"+file);
    }

    private static final Logger LOGGER = Logger.getLogger(DownloadQueue.class.getName());

    /**
     * Number of milliseconds to wait before trying to download a report.
     */
    private static final int RETRIEVAL_DELAY = 15*1000;

    /**
     * Files to download from SauceLabs.
     */
    private static final String[] FILES = {"video.flv","selenium-server.log"};
}
