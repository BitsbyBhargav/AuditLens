CREATE DATABASE AuditLensDB;
go

USE AuditLensDB;
go

-- Users table — 3 roles per revised scope
CREATE TABLE Users (
    UserID INT IDENTITY(1,1) PRIMARY KEY,
    Name NVARCHAR(100) NOT NULL,
    Email NVARCHAR(150) NOT NULL UNIQUE,
    Role NVARCHAR(30) NOT NULL 
        CHECK (Role IN ('Employee', 'ReviewerCompliance', 'TechnicalAdmin')),
    CreatedAt DATETIME2 DEFAULT SYSUTCDATETIME()
);
GO

-- Documents table — status field is the key addition from mentor feedback
CREATE TABLE Documents (
    DocumentID INT IDENTITY(1,1) PRIMARY KEY,
    FileName NVARCHAR(255) NOT NULL,
    S3Key NVARCHAR(500) NOT NULL,          -- path/key inside your S3 bucket
    DocumentType NVARCHAR(50),              -- contract / invoice / log (from NLP classification)
    RiskScore FLOAT,                        -- from NLP risk scoring
    Status NVARCHAR(30) NOT NULL DEFAULT 'Draft'
        CHECK (Status IN ('Draft', 'UnderReview', 'RevisionRequested', 'Approved', 'Locked')),
    VersionNumber INT NOT NULL DEFAULT 1,
    UploadedBy INT NOT NULL FOREIGN KEY REFERENCES Users(UserID),
    UploadedAt DATETIME2 DEFAULT SYSUTCDATETIME(),
    LockedAt DATETIME2 NULL                 -- populated only when Object Lock is actually applied
);
GO

-- Version history — Problem 2: don't overwrite, keep full history
CREATE TABLE DocumentVersions (
    VersionID INT IDENTITY(1,1) PRIMARY KEY,
    DocumentID INT NOT NULL FOREIGN KEY REFERENCES Documents(DocumentID),
    VersionNumber INT NOT NULL,
    S3Key NVARCHAR(500) NOT NULL,
    SubmittedBy INT NOT NULL FOREIGN KEY REFERENCES Users(UserID),
    SubmittedAt DATETIME2 DEFAULT SYSUTCDATETIME()
);
GO

-- Audit log — Problem 4: track WHO did WHAT and WHEN, not just storage state
CREATE TABLE AuditLog (
    LogID INT IDENTITY(1,1) PRIMARY KEY,
    DocumentID INT NOT NULL FOREIGN KEY REFERENCES Documents(DocumentID),
    UserID INT NOT NULL FOREIGN KEY REFERENCES Users(UserID),
    Action NVARCHAR(50) NOT NULL
        CHECK (Action IN ('Uploaded','Classified','ReviewRequested','RevisionRequested',
                           'Resubmitted','Approved','Locked','Viewed')),
    ActionTimestamp DATETIME2 DEFAULT SYSUTCDATETIME(),
    Notes NVARCHAR(500) NULL
);
GO