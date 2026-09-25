/**
 * Reference client for Jetvam's three frontend authentication flows.
 * Keep refresh tokens in secure platform storage or a BFF session.
 *
 * @author reza jamshidi
 * @since 9/23/2026
 */

const UAA_BASE_URL = "http://localhost:8081";
const TOKEN_ENDPOINT = `${UAA_BASE_URL}/oauth2/token`;
const OTP_GRANT = "urn:jetvam:params:oauth:grant-type:otp";
const PASSWORD_GRANT = "urn:jetvam:params:oauth:grant-type:password";
const SCOPE = "jetvam.api offline_access";

export type Portal = "system" | "merchant";

export interface OtpChallenge {
  challengeId: string;
  expiresAt: string;
  resendAvailableAt: string;
}

export interface SecondFactorRequiredResponse {
  error: "second_factor_required";
  error_description: string;
  challenge_id: string;
  expires_at: string;
  resend_available_at: string;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  token_type: "Bearer";
  expires_in: number;
  scope: string;
}

export interface ApiEnvelope<T> {
  success: boolean;
  data: T;
  error: { code: string; message: string } | null;
  meta: { timestamp: string; requestId: string; traceId: string };
}

interface ErrorPayload {
  error?: string | { message?: string };
  error_description?: string;
  message?: string;
}

export type PasswordLoginOutcome =
  | { status: "authenticated"; tokens: TokenResponse }
  | { status: "second_factor_required"; challenge: OtpChallenge };

export async function requestCustomerLoginOtp(mobile: string): Promise<OtpChallenge> {
  return postJson<OtpChallenge>("/api/v1/customer/auth/otp", { mobile });
}

export async function completeCustomerLogin(
  challengeId: string,
  otp: string,
): Promise<TokenResponse> {
  return requestToken({
    client_id: "jetvam-portal",
    grant_type: OTP_GRANT,
    challenge_id: challengeId,
    otp,
    scope: SCOPE,
  });
}

export async function startPasswordLogin(
  portal: Portal,
  username: string,
  password: string,
): Promise<PasswordLoginOutcome> {
  const response = await sendTokenRequest({
    client_id: portal === "system" ? "jetvam-backoffice" : "jetvam-merchant-portal",
    grant_type: PASSWORD_GRANT,
    username,
    password,
    scope: SCOPE,
  });
  const payload = await response.json();
  if (response.ok) {
    return { status: "authenticated", tokens: payload as TokenResponse };
  }
  if (payload.error === "second_factor_required") {
    const secondFactor = payload as SecondFactorRequiredResponse;
    return {
      status: "second_factor_required",
      challenge: {
        challengeId: secondFactor.challenge_id,
        expiresAt: secondFactor.expires_at,
        resendAvailableAt: secondFactor.resend_available_at,
      },
    };
  }
  throw authenticationError(payload);
}

export async function completePasswordLogin(input: {
  portal: Portal;
  username: string;
  password: string;
  challengeId: string;
  otp: string;
}): Promise<TokenResponse> {
  const { portal, username, password, challengeId, otp } = input;
  return requestToken({
    client_id: portal === "system" ? "jetvam-backoffice" : "jetvam-merchant-portal",
    grant_type: PASSWORD_GRANT,
    username,
    password,
    challenge_id: challengeId,
    otp,
    scope: SCOPE,
  });
}

export async function refreshAccessToken(
  clientId: "jetvam-portal" | "jetvam-backoffice" | "jetvam-merchant-portal",
  refreshToken: string,
): Promise<TokenResponse> {
  return requestToken({
    client_id: clientId,
    grant_type: "refresh_token",
    refresh_token: refreshToken,
  });
}

async function postJson<T>(path: string, body: unknown): Promise<T> {
  const response = await fetch(`${UAA_BASE_URL}${path}`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  const envelope = await readResponse<ApiEnvelope<T>>(response);
  return envelope.data;
}

async function requestToken(parameters: Record<string, string>): Promise<TokenResponse> {
  return readResponse<TokenResponse>(await sendTokenRequest(parameters));
}

async function sendTokenRequest(parameters: Record<string, string>): Promise<Response> {
  return fetch(TOKEN_ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(parameters),
  });
}

async function readResponse<T>(response: Response): Promise<T> {
  const payload = await response.json();
  if (!response.ok) {
    throw authenticationError(payload);
  }
  return payload as T;
}

function authenticationError(payload: unknown): Error {
  const error = (payload ?? {}) as ErrorPayload;
  const oauthError = typeof error.error === "string" ? error.error : undefined;
  const apiError = typeof error.error === "object" ? error.error.message : undefined;
  return new Error(error.error_description ?? apiError ?? error.message ?? oauthError ?? "Authentication failed");
}
