package com.smartbear.readyapi.testserver.cucumber;

import com.smartbear.readyapi.client.model.TestCase;
import com.smartbear.readyapi4j.ExecutionListener;
import com.smartbear.readyapi4j.TestRecipe;
import com.smartbear.readyapi4j.execution.Execution;
import com.smartbear.readyapi4j.execution.RecipeExecutor;
import com.smartbear.readyapi4j.facade.execution.RecipeExecutorBuilder;
import cucumber.api.Scenario;
import io.swagger.util.Json;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;

/**
 * Executes a TestServer Recipe class using the TestServer endpoint specified
 * in the testserver.endpoint system property (defaults to the public
 * TestServer instance).
 */

public class CucumberRecipeExecutor {

    private final static Logger LOG = LoggerFactory.getLogger(CucumberRecipeExecutor.class);
    private static final String TESTSERVER_ENDPOINT = "testserver.endpoint";
    private static final String TESTSERVER_USER = "testserver.user";
    private static final String TESTSERVER_PASSWORD = "testserver.password";

    private RecipeExecutor executor;
    private boolean async = false;

    public CucumberRecipeExecutor() {
        executor = RecipeExecutorBuilder.buildDefault();
    }

    /**
     * Executes the specified TestCase and returns the Execution. If a scenario
     * is specified and the testserver.cucumber.logfolder system property is set,
     * the generated recipe will be written to the specified folder.
     *
     * It is possible to temporarily "bypass" recipe execution by specifying
     * a testserver.cucumber.silent property - in which case testcases will not be
     * submitted to the server, but still logged to the above folder.
     *
     * @param testCase the TestCase to execute
     * @param scenario the Cucumber scenario used to generate the specified Recipe
     * @return the TestServer Execution for the executed TestCase
     * @throws com.smartbear.readyapi.client.execution.ApiException if recipe execution failes
     */

    public Execution runTestCase(TestCase testCase, Scenario scenario) {

        TestRecipe testRecipe = new TestRecipe(testCase);

        if (LOG.isDebugEnabled()) {
            LOG.debug(testRecipe.toString());
        }

        String logFolder = System.getProperty( "testserver.cucumber.logfolder", null );
        if( scenario != null && logFolder != null ){
            logScenarioToFile(testRecipe, scenario, logFolder);
        }

        return async ? executor.submitRecipe( testRecipe ) : executor.executeRecipe(testRecipe);
    }

    /**
     * Writes the specified testRecipe to a folder/file name deducted from the
     * specified scenario
     *
     * @param testRecipe the test recipe to log
     * @param scenario the associated Cucumber scenario
     * @param logFolder the root folder for generated folders and files
     */

    protected void logScenarioToFile(TestRecipe testRecipe, Scenario scenario, String logFolder) {
        try {
            File folder = new File( logFolder );
            if( !folder.exists() || !folder.isDirectory()){
                folder.mkdirs();
            }

            String[] pathSegments = scenario.getId().split(";");
            File scenarioFolder = folder;
            int fileIndex = 0;

            if( pathSegments.length > 1 ) {
                scenarioFolder = new File(folder, pathSegments[0]);
                if (scenarioFolder.exists() || !scenarioFolder.isDirectory()) {
                    scenarioFolder.mkdirs();
                }

                fileIndex = 1;
            }

            String filename = pathSegments[fileIndex];
            for( int c = fileIndex+1; c < pathSegments.length; c++ ){
                String segment = pathSegments[c].trim();
                if( !StringUtils.isBlank( segment )){
                    filename += "_" + segment;
                }
            }

            filename += ".json";

            File scenarioFile = new File( scenarioFolder, filename );
            FileWriter writer = new FileWriter( scenarioFile );

            LOG.info("Writing recipe to " + folder.getName() + File.separatorChar + scenarioFolder.getName() +
                File.separatorChar + scenarioFile.getName());

            writer.write( Json.pretty(testRecipe) );
            writer.close();
        } catch (Exception e) {
            LOG.error("Failed to write recipe to logFolder [" + logFolder + "]", e );
        }
    }

    /**
     * Adds a listener for test execution events
     *
     * @param listener the listener to add
     */

    public void addExecutionListener(ExecutionListener listener) {
        executor.addExecutionListener(listener);
    }

    /**
     * Removes a previously added listener for test execution events
     *
     * @param listener the listener to remove
     */

    public void removeExecutionListener(ExecutionListener listener) {
        executor.removeExecutionListener(listener);
    }

    /**
     * Get the underlying RecipeExecutor used to execute the generated recipes.
     *
     * @return the underlying RecipeExecutor
     */

    public RecipeExecutor getExecutor() {
        return executor;
    }

    /**
     * Tells is execution of recipes will be async
     *
     * @return execution mode
     */

    public boolean isAsync() {
        return async;
    }

    /**
     * Sets if recipe execution will be async
     *
     * @param async execution mode
     */
    public void setAsync(boolean async) {
        this.async = async;
    }
}
