package hudson.plugins.sauce_ondemand;

import hudson.model.AbstractProject;

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
        return true;
    }


}
