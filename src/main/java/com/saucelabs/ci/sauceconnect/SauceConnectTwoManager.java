package com.saucelabs.ci.sauceconnect;

import com.saucelabs.sauceconnect.SauceConnect;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Handles opening a SSH Tunnel using the Sauce Connect 2 logic. The class  maintains a cache of {@link Process } instances mapped against
 * the corresponding plan key.  This class can be considered a singleton, and is instantiated via the 'component' element of the atlassian-plugin.xml
 * file (ie. using Spring).
 *
 * @author Ross Rowe
 */
public class SauceConnectTwoManager implements SauceTunnelManager {

    private static final Logger logger = Logger.getLogger(SauceConnectTwoManager.class);
    private Map<String, List<Process>> tunnelMap;
    /**
     * Restricts invocations of Sauce Connect to be single threaded.
     */
    private final ReentrantLock accessLock = new ReentrantLock();
    /**
     * Semaphore initialized with a single permit that is used to ensure that the main worker thread
     * waits until the Sauce Connect process is fully initialized before tests are run.
     */
    private final Semaphore semaphore = new Semaphore(1);

    public SauceConnectTwoManager() {
        this.tunnelMap = new HashMap<String, List<Process>>();
    }

    public void closeTunnelsForPlan(String planKey) {
        if (tunnelMap.containsKey(planKey)) {
            List<Process> tunnelList = tunnelMap.get(planKey);
            for (Process sauceConnect : tunnelList) {
                logger.info("Closing Sauce Connect");

                closeStream(sauceConnect.getInputStream());
                closeStream(sauceConnect.getOutputStream());
                closeStream(sauceConnect.getErrorStream());


                sauceConnect.destroy();
                //release lock
                accessLock.unlock();
            }

            tunnelMap.remove(planKey);
        }
    }

    private void closeStream(OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private void closeStream(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    public void addTunnelToMap(String planKey, Object tunnel) {
        if (!tunnelMap.containsKey(planKey)) {
            tunnelMap.put(planKey, new ArrayList<Process>());
        }

        tunnelMap.get(planKey).add((Process) tunnel);
    }

    /**
     * Creates a new Java process to run the Sauce Connect 2 library.  We have to launch a separate process
     * because of a version conflict with Jython (Bamboo includes v2.2 but Sauce Connect requires v 2.5).
     *
     * @param username
     * @param apiKey
     * @param localHost
     * @param intLocalPort
     * @return
     * @throws IOException
     */
    public Object openConnection(String username, String apiKey) throws IOException {

        try {
            //only allow one thread to launch Sauce Connect
            accessLock.lock();
            File jarFile = new File
                    (SauceConnect.class.getProtectionDomain().getCodeSource().getLocation().toURI());

            List<String> jarFiles = extractSauceConnectJar(jarFile);

            StringBuilder builder = new StringBuilder();
            String pathSeparator = "";
            for (String fileName : jarFiles) {
                builder.append(pathSeparator).append(fileName);
                pathSeparator = File.pathSeparator;
            }

            String fileSeparator = File.separator;
            //File jarFile = new File("/Developer/workspace/bamboo_sauce/target/bamboo-sauceondemand-plugin-1.4.0.jar");
            String path = System.getProperty("java.home")
                    + fileSeparator + "bin" + fileSeparator + "java";
            String[] args = new String[]{path, "-cp",
                    builder.toString(),
                    SauceConnect.class.getName(),
                    username,
                    apiKey
            };
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            if (logger.isInfoEnabled()) {
                logger.info("Launching Sauce Connect " + Arrays.toString(args));
            }
            final Process process = processBuilder.start();
            try {
                semaphore.acquire();
                StreamGobbler errorGobbler = new SystemErrorGobbler("ErrorGobbler", process.getErrorStream());
                errorGobbler.start();
                StreamGobbler outputGobbler = new SystemOutGobbler("OutputGobbler", process.getInputStream());
                outputGobbler.start();

                boolean sauceConnectStarted = semaphore.tryAcquire(2, TimeUnit.MINUTES);
                if (!sauceConnectStarted) {
                    //log an error message
                    logger.error("Time out while waiting for Sauce Connect to start, attempting to continue");
                }
            } catch (InterruptedException e) {
                //continue;
            }
            logger.info("Sauce Connect now launched");
            return process;

        } catch (URISyntaxException e) {
            //shouldn't happen
            logger.error("Exception occured during retrieval of bamboo-sauce.jar URL", e);
        } finally {
            //release the semaphore when we're finished
            semaphore.release();
        }

        return null;
    }

    private List<String> extractSauceConnectJar(File jarFile) throws IOException {
        List<String> files = new ArrayList<String>();
        if (jarFile.getName().equals("sauce-connect-3.0.jar")) {
            files.add(jarFile.getPath());
        } else {
            JarFile jar = new JarFile(jarFile);
            java.util.Enumeration entries = jar.entries();
            final File destDir = new File(System.getProperty("user.home"));
            while (entries.hasMoreElements()) {
                JarEntry file = (JarEntry) entries.nextElement();

                if (file.getName().endsWith("sauce-connect-3.0.jar")) {
                    File f = new File(destDir, file.getName());

                    if (f.exists()) {
                        f.delete();
                    }
                    f.getParentFile().mkdirs();
                    f.createNewFile();
                    f.deleteOnExit();
                    InputStream is = jar.getInputStream(file); // get the input stream
                    FileOutputStream fos = new java.io.FileOutputStream(f);
                    IOUtils.copy(is, fos);
                    files.add(f.getPath());
                }
            }
        }
        return files;
    }


    public Map getTunnelMap() {
        return tunnelMap;
    }

    private abstract class StreamGobbler extends Thread {
        private InputStream is;

        private StreamGobbler(String name, InputStream is) {
            super(name);
            this.is = is;
        }

        public void run() {
            try {
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ((line = br.readLine()) != null) {
                    processLine(line);
                }
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        protected void processLine(String line) {
            getPrintStream().println(line);
        }

        public abstract PrintStream getPrintStream();
    }

    private class SystemOutGobbler extends StreamGobbler {

        SystemOutGobbler(String name, InputStream is) {
            super(name, is);
        }

        @Override
        public PrintStream getPrintStream() {
            return System.out;
        }

        @Override
        protected void processLine(String line) {
            super.processLine(line);
            if (line.contains("started")) {
                //unlock processMonitor
                semaphore.release();
            }
        }

    }

    private class SystemErrorGobbler extends StreamGobbler {

        SystemErrorGobbler(String name, InputStream is) {
            super(name, is);
        }

        @Override
        public PrintStream getPrintStream() {
            return System.err;
        }
    }


}
