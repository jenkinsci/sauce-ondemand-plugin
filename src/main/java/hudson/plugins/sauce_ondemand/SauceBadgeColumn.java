package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.model.Job;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;
import hudson.views.ListViewColumn;
import hudson.views.ListViewColumnDescriptor;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Calendar;
import java.util.Date;

/**
 * Presents a <a href="https://saucelabs.com/docs/status-images">Sauce Badge</a> column on the Jenkins dashboard.
 *
 * @author Ross Rowe
 */
public class SauceBadgeColumn extends ListViewColumn {

    /** The time which we last performed a lookup of the Sauce jobs*/
    private Date lastLookup;

    /** The build number retrieved from the last lookup of Sauce jobs*/
    private String lastBuildNumber;

    @DataBoundConstructor
    public SauceBadgeColumn() {
        super();
    }

    /**
     *
     * @return Boolean indicating whether the Sauce badge column should be shown.
     */
    public boolean isColumnDisabled() {
        return PluginImpl.get().isDisableStatusColumn();
    }

    /**
     * @param job
     * @return the username to be used to retrieve the Sauce badge.  If null, then the badge won't be displayed.
     */
    public String getSauceUser(Job job) {

        if (job instanceof BuildableItemWithBuildWrappers) {
            BuildableItemWithBuildWrappers project = (BuildableItemWithBuildWrappers) job;
            DescribableList<BuildWrapper, Descriptor<BuildWrapper>> buildWrappers = project.getBuildWrappersList();
            for (BuildWrapper buildWrapper : buildWrappers) {
                if (buildWrapper instanceof SauceOnDemandBuildWrapper) {
                    SauceOnDemandBuildWrapper sauceWrapper = (SauceOnDemandBuildWrapper) buildWrapper;
                    String buildNumber = SauceOnDemandBuildWrapper.sanitiseBuildNumber(SauceEnvironmentUtil.getBuildName(project.asProject().getLastBuild()));

                    if (shouldRetrieveJobs()) {
                      try {
                        JenkinsSauceREST sauceRest = new JenkinsSauceREST(sauceWrapper.getUserName(), sauceWrapper.getApiKey());
                        String lastJob = sauceRest.retrieveResults("/jobs?limit=1&full=true");
                        lastLookup = new Date();
                        //parse JSON
                        JSONArray jsonArray = JSONArray.fromObject(lastJob);
                        //does job have a build number and if so, does it equal the selected job?
                        lastBuildNumber = jsonArray.getJSONObject(0).getString("build");
                        if (buildNumber.equals(lastBuildNumber)) {
                          //if so, return the username
                          return sauceWrapper.getUserName();
                        }
                      } catch(JSONException e) {
                        // If it is unable to connect to saucelabs, we should
                        // not display the badge.
                        e.printStackTrace();
                        return null;
                      }
                    } else if (buildNumber.equals(lastBuildNumber)) {
                        return sauceWrapper.getUserName();
                    }
                }
            }
        }

        return null;
    }

    /**
     * @return true if more than 5 minutes have passed since the last time we retrieved the jobs from the Sauce REST API
     */
    private boolean shouldRetrieveJobs() {
        if (lastLookup == null) {
            return true;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(lastLookup);
        calendar.add(Calendar.MINUTE, 5);
        return new Date().after(calendar.getTime());
    }

    @Extension
    public static class DescriptorImpl extends ListViewColumnDescriptor {

        @Override
        public boolean shownByDefault() {
            return !PluginImpl.get().isDisableStatusColumn();
        }

        @Override
        public String getDisplayName() {
            return "Sauce Build Status";
        }
    }
}
