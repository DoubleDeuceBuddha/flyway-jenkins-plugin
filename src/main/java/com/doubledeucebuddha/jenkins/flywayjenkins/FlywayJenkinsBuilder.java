/**
 * 
 */
package com.doubledeucebuddha.jenkins.flywayjenkins;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Map;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.ListBoxModel;
import hudson.util.ListBoxModel.Option;
import jenkins.tasks.SimpleBuildStep;
import jenkins.util.AntClassLoader;
import net.sf.json.JSONObject;

import org.apache.hadoop.util.StringUtils;
import org.apache.jasper.tagplugins.jstl.core.Url;
import org.flywaydb.core.Flyway;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;

import com.google.appengine.labs.repackaged.com.google.common.collect.Maps;
import com.mysql.jdbc.Driver;

/**
 * @author nate-kingsley
 *
 */
public class FlywayJenkinsBuilder extends Builder implements SimpleBuildStep {

	private final String datasource;
	private final String username;
	private final String password;
	private final String locations;
	private final String goals;
	
	private Map<String, Integer> goalMap;
	

	/**
	 * @param datasource
	 * @param username
	 * @param password
	 * @param goal
	 * @param locations
	 */
	@DataBoundConstructor
	public FlywayJenkinsBuilder(String datasource, String username, String password, String locations, String goals) {
				
		this.datasource = datasource;
		this.username = username;
		this.password = password;
		this.locations = locations;
		this.goals = goals;
		
		this.goalMap = Maps.newHashMap();
		this.goalMap.put("clean", 1);
		this.goalMap.put("migrate", 2);
		this.goalMap.put("info", 3);
		this.goalMap.put("baseline", 4);
		this.goalMap.put("repair", 5);
		
		
	}

	/**
	 * @return the datasource
	 */
	public String getDatasource() {
		return datasource;
	}




	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}


	/**
	 * @return the goals
	 */
	public String getGoals(){
		return goals;
	}


	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}


	/**
	 * @return the locations
	 */
	public String getLocations() {
		return locations;
	}


	@Override
	public void perform(Run<?, ?> build, FilePath workspace, Launcher launcher,
			TaskListener listener) throws InterruptedException, IOException {
		
		AntClassLoader cl = (AntClassLoader) getClass().getClassLoader();

		Thread.currentThread().setContextClassLoader(cl);
		
		
		Flyway flyway = new Flyway();

		
		flyway.setDataSource(this.datasource, this.username, this.password);
		
		
		if(this.locations != null)
			flyway.setLocations(workspace.toVirtualFile().toURI().toString().replaceAll("file:", "filesystem:") + this.locations);
		else
			flyway.setLocations(workspace.toVirtualFile().toURI().toString().replaceAll("file:", "filesystem:") + "db/migrations");


		for(String s: StringUtils.split(this.goals, ' ')){
			if(this.goalMap.containsKey(s)){
				switch(this.goalMap.get(s)){
					case 1:
						flyway.clean();
						break;
					case 2:
						flyway.migrate();
						break;
					case 3:
						flyway.info();
						break;
					case 4:
						flyway.baseline();
						break;
					case 5:
						flyway.repair();
						break;
					default:
						listener.fatalError("No goal defined");
						break;
				}
			} else {
				listener.fatalError("No goal defined");
			}
		}
		


		
	}
	
	@Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl)super.getDescriptor();
    }
	
	@Extension // This indicates to Jenkins that this is an implementation of an extension point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

		int flywayGoal;
		
		public DescriptorImpl() {
            load();
        }
		
		@Override
		public boolean isApplicable(Class<? extends AbstractProject> arg0) {
			// Indicates that this builder can be used with all kinds of project types 
			return true;
		}

		@Override
		public String getDisplayName() {
			return "Run Flyway DB Migration";
		}
		
		
		
	}

}
