#fine tune.py
import re
import sys
from pathlib import Path

import numpy as np
import pandas as pd
import torch
from datasets import Dataset, load_from_disk
from sklearn.metrics import (
    accuracy_score,
    classification_report,
    confusion_matrix,
    precision_recall_fscore_support,
)
from sklearn.model_selection import train_test_split
from torch.nn import CrossEntropyLoss
from transformers import (
    AutoModelForSequenceClassification,
    AutoTokenizer,
    Trainer,
    TrainingArguments,
)

ROOT = Path(__file__).resolve().parents[1]
if str(ROOT) not in sys.path:
    sys.path.insert(0, str(ROOT))


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


def build_final_df():
    dataset_path = ROOT / 'data' / 'raw' / 'cuad-qa'
    ds = load_from_disk(str(dataset_path))
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
    return final_df


def compute_metrics(eval_pred):
    logits, labels = eval_pred
    predictions = logits.argmax(axis=-1)
    precision, recall, f1, _ = precision_recall_fscore_support(
        labels, predictions, average='weighted', zero_division=0
    )
    acc = accuracy_score(labels, predictions)
    return {
        'accuracy': acc,
        'f1': f1,
        'precision': precision,
        'recall': recall,
    }


final_df = build_final_df()
train_df, test_df = train_test_split(
    final_df,
    test_size=0.2,
    random_state=42,
    stratify=final_df['risk_label'],
)

tokenizer = AutoTokenizer.from_pretrained('bert-base-uncased')


def tokenize_fn(examples):
    return tokenizer(
        examples['risk_text'],
        truncation=True,
        max_length=512,
        padding='max_length',
    )


label2id = {'Low': 0, 'Medium': 1, 'High': 2}
id2label = {v: k for k, v in label2id.items()}

train_df['risk_label'] = train_df['risk_label'].map(label2id)
test_df['risk_label'] = test_df['risk_label'].map(label2id)

train_dataset = Dataset.from_pandas(train_df).map(tokenize_fn, batched=True)
test_dataset = Dataset.from_pandas(test_df).map(tokenize_fn, batched=True)

train_dataset = train_dataset.rename_column('risk_label', 'labels')
test_dataset = test_dataset.rename_column('risk_label', 'labels')
train_dataset.set_format('torch', columns=['input_ids', 'attention_mask', 'labels'])
test_dataset.set_format('torch', columns=['input_ids', 'attention_mask', 'labels'])

model = AutoModelForSequenceClassification.from_pretrained(
    'bert-base-uncased',
    num_labels=3,
    id2label=id2label,
    label2id=label2id,
)

training_args = TrainingArguments(
    output_dir=str(ROOT / 'results'),
    eval_strategy='epoch',
    save_strategy='epoch',
    learning_rate=2e-5,
    per_device_train_batch_size=8,
    per_device_eval_batch_size=8,
    num_train_epochs=1,
    weight_decay=0.01,
    logging_steps=25,
    load_best_model_at_end=True,
    report_to='none',
)

class_weights = torch.tensor([1.0, 1.6, 1.6])


class WeightedTrainer(Trainer):
    def compute_loss(self, model, inputs, return_outputs=False, **kwargs):
        labels = inputs.pop('labels')
        outputs = model(**inputs)
        logits = outputs.logits
        loss_fct = CrossEntropyLoss(weight=class_weights.to(logits.device))
        loss = loss_fct(logits.view(-1, 3), labels.view(-1))
        return (loss, outputs) if return_outputs else loss


trainer = WeightedTrainer(
    model=model,
    args=training_args,
    train_dataset=train_dataset,
    eval_dataset=test_dataset,
    compute_metrics=compute_metrics,
)

trainer.train()

# After training, run final evaluation explicitly
eval_results = trainer.evaluate()
print(eval_results)

predictions = trainer.predict(test_dataset)
preds = np.argmax(predictions.predictions, axis=1)
labels = predictions.label_ids

print(confusion_matrix(labels, preds))
print(classification_report(labels, preds, target_names=['Low', 'Medium', 'High']))