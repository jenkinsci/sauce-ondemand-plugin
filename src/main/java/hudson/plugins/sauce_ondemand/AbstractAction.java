package hudson.plugins.sauce_ondemand;

import hudson.model.Action;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author Ross Rowe
 */
public abstract class AbstractAction implements Action {

    /**
     * @return
     */
    public String getIconFileName() {
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

    /**
     *
     * @param req
     * @param rsp
     * @throws IOException
     */
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
                return PluginImpl.get().calcHMAC(id);
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
