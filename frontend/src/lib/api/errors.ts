/**
 * The single error shape the backend returns for every handled error
 * (see backend `ApiError`). `fieldErrors` is present only on 400 VALIDATION_ERROR.
 */
export interface ApiErrorBody {
  status?: number;
  code?: string;
  message?: string;
  path?: string;
  timestamp?: string;
  fieldErrors?: Record<string, string>;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly path?: string;
  readonly fieldErrors?: Record<string, string>;

  constructor(
    status: number,
    code: string,
    message: string,
    options?: { path?: string; fieldErrors?: Record<string, string> },
  ) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.path = options?.path;
    this.fieldErrors = options?.fieldErrors;
  }
}

/** 400 with per-field messages — forms map these back onto the fields. */
export class ValidationError extends ApiError {
  constructor(
    message: string,
    fieldErrors: Record<string, string>,
    path?: string,
  ) {
    super(400, "VALIDATION_ERROR", message, { path, fieldErrors });
    this.name = "ValidationError";
  }
}

/**
 * A transfer that came back `status: "FAILED"` (or an idempotent replay of one).
 * The HTTP call was 201, but the operation did not succeed.
 */
export class TransferFailedError extends ApiError {
  constructor(reason: string | null) {
    super(422, "TRANSFER_FAILED", reason ?? "A transferência falhou");
    this.name = "TransferFailedError";
  }
}

export function apiErrorFromBody(status: number, body: ApiErrorBody): ApiError {
  const message = body.message ?? "Erro inesperado";
  if (body.code === "VALIDATION_ERROR" || body.fieldErrors) {
    return new ValidationError(message, body.fieldErrors ?? {}, body.path);
  }
  return new ApiError(status, body.code ?? "UNKNOWN", message, {
    path: body.path,
  });
}
