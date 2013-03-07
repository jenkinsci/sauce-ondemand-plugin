package hudson.plugins.sauce_ondemand;

import hudson.Extension;
import hudson.model.AbstractProject;
import hudson.model.Action;
import hudson.model.TransientProjectActionFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Ross Rowe
 */
@Extension
public class SauceOnDemandProjectActionFactory extends TransientProjectActionFactory {
    @Override
    public Collection<? extends Action> createFor(AbstractProject target) {
        return Collections.singleton(new SauceOnDemandProjectAction(target));
    }
}
