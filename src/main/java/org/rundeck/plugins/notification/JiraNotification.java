package org.rundeck.plugins.notification;


import com.dtolabs.rundeck.core.plugins.Plugin;
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope;
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription;
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty;
import com.dtolabs.rundeck.plugins.notification.NotificationPlugin;

import net.rcarz.jiraclient.BasicCredentials;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;

import java.util.Map;

/**
 * Jira Notification Plugin
 */
@Plugin(service = "Notification", name = "JIRA")
@PluginDescription(title = "JIRA Notification", description = "Append notification messages to a Jira issue.")
public class JiraNotification implements NotificationPlugin {

    @PluginProperty(name = "issue key", title = "issue key", description = "Jira issue ID")
    private String issueKey;

    @PluginProperty(name = "url", title = "Server URL", description = "Jira server URL", scope = PropertyScope.Project)
    private String serverURL;

    @PluginProperty(name = "login", title = "login", description = "The account login name", scope = PropertyScope.Project)
    private String login;

    @PluginProperty(name = "password", title = "password", description = "The account password", scope = PropertyScope.Project)
    private String password;

    public JiraNotification() {

    }

    @Override
    public boolean postNotification(String trigger, Map executionData, Map config) {
        if (null == login) {
            throw new IllegalStateException("login is required");
        }
        if (null == password) {
            throw new IllegalStateException("password is required");
        }
        if (null == serverURL) {
            throw new IllegalStateException("server URL is required");
        }



        final BasicCredentials creds = new BasicCredentials(login, password);
        final JiraClient jira = new JiraClient(serverURL, creds);

        try {
            /* Retrieve the issue from JIRA */
            Issue issue = jira.getIssue(issueKey);
            /* Add a comment to the issue */
            issue.addComment(generateMessage(trigger, executionData));

        } catch (JiraException je) {
            je.printStackTrace(System.err);
            return false;
        }

        return true;
    }

    /**
     * Format the message to send
     * @param trigger Job trigger event
     * @param executionData    Job execution data
     * @return String formatted with job data
     */
    private String generateMessage(String trigger, Map executionData) {
        Object job = executionData.get("job");
        Map jobdata = (Map) job;
        Object groupPath = jobdata.get("group");
        Object jobname = jobdata.get("name");
        String jobdesc = (!isBlank(groupPath.toString()) ? groupPath + "/" : "") + jobname;

        return "[" + trigger.toUpperCase() + "] \"" + jobdesc + "\" run by " + executionData.get("user") + ": " +
                executionData.get("href");
    }

    private boolean isBlank(String string) {
        return null == string || "".equals(string);
    }


}
