"""
Tests for evaluation metrics.
"""
import pytest
from app.metrics.traditional import (
    BLEUMetric,
    Rouge1Metric,
    RougeLMetric,
    ExactMatchMetric,
    ContainsMetric,
    LevenshteinMetric,
)


class TestBLEUMetric:
    """Tests for BLEU metric."""

    def test_bleu_identical_text(self):
        """Test BLEU with identical text."""
        metric = BLEUMetric()
        result = metric.calculate(
            question="What is Python?",
            generated_output="Python is a programming language.",
            ground_truth="Python is a programming language.",
        )
        assert result.score > 0.9
        assert result.error is None

    def test_bleu_different_text(self):
        """Test BLEU with completely different text."""
        metric = BLEUMetric()
        result = metric.calculate(
            question="What is Python?",
            generated_output="The sky is blue.",
            ground_truth="Python is a programming language.",
        )
        assert result.score < 0.3
        assert result.error is None

    def test_bleu_partial_match(self):
        """Test BLEU with partial overlap."""
        metric = BLEUMetric()
        result = metric.calculate(
            question="What is Python?",
            generated_output="Python is a great programming language for beginners.",
            ground_truth="Python is a programming language.",
        )
        assert 0.3 < result.score < 0.9
        assert result.error is None

    def test_bleu_missing_ground_truth(self):
        """Test BLEU with missing ground truth."""
        metric = BLEUMetric()
        result = metric.calculate(
            question="What is Python?",
            generated_output="Python is a programming language.",
            ground_truth=None,
        )
        assert result.error is not None


class TestROUGEMetric:
    """Tests for ROUGE metrics."""

    def test_rouge1_identical(self):
        """Test ROUGE-1 with identical text."""
        metric = Rouge1Metric()
        result = metric.calculate(
            question="Summarize",
            generated_output="The quick brown fox jumps.",
            ground_truth="The quick brown fox jumps.",
        )
        assert result.score == 1.0
        assert "precision" in result.details
        assert "recall" in result.details

    def test_rougel_overlap(self):
        """Test ROUGE-L with overlapping sequences."""
        metric = RougeLMetric()
        result = metric.calculate(
            question="Summarize",
            generated_output="The quick brown fox jumps over the lazy dog.",
            ground_truth="A quick brown fox jumped over a lazy dog.",
        )
        assert result.score > 0.5


class TestExactMatchMetric:
    """Tests for Exact Match metric."""

    def test_exact_match_true(self):
        """Test exact match with identical text."""
        metric = ExactMatchMetric()
        result = metric.calculate(
            question="What is 2+2?",
            generated_output="4",
            ground_truth="4",
        )
        assert result.score == 1.0

    def test_exact_match_case_insensitive(self):
        """Test case-insensitive match."""
        metric = ExactMatchMetric()
        result = metric.calculate(
            question="What is the capital?",
            generated_output="PARIS",
            ground_truth="paris",
        )
        assert result.score == 1.0

    def test_exact_match_whitespace(self):
        """Test match with different whitespace."""
        metric = ExactMatchMetric()
        result = metric.calculate(
            question="Name?",
            generated_output="John  Doe",
            ground_truth="John Doe",
        )
        assert result.score == 1.0

    def test_exact_match_false(self):
        """Test non-matching text."""
        metric = ExactMatchMetric()
        result = metric.calculate(
            question="What is 2+2?",
            generated_output="5",
            ground_truth="4",
        )
        assert result.score == 0.0


class TestContainsMetric:
    """Tests for Contains metric."""

    def test_contains_true(self):
        """Test when text contains ground truth."""
        metric = ContainsMetric()
        result = metric.calculate(
            question="What is the capital of France?",
            generated_output="The capital of France is Paris, a beautiful city.",
            ground_truth="Paris",
        )
        assert result.score == 1.0

    def test_contains_false(self):
        """Test when text doesn't contain ground truth."""
        metric = ContainsMetric()
        result = metric.calculate(
            question="What is the capital of France?",
            generated_output="The capital is London.",
            ground_truth="Paris",
        )
        assert result.score == 0.0


class TestLevenshteinMetric:
    """Tests for Levenshtein metric."""

    def test_levenshtein_identical(self):
        """Test with identical text."""
        metric = LevenshteinMetric()
        result = metric.calculate(
            question="Spell check",
            generated_output="hello world",
            ground_truth="hello world",
        )
        assert result.score == 1.0
        assert result.raw_score == 0  # Zero edit distance

    def test_levenshtein_small_difference(self):
        """Test with small edit distance."""
        metric = LevenshteinMetric()
        result = metric.calculate(
            question="Spell check",
            generated_output="hello word",
            ground_truth="hello world",
        )
        assert result.score > 0.8  # One character difference
        assert result.raw_score == 1  # One edit

    def test_levenshtein_large_difference(self):
        """Test with large edit distance."""
        metric = LevenshteinMetric()
        result = metric.calculate(
            question="Spell check",
            generated_output="abc",
            ground_truth="xyz123",
        )
        assert result.score < 0.5


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
