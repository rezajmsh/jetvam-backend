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

export interface PasswordLoginPreparation {
  secondFactorRequired: boolean;
  challengeId: string | null;
  expiresAt: string | null;
  resendAvailableAt: string | null;
}

export interface TokenResponse {
  access_token: string;
  refresh_token: string;
  token_type: "Bearer";
  expires_in: number;
  scope: string;
}

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

export async function preparePasswordLogin(
  username: string,
  password: string,
): Promise<PasswordLoginPreparation> {
  return postJson<PasswordLoginPreparation>("/api/v1/password-users/auth/prepare", {
    username,
    password,
  });
}

export async function completePasswordLogin(input: {
  portal: Portal;
  username: string;
  password: string;
  preparation: PasswordLoginPreparation;
  otp?: string;
}): Promise<TokenResponse> {
  const { portal, username, password, preparation, otp } = input;
  if (preparation.secondFactorRequired && (!preparation.challengeId || !otp)) {
    throw new Error("OTP and challengeId are required by the current 2FA policy");
  }

  const parameters: Record<string, string> = {
    client_id: portal === "system" ? "jetvam-backoffice" : "jetvam-merchant-portal",
    grant_type: PASSWORD_GRANT,
    username,
    password,
    scope: SCOPE,
  };
  if (preparation.secondFactorRequired) {
    parameters.challenge_id = preparation.challengeId!;
    parameters.otp = otp!;
  }
  return requestToken(parameters);
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
  return readResponse<T>(response);
}

async function requestToken(parameters: Record<string, string>): Promise<TokenResponse> {
  const response = await fetch(TOKEN_ENDPOINT, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams(parameters),
  });
  return readResponse<TokenResponse>(response);
}

async function readResponse<T>(response: Response): Promise<T> {
  const payload = await response.json();
  if (!response.ok) {
    throw new Error(payload.error_description ?? payload.message ?? payload.error ?? "Authentication failed");
  }
  return payload as T;
}
