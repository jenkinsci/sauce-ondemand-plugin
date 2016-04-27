package hudson.plugins.sauce_ondemand;

import javax.annotation.Nonnull;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by gavinmogan on 2016-04-20.
 */
public class TestIDDetails {
    public static final Pattern SESSION_ID_PATTERN = Pattern.compile("SauceOnDemandSessionID=([0-9a-fA-F]+)(?:.job-name=(.*))?");
    private final String jobId;
    private final String jobName;

    public TestIDDetails(String jobId, String jobName) {

        this.jobId = jobId;
        this.jobName = jobName;
    }

    public static TestIDDetails processString(@Nonnull String line) {
        Matcher m = SESSION_ID_PATTERN.matcher(line);
        if (!m.find()) { return null; }
        TestIDDetails details = new TestIDDetails(
            m.group(1),
            m.groupCount() >= 2 ? m.group(2) : null
        );
        return details;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobId() {
        return jobId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TestIDDetails that = (TestIDDetails) o;

        if (jobId != null ? !jobId.equals(that.jobId) : that.jobId != null) return false;
        return jobName != null ? jobName.equals(that.jobName) : that.jobName == null;

    }

    @Override
    public int hashCode() {
        int result = jobId != null ? jobId.hashCode() : 0;
        result = 31 * result + (jobName != null ? jobName.hashCode() : 0);
        return result;
    }
}
