package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import org.apache.http.client.utils.URIBuilder;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nullable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Objects;

@ExportedBean
public class JenkinsJobInformation extends JobInformation {
    public JenkinsJobInformation(String jobId, String hmac) {
        super(jobId, hmac);
    }

    @Exported(visibility=2)
    public String getResult() {
       return Objects.equals("true", getStatus()) ? "OK" : "FAILURE";
    }

    @Override
    @Exported(visibility=2)
    public String getLogUrl() {
        try {
            URIBuilder uriBuilder = new URIBuilder(super.getLogUrl());
            uriBuilder.addParameter("auth", this.getHmac());
            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return super.getLogUrl();
    }

    @Override
    @Exported(visibility=2)
    public String getVideoUrl() {
        try {
            URIBuilder uriBuilder = new URIBuilder(super.getVideoUrl());
            uriBuilder.addParameter("auth", this.getHmac());
            return uriBuilder.toString();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return super.getVideoUrl();
    }

    @Override
    @Exported(visibility=2)
    public String getHmac() {
        return super.getHmac();
    }

    @Override
    @Exported(visibility=2)
    public String getJobId() {
        return super.getJobId();
    }

    @Nullable
    @Override
    @Exported(visibility=2)
    public String getStatus() {
        return super.getStatus();
    }

    @Nullable
    @Override
    @Exported(visibility=2)
    public String getName() {
        return super.getName();
    }

    @Override
    @Exported(visibility=2)
    public String getBrowser() {
        return super.getBrowser();
    }

    @Override
    @Exported(visibility=2)
    public String getOs() {
        return super.getOs();
    }

    @Override
    @Exported(visibility=2)
    public String getVersion() {
        return super.getVersion();
    }
}
