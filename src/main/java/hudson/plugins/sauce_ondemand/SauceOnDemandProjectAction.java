package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildableItemWithBuildWrappers;
import hudson.model.Descriptor;
import hudson.tasks.BuildWrapper;
import hudson.util.DescribableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author Ross Rowe
 */
public class SauceOnDemandProjectAction extends AbstractAction {

    private static final Logger logger = Logger.getLogger(SauceOnDemandProjectAction.class.getName());

    private AbstractProject<?, ?> project;

    public SauceOnDemandProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    /**
     * Get associated project.
     *
     * @return
     */
    public AbstractProject<?, ?> getProject() {
        return project;
    }

    public boolean hasSauceOnDemandResults() {
        logger.info("Inside hasSauceOnDemandResults v1.64");
        if (isSauceEnabled()) {
            logger.info("Checking to see if project has Sauce results");
            List<SauceOnDemandBuildAction> sauceOnDemandBuildActions = getSauceBuildActions();
            if (sauceOnDemandBuildActions != null) {
                boolean result = false;
                for (SauceOnDemandBuildAction action : sauceOnDemandBuildActions) {
                    if (action.hasSauceOnDemandResults()) {
                        logger.info("Found Sauce results");
                        result = true;
                        break;
                    }
                }
                return result;
            }
        }
        logger.info("Did not find Sauce results");
        return false;
    }

    public SauceOnDemandBuildWrapper getBuildWrapper() {

        return SauceEnvironmentUtil.getBuildWrapper(project);

    }

    private boolean isSauceEnabled() {
        return getBuildWrapper() != null;
    }

    private List<SauceOnDemandBuildAction> getSauceBuildActions() {
        AbstractBuild<?, ?> build = getProject().getLastBuild();
        if (build != null) {
            if (build instanceof MatrixBuild) {
                List<SauceOnDemandBuildAction> buildActions = new ArrayList<SauceOnDemandBuildAction>();
                MatrixBuild matrixBuild = (MatrixBuild) build;
                for (MatrixRun matrixRun : matrixBuild.getRuns()) {
                    SauceOnDemandBuildAction buildAction = matrixRun.getAction(SauceOnDemandBuildAction.class);
                    if (buildAction != null) {
                        buildActions.add(buildAction);
                    }
                }
                return buildActions;
            } else {
                SauceOnDemandBuildAction buildAction = build.getAction(SauceOnDemandBuildAction.class);
                if (buildAction == null) {
                    logger.info("No Sauce Build Action found for " + build.toString() + " adding a new one");
                    buildAction = new SauceOnDemandBuildAction(build,
                            getBuildWrapper().getUserName(), getBuildWrapper().getApiKey());
                    build.addAction(buildAction);
                }
                return Collections.singletonList(buildAction);
            }
        }
        logger.info("No Sauce Build Action found for " + build.toString());
        return Collections.emptyList();
    }

    public List<JobInformation> getJobs() {
        List<SauceOnDemandBuildAction> sauceOnDemandBuildAction = getSauceBuildActions();
        if (sauceOnDemandBuildAction != null) {
            List<JobInformation> allJobs = new ArrayList<JobInformation>();
            for (SauceOnDemandBuildAction action : sauceOnDemandBuildAction) {
                allJobs.addAll(action.getJobs());
            }
            return allJobs;
        }
        logger.info("No Sauce jobs found");
        return Collections.emptyList();
    }
}
