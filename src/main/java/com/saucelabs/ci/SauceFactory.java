package com.saucelabs.ci;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import sun.misc.BASE64Encoder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Ross Rowe
 */
public class SauceFactory {
    private static final Logger logger = Logger.getLogger(SauceFactory.class);

    public String doREST(String urlText) throws IOException {
        return doREST(urlText, null, null);
    }

    /**
     * Invokes a Sauce REST API command 
     * @param urlText
     * @param userName
     * @param password
     * @return results of REST command
     * @throws java.io.IOException
     */
    public synchronized String doREST(String urlText, final String userName, final String password) throws IOException {

        URL url = new URL(urlText);
        String auth = userName + ":" + password;
        BASE64Encoder encoder = new BASE64Encoder();
        auth = "Basic " + encoder.encode(auth.getBytes());

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Authorization", auth);

        // Get the response
        BufferedReader rd = null;
        try {

            rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            rd.close();

            return sb.toString();
        } finally {
            if (rd != null) {
                try {
                    rd.close();
                } catch (IOException e) {
                    logger.warn("Exception occurred when closing stream", e);
                }
            }
        }
    }

    /**
     * Populates the http proxy system properties.
     * 
     * @param proxyHost
     * @param proxyPort
     * @param userName
     * @param password
     */
    public void setupProxy(String proxyHost, String proxyPort, final String userName, final String password) {
        if (StringUtils.isNotBlank(proxyHost)) {
            System.setProperty("http.proxyHost", proxyHost);
            System.setProperty("https.proxyHost", proxyHost);
        }
        if (StringUtils.isNotBlank(proxyPort)) {
            System.setProperty("http.proxyPort", proxyPort);
            System.setProperty("https.proxyPort", proxyPort);
        }
        if (StringUtils.isNotBlank(userName) && StringUtils.isNotBlank(password)) {
            System.setProperty("http.proxyUser", userName);
            System.setProperty("https.proxyUser", userName);
            System.setProperty("http.proxyPassword", password);
            System.setProperty("https.proxyPassword", password);
        }
    }

    /**
     * 
     * @param downloadUrl
     * @return
     * @throws IOException
     */
    public byte[] doHTTPGet(String downloadUrl) throws IOException {
         URL u;
        InputStream is = null;
        OutputStream stream = null;

        try {
            u = new URL(downloadUrl.replaceAll(" ", "+"));
            HttpURLConnection con = (HttpURLConnection) u.openConnection();
            con.addRequestProperty("Accept", "text/plain");
            con.addRequestProperty("Accept-Charset", "ISO-8859-1,utf-8");
            con.connect();

            is = con.getInputStream(); // throws an IOException
            

        }
        catch (MalformedURLException mue) {
            logger.warn("Error in doHTTPGet", mue);
        }
        catch (IOException ioe) {
            logger.warn("Error in doHTTPGet", ioe);
        }
        finally {
            try {
                if (is != null)
                    is.close();
            }
            catch (IOException ioe) {
                logger.warn("Error in doHTTPGet", ioe);
            }
        }
        return IOUtils.toByteArray(is);
    }
}
