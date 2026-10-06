"""
Prepare CUAD test-split contracts for the AuditLens portal demo.

Rebuilds final_df and the train/test split with the SAME code and random_state as nlp/fine_tune.py,
checks that the test split matches the one the model was evaluated on (36 Low / 25 Medium / 21 High),
then writes, for a class-balanced sample of TEST contracts only:

    demo_data/cuad/<id>.txt             full contract text (CUAD 'context')
    demo_data/cuad/<id>.snippets.json   title, risk clause snippets, exact model input, gold score/label

Usage (from repo root, venv active):
    python etl/prepare_cuad_demo.py --per-class 4
"""
import argparse
import json
import re
from pathlib import Path

import pandas as pd
from datasets import load_from_disk
from sklearn.model_selection import train_test_split

ROOT = Path(__file__).resolve().parents[1]

RISK_CLAUSES = [
    'Anti-Assignment', 'Audit Rights', 'Change Of Control', 'Most Favored Nation', 'Non-Compete',
    'Uncapped Liability', 'Exclusivity', 'Termination For Convenience', 'Ip Ownership Assignment',
    'Liquidated Damages',
]
# True-label counts of the 82-contract test set, from the reported confusion matrix.
EXPECTED_TEST_COUNTS = {'Low': 36, 'Medium': 25, 'High': 21}


# ---- identical to fine_tune.py ----
def extract_clause_name(question):
    match = re.search(r'"(.*?)"', question)
    return match.group(1) if match else question


def get_risk_text(title_group, max_words_per_clause=20):
    texts = []
    for _, row in title_group.iterrows():
        if len(row['answers']['text']) > 0:
            clause_text = row['answers']['text'][0]
            words = clause_text.split()[:max_words_per_clause]
            texts.append(' '.join(words))
    return ' '.join(texts)


def build_final_df(df):
    df['has_clause'] = df['answers'].apply(lambda a: len(a['text']) > 0)
    pivot = df.pivot_table(index='title', columns='question', values='has_clause',
                           aggfunc='max', fill_value=False)
    pivot.columns = [extract_clause_name(q) for q in pivot.columns]
    pivot['risk_score'] = pivot[RISK_CLAUSES].sum(axis=1)
    risk_text = df.groupby('title').apply(get_risk_text)
    final_df = pivot[['risk_score']].join(risk_text.rename('risk_text')).reset_index()
    final_df['risk_label'] = pd.cut(final_df['risk_score'], bins=[-1, 2, 4, 10],
                                    labels=['Low', 'Medium', 'High'], right=True)
    return final_df
# -----------------------------------


def safe_id(title, used):
    base = re.sub(r'[^A-Za-z0-9._-]+', '_', title).strip('._-')[:80] or 'contract'
    cid, k = base, 2
    while cid in used:
        cid, k = f'{base}_{k}', k + 1
    used.add(cid)
    return cid


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--per-class', type=int, default=4, help='test contracts per risk label')
    ap.add_argument('--seed', type=int, default=7, help='which test contracts get sampled')
    ap.add_argument('--out', default=str(ROOT / 'demo_data' / 'cuad'))
    args = ap.parse_args()

    df = load_from_disk(str(ROOT / 'data' / 'raw' / 'cuad-qa'))['train'].to_pandas()
    final_df = build_final_df(df)
    _, test_df = train_test_split(final_df, test_size=0.2, random_state=42,
                                  stratify=final_df['risk_label'])

    counts = {str(k): int(v) for k, v in test_df['risk_label'].value_counts().items()}
    if counts != EXPECTED_TEST_COUNTS:
        raise SystemExit(f'Test split {counts} differs from the evaluated one {EXPECTED_TEST_COUNTS}. Stopping.')
    print(f'Test split verified: {counts}')

    picked = pd.concat([g.sample(min(len(g), args.per_class), random_state=args.seed)
                        for _, g in test_df.groupby('risk_label', observed=True)])

    df['clause'] = df['question'].map(extract_clause_name)
    out = Path(args.out)
    out.mkdir(parents=True, exist_ok=True)
    used = set()

    for _, r in picked.iterrows():
        rows = df[df['title'] == r['title']]
        clauses = {}
        for c in RISK_CLAUSES:
            hit = rows[(rows['clause'] == c) & rows['has_clause']]
            if len(hit):
                clauses[c] = ' '.join(hit.iloc[0]['answers']['text'][0].split()[:20])
        if len(clauses) != int(r['risk_score']):
            raise SystemExit(f"Clause count mismatch for {r['title']}")

        cid = safe_id(r['title'], used)
        (out / f'{cid}.txt').write_text(rows['context'].iloc[0], encoding='utf-8')
        meta = {
            'title': r['title'],
            'split': 'test',
            'risk_score': int(r['risk_score']),
            'risk_label': str(r['risk_label']),
            'clauses': clauses,
            'model_input': r['risk_text'],   # exactly what the model saw in training/eval
        }
        (out / f'{cid}.snippets.json').write_text(json.dumps(meta, ensure_ascii=False, indent=2),
                                                  encoding='utf-8')
        print(f"{meta['risk_label']:<7} score {meta['risk_score']:>2}  {cid}")

    print(f'\nWrote {len(picked)} contracts to {out.resolve()}')


if __name__ == '__main__':
    main()
