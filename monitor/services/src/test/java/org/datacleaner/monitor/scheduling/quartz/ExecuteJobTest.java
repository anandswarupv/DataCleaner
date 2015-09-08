/**
 * DataCleaner (community edition)
 * Copyright (C) 2014 Neopost - Customer Information Management
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.datacleaner.monitor.scheduling.quartz;

import java.io.File;

import junit.framework.TestCase;

import org.datacleaner.configuration.DataCleanerEnvironmentImpl;
import org.datacleaner.monitor.configuration.TenantContext;
import org.datacleaner.monitor.configuration.TenantContextFactory;
import org.datacleaner.monitor.configuration.TenantContextFactoryImpl;
import org.datacleaner.monitor.scheduling.SchedulingService;
import org.datacleaner.monitor.scheduling.model.ExecutionLog;
import org.datacleaner.monitor.scheduling.model.ScheduleDefinition;
import org.datacleaner.monitor.scheduling.model.TriggerType;
import org.datacleaner.monitor.server.SchedulingServiceImpl;
import org.datacleaner.monitor.server.job.MockJobEngineManager;
import org.datacleaner.monitor.shared.model.JobIdentifier;
import org.datacleaner.monitor.shared.model.TenantIdentifier;
import org.datacleaner.repository.Repository;
import org.datacleaner.repository.RepositoryNode;
import org.datacleaner.repository.file.FileRepository;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;

public class ExecuteJobTest extends TestCase {

    public void testAssumptionsAboutDisallowConcurrentExecution() throws Exception {
        final Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        try {
            scheduler.clear();

            if (!scheduler.getJobGroupNames().isEmpty()) {
                fail("Test prerequisites failed. Expecting empty getJobGroupNames() but got: "
                        + scheduler.getJobGroupNames());
            }

            JobDetail job1 = JobBuilder.newJob(MockNonConcurrentJob.class).withIdentity("job1", "tenant1")
                    .storeDurably().build();
            JobDetail job2 = JobBuilder.newJob(MockNonConcurrentJob.class).withIdentity("job2", "tenant1")
                    .storeDurably().build();

            scheduler.addJob(job1, true);
            scheduler.addJob(job2, true);

            assertEquals(1, scheduler.getJobGroupNames().size());
            assertEquals(2, scheduler.getJobKeys(GroupMatcher.jobGroupEquals("tenant1")).size());

            JobDetail job3 = JobBuilder.newJob(MockNonConcurrentJob.class).withIdentity("job1", "tenant2")
                    .storeDurably().build();
            scheduler.addJob(job3, true);

            assertEquals(2, scheduler.getJobGroupNames().size());
            assertEquals(2, scheduler.getJobKeys(GroupMatcher.jobGroupEquals("tenant1")).size());
            assertEquals(1, scheduler.getJobKeys(GroupMatcher.jobGroupEquals("tenant2")).size());

            scheduler.start();

            scheduler.triggerJob(new JobKey("job1", "tenant1"));
            scheduler.triggerJob(new JobKey("job1", "tenant1"));
            scheduler.triggerJob(new JobKey("job1", "tenant1"));
            scheduler.triggerJob(new JobKey("job1", "tenant1"));
            Thread.sleep(40);

            int executingJobs;
            executingJobs = scheduler.getCurrentlyExecutingJobs().size();
            assertTrue("Expected max 1 executing jobs but got " + executingJobs, executingJobs <= 1);

            scheduler.triggerJob(new JobKey("job2", "tenant1"));
            Thread.sleep(40);

            executingJobs = scheduler.getCurrentlyExecutingJobs().size();
            assertTrue("Expected max 2 executing jobs but got " + executingJobs, executingJobs <= 2);

            scheduler.triggerJob(new JobKey("job1", "tenant2"));
            scheduler.triggerJob(new JobKey("job1", "tenant2"));
            scheduler.triggerJob(new JobKey("job1", "tenant2"));
            Thread.sleep(40);

            executingJobs = scheduler.getCurrentlyExecutingJobs().size();
            assertTrue("Expected max 3 executing jobs but got " + executingJobs, executingJobs <= 3);

        } finally {
            scheduler.shutdown();
        }
    }

    public void testFileNotFound() throws Exception {
        final Repository repo = new FileRepository("src/test/resources/example_repo");
        final TenantContextFactory tenantContextFactory = new TenantContextFactoryImpl(repo,
                new DataCleanerEnvironmentImpl(), new MockJobEngineManager());

        TenantContext tenantContext = tenantContextFactory.getContext("tenant3");
        JobIdentifier job = new JobIdentifier("some_csv_profiling");
        TenantIdentifier tenantIdentifier = new TenantIdentifier("tenant3");
        ScheduleDefinition schedule = new ScheduleDefinition(tenantIdentifier, job, "SomeCSV");
        ExecutionLog execution = new ExecutionLog(schedule, TriggerType.MANUAL);

        String executionId = new ExecuteJob().executeJob(tenantContext, execution, null, new MockJobEngineManager());

        assertNotNull(executionId);
        try {
            SchedulingService schedulingService = new SchedulingServiceImpl(repo, tenantContextFactory);

            ExecutionLog log = schedulingService.getExecution(tenantIdentifier, execution);
            String logOutput = log.getLogOutput();
            assertTrue(
                    logOutput,
                    logOutput.indexOf("Resource does not exist: FileResource["
                            + new File("src/test/resources/example_repo/tenant3/foo/bar.csv").getPath()
                            + "] (ResourceException)") != -1);
            assertTrue(logOutput, logOutput.indexOf("org.apache.metamodel.util.ResourceException: ") != -1);
        } finally {
            RepositoryNode logNode = repo.getRepositoryNode("/tenant3/results/" + executionId
                    + ".analysis.execution.log.xml");
            assertNotNull(logNode);

            // cleanup
            logNode.delete();
        }
    }

    public void testInvalidDatastoreInJob() throws Exception {
        final Repository repo = new FileRepository("src/test/resources/example_repo");
        final TenantContextFactory tenantContextFactory = new TenantContextFactoryImpl(repo,
                new DataCleanerEnvironmentImpl(), new MockJobEngineManager());

        TenantContext tenantContext = tenantContextFactory.getContext("tenant3");
        JobIdentifier job = new JobIdentifier("product_profiling");
        TenantIdentifier tenantIdentifier = new TenantIdentifier("tenant3");
        ScheduleDefinition schedule = new ScheduleDefinition(tenantIdentifier, job, "orderdb");
        ExecutionLog execution = new ExecutionLog(schedule, TriggerType.MANUAL);

        String executionId = new ExecuteJob().executeJob(tenantContext, execution, null, new MockJobEngineManager());
        assertNotNull(executionId);
        try {
            SchedulingService schedulingService = new SchedulingServiceImpl(repo, tenantContextFactory);

            ExecutionLog log = schedulingService.getExecution(tenantIdentifier, execution);
            String logOutput = log.getLogOutput();
            assertTrue(logOutput, logOutput.indexOf("- No such datastore: orderdb (NoSuchDatastoreException)") != -1);
            assertTrue(logOutput,
                    logOutput.indexOf("org.datacleaner.job.NoSuchDatastoreException: No such datastore: orderdb") != -1);
        } finally {
            RepositoryNode logNode = repo.getRepositoryNode("/tenant3/results/" + executionId
                    + ".analysis.execution.log.xml");
            assertNotNull(logNode);

            // cleanup
            logNode.delete();
        }
    }
}
