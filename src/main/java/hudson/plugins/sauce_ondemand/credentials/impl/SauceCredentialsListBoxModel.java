package hudson.plugins.sauce_ondemand.credentials.impl;

import com.cloudbees.plugins.credentials.common.AbstractIdCredentialsListBoxModel;
import com.cloudbees.plugins.credentials.common.Messages;
import com.cloudbees.plugins.credentials.common.StandardUsernameCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Created by halkeye on 2015-11-15.
 */
public class SauceCredentialsListBoxModel extends StandardUsernameListBoxModel {

    @Override
    @NonNull
    public AbstractIdCredentialsListBoxModel<StandardUsernameListBoxModel, StandardUsernameCredentials> withEmptySelection() {
        this.add("- global -", "");
        return this;
    }
}
