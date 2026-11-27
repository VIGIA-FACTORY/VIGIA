/** Runtime settings written by the deployment into /config.js, so one build serves QA and production. */
export interface VigiaConfig {
  authority: string;
  clientId: string;
  apiUrl: string;
}

declare global {
  interface Window {
    VIGIA_CONFIG?: Partial<VigiaConfig>;
  }
}

export function readConfig(source: Partial<VigiaConfig> | undefined): VigiaConfig {
  const authority = source?.authority?.replace(/\/+$/, "");
  const apiUrl = source?.apiUrl?.replace(/\/+$/, "");
  if (!authority || !apiUrl) {
    throw new Error("Missing authority or apiUrl in /config.js");
  }
  return { authority, apiUrl, clientId: source?.clientId ?? "vigia-admin" };
}
