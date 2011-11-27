package com.saucelabs.ci.sauceconnect;

import com.saucelabs.sauceconnect.SauceConnect;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author Ross Rowe
 */
public final class SauceConnectUtils {
    public static final String SAUCE_CONNECT_JAR = "sauce-connect-3.0.jar";

    private SauceConnectUtils() {
    }

    public static File extractSauceConnectJarFile() throws URISyntaxException, IOException {
        Class clazz = SauceConnect.class;
        ProtectionDomain protectionDomain = clazz.getProtectionDomain();
        CodeSource codeSource = protectionDomain.getCodeSource();
        URL location = codeSource.getLocation();        
        File jarFile = new File(location.toURI());
        return extractSauceConnectJar(jarFile);
    }

    private static File extractSauceConnectJar(File jarFile) throws IOException {
        if (jarFile.getName().equals(SAUCE_CONNECT_JAR)) {
            return jarFile;
        } else {
            JarFile jar = new JarFile(jarFile);
            java.util.Enumeration entries = jar.entries();
            final File destDir = new File(System.getProperty("user.home"));
            while (entries.hasMoreElements()) {
                JarEntry file = (JarEntry) entries.nextElement();

                if (file.getName().endsWith(SAUCE_CONNECT_JAR)) {
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
                    return f;
                }
            }
        }
        return null;
    }
}
