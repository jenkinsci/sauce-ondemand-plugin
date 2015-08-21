package hudson.plugins.sauce_ondemand;

import com.saucelabs.ci.JobInformation;
import com.saucelabs.ci.sauceconnect.SauceConnectFourManager;
import com.saucelabs.hudson.HudsonSauceManagerFactory;
import hudson.matrix.MatrixBuild;
import hudson.matrix.MatrixRun;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.kohsuke.stapler.bind.JavaScriptMethod;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Backing logic for the Sauce UI component displayed on the Jenkins project page.
 *
 * @author Ross Rowe
 */
public class SauceOnDemandProjectAction extends AbstractAction {

    /**
     * Logger instance.
     */
    private static final Logger logger = Logger.getLogger(SauceOnDemandProjectAction.class.getName());

    /**
     * The Jenkins project that is being displayed.
     */
    private AbstractProject<?, ?> project;

    /**
     * Constructs a new instance.
     *
     * @param project the Jenkins project that is being displayed
     */
    public SauceOnDemandProjectAction(AbstractProject<?, ?> project) {
        this.project = project;
    }

    /**
     * @return The Jenkins project that is being displayed
     */
    public AbstractProject<?, ?> getProject() {
        return project;
    }

    /**
     * @return
     */
    public boolean hasSauceOnDemandResults() {
        logger.fine("checking if project has sauce results");
        if (isSauceEnabled()) {
            logger.fine("Checking to see if project has Sauce results");
            List<SauceOnDemandBuildAction> sauceOnDemandBuildActions = getSauceBuildActions();
            if (sauceOnDemandBuildActions != null) {
                boolean result = false;
                for (SauceOnDemandBuildAction action : sauceOnDemandBuildActions) {
                    if (action.hasSauceOnDemandResults()) {
                        logger.fine("Found Sauce results");
                        result = true;
                        break;
                    }
                }
                logger.fine("hasSauceOnDemandResults: " + result);
                return result;
            }
        }
        logger.fine("Did not find Sauce results");
        return false;
    }

    private SauceOnDemandBuildWrapper getBuildWrapper() {
        return SauceEnvironmentUtil.getBuildWrapper(project);
    }

    /**
     *
     * @return boolean indicating whether the build is configured to include Sauce support
     */
    public boolean isSauceEnabled() {
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
                    logger.fine("No Sauce Build Action found for " + build.toString() + " adding a new one");
                    return Collections.emptyList();
                }
                return Collections.singletonList(buildAction);
            }
        }

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
        logger.fine("No Sauce jobs found");
        return Collections.emptyList();
    }

    /**
     * Generates a zip file containing:
     * <ul>
     * <li>Sauce Connect log file</li>
     * <li>Jenkins console output</li>
     * </ul>
     *
     * @return HTML to be displayed to the user when the generation has been completed
     */
    @JavaScriptMethod
    public String generateSupportZip() {

        try {
            SauceConnectFourManager manager = HudsonSauceManagerFactory.getInstance().createSauceConnectFourManager();
            SauceOnDemandBuildWrapper sauceBuildWrapper = getBuildWrapper();
            ZipArchiver archiver = new ZipArchiver();
            File sauceConnectLogFile = null;
            if (sauceBuildWrapper.isEnableSauceConnect()) {
                sauceConnectLogFile = manager.getSauceConnectLogFile(sauceBuildWrapper.getOptions());
                if (sauceConnectLogFile != null) {
                    archiver.addFile(sauceConnectLogFile, "sc.log");
                }
            }
            //add Jenkins build output to zip
            archiver.addFile(getProject().getLastBuild().getLogFile(), "jenkins_build_output.log");

            File destinationDirectory = sauceConnectLogFile == null ? new File(System.getProperty("user.home")) : sauceConnectLogFile.getParentFile();
            File destFile = new File(destinationDirectory, "sauce_support.zip");
            archiver.setDestFile(destFile);
            archiver.createArchive();
            return "Generation of Sauce support zip file was successful, file is located at: " + destFile.getAbsolutePath();

        } catch (ComponentLookupException e) {
            logger.log(Level.WARNING, "Unable to retrieve Sauce Connect manager", e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Unable to create zip file", e);
        }

        return "Error creating Sauce support zip";
    }

}
