# AuditLens portal (local MVP)

Spring Boot 3.5 + Thymeleaf + Spring Security. Runs fully locally: H2 database, local file storage,
FastAPI classifier. Cloud pieces (S3 Object Lock, Lambda, Step Functions, CloudTrail) plug in later
behind `StorageService` and `ClassifierClient`.

## Run it (Windows PowerShell)

Requires Java 25 (LTS) and Maven 3.9 or later.

1. **Portal**: from the repository root, run `cd portal\portal; mvn spring-boot:run`.
   In IntelliJ, open `portal/portal/pom.xml`, reload the Maven project, and build it before
   running `PortalApplication`. A Maven `clean` removes compiled classes; build again before
   launching the main class directly. Open http://localhost:8080
2. **Classifier** (separate terminal, repo root, venv active). The trained model checkpoint
   must already exist locally at `results/checkpoint-41`; model weights are intentionally not
   stored in Git because of their size.
   ```
   pip install -r nlp/nlp/requirements-serve.txt
   $env:PYTHONPATH = "$PWD\nlp"
   $env:AUDITLENS_MODEL_DIR = "results\checkpoint-41"
   python -m uvicorn nlp.serve:app --port 8000
   ```
   Check http://localhost:8000/health. If it's not running, CUAD imports still work, with a clause count but no model prediction.
3. **CUAD demo contracts**: `demo_data/cuad/<id>.txt` + `<id>.snippets.json` (prepared by the CUAD script).

The portal looks for CUAD examples at `../../demo_data/cuad` when started from
`portal/portal`. Set `AUDITLENS_CUAD_DIR` to override this location.

Demo accounts (password `demo123`): riya@auditlens.local, aman@auditlens.local (employees),
karan@auditlens.local (compliance reviewer). The technical admin account cannot sign in by design.

To reset the demo: stop the portal, delete `portal/data/` and `portal/storage/`. Locked files are read-only;
clear the attribute first if Windows refuses: `attrib -R portal\storage\* /S`.

## Rules the code enforces
- Status moves only along Draft → UnderReview → (RevisionRequested → UnderReview)* → Approved → Locked.
- Locking happens only on approval, never on upload.
- A reviewer cannot review their own submission.
- Before locking, the file is re-hashed; if it no longer matches the upload hash, the lock is refused.
- Every action writes an AuditLog row; rows are never updated or deleted by the application.

## Honest limits of the local version
- "Locked" = OS read-only flag + recorded SHA-256. Tamper-evident, not tamper-proof: an OS admin can clear the flag,
  but the hash check exposes any change. S3 Object Lock (Compliance mode) is the production mechanism.
- Only CUAD test-split contracts are risk-scored, using their annotated clause snippets.
  Uploaded files are stored and reviewed but not scored: automatic clause extraction is future work.

## Switching to SQL Server Express later
Run `db/schema.sql`, then `db/migrations/002_portal_columns.sql`, and replace the datasource lines:
```
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=AuditLensDB;encrypt=true;trustServerCertificate=true
spring.datasource.username=<sql login>
spring.datasource.password=<password>
spring.jpa.hibernate.ddl-auto=validate
```
