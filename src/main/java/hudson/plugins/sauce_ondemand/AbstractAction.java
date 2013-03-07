package hudson.plugins.sauce_ondemand;

import hudson.model.Action;
import org.apache.commons.codec.binary.Hex;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * @author Ross Rowe
 */
public abstract class AbstractAction implements Action {

    private static final String HMAC_KEY = "HMACMD5";


    /**
     * @return
     */
    public String getIconFileName() {
//        return "/plugin/sauce-ondemand/images/24x24/video.gif";
        return null;
    }

    /**
     * @return
     */
    public String getDisplayName() {
//        return "Sauce OnDemand Results";
        return null;
    }

    /**
     * @return
     */
    public String getUrlName() {
        return "sauce-ondemand-report";
    }

    public void doJobReport(StaplerRequest req, StaplerResponse rsp)
            throws IOException {

        ById byId = new ById(req.getParameter("jobId"));
        try {
            req.getView(byId, "index.jelly").forward(req, rsp);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

    /**
     *
     */
    public class ById {
        public final String id;

        public ById(String id) {
            this.id = id;
        }

        public String getAuth() throws IOException {
            try {
                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH");
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                String key = PluginImpl.get().getUsername() + ":" + PluginImpl.get().getApiKey() + ":" + format.format(calendar.getTime());
                byte[] keyBytes = key.getBytes();
                SecretKeySpec sks = new SecretKeySpec(keyBytes, HMAC_KEY);
                Mac mac = Mac.getInstance(sks.getAlgorithm());
                mac.init(sks);
                byte[] hmacBytes = mac.doFinal(id.getBytes());
                byte[] hexBytes = new Hex().encode(hmacBytes);
                return new String(hexBytes, "ISO-8859-1");


            } catch (NoSuchAlgorithmException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            } catch (InvalidKeyException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            } catch (UnsupportedEncodingException e) {
                throw new IOException("Could not generate Sauce-OnDemand access code", e);
            }

        }

    }
}
