import { User } from './user.model';

/** Sent to POST /api/auth/signup */
export interface SignupRequest {
  name: string;
  email: string;
  password: string;
}

/** Sent to POST /api/auth/login */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Sent to POST /api/auth/forgot-password */
export interface ForgotPasswordRequest {
  email: string;
}

/** Sent to POST /api/auth/reset-password */
export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

/** Returned from signup and login endpoints */
export interface AuthResponse {
  message: string;
  user: User;
}

/** Returned from logout, forgot-password, reset-password */
export interface MessageResponse {
  message: string;
}

