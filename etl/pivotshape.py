import pandas as pd
from datasets import Dataset, load_from_disk
from transformers import AutoTokenizer

ds = load_from_disk('data/raw/cuad-qa')
df = ds['train'].to_pandas()

# Mark whether each clause question actually has an answer in this contract
df['has_clause'] = df['answers'].apply(lambda a: len(a['text']) > 0)

# One row per contract, one column per clause type, True/False if that clause exists
pivot = df.pivot_table(index='title', columns='question', values='has_clause', aggfunc='max', fill_value=False)

print(pivot.shape)          # should show (num_contracts, 41-ish clause categories)
print(pivot.columns.tolist()[:5])   # preview a few clause-category names

import re

def extract_clause_name(question):
    match = re.search(r'"(.*?)"', question)
    return match.group(1) if match else question

pivot.columns = [extract_clause_name(q) for q in pivot.columns]
print(pivot.columns.tolist())

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
    'Liquidated Damages'
]

pivot['risk_score'] = pivot[risk_clauses].sum(axis=1)
print(pivot['risk_score'].describe())
print(pivot['risk_score'].value_counts().sort_index())

# Get one representative text chunk per contract (from the original QA-format dataframe)
contract_text = df.groupby('title')['context'].first()

# Merge risk scores with contract text
final_df = pivot[['risk_score']].join(contract_text)
final_df = final_df.reset_index()  # 'title' becomes a normal column

# Derive a simple risk label from the numeric score for downstream reporting
final_df['risk_label'] = pd.cut(
    final_df['risk_score'],
    bins=[-1, 2, 4, 10],
    labels=['Low', 'Medium', 'High'],
    right=True
)

print(final_df.shape)
print(final_df.head(2))
print(final_df['context'].str.len().describe())  # check text length spread


# For each contract, concatenate only the actual flagged clause text (not full document)
def get_risk_text(title_group, max_words_per_clause=20):
    texts = []
    for _, row in title_group.iterrows():
        if len(row['answers']['text']) > 0:
            clause_text = row['answers']['text'][0]
            words = clause_text.split()[:max_words_per_clause]
            texts.append(' '.join(words))
    return ' '.join(texts)

risk_text = df.groupby('title').apply(get_risk_text)

final_df = pivot[['risk_score']].join(risk_text.rename('risk_text')).reset_index()
final_df['risk_label'] = pd.cut(
    final_df['risk_score'],
    bins=[-1, 2, 4, 10],
    labels=['Low', 'Medium', 'High'],
    right=True
)

print(final_df['risk_text'].str.len().describe())
print(final_df[['title', 'risk_score', 'risk_text', 'risk_label']].head(2))

tokenizer = AutoTokenizer.from_pretrained('bert-base-uncased')
token_lengths = final_df['risk_text'].apply(
    lambda text: len(tokenizer.encode(text, truncation=False))
)
print(token_lengths.describe())
print((token_lengths > 512).sum(), 'contracts still exceed 512 tokens')


def tokenize_fn(examples):
    return tokenizer(
        examples['risk_text'],
        truncation=True,
        max_length=512,
        padding='max_length'
    )


hf_dataset = Dataset.from_dict(final_df.to_dict(orient='list'))
tokenized_dataset = hf_dataset.map(
    tokenize_fn,
    batched=True,
    new_fingerprint='risk-text-tokenized-v1'
)

print(tokenized_dataset)
print(tokenized_dataset[0].keys())

print(final_df['risk_label'].value_counts())