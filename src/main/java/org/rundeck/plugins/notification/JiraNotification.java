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

import java.sql.Timestamp;
import java.util.Date;
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
        /**
         * Connect to JIRA using the configured credentials.
         */
        final BasicCredentials creds = new BasicCredentials(login, password);
        final JiraClient jira = new JiraClient(serverURL, creds);
        try {
            /* Retrieve the issue from JIRA */
            Issue issue = jira.getIssue(issueKey);

            /* Add a comment to the issue */
            String message = generateMessage(trigger, executionData);
            issue.addComment(message);

        } catch (JiraException je) {
            je.printStackTrace(System.err);
            return false;
        }

        return true;
    }

    /**
     * Format the message to send using JIRA's wiki-ish https://jira.atlassian.com/secure/WikiRendererHelpAction.jspa
     * Content includes execution and job info, as well as, options and failed nodes.
     * @param trigger Job trigger event
     * @param executionData    Job execution data
     * @return String formatted with job data
     */
    private String generateMessage(String trigger, Map executionData) {
        Object job = executionData.get("job");
        Map jobdata = (Map) job;
        Object groupPath = jobdata.get("group");
        Object jobname = jobdata.get("name");
        String jobgroup =  (!isBlank(groupPath.toString()) ? groupPath + "/" : "");
        String jobdesc = (String)jobdata.get("description");
        String emoticon = (trigger.equals("success") ? "(/)" : "(x)");
        Date date = (trigger.equals("running") ? (Date)executionData.get("dateStarted") : (Date)executionData.get("dateEnded"));

        StringBuilder sb = new StringBuilder();

        sb.append("{panel:title=Rundeck Job Notification}\n");
        sb.append("h3. ").append(emoticon).append(" [#"+executionData.get("id"));
        sb.append(" ").append(executionData.get("status"));
        sb.append(" by " + executionData.get("user"));
        sb.append(" at ").append(date);
        sb.append("|").append(executionData.get("href")).append("]\n");

        sb.append("\n");
        sb.append("h4. Job: [").append(jobdata.get("project")+"] \"").append(jobgroup+jobname).append("\"\n");
        sb.append("_").append(jobdesc).append("_\n");
        sb.append("\n");

        Map context = (Map) executionData.get("context");

        Map options = (Map) context.get("option");
        Map secureOption = (Map) context.get("secureOption");
        if (null != options && options.size()>0) {
            sb.append("h6. User Options\n");
            for (Object o : options.entrySet()) {
                Map.Entry entry = (Map.Entry) o;
                if (secureOption == null || !secureOption.containsKey(entry.getKey())) {
                    sb.append("* ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
            }
        }
        String status = (String)executionData.get("status");
        if (status.equals("failed")) {
            Map nodestatus = (Map)executionData.get("nodestatus");
            sb.append("\n");
            sb.append("h6. Nodes failed [").append(nodestatus.get("failed"))
                    .append(" out of ").append(nodestatus.get("total")).append("]\n");
            sb.append("* ").append(executionData.get("failedNodeListString")).append("\n");
        }
        sb.append("\n");
        sb.append("Job execution output: ").append(executionData.get("href")).append("\n");

        sb.append("{panel}");
        return sb.toString();
    }

    private boolean isBlank(String string) {
        return null == string || "".equals(string);
    }


}