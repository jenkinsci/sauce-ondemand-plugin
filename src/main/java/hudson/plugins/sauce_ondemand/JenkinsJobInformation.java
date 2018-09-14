package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import org.apache.http.client.utils.URIBuilder;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import javax.annotation.Nullable;
import java.net.URISyntaxException;
import java.util.Objects;
import java.time.Duration;

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
    @Exported(visibility=2)
    public String getStatusColor() {
        String status = this.getStatus();
        if ("Passed".equals(status)) {
            return "green";
        } else if ("Failed".equals(status)) {
            return "red";
        } else if ("Error".equals(status)) {
            return "orange";
        }
        return status;
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
        String browser = super.getBrowser();
        // this is incomplete (e.g. missing mobile)
        browser = browser.replace("firefox","Firefox");
        browser = browser.replace("iexplore","Internet Explorer");
        browser = browser.replace("googlechrome","Google Chrome");
        browser = browser.replace("safari","Safari");
        browser = browser.replace("microsoftedge","Microsoft Edge");
        browser = browser.replace("iphone","iPhone");
        browser = browser.replace("android","Android");
        return browser;
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

    @Nullable
    @Override
    @Exported(visibility=2)
    public String getFailureMessage() {
        return super.getFailureMessage();
    }

    @Override
    @Exported(visibility=2)
    public boolean hasFailureMessage() {
        return super.hasFailureMessage();
    }

    @Exported(visibility=2)
    public String getPrettyDuration() {
        Duration duration = Duration.ofSeconds(super.getDuration());
        long hours = duration.toHours();
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() % 60;

        StringBuffer prettyDuration = new StringBuffer();
        if (hours>0) {
            prettyDuration.append(hours).append("hr ");
        }
        if (minutes>0) {
            prettyDuration.append(minutes).append("m ");
        }
        if (seconds>0) {
            prettyDuration.append(seconds).append("s");
        }

        return prettyDuration.toString().trim();
    }
}
