export type UserRole = "ADMIN" | "EVALUATOR" | "DEVELOPER" | "VIEWER";
export type AccessLevel = "READ" | "WRITE" | "ADMIN";
export type ProjectStatus = "ACTIVE" | "ARCHIVED";
export type TeamMemberRole = "OWNER" | "CONTRIBUTOR" | "MEMBER";

export interface User {
  userId: string;
  email: string;
  fullName: string;
  role: UserRole;
  organizationId: string;
  organizationName: string;
  mustChangePassword: boolean;
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
  userId: string;
  email: string;
  fullName: string;
  role: UserRole;
  organizationId: string;
  organizationName: string;
  mustChangePassword: boolean;
}

export interface ApiResponse<T> {
  success: boolean;
  statusCode: number;
  message?: string;
  data?: T;
  timestamp: string;
  error?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

// Project Types
export interface ProjectTeam {
  teamId: string;
  teamName: string;
  accessLevel: AccessLevel;
  assignedAt: string;
}

export interface ProjectMember {
  userId: string;
  fullName: string;
  email: string;
  accessLevel: AccessLevel;
  assignedAt: string;
}

export interface Project {
  id: string;
  name: string;
  description: string;
  status: ProjectStatus;
  createdById: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
  teams?: ProjectTeam[];
  members?: ProjectMember[];
  myAccessLevel?: AccessLevel;
}

export interface CreateProjectRequest {
  name: string;
  description?: string;
}

export interface UpdateProjectRequest {
  name?: string;
  description?: string;
}

// Team Types
export interface TeamMember {
  userId: string;
  fullName: string;
  email: string;
  role: TeamMemberRole;
  joinedAt: string;
}

export interface Team {
  id: string;
  name: string;
  description: string;
  memberCount?: number;
  members?: TeamMember[];
  createdById?: string;
  createdByName?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamRequest {
  name: string;
  description?: string;
}

export interface UpdateTeamRequest {
  name?: string;
  description?: string;
}

// Admin Types
export type InvitationStatus = "PENDING" | "ACCEPTED" | "EXPIRED" | "REVOKED";

export interface Invitation {
  id: string;
  email: string;
  role: UserRole;
  status: InvitationStatus;
  invitedById: string;
  invitedByName: string;
  expiresAt: string;
  createdAt: string;
  acceptedAt?: string;
}

export interface InviteUserRequest {
  email: string;
  role: UserRole;
}

export interface OrganizationUser {
  id: string;
  email: string;
  fullName: string;
  role: UserRole;
  isActive: boolean;
  emailVerified: boolean;
  lastLoginAt?: string;
  createdAt: string;
}

export interface UpdateUserRoleRequest {
  role: UserRole;
}

// Invitation Acceptance Types
export interface InvitationDetails {
  email: string;
  role: UserRole;
  organizationName: string;
  invitedByName: string;
  expiresAt: string;
  status: InvitationStatus;
}

export interface AcceptInvitationRequest {
  token: string;
  fullName: string;
  password: string;
}

// Dataset Types
export interface Dataset {
  id: string;
  projectId: string;
  projectName: string;
  name: string;
  description?: string;
  isActive: boolean;
  testCaseCount: number;
  testCases?: TestCaseSummary[];
  createdById: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface TestCaseSummary {
  id: string;
  projectId: string;
  question: string;
  groundTruth?: string;
  isActive: boolean;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDatasetRequest {
  name: string;
  description?: string;
}

export interface UpdateDatasetRequest {
  name?: string;
  description?: string;
}

// Test Case Types
export interface TestCase {
  id: string;
  projectId: string;
  projectName?: string;
  question: string;
  llmOutput?: string;
  retrievedContext?: string[];
  groundTruth?: string;
  isActive: boolean;
  createdById: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTestCaseRequest {
  question: string;
  llmOutput?: string;
  retrievedContext?: string[];
  groundTruth?: string;
}

export interface UpdateTestCaseRequest {
  question?: string;
  llmOutput?: string;
  retrievedContext?: string[];
  groundTruth?: string;
}

// LLM Endpoint Types
export type ProviderType = "OPENAI" | "ANTHROPIC" | "AZURE_OPENAI" | "AWS_BEDROCK" | "GOOGLE_VERTEX" | "CUSTOM";
export type AuthType = "API_KEY" | "BEARER" | "BASIC" | "NONE";
export type LlmType = "CLIENT" | "JUDGE";

export interface LlmEndpoint {
  id: string;
  projectId: string;
  projectName: string;
  name: string;
  description?: string;
  providerType: ProviderType;
  modelName: string;
  apiUrl?: string;
  authType: AuthType;
  hasApiKey: boolean;
  modelConfig?: Record<string, unknown>;
  llmType: LlmType;
  isActive: boolean;
  isDefault: boolean;
  createdById: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface LlmEndpointSummary {
  id: string;
  projectId: string;
  projectName: string;
  name: string;
  providerType: ProviderType;
  modelName: string;
  llmType: LlmType;
  isActive: boolean;
  isDefault: boolean;
  createdAt: string;
}

export interface CreateLlmEndpointRequest {
  name: string;
  description?: string;
  providerType: ProviderType;
  modelName: string;
  apiUrl?: string;
  authType?: AuthType;
  apiKey: string;
  modelConfig?: Record<string, unknown>;
  llmType?: LlmType;
  isDefault?: boolean;
}

export interface UpdateLlmEndpointRequest {
  name?: string;
  description?: string;
  providerType?: ProviderType;
  modelName?: string;
  apiUrl?: string;
  authType?: AuthType;
  apiKey?: string;
  modelConfig?: Record<string, unknown>;
  llmType?: LlmType;
  isDefault?: boolean;
}

// Evaluation Metric Types
export type MetricCategory = "TRADITIONAL_NLP" | "SEMANTIC" | "RAG_SPECIFIC" | "LLM_AS_JUDGE" | "PERFORMANCE";

export interface EvaluationMetric {
  id: string;
  code: string;
  displayName: string;
  description?: string;
  category: MetricCategory;
  requiresGroundTruth: boolean;
  requiresContext: boolean;
  requiresJudgeLlm: boolean;
  defaultWeight: number;
  defaultThreshold?: number;
  isSystem: boolean;
  isActive: boolean;
  createdAt: string;
}

// Evaluation Run Types
export type EvaluationMode = "PRE_GENERATED" | "LIVE_GENERATION";
export type EvaluationStatus = "PENDING" | "RUNNING" | "COMPLETED" | "FAILED" | "CANCELLED";
export type TriggerType = "MANUAL" | "CI_CD" | "SCHEDULED";
export type ResultStatus = "PENDING" | "COMPLETED" | "FAILED" | "SKIPPED";

export interface MetricConfigRequest {
  metricId: string;
  weight?: number;
  passThreshold?: number;
  customPromptTemplate?: string;
}

export interface StartEvaluationRequest {
  name: string;
  datasetId: string;
  evaluationMode?: EvaluationMode;
  targetLlmEndpointId?: string;
  judgeLlmEndpointId?: string;
  metrics: MetricConfigRequest[];
  triggerType?: TriggerType;
  triggerReference?: string;
}

export interface MetricSummary {
  metricCode: string;
  metricName: string;
  averageScore: number;
  minScore: number;
  maxScore: number;
  stdDev: number;
  passRate?: number;
  totalEvaluated: number;
}

export interface EvaluationRunMetric {
  metricId: string;
  metricCode: string;
  metricName: string;
  category: MetricCategory;
  weight: number;
  passThreshold?: number;
  hasCustomPrompt: boolean;
}

export interface EvaluationRun {
  id: string;
  projectId: string;
  projectName: string;
  datasetId: string;
  datasetName: string;
  name: string;
  runNumber: number;
  evaluationMode: EvaluationMode;
  targetLlmEndpointId?: string;
  targetLlmEndpointName?: string;
  judgeLlmEndpointId?: string;
  judgeLlmEndpointName?: string;
  status: EvaluationStatus;
  totalTestCases: number;
  completedTestCases: number;
  failedTestCases: number;
  overallScore?: number;
  metricSummaries?: Record<string, MetricSummary>;
  totalTokens?: number;
  totalCost?: number;
  triggerType: TriggerType;
  triggerReference?: string;
  errorMessage?: string;
  createdById: string;
  createdByName: string;
  createdAt: string;
  startedAt?: string;
  completedAt?: string;
  metrics?: EvaluationRunMetric[];
}

export interface EvaluationRunSummary {
  id: string;
  name: string;
  runNumber: number;
  datasetName: string;
  status: EvaluationStatus;
  totalTestCases: number;
  completedTestCases: number;
  overallScore?: number;
  createdAt: string;
  completedAt?: string;
}

export interface EvaluationScore {
  metricCode: string;
  metricName: string;
  score: number;
  rawScore: number;
  passed?: boolean;
  details?: Record<string, unknown>;
  judgeReasoning?: string;
  errorMessage?: string;
}

export interface EvaluationResult {
  id: string;
  testCaseId: string;
  question: string;
  groundTruth?: string;
  llmOutput?: string;
  generatedOutput?: string;
  status: ResultStatus;
  llmLatencyMs?: number;
  processingTimeMs?: number;
  inputTokens?: number;
  outputTokens?: number;
  errorMessage?: string;
  createdAt: string;
  scores: EvaluationScore[];
}

// ── Admin Settings ────────────────────────────────────────────────────────────
export interface EmailConfigResponse {
  smtpHost?: string;
  smtpPort?: number;
  smtpUsername?: string;
  hasPassword: boolean;
  fromAddress?: string;
  fromName?: string;
  enableTls: boolean;
  enableSsl: boolean;
  connectionTimeoutMs: number;
  emailEnabled: boolean;
  configured: boolean;
  lastUpdatedAt?: string;
}

export interface TestEmailResponse {
  success: boolean;
  message: string;
  errorDetail?: string;
  latencyMs: number;
}

export interface PlatformSettingsResponse {
  baseUrl?: string;
  platformName?: string;
  supportEmail?: string;
}

// ── LLM Health ────────────────────────────────────────────────────────────────
export type HealthStatus = "UP" | "DOWN" | "DEGRADED" | "UNKNOWN";
export type CheckType = "SCHEDULED" | "MANUAL";

export interface LlmHealthResponse {
  recordId?: string;
  endpointId: string;
  endpointName?: string;
  providerType?: string;
  modelName?: string;
  status: HealthStatus;
  latencyMs?: number;
  httpStatusCode?: number;
  errorMessage?: string;
  contextWindow?: number;
  maxOutputTokens?: number;
  checkType?: CheckType;
  checkedAt?: string;
}

export interface EndpointHealthSummary {
  endpointId: string;
  endpointName?: string;
  providerType?: string;
  modelName?: string;
  status: HealthStatus;
  latencyMs?: number;
  lastCheckedAt?: string;
  lastErrorMessage?: string;
  consecutiveFailures?: number;
  uptimePercentage?: number;
}

export interface HealthDashboardResponse {
  totalEndpoints: number;
  upCount: number;
  downCount: number;
  degradedCount: number;
  unknownCount: number;
  endpoints: EndpointHealthSummary[];
}

// ── Webhooks ──────────────────────────────────────────────────────────────────
export type DeliveryStatus = "PENDING" | "SUCCESS" | "FAILED" | "RETRYING";

export interface WebhookResponse {
  id: string;
  organizationId: string;
  projectId?: string;
  projectName?: string;
  name: string;
  url: string;
  hasSecret: boolean;
  events: string[];
  isActive: boolean;
  failureCount: number;
  lastTriggeredAt?: string;
  createdById: string;
  createdByName: string;
  createdAt: string;
  updatedAt: string;
}

export interface WebhookDeliveryResponse {
  id: string;
  webhookId: string;
  eventType: string;
  payload: Record<string, unknown>;
  status: DeliveryStatus;
  responseStatusCode?: number;
  responseBody?: string;
  errorMessage?: string;
  attemptCount: number;
  maxAttempts: number;
  nextRetryAt?: string;
  triggeredAt: string;
  deliveredAt?: string;
}
