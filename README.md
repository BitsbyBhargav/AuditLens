# AuditLens
### Enterprise Document Review, NLP Risk Analysis & Auditability

![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-6DB33F?style=flat&logo=springboot&logoColor=white)
![Python](https://img.shields.io/badge/Python-NLP-3776AB?style=flat&logo=python&logoColor=white)
![BERT](https://img.shields.io/badge/BERT-Contract%20Risk-6F42C1?style=flat)
![Database](https://img.shields.io/badge/Database-H2%20Local-365B81?style=flat)
![Status](https://img.shields.io/badge/Status-Academic%20PBL%20%7C%20In%20Development-orange?style=flat)

---

## Overview

**AuditLens** is an **academic Project-Based Learning (PBL) project** exploring how enterprises—especially financial institutions—could manage, classify, review and audit large collections of unstructured documents such as contracts, invoices and transaction records.

The longer-term vision is a **cloud-based document data lake** with NLP classification and risk scoring, tamper-evident audit records, and a compliance-officer-facing review interface. The **current repository is a local development MVP**, not a fully deployed enterprise compliance platform.

Implemented local components include a **Spring Boot document portal**, role-aware review workflow, version records, an application audit log, SHA-256 integrity checks, and a separate **FastAPI/BERT** service for prepared contract examples.

> **Current vs planned:** The local MVP uses H2 and local files. Production S3 Object Lock, distributed cloud orchestration, and automatic NLP risk scoring for arbitrary newly uploaded documents are **future integration work**.

---

## Business Problem

Regulated organisations work with large collections of documents that may be stored across different systems. Without consistent version history, review controls and audit trails, it becomes harder to demonstrate which document was submitted, reviewed, changed or approved.

**Project objectives:**

- Manage document submission, review and revision history.
- Give employees and reviewers distinct responsibilities.
- Record changes in an auditable application log.
- Check file integrity before marking approved content as locked.
- Explore clause-based NLP risk classification on an annotated contract dataset.
- Design toward future cloud storage and searchable compliance analysis.

**Academic focus:** Demonstrate a coherent, auditable workflow and explore NLP-assisted review. **Not claimed:** Regulatory certification, legal advice, production immutability or enterprise-wide deployment.

---

## Project Structure

```text
AuditLens/
├── portal/portal/
│   ├── pom.xml                              # Spring Boot/Maven app
│   ├── README.md                            # Detailed local portal instructions
│   └── src/main/
│       ├── java/com/auditlens/portal/
│       │   ├── domain/                     # Documents, versions, users, logs
│       │   ├── repo/                       # Spring Data repositories
│       │   ├── security/                   # Login and security configuration
│       │   ├── service/                    # Workflow, storage, audit, classifier
│       │   └── web/                        # Controllers
│       └── resources/
│           ├── templates/                  # Employee/reviewer Thymeleaf pages
│           └── static/css/
├── nlp/
│   ├── fine_tune.py                         # BERT fine-tuning logic
│   ├── train_test_split.py
│   └── nlp/
│       ├── serve.py                         # FastAPI classification service
│       └── requirements-serve.txt
├── demo_data/cuad/                          # Prepared CUAD examples
├── etl/                                     # Dataset preparation utilities
├── db/
│   ├── schema.sql                           # SQL Server design
│   └── db/migrations/
├── infra/                                    # IAM/cloud integration definitions
├── docs/
│   ├── logbook.md
│   └── nlp_finetuning_results.md
├── README.md
└── first_contract.pdf
```

The main `README.md` is the project overview; [`portal/portal/README.md`](portal/portal/README.md) contains the precise local run instructions and functionality boundaries.

---

## System Architecture

### Current Local MVP

```text
        Employee / Reviewer browser
                    │
                    ▼
          Spring Boot + Thymeleaf
              Spring Security
                    │
      ┌─────────────┼──────────────────┐
      ▼             ▼                  ▼
 Role-aware     Document service   Audit service
 workflow       Versions / hash    Log entries
      │             │                  │
      │             ▼                  ▼
      │         Local files         H2 database
      │       (OS read-only        (app records)
      │        + SHA-256)
      │
      └────> Prepared CUAD examples
                     │
                     ▼
              FastAPI service
            Fine-tuned BERT model
          (local checkpoint needed)
                     │
                     ▼
             Risk label / score
        for supported CUAD inputs only
```

### Proposed Cloud Architecture — Not Yet Deployed

```text
Approved document / audit events
                  │
     AWS S3 Object Lock (proposed)
                  │
        Lambda / Step Functions
                  │
         Cloud audit and review
                  │
     Searchable compliance interface
```

The storage and classifier interfaces are designed for future integration. **Architecture goals must not be confused with running cloud infrastructure.**

---

## Document Data & NLP

| Component | Current approach |
|---|---|
| Document type emphasis | Enterprise contracts and related unstructured records |
| Demonstration data | Prepared **CUAD** example contracts and annotated clause snippets |
| NLP task | Three-class contract risk label: Low, Medium, High |
| Model approach | BERT fine-tuning / sequence classification |
| Local prediction service | FastAPI [`nlp/nlp/serve.py`](nlp/nlp/serve.py) |
| Backend database | H2 for current portal; SQL Server schema also supplied |
| File persistence | Local file storage and SHA-256 hash records |
| Future storage | S3 Object Lock and cloud ingestion/orchestration (planned) |

**Critical distinction:** Prepared CUAD examples can be risk-scored through the documented model path. **General uploaded files are not automatically classified** because extraction and preprocessing of arbitrary documents is not yet implemented.

The model's trained checkpoint is **not included in Git**. It must be available locally to run the NLP service; the Java portal can still operate in a reduced mode without it.

---

## Tech Stack

| Layer | Technologies | Status |
|---|---|---|
| Backend web portal | Java 25, Spring Boot 3.5.x | ✅ Source present |
| Authentication and roles | Spring Security | ✅ Source present |
| Frontend views | Thymeleaf, HTML, CSS | ✅ Templates present |
| Local persistence | H2, Spring Data JPA | ✅ Current local configuration |
| SQL design | Microsoft SQL Server schema/migrations | 📁 Scripts present; not the default running DB |
| Document integrity | SHA-256 and local read-only flags | ✅ Implemented workflow code |
| NLP / API | Python, PyTorch/Transformers, FastAPI, BERT | ✅ Service/training code; checkpoint external |
| Proposed cloud stack | AWS S3 Object Lock, Lambda, Step Functions, CloudTrail | ⏳ Planned integration |
| Version control | Git / GitHub | ✅ Repository |

---

## Current Features & Technical Verification

### Document Review Lifecycle

```text
Draft → UnderReview → Approved → Locked
                  │
                  └→ RevisionRequested → UnderReview
```

The [`DocumentService`](portal/portal/src/main/java/com/auditlens/portal/service/DocumentService.java) code defines allowed state transitions, revision handling, role-aware review constraints, file rehashing before approval/locking and audit records.

### File Integrity and Audit Trail

- Files are hashed with **SHA-256** and checked before locking.
- The local lock is an **OS read-only attribute**, **not** immutable storage. An administrator may remove the attribute; hash comparison helps reveal later modifications.
- Document, version and audit-log entities exist in the Java source.
- Logs are append-oriented at the application layer; this is **not** equivalent to a independently enforced, immutable, cross-system audit infrastructure.

### NLP Experiment Results (Repository Report)

The repository's [`nlp_finetuning_results.md`](docs/nlp_finetuning_results.md) records a BERT evaluation on **82 test samples**:

| Metric | Reported value |
|---|---:|
| Accuracy | 0.488 |
| Weighted F1 | 0.472 |
| High-risk recall | 0.86 |
| Medium-class F1 | 0.23 |

These are **recorded experiment results**, **not independently rerun** here. The small test set, weak Medium-class performance, and use of annotated clause snippets limit what the scores can establish about real compliance-risk detection.

---

## Key Findings

- **Workflow enforcement:** Approval, revision and locking are represented as explicit document-state transitions.
- **Integrity control:** Hash verification is used before a local document is marked locked.
- **NLP feasibility:** A BERT experiment can return Low/Medium/High risk labels for prepared CUAD inputs, but general document extraction is still missing.
- **Compliance limits:** A local audit log and read-only files do not, on their own, satisfy production audit-retention or regulatory controls.
- **Integration path:** Storage and classifier interfaces create a basis for future cloud functionality without implying that it has been deployed.

---

## Phase Completion

| Phase | Deliverable | Status |
|---|---|---|
| 1 | PBL problem definition / architecture | ✅ Documentation present |
| 2 | Local Spring Boot portal and review workflow | ✅ Implementation source present |
| 3 | H2 persistence, versions, audit records, integrity checks | ✅ Implementation source present |
| 4 | Prepared CUAD contract examples and BERT experiment | ✅ Code/report present; model checkpoint external |
| 5 | Automatic classification of arbitrary uploaded files | ⏳ Not implemented |
| 6 | S3 Object Lock / cloud orchestration deployment | ⏳ Not independently deployed / verified |
| 7 | Production compliance validation | ❌ Not claimed |

---

## Getting Started

### Prerequisites

- **Java 25**, **Maven 3.9+** and an IDE or terminal
- Python virtual environment for the optional classification API
- Local model checkpoint (not committed) if running the NLP service

### Run the Local Portal

```powershell
git clone https://github.com/BitsbyBhargav/AuditLens.git
cd AuditLens\portal\portal
mvn spring-boot:run
```

Open: `http://localhost:8080`

### Run the Optional Classifier

In a **separate PowerShell terminal from the repository root**, with the checkpoint present:

```powershell
pip install -r nlp/nlp/requirements-serve.txt
$env:PYTHONPATH = "$PWD\nlp"
$env:AUDITLENS_MODEL_DIR = "results\checkpoint-41"
python -m uvicorn nlp.serve:app --port 8000
```

Health endpoint: `http://localhost:8000/health`.

For demo-account details, CUAD paths, Windows permissions and limitations, read the dedicated [portal setup guide](portal/portal/README.md). The exact UI/runtime workflow was **not independently rerun** in the source audit; follow that guide and validate locally.

---

## Limitations & Future Scope

- Implement extraction and classification for **arbitrary** uploaded contracts/invoices, not just prepared examples.
- Improve the NLP training dataset, validation protocol and Medium-class performance.
- Integrate properly configured S3 Object Lock, durable event logging and cloud orchestration.
- Add robust automated tests, security review, search capability and deployment verification.
- Evaluate actual compliance requirements with qualified domain reviewers; the prototype is **not legal or regulatory advice**.

---

## Project Context

**Type:** **Academic Project-Based Learning (PBL)** project.  
**Team:** College project context; individual contribution should be documented separately from team work.  
**Status:** In development, with a local MVP and a proposed cloud expansion.  
**Purpose:** Investigate document traceability, review controls and NLP-assisted compliance workflows.

---

## Contributor

**Bhargav Sonawane**  
Computer Science — Big Data & Cloud Engineering · MIT ADT University

[GitHub](https://github.com/BitsbyBhargav) · [LinkedIn](https://www.linkedin.com/in/bhargav-sonawane-b50605327/) · [Portfolio](https://bitsbybhargav.github.io/)

---

## Usage & Attribution

Academic PBL prototype. The repository does not currently include a standalone `LICENSE` file; reuse permissions and third-party dataset/model terms should be reviewed before redistribution.
