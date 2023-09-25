package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.BuildInformation;
import com.saucelabs.saucerest.model.builds.Build;
import edu.umd.cs.findbugs.annotations.Nullable;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.Duration;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

@ExportedBean
public class JenkinsBuildInformation extends BuildInformation {

    private static final DecimalFormat df = new DecimalFormat("#.#");

    public JenkinsBuildInformation(String buildId) {
        super(buildId);
    }

    public JenkinsBuildInformation(Build build) {
        super(build);
    }

    @Override
    @Exported(visibility=2)
    public String getBuildId() {
        return super.getBuildId();
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

    @Exported(visibility=2)
    public String getStartDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss");
        return sdf.format(1000 * super.getStartTime());
    }

    @Exported(visibility=2)
    public long getDuration() {
        return super.getEndTime() - super.getStartTime();
    }

    @Override
    @Exported(visibility=2)
    public long getStartTime() {
        return super.getStartTime();
    }

    @Override
    @Exported(visibility=2)
    public long getEndTime() {
        return super.getEndTime();
    }

    @Exported(visibility=2)
    public String getPrettyDuration() {
        Duration duration = Duration.ofSeconds(getDuration());
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

    @Exported(visibility=2)
    public String getEfficiency(long maxJobDuration, long totalJobDuration) {
        if (totalJobDuration == getDuration()) {
            return "0% (Sequential)";
        }
        float efficiency = (float)maxJobDuration * 100 / getDuration();
        if (efficiency <= 90) {
            return df.format(efficiency) + "% (Semi-parallel)";
        }
        return df.format(efficiency) + "% (Parallel)";
    }

    @Override
    @Exported(visibility=2)
    public int getJobsFinished() {
        return super.getJobsFinished();
    }

    @Override
    @Exported(visibility=2)
    public int getJobsPassed() {
        return super.getJobsPassed();
    }

    @Override
    @Exported(visibility=2)
    public int getJobsFailed() {
        return super.getJobsFailed();
    }

    @Override
    @Exported(visibility=2)
    public int getJobsErrored() {
        return super.getJobsErrored();
    }

    @Exported(visibility=2)
    public String getJobsPassRate() {
        return df.format((float)getJobsPassed() * 100 / getJobsFinished());
    }

    @Exported(visibility=2)
    public String getJobsFailRate() {
        return df.format((float)getJobsFailed() * 100 /getJobsFinished());
    }

    @Exported(visibility=2)
    public String getJobsErrorRate() {
        return df.format((float)getJobsErrored() * 100 /getJobsFinished());
    }

}
