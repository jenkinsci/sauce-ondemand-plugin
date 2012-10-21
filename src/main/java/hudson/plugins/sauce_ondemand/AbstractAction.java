package hudson.plugins.sauce_ondemand;

import hudson.model.Action;

/**
 * @author Ross Rowe
 */
public abstract class AbstractAction implements Action {


    /**
     *
     * @return
     */
    public String getIconFileName() {
//        return "/plugin/sauce-ondemand/images/24x24/video.gif";
        return null;
    }

    /**
     *
     * @return
     */
    public String getDisplayName() {
//        return "Sauce OnDemand Results";
        return null;
    }

    /**
     *
     * @return
     */
    public String getUrlName() {
        return "sauce-ondemand-report";
    }
}
