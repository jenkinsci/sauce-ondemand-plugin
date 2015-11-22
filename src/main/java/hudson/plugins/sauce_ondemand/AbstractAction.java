package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import hudson.model.Action;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ross Rowe
 */
public abstract class AbstractAction implements Action {
    abstract public String getUsername();
    abstract public String getAccessKey();

    @Override
    public String getIconFileName() {
        return null;
    }

    @Override
    public String getDisplayName() { return null; }

    @Override
    public String getUrlName() {
        return "sauce-ondemand-report";
    }

    public abstract List<JobInformation> getJobs();

    @SuppressWarnings("unused") // used by stapler
    public List<JobInformation> getJobsWithAuth() {
        List<JobInformation> allJobs = this.getJobs();
        for(JobInformation j: allJobs) {
            try {
                j.setHmac(PluginImpl.calcHMAC(this.getUsername(), this.getAccessKey(), j.getJobId()));
            } catch (NoSuchAlgorithmException e) {
            } catch (InvalidKeyException e) {
            } catch (UnsupportedEncodingException e) {
            }
        }
        return allJobs;
    }

    /**
     *
     * @param req Standard Request Object
     * @param rsp Standard Response Object
     * @throws IOException Unable to load index.jelly template
     */
    public void doJobReport(StaplerRequest req, StaplerResponse rsp)
        throws IOException {

        SauceTestResultsById byId = new SauceTestResultsById(
            req.getParameter("jobId"),
            this.getUsername(),
            this.getAccessKey()
        );
        try {
            req.getView(byId, "index.jelly").forward(req, rsp);
        } catch (ServletException e) {
            throw new IOException(e);
        }
    }

}
