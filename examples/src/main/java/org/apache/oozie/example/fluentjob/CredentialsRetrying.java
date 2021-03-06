/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.oozie.example.fluentjob;

import com.google.common.collect.Lists;
import org.apache.oozie.fluentjob.api.action.*;
import org.apache.oozie.fluentjob.api.factory.WorkflowFactory;
import org.apache.oozie.fluentjob.api.workflow.*;

/**
 * This {@link WorkflowFactory} generates a workflow definition that has {@code credentials} section, and an action that has
 * {@code retry-interval}, {@code retry-max}, and {@code retry-policy} attributes set.
 * <p>
 * Note that {@link WorkflowBuilder#withCredentials(Credentials)} doesn't necessarily have to be called, since if
 * {@link WorkflowBuilder#credentialsBuilder} is emtpy by the time {@link WorkflowBuilder#build()} is called,
 * {@link Workflow#credentials} is built based on all the {@link Node#getCredentials()} that have been added to that
 * {@code Workflow} in beforehand.
 * <p>
 * Note also that when {@code WorkflowBuilder#withCredentials(Credentials)} is explicitly called, the {@code <workflowapp />}'s
 * {@code <credential />} section is generated only by using the {@code Credentials} defined on the {@code Workflow} level.
 * <p>
 * This way, users can, if they want to, omit calling {@code WorkflowBuilder#withCredentials(Credentials)} by default, but can
 * also override the autogenerated {@code <credentials />} section of {@code <workflowapp />} by explicitly calling that method
 * after another call to {@link CredentialsBuilder#build()}.
 * {@link CredentialsBuilder#build()}.
 */
public class CredentialsRetrying implements WorkflowFactory {
    @Override
    public Workflow create() {
        final Credential hbaseCredential = CredentialBuilder.create()
                .withName("hbase")
                .withType("hbase")
                .build();

        final Credential hcatalogCredential = CredentialBuilder.create()
                .withName("hcatalog")
                .withType("hcatalog")
                .withConfigurationEntry("hcat.metastore.uri", "thrift://<host>:<port>")
                .withConfigurationEntry("hcat.metastore.principal", "hive/<host>@<realm>")
                .build();

        final Credential hive2Credential = CredentialBuilder.create()
                .withName("hive2")
                .withType("hive2")
                .withConfigurationEntry("jdbcUrl", "jdbc://<host>/<database>")
                .build();

        final ShellAction shellActionWithHBase = ShellActionBuilder
                .create()
                .withName("shell-with-hbase-credential")
                .withCredential(hbaseCredential)
                .withResourceManager("${resourceManager}")
                .withNameNode("${nameNode}")
                .withExecutable("call-hbase.sh")
                .build();

        Hive2ActionBuilder
                .createFromExistingAction(shellActionWithHBase)
                .withParent(shellActionWithHBase)
                .withName("hive2-action-with-hcatalog-and-hive2-credentials")
                .clearCredentials()
                .withCredential(hcatalogCredential)
                .withCredential(hive2Credential)
                .withRetryInterval(1)
                .withRetryMax(3)
                .withRetryPolicy("exponential")
                .withScript("call-hive2.sql")
                .build();

        final Workflow workflow = new WorkflowBuilder()
                .withName("workflow-with-credentials")
                .withDagContainingNode(shellActionWithHBase)
                .build();

        return workflow;
    }
}
