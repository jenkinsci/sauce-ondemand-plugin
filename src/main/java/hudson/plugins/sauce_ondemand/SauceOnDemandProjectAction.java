package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ross Rowe
 */
public class SauceOnDemandProjectAction extends AbstractAction {

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
        List<SauceOnDemandBuildAction> sauceOnDemandBuildActions = getSauceBuildActions();
        if (sauceOnDemandBuildActions != null) {
            boolean result = false;
            for (SauceOnDemandBuildAction action : sauceOnDemandBuildActions) {
                if (action.hasSauceOnDemandResults()) {
                    result = true;
                    break;
                }
            }
            return result;
        }
        return false;
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
            }
            return Collections.singletonList(build.getAction(SauceOnDemandBuildAction.class));
        }
        return null;
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
        return Collections.emptyList();
    }
}
