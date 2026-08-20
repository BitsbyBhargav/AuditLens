from pathlib import Path
import os

# Xet reconstruction can fail on very large files; use resumable HTTP downloads.
os.environ.setdefault('HF_HUB_DISABLE_XET', '1')

from datasets import Dataset, load_dataset


RAW_DATA_DIR = Path(__file__).resolve().parents[1] / 'data' / 'raw'
RAW_DATA_DIR.mkdir(parents=True, exist_ok=True)


# CUAD - contracts
cuad_dir = RAW_DATA_DIR / 'cuad'
if not cuad_dir.exists():
	ds = load_dataset('theatticusproject/cuad', verification_mode='no_checks')
	ds.save_to_disk(cuad_dir)

# RVL-CDIP - stream only the requested subset.
if os.getenv('DOWNLOAD_RVL', '').lower() in {'1', 'true', 'yes'}:
	ds2 = load_dataset('aharley/rvl_cdip', split='train', streaming=True)
	subset = list(ds2.take(2000))
	ds2 = Dataset.from_list(subset)
	ds2.save_to_disk(RAW_DATA_DIR / 'rvl-cdip')