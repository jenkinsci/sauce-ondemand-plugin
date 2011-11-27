package com.saucelabs.ci.sauceconnect;

import com.saucelabs.sauceconnect.SauceConnect;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;


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
     * Semaphore initialized with a single permit that is used to ensure that the main worker thread
     * waits until the Sauce Connect process is fully initialized before tests are run.
     */
    private final Semaphore semaphore = new Semaphore(1);
    private PrintStream printStream;
    private File sauceConnectJar;

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
            }

            tunnelMap.remove(planKey);
        }
    }

    private void closeStream(OutputStream outputStream) {
        try {
            outputStream.close();
        } catch (IOException e) {
            logger.error("Error closing stream", e);
        }
    }

    private void closeStream(InputStream inputStream) {
        try {
            inputStream.close();
        } catch (IOException e) {
            logger.error("Error closing stream", e);
        }
    }

    public void addTunnelToMap(String planKey, Object tunnel) {
        if (!tunnelMap.containsKey(planKey)) {
            tunnelMap.put(planKey, new ArrayList<Process>());
        }

        tunnelMap.get(planKey).add((Process) tunnel);
    }

    /**
     * Creates a new Java process to run the Sauce Connect 2 library.
     *
     * @param username
     * @param apiKey
     * @return
     * @throws IOException
     */
    public Object openConnection(String username, String apiKey) throws IOException {

        try {

            //we are running under a slave
            StringBuilder builder = new StringBuilder();
            if (sauceConnectJar != null && sauceConnectJar.exists()) {
                //copy the file to the user home, sauce connect fails to run when the jar is held in the temp directory
                File userHome = new File(System.getProperty("user.home"));
                File newJar = new File(userHome, SauceConnectUtils.SAUCE_CONNECT_JAR);
                FileUtils.copyFile(sauceConnectJar, newJar);
                builder.append(newJar.getPath());
            } else {
                File jarFile = SauceConnectUtils.extractSauceConnectJarFile();
                if (jarFile == null) {
                    printStream.print("Unable to find sauce connect jar");
                    return null;
                }
                builder.append(jarFile.getPath());
            }

            String fileSeparator = File.separator;
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
            if (printStream != null) {
                printStream.println("Launching Sauce Connect " + Arrays.toString(args));
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

        }

        catch (URISyntaxException e)
        {
            //shouldn't happen
            logger.error("Exception occured during retrieval of sauce connect jar URL", e);
        }
        finally
        {
            //release the semaphore when we're finished
            semaphore.release();
        }

        return null;
    }    

    public Map getTunnelMap() {
        return tunnelMap;
    }

    public void setPrintStream(PrintStream printStream) {
        this.printStream = printStream;
    }

    public void setSauceConnectJar(File sauceConnectJar) {
        this.sauceConnectJar = sauceConnectJar;
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
            if (printStream != null) {
                return printStream;
            } else {
                return System.out;
            }
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
