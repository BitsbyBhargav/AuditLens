-- AuditLens: columns added for the portal MVP. Apply to SQL Server after db/schema.sql.
-- (The local demo runs on H2, where Hibernate creates these automatically.)
USE AuditLensDB;
GO

-- Portal sign-in. NULL = account cannot use the portal (Technical admins).
ALTER TABLE Users ADD PasswordHash NVARCHAR(100) NULL;
GO

ALTER TABLE Documents ADD
    RuleLabel          NVARCHAR(10)   NULL,  -- label implied by RiskScore (clause count)
    RiskLabel          NVARCHAR(10)   NULL,  -- model prediction
    RiskConfidence     FLOAT          NULL,
    FlaggedClauses     NVARCHAR(1000) NULL,
    ClauseSnippets     NVARCHAR(MAX)  NULL,
    ClassificationNote NVARCHAR(500)  NULL,
    Source             NVARCHAR(20)   NULL,  -- CUAD / Manual
    ContentHash        CHAR(64)       NULL,  -- SHA-256 of current version
    ReviewerNotes      NVARCHAR(1000) NULL;
GO

ALTER TABLE DocumentVersions ADD
    ContentHash CHAR(64)      NULL,
    FileName    NVARCHAR(255) NULL;
GO

-- Automated actions (classifier) have no human actor.
ALTER TABLE AuditLog ALTER COLUMN UserID INT NULL;
GO

-- Widen the allowed audit actions.
DECLARE @ck sysname = (SELECT name FROM sys.check_constraints
                       WHERE parent_object_id = OBJECT_ID('AuditLog') AND definition LIKE '%Uploaded%');
IF @ck IS NOT NULL EXEC('ALTER TABLE AuditLog DROP CONSTRAINT ' + @ck);
GO
ALTER TABLE AuditLog ADD CONSTRAINT CK_AuditLog_Action CHECK (Action IN (
    'Uploaded','Classified','ReviewRequested','RevisionRequested','Resubmitted','Approved','Locked','Viewed',
    'ClassificationSkipped','IntegrityVerified','IntegrityCheckFailed'));
GO
