package com.saucelabs.jenkins.pipeline;

import hudson.EnvVars;
import org.jenkinsci.plugins.workflow.steps.EnvironmentExpander;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

final class ExpanderImpl extends EnvironmentExpander {
    private static final long serialVersionUID = 1;
    private final Map<String, String> overrides;

    ExpanderImpl(HashMap<String, String> overrides) {
        this.overrides = overrides;

    }

    @Override
    public void expand(@NonNull EnvVars env) throws IOException, InterruptedException {
        env.overrideAll(overrides);
    }
}
