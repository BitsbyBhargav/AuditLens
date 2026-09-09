# NLP Fine-Tuning Results

## Final Configuration

- Model: `bert-base-uncased`
- Task: Three-class contract risk classification
- Class weights: `[1.0, 1.6, 1.6]` for Low, Medium, High
- Training epochs: 1
- Test samples: 82

## Evaluation Metrics

```text
eval_loss: 1.0374914407730103
eval_accuracy: 0.4878048780487805
eval_f1: 0.4717294900221729
eval_precision: 0.5094055495210823
eval_recall: 0.4878048780487805
```

## Confusion Matrix

Rows are actual labels and columns are predicted labels. Label order: `Low`, `Medium`, `High`.

```text
[[17 13  6]
 [ 5  5 15]
 [ 2  1 18]]
```

## Classification Report

```text
              precision    recall  f1-score   support

         Low       0.71      0.47      0.57        36
      Medium       0.26      0.20      0.23        25
        High       0.46      0.86      0.60        21

    accuracy                           0.49        82
   macro avg       0.48      0.51      0.46        82
weighted avg       0.51      0.49      0.47        82
```

The weighted model predicts all three classes. High-risk recall improved substantially, while Medium remains the weakest class.
