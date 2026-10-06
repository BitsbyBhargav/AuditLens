#train_test_split.py
import re

import pandas as pd
from datasets import load_from_disk
from sklearn.model_selection import train_test_split


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


ds = load_from_disk('data/raw/cuad-qa')
df = ds['train'].to_pandas()

df['has_clause'] = df['answers'].apply(lambda a: len(a['text']) > 0)

pivot = df.pivot_table(
    index='title',
    columns='question',
    values='has_clause',
    aggfunc='max',
    fill_value=False,
)

pivot.columns = [extract_clause_name(q) for q in pivot.columns]

risk_clauses = [
    'Anti-Assignment',
    'Audit Rights',
    'Change Of Control',
    'Most Favored Nation',
    'Non-Compete',
    'Uncapped Liability',
    'Exclusivity',
    'Termination For Convenience',
    'Ip Ownership Assignment',
    'Liquidated Damages',
]

pivot['risk_score'] = pivot[risk_clauses].sum(axis=1)

risk_text = df.groupby('title').apply(get_risk_text)
final_df = pivot[['risk_score']].join(risk_text.rename('risk_text')).reset_index()
final_df['risk_label'] = pd.cut(
    final_df['risk_score'],
    bins=[-1, 2, 4, 10],
    labels=['Low', 'Medium', 'High'],
    right=True,
)

train_df, test_df = train_test_split(
    final_df,
    test_size=0.2,
    random_state=42,
    stratify=final_df['risk_label'],
)

print(train_df['risk_label'].value_counts())
print(test_df['risk_label'].value_counts())