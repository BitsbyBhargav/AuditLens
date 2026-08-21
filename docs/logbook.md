# Project Logbook — AuditLens

## Project Start Date
[01/08/26]

## 6-Week Sprint Timeline

### Phase 0: Cloud Foundation & Dataset Preparation (Week 1)
**Repository Focus:** infra/, data/raw/, .gitignore

**Objective:** Establish AWS cloud infrastructure baseline and prepare real document datasets for classification.

**Tasks & Deliverables:**
Set up AWS account with a Budget Alert configured before any other cloud work begins. Create IAM baseline roles for development access, avoiding root account use. Create the S3 bucket with Object Lock enabled at creation (Compliance Mode, cannot be added later) and enable versioning. Download real document datasets — CUAD (contracts), RVL-CDIP (general documents), SROIE (receipts/invoices) — into data/raw/, excluded from Git via .gitignore.

### Phase 1: Fine-Tuning & Portal Skeleton (Week 2)
**Repository Focus:** nlp/, portal/, db/

**Objective:** Fine-tune the NLP classification model on real datasets while portal development begins in parallel.

**Tasks & Deliverables:**
Fine-tune BERT/FinBERT on labeled dataset subsets using free-tier GPU (Google Colab). Evaluate using precision, recall, and F1-score per document category, not accuracy alone. Initialize the Java Spring Boot portal with role-based view stubs (Employee, Reviewer/Compliance, Technical Admin). Design and create the SQL Server schema — Users, Documents, DocumentVersions, AuditLog — with a Status field enabling the review-before-lock workflow.

### Phase 2: Local Integration (Week 3)
**Repository Focus:** portal/, nlp/, db/

**Objective:** Connect the portal, fine-tuned model, and database locally before touching live cloud resources.

**Tasks & Deliverables:**
Connect the portal to local MinIO (S3-compatible) for document upload testing. Wire the fine-tuned model into the backend for classification and risk scoring on uploaded documents. Test the full local flow: upload → classify → store → review → approve → view on dashboard.

### Phase 3: Cloud Integration (Week 4)
**Repository Focus:** infra/, nlp/, portal/

**Objective:** Move the working local system onto real AWS infrastructure incrementally.

**Tasks & Deliverables:**
Replace MinIO calls with real S3 bucket calls. Deploy the Lambda function that triggers on S3 upload events and runs the classification pipeline. Attach IAM roles to the backend service only, kept separate from application-level user login. Configure Step Functions to orchestrate ingestion, classification, review, and lock stages.

### Phase 4: Full Deployment & Testing (Week 5)
**Repository Focus:** docs/, tests/

**Objective:** Deploy end-to-end and validate the complete pipeline under realistic conditions.

**Tasks & Deliverables:**
Deploy the Java application to EC2/Elastic Beanstalk. Verify CloudTrail logging produces a timestamped entry for every access and action. Run a bulk document ingestion (200–500 documents) from the prepared datasets. Confirm immutability by attempting to edit or delete a locked document and verifying failure. Rehearse the full demo walkthrough as a team.

### Buffer Week
Reserved for exam periods, jury reviews, or re-work on any phase running long. Not pre-assigned — placed wherever the sprint tracker shows an unresolved item.

---

## Session Log

### [17/08/26]
**SESSION 001-**
**Member: Bhargav**
**Phase: Repository & Foundation Setup**

**WHAT WAS DONE:**
Initialized the AuditLens GitHub repository with the standard folder structure (portal, nlp, etl, infra, db, docs, tests). Added .gitignore covering datasets, AWS credentials, and IDE files. Set branch protection on main, requiring pull requests before merging.

### [20/08/26]
**SESSION 002 —**
**Member: Bhargav**
**Phase: Cloud Foundation**

**WHAT WAS DONE:**
Configured AWS CLI and created the S3 bucket (auditlens-doc-lake) with Object Lock enabled at creation. Enabled versioning, required for Object Lock to function. Verified configuration via CLI — confirmed Object Lock and versioning both active, IAM identity confirmed working.

### [20/08/26]
**SESSION 003 —**
**Member: Bhargav**
**Phase: Database Schema**

**WHAT WAS DONE:**
Designed and created the SQL Server schema — Users, Documents, DocumentVersions, and AuditLog tables. Added a Status field to Documents (Draft/UnderReview/RevisionRequested/Approved/Locked), the mechanism that determines when Object Lock is applied — only on approval, per mentor feedback, not automatically on upload.