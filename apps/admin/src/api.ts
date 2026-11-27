/** Client of the VIGIA APIs behind api.<domain> (ADR-033 problem details on errors). */
export interface Me {
  subject: string;
  username: string;
  email: string | null;
  displayName: string | null;
  roles: string[];
}

export type MemberType = "STUDENT" | "PROFESSOR" | "STAFF";

export interface Person {
  id: string;
  documentNumber: string;
  firstName: string;
  lastName: string;
  email: string | null;
  memberType: MemberType;
}

export interface Vehicle {
  id: string;
  plate: string;
  ownerId: string;
  brand: string | null;
  model: string | null;
  color: string | null;
}

export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string | undefined,
    message: string,
  ) {
    super(message);
  }
}

/** Turns an RFC 9457 problem detail into a message for the user. */
export async function toApiError(response: Response): Promise<ApiError> {
  let code: string | undefined;
  let detail = `Error ${response.status}`;
  try {
    const body = (await response.json()) as { code?: string; detail?: string; title?: string };
    code = body.code;
    detail = body.detail ?? body.title ?? detail;
  } catch {
    // not JSON: keep the status message
  }
  return new ApiError(response.status, code, detail);
}

export function createApi(apiUrl: string, token: () => string | undefined) {
  async function call<T>(path: string, init?: RequestInit): Promise<T> {
    const response = await fetch(`${apiUrl}${path}`, {
      ...init,
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token() ?? ""}`,
        ...init?.headers,
      },
    });
    if (!response.ok) {
      throw await toApiError(response);
    }
    return (await response.json()) as T;
  }
  return {
    me: () => call<Me>("/v1/me"),
    people: () => call<Person[]>("/v1/people"),
    registerPerson: (person: Omit<Person, "id">) =>
      call<Person>("/v1/people", { method: "POST", body: JSON.stringify(person) }),
    vehicles: () => call<Vehicle[]>("/v1/vehicles"),
    registerVehicle: (vehicle: Omit<Vehicle, "id">) =>
      call<Vehicle>("/v1/vehicles", { method: "POST", body: JSON.stringify(vehicle) }),
  };
}

export type Api = ReturnType<typeof createApi>;
