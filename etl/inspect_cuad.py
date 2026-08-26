from pathlib import Path

from datasets import load_from_disk


DATASET_DIR = Path(__file__).resolve().parents[1] / 'data' / 'raw' / 'cuad'


def main():

    ds = load_from_disk(DATASET_DIR)
    train = ds['train']
    example = train[0]

    print(f'splits: {list(ds.keys())}')
    print(f'train rows: {len(train)}')
    print(f'features: {train.features}')
    print(f'example keys: {list(example.keys())}')

    pdf = example.get('pdf')
    print(f'pdf type: {type(pdf).__name__}')
    if pdf is None:
        return

    print(f'pdf pages: {len(pdf.pages)}')
    text = '\n'.join(page.extract_text() or '' for page in pdf.pages)
    print(f'extracted text characters: {len(text)}')
    print('text sample:')
    print(text[:2000])


if __name__ == '__main__':
    main()