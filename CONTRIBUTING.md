# Contributing to LLMOps Eval Platform

First off, thank you for considering contributing to LLMOps Eval! It's people like you that make this platform a great tool for the LLM community.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [How Can I Contribute?](#how-can-i-contribute)
- [Development Setup](#development-setup)
- [Pull Request Process](#pull-request-process)
- [Coding Standards](#coding-standards)
- [Project Structure](#project-structure)

## Code of Conduct

This project and everyone participating in it is governed by our commitment to fostering an open and welcoming environment. By participating, you are expected to uphold this standard.

## How Can I Contribute?

### Reporting Bugs

Before creating bug reports, please check the existing issues to avoid duplicates. When you create a bug report, include as many details as possible:

- **Use a clear and descriptive title**
- **Describe the exact steps to reproduce the problem**
- **Provide specific examples**
- **Describe the behavior you observed and what you expected**
- **Include screenshots if relevant**
- **Include your environment details** (OS, Java version, Python version, etc.)

### Suggesting Enhancements

Enhancement suggestions are tracked as GitHub issues. When creating an enhancement suggestion, include:

- **Use a clear and descriptive title**
- **Provide a detailed description of the suggested enhancement**
- **Explain why this enhancement would be useful**
- **List any similar features in other tools**

### Pull Requests

- Fill in the required template
- Follow the coding standards
- Include tests for new features
- Update documentation as needed
- Ensure all tests pass

## Development Setup

### Prerequisites

- Java 21+
- Python 3.11+
- Node.js 18+
- PostgreSQL 16
- Maven 3.8+
- Git

### 1. Fork and Clone

```bash
# Fork the repository on GitHub
# Clone your fork
git clone https://github.com/YOUR_USERNAME/llm-eval-ops.git
cd llm-eval-ops

# Add upstream remote
git remote add upstream https://github.com/ashwithpoojary98/llm-eval-ops.git
```

### 2. Database Setup

```bash
# Create development database
createdb llmops_eval_dev

# Create test database
createdb llmops_eval_test
```

### 3. Spring Boot Backend

```bash
# Install dependencies
./mvnw clean install

# Run tests
./mvnw test

# Run the application
./mvnw spring-boot:run
```

### 4. FastAPI Evaluation Engine

```bash
cd evaluation-engine

# Create virtual environment
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install -r requirements.txt
pip install -r requirements-dev.txt  # Development dependencies

# Run tests
pytest

# Run the application
python run.py
```

### 5. Next.js Frontend

```bash
cd frontend

# Install dependencies
npm install

# Run in development mode
npm run dev

# Run linter
npm run lint

# Build for production
npm run build
```

## Pull Request Process

1. **Create a feature branch**
   ```bash
   git checkout -b feature/amazing-feature
   ```

2. **Make your changes**
   - Write clean, readable code
   - Follow the coding standards
   - Add tests for new functionality
   - Update documentation

3. **Commit your changes**
   ```bash
   git add .
   git commit -m "Add amazing feature"
   ```

   Use clear commit messages:
   - `feat: Add new evaluation metric`
   - `fix: Resolve authentication issue`
   - `docs: Update installation guide`
   - `test: Add tests for dataset service`
   - `refactor: Improve evaluation engine performance`

4. **Keep your fork up to date**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

5. **Push to your fork**
   ```bash
   git push origin feature/amazing-feature
   ```

6. **Create a Pull Request**
   - Go to the original repository
   - Click "New Pull Request"
   - Select your feature branch
   - Fill in the PR template
   - Link any related issues

7. **Code Review**
   - Address reviewer feedback
   - Keep the PR focused and small
   - Be responsive to comments

## Coding Standards

### Java (Spring Boot)

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use meaningful variable and method names
- Add JavaDoc for public APIs
- Keep methods small and focused
- Use Lombok to reduce boilerplate
- Write unit tests for services and controllers

Example:
```java
/**
 * Service for managing evaluation runs.
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository repository;

    /**
     * Creates a new evaluation run.
     *
     * @param request the evaluation request
     * @return the created evaluation run
     */
    public EvaluationRun createRun(CreateEvaluationRequest request) {
        // Implementation
    }
}
```

### Python (FastAPI)

- Follow [PEP 8](https://pep8.org/)
- Use type hints
- Add docstrings for functions and classes
- Use `black` for code formatting
- Use `pylint` or `flake8` for linting
- Write unit tests with pytest

Example:
```python
from typing import List, Optional
from pydantic import BaseModel

class EvaluationRequest(BaseModel):
    """Request model for evaluation."""

    dataset_id: str
    endpoint_id: str
    metric_ids: List[str]

async def run_evaluation(request: EvaluationRequest) -> EvaluationResult:
    """
    Run evaluation with the specified configuration.

    Args:
        request: The evaluation request

    Returns:
        The evaluation result
    """
    # Implementation
```

### TypeScript/React (Frontend)

- Follow [Airbnb JavaScript Style Guide](https://github.com/airbnb/javascript)
- Use TypeScript for type safety
- Use functional components with hooks
- Follow React best practices
- Use ESLint and Prettier

Example:
```typescript
interface Project {
  id: string;
  name: string;
  description: string;
}

export const ProjectCard: React.FC<{ project: Project }> = ({ project }) => {
  return (
    <div className="card">
      <h3>{project.name}</h3>
      <p>{project.description}</p>
    </div>
  );
};
```

### Testing

- Write tests for all new features
- Maintain or improve code coverage
- Use meaningful test names
- Follow AAA pattern: Arrange, Act, Assert

Java example:
```java
@Test
void createEvaluationRun_WithValidRequest_ShouldReturnCreatedRun() {
    // Arrange
    CreateEvaluationRequest request = new CreateEvaluationRequest();

    // Act
    EvaluationRun result = evaluationService.createRun(request);

    // Assert
    assertNotNull(result);
    assertEquals("PENDING", result.getStatus());
}
```

Python example:
```python
def test_calculate_bleu_score_returns_correct_value():
    # Arrange
    reference = "The cat is on the mat"
    candidate = "The cat is on the mat"

    # Act
    score = calculate_bleu(reference, candidate)

    # Assert
    assert score == 1.0
```

## Project Structure

```
llmops-eval/
├── src/                          # Spring Boot backend
│   ├── main/
│   │   ├── java/
│   │   │   └── io/github/ashwithpoojary98/llmops_eval/
│   │   │       ├── config/       # Configuration
│   │   │       ├── controller/   # REST controllers
│   │   │       ├── dto/          # Data transfer objects
│   │   │       ├── entity/       # JPA entities
│   │   │       ├── repository/   # Data repositories
│   │   │       ├── service/      # Business logic
│   │   │       └── security/     # Security configuration
│   │   └── resources/
│   │       └── db/migration/     # Flyway migrations
│   └── test/                     # Tests
│
├── evaluation-engine/            # FastAPI evaluation engine
│   ├── app/
│   │   ├── api/                  # API routes
│   │   ├── core/                 # Core configuration
│   │   ├── models/               # Pydantic models
│   │   ├── services/             # Business logic
│   │   │   ├── llm_clients/      # LLM provider clients
│   │   │   └── metrics/          # Metric implementations
│   │   └── utils/                # Utilities
│   └── tests/                    # Tests
│
├── frontend/                     # Next.js frontend
│   ├── src/
│   │   ├── app/                  # App router pages
│   │   ├── components/           # React components
│   │   ├── lib/                  # Utilities
│   │   └── types/                # TypeScript types
│   └── public/                   # Static assets
│
└── docs/                         # Documentation
```

## Areas We Need Help

- **Metrics Implementation**: More evaluation metrics
- **LLM Providers**: Additional provider integrations
- **Documentation**: Tutorials, examples, API docs
- **Testing**: Improve test coverage
- **UI/UX**: Frontend improvements
- **DevOps**: Kubernetes configs, CI/CD improvements
- **Examples**: Real-world usage examples

## Getting Help

- Check the [documentation](docs/)
- Search [existing issues](https://github.com/ashwithpoojary98/llm-eval-ops/issues)
- Ask in [GitHub Discussions](https://github.com/ashwithpoojary98/llm-eval-ops/discussions)
- Join our community (Discord link coming soon)

## Recognition

Contributors will be recognized in:
- README.md contributors section
- Release notes
- Project website (coming soon)

## License

By contributing, you agree that your contributions will be licensed under the Apache License 2.0.

---

Thank you for contributing to LLMOps Eval! 🎉
