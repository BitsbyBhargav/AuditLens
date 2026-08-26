from pathlib import Path

from datasets import load_dataset


PROJECT_ROOT = Path(__file__).resolve().parents[1]
OUTPUT_DIR = PROJECT_ROOT / 'data' / 'raw' / 'cuad-qa'


def main():
    ds = load_dataset('theatticusproject/cuad-qa', trust_remote_code=True)
    ds.save_to_disk(OUTPUT_DIR)

    print(ds)
    print(ds['train'][0])
    print('features:', ds['train'].features)


if __name__ == '__main__':
    main()