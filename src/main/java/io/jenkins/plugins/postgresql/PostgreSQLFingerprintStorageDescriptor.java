/*
 * The MIT License
 *
 * Copyright (c) 2020, Jenkins project contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package io.jenkins.plugins.postgresql;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.fingerprints.FingerprintStorageDescriptor;
import jenkins.model.Jenkins;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.interceptor.RequirePOST;

import javax.servlet.ServletException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;

public class PostgreSQLFingerprintStorageDescriptor extends FingerprintStorageDescriptor {

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 5432;
    public static final String DEFAULT_DATABASE_NAME = "defaultDB";
    public static final boolean DEFAULT_SSL = false;
    public static final int DEFAULT_CONNECTION_TIMEOUT = 2000;
    public static final int DEFAULT_SOCKET_TIMEOUT = 2000;
    public static final String DEFAULT_CREDENTIALS_ID = "";

    @Override
    public @NonNull String getDisplayName() {
        return Messages.PostgreSQLFingerprintStorage_DisplayName();
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public ListBoxModel doFillCredentialsIdItems(@AncestorInPath Item item, @QueryParameter String credentialsId) {
        StandardListBoxModel result = new StandardListBoxModel();
        if ((item == null && !Jenkins.get().hasPermission(Jenkins.ADMINISTER)) ||
                (item != null && !item.hasPermission(Item.EXTENDED_READ) &&
                        !item.hasPermission(CredentialsProvider.USE_ITEM))) {
            return result.includeCurrentValue(credentialsId);
        }
        return result
                .includeEmptyValue()
                .includeMatchingAs(
                        ACL.SYSTEM,
                        Jenkins.get(),
                        StandardUsernamePasswordCredentials.class,
                        Collections.emptyList(),
                        CredentialsMatchers.always()
                )
                .includeCurrentValue(credentialsId);
    }

    @Restricted(NoExternalUse.class)
    public FormValidation doCheckCredentialsId(@AncestorInPath Item item, @QueryParameter String value) {
        if (item == null) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return FormValidation.ok();
            }
        } else {
            if (!item.hasPermission(Item.EXTENDED_READ) && !item.hasPermission(CredentialsProvider.USE_ITEM)) {
                return FormValidation.ok();
            }
        }
        if (StringUtils.isBlank(value)) {
            return FormValidation.ok();
        }
        if (CredentialsProvider.listCredentials(
                StandardUsernamePasswordCredentials.class,
                Jenkins.get(),
                ACL.SYSTEM,
                Collections.emptyList(),
                CredentialsMatchers.withId(value)
        ).isEmpty()) {
            return FormValidation.error("Cannot find currently selected credentials");
        }
        return FormValidation.ok();
    }

    @RequirePOST
    @Restricted(NoExternalUse.class)
    public FormValidation doTestRedisConnection(
            @QueryParameter("host") final String host,
            @QueryParameter("port") final int port,
            @QueryParameter("databaseName") final String databaseName,
            @QueryParameter("ssl") final boolean ssl,
            @QueryParameter("credentialsId") final String credentialsId,
            @QueryParameter("connectionTimeout") final int connectionTimeout,
            @QueryParameter("socketTimeout") final int socketTimeout
    ) throws IOException, ServletException {
        if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
            return FormValidation.error("Need admin permission to perform this action");
        }
        try {
            testConnection(host, port, databaseName, credentialsId, ssl, connectionTimeout, socketTimeout);
            return FormValidation.ok("Success");
        } catch (Exception e) {
            return FormValidation.error("Connection error : " + e.getMessage());
        }
    }

    protected void testConnection (String host, int port, String databaseName, String credentialsId, boolean ssl,
                                   int connectionTimeout, int socketTimeout) throws SQLException {
        PostgreSQLConnection.getConnection(host, port, databaseName, credentialsId, ssl, connectionTimeout,
                socketTimeout);
    }

}
