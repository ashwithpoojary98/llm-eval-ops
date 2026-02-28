-- V9: Seed default evaluation metrics
-- These are system-defined metrics available to all projects

INSERT INTO evaluation_metrics (code, display_name, description, category, requires_ground_truth, requires_context, requires_judge_llm, default_weight, default_threshold, is_system) VALUES
-- Traditional NLP Metrics
('BLEU', 'BLEU Score', 'Bilingual Evaluation Understudy - measures n-gram overlap between generated and reference text', 'TRADITIONAL_NLP', true, false, false, 1.0, 0.3, true),
('ROUGE_1', 'ROUGE-1', 'Recall-Oriented Understudy for Gisting Evaluation - unigram overlap', 'TRADITIONAL_NLP', true, false, false, 1.0, 0.4, true),
('ROUGE_2', 'ROUGE-2', 'ROUGE with bigram overlap', 'TRADITIONAL_NLP', true, false, false, 1.0, 0.2, true),
('ROUGE_L', 'ROUGE-L', 'ROUGE with longest common subsequence', 'TRADITIONAL_NLP', true, false, false, 1.0, 0.35, true),
('EXACT_MATCH', 'Exact Match', 'Binary score - 1 if generated text exactly matches reference', 'TRADITIONAL_NLP', true, false, false, 1.0, 1.0, true),
('CONTAINS', 'Contains Match', 'Binary score - 1 if generated text contains the reference text', 'TRADITIONAL_NLP', true, false, false, 1.0, 1.0, true),
('LEVENSHTEIN', 'Levenshtein Similarity', 'Edit distance-based similarity (1 - normalized_distance)', 'TRADITIONAL_NLP', true, false, false, 1.0, 0.7, true),

-- Semantic Metrics
('SEMANTIC_SIMILARITY', 'Semantic Similarity', 'Cosine similarity between sentence embeddings', 'SEMANTIC', true, false, false, 1.0, 0.7, true),
('BERTSCORE', 'BERTScore', 'Contextual embedding similarity using BERT', 'SEMANTIC', true, false, false, 1.0, 0.7, true),

-- RAG-Specific Metrics
('FAITHFULNESS', 'Faithfulness', 'Measures if the answer is grounded in the provided context (no hallucination)', 'RAG_SPECIFIC', false, true, true, 1.0, 0.7, true),
('ANSWER_RELEVANCY', 'Answer Relevancy', 'Measures if the answer addresses the question', 'RAG_SPECIFIC', false, true, true, 1.0, 0.7, true),
('CONTEXT_PRECISION', 'Context Precision', 'Measures if retrieved context is useful for answering', 'RAG_SPECIFIC', true, true, true, 1.0, 0.7, true),
('CONTEXT_RECALL', 'Context Recall', 'Measures if all relevant information was retrieved', 'RAG_SPECIFIC', true, true, true, 1.0, 0.7, true),
('CONTEXT_RELEVANCY', 'Context Relevancy', 'Measures relevance of retrieved context to the question', 'RAG_SPECIFIC', false, true, true, 1.0, 0.7, true),

-- LLM-as-Judge Metrics
('RELEVANCE', 'Relevance (Judge)', 'LLM judges how relevant the response is to the query', 'LLM_AS_JUDGE', false, false, true, 1.0, 0.6, true),
('COHERENCE', 'Coherence (Judge)', 'LLM judges logical flow and structure of response', 'LLM_AS_JUDGE', false, false, true, 1.0, 0.6, true),
('FLUENCY', 'Fluency (Judge)', 'LLM judges grammar and readability', 'LLM_AS_JUDGE', false, false, true, 1.0, 0.6, true),
('TOXICITY', 'Toxicity Detection', 'LLM detects harmful or inappropriate content (higher score = safer)', 'LLM_AS_JUDGE', false, false, true, 1.0, 0.8, true),
('ANSWER_CORRECTNESS', 'Answer Correctness (Judge)', 'LLM judges if the answer is factually correct', 'LLM_AS_JUDGE', true, false, true, 1.0, 0.7, true),
('CUSTOM_JUDGE', 'Custom Judge Metric', 'Custom evaluation using user-defined prompt template', 'LLM_AS_JUDGE', false, false, true, 1.0, NULL, true),

-- Performance Metrics
('LATENCY', 'Response Latency', 'Time taken for LLM to respond (in milliseconds)', 'PERFORMANCE', false, false, false, 0.5, NULL, true),
('TOKEN_COUNT', 'Token Count', 'Total tokens used (input + output)', 'PERFORMANCE', false, false, false, 0.5, NULL, true),
('COST', 'API Cost', 'Estimated API cost in USD', 'PERFORMANCE', false, false, false, 0.5, NULL, true);
