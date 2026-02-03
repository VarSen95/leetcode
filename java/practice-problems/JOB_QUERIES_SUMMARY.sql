-- ============================================================================
-- JOB EXECUTION QUERIES SUMMARY
-- ============================================================================
-- Tables:
--   - job_executions(execution_id BIGINT NOT NULL, job_name VARCHAR NOT NULL)
--   - job_status_history(execution_id, timestamp, status)
-- ============================================================================

-- ============================================================================
-- QUERY 1: Latest Execution ID per Job
-- ============================================================================
-- Result: job_name, last_execution_id

SELECT 
    job_name,
    MAX(execution_id) AS last_execution_id
FROM 
    job_executions
GROUP BY 
    job_name
ORDER BY 
    job_name;

-- ============================================================================
-- QUERY 2: Latest Execution with Last Status Change Timestamp
-- ============================================================================
-- Result: job_name, last_execution_id, last_status_change_timestamp

SELECT 
    je.job_name,
    je.execution_id AS last_execution_id,
    MAX(jsh.timestamp) AS last_status_change_timestamp
FROM job_executions je
INNER JOIN (
    SELECT job_name, MAX(execution_id) AS max_execution_id
    FROM job_executions
    GROUP BY job_name
) latest_exec 
    ON je.job_name = latest_exec.job_name 
    AND je.execution_id = latest_exec.max_execution_id
LEFT JOIN job_status_history jsh 
    ON je.execution_id = jsh.execution_id
GROUP BY je.job_name, je.execution_id
ORDER BY je.job_name;

-- ============================================================================
-- QUERY 3: Job Name with Last Status of Highest Execution
-- ============================================================================
-- Result: job_name, last_status

SELECT 
    je.job_name,
    jsh.status AS last_status
FROM 
    job_executions je
INNER JOIN (
    SELECT 
        job_name,
        MAX(execution_id) AS max_execution_id
    FROM 
        job_executions
    GROUP BY 
        job_name
) latest_exec 
    ON je.job_name = latest_exec.job_name 
    AND je.execution_id = latest_exec.max_execution_id
INNER JOIN (
    SELECT DISTINCT ON (execution_id)
        execution_id,
        status
    FROM 
        job_status_history
    ORDER BY 
        execution_id,
        timestamp DESC
) jsh ON je.execution_id = jsh.execution_id
ORDER BY 
    je.job_name;

-- ============================================================================
-- RECOMMENDED INDEXES FOR PERFORMANCE
-- ============================================================================
CREATE INDEX idx_job_executions_name_id ON job_executions(job_name, execution_id);
CREATE INDEX idx_job_status_execution_timestamp ON job_status_history(execution_id, timestamp DESC);
