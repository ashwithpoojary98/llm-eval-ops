import React from 'react';
import Link from '@docusaurus/Link';
import useDocusaurusContext from '@docusaurus/useDocusaurusContext';
import Layout from '@theme/Layout';
import styles from './index.module.css';

const FEATURES = [
  {
    icon: '🎯',
    title: 'No-Code Evaluation',
    description:
      'Configure and run LLM evaluations entirely through the UI. No custom code or ML expertise required.',
  },
  {
    icon: '📊',
    title: '20+ Built-in Metrics',
    description:
      'BLEU, ROUGE, BERTScore, Faithfulness, Context Relevance, LLM-as-Judge and more — all out of the box.',
  },
  {
    icon: '🔌',
    title: 'Multi-Provider Support',
    description:
      'Connect to OpenAI, Anthropic, Azure OpenAI, AWS Bedrock, Google Vertex AI, or any custom API.',
  },
  {
    icon: '🔍',
    title: 'RAG Evaluation',
    description:
      'Purpose-built metrics for RAG systems: Faithfulness, Answer Relevancy, Context Precision and Recall.',
  },
  {
    icon: '⚙️',
    title: 'CI/CD Integration',
    description:
      'Trigger evaluations via API, set quality gates, and integrate with GitHub Actions or GitLab CI.',
  },
  {
    icon: '🏢',
    title: 'Multi-Tenant',
    description:
      'Organizations, teams, projects, and role-based access control — built for enterprise use.',
  },
];

const WORKFLOW_STEPS = [
  'Define Project',
  'Upload Dataset',
  'Configure Endpoint',
  'Select Metrics',
  'Run Evaluation',
  'View Results',
];

const SCREENSHOTS = [
  { src: 'img/screenshots/dashboard.png', label: 'Dashboard' },
  { src: 'img/screenshots/project.png', label: 'Projects' },
  { src: 'img/screenshots/datasets.png', label: 'Datasets' },
  { src: 'img/screenshots/testcases.png', label: 'Test Cases' },
  { src: 'img/screenshots/teams.png', label: 'Teams' },
  { src: 'img/screenshots/Setting.png', label: 'Settings' },
];

function HeroBanner() {
  return (
    <header className="hero-banner">
      <div>
        <h1 className="hero-title">LLMOps Eval</h1>
        <p className="hero-subtitle">
          Production-grade LLM &amp; RAG evaluation platform.<br />
          No-code. Multi-provider. CI/CD ready.
        </p>
        <div className="hero-buttons">
          <Link className="button button--primary button--lg" to="/docs/intro">
            Get Started →
          </Link>
          <Link
            className="button button--secondary button--lg"
            href="https://github.com/ashwithpoojary98/llmops-eval"
          >
            ⭐ Star on GitHub
          </Link>
        </div>
        <div className="tech-stack">
          {['Java 21', 'Spring Boot', 'FastAPI', 'Next.js 14', 'PostgreSQL', 'Apache 2.0'].map(
            (t) => (
              <span key={t} className="tech-badge">
                {t}
              </span>
            ),
          )}
        </div>
      </div>
    </header>
  );
}

function WorkflowSection() {
  return (
    <section className="workflow-section">
      <div className="container">
        <h2 className="section-title">Simple 6-step workflow</h2>
        <p className="section-subtitle">From dataset to evaluation results in minutes</p>
        <div className="workflow-steps">
          {WORKFLOW_STEPS.map((step, i) => (
            <div key={i} className="workflow-step">
              <div className="step-number">{i + 1}</div>
              <div className="step-label">{step}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function FeaturesSection() {
  return (
    <section className="features-section">
      <div className="container">
        <h2 className="section-title">Everything you need to evaluate LLMs</h2>
        <p className="section-subtitle">
          Stop building custom evaluation frameworks. Start evaluating.
        </p>
        <div className="features-grid">
          {FEATURES.map((f, i) => (
            <div key={i} className="feature-card">
              <div className="feature-icon">{f.icon}</div>
              <h3>{f.title}</h3>
              <p>{f.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function ScreenshotsSection() {
  return (
    <section className="screenshots-section">
      <div className="container">
        <h2 className="section-title">See it in action</h2>
        <p className="section-subtitle">A clean, intuitive UI — no code required</p>
        <div className="screenshots-grid">
          {SCREENSHOTS.map((s, i) => (
            <div key={i} className="screenshot-card">
              <img src={s.src} alt={s.label} />
              <div className="screenshot-label">{s.label}</div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function MetricsSection() {
  return (
    <section className="features-section">
      <div className="container">
        <h2 className="section-title">Comprehensive metric coverage</h2>
        <p className="section-subtitle">20+ metrics across four categories</p>
        <div className="features-grid">
          {[
            {
              icon: '📝',
              title: 'Traditional NLP',
              description: 'BLEU, ROUGE, Exact Match, Levenshtein, BERTScore',
            },
            {
              icon: '🔍',
              title: 'RAG-Specific',
              description: 'Faithfulness, Answer Relevancy, Context Precision, Context Recall',
            },
            {
              icon: '⚖️',
              title: 'LLM-as-Judge',
              description: 'Relevance, Coherence, Fluency, Toxicity, Custom criteria',
            },
            {
              icon: '⚡',
              title: 'Performance',
              description: 'Latency, Token Count, Cost per evaluation',
            },
          ].map((m, i) => (
            <div key={i} className="feature-card">
              <div className="feature-icon">{m.icon}</div>
              <h3>{m.title}</h3>
              <p>{m.description}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

function CTASection() {
  return (
    <section className="cta-section">
      <h2 className="cta-title">Ready to evaluate your LLMs?</h2>
      <p className="cta-subtitle">
        Open source. Free forever. Apache 2.0 license.
      </p>
      <div className="cta-buttons">
        <Link className="button button--secondary button--lg" to="/docs/quick-start">
          Quick Start Guide
        </Link>
        <Link
          className="button button--outline button--lg"
          style={{ color: 'white', borderColor: 'white' }}
          href="https://github.com/ashwithpoojary98/llmops-eval"
        >
          View on GitHub
        </Link>
      </div>
    </section>
  );
}

export default function Home() {
  const { siteConfig } = useDocusaurusContext();
  return (
    <Layout
      title={siteConfig.title}
      description="Production-grade LLM/RAG evaluation platform. No-code, multi-provider, CI/CD ready."
    >
      <HeroBanner />
      <main>
        <WorkflowSection />
        <FeaturesSection />
        <ScreenshotsSection />
        <MetricsSection />
        <CTASection />
      </main>
    </Layout>
  );
}
