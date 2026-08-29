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

### [29/08/26]
**SESSION 004 —**
**Member: Bhargav**
**Phase: NLP Data Preparation (Week 2)**

**WHAT WAS DONE:**
Corrected initial dataset loading error — 'cuad' loaded only raw PDFs with no labels; switched to 'theatticusproject/cuad-qa', the correctly labeled variant (22,450 QA-format rows across 408 contracts, 41 clause categories).

Reconstructed per-contract structure from the QA format by pivoting on contract title, producing one row per contract with True/False presence across all 41 clause categories.

Selected 10 risk-relevant clause categories (Anti-Assignment, Audit Rights, Change Of Control, Most Favored Nation, Non-Compete, Uncapped Liability, Exclusivity, Termination For Convenience, Ip Ownership Assignment, Liquidated Damages) and computed a risk_score (0–8) per contract based on how many of these clauses are present. Verified the resulting distribution was well-spread (mean 3.04, std 2.08), not clustered at a single value.

Identified that using full contract text (mean ~54,000 characters) was far beyond BERT's 512-token input limit. Replaced full-text input with concatenated risk-clause text spans only (the actual flagged clause snippets, not the whole document), reducing average length substantially. Iteratively capped each clause snippet to 20 words to bring the majority of contracts within tokenizer limits, applying standard 512-token truncation as a safeguard for the remaining outliers (172 of 408 contracts, ~42%).

Converted the continuous risk_score into a 3-class classification label (Low: 0–2, Medium: 3–5, High: 6–8) to make the task more robust given the limited dataset size (408 contracts). Verified class balance: Low 179, Medium 126, High 103 — no class is under-represented.

Completed tokenization using BERT tokenizer (truncation=True, max_length=512, padding='max_length'). Dataset is now in fine-tuning-ready format (input_ids, attention_mask, token_type_ids, risk_label).

**Next:** Train/test split and fine-tuning loop (BERT/FinBERT sequence classification, 3-class risk prediction).