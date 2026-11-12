declare const CONECTA_API_URL: string | undefined;

export const environment = {
  apiUrl: typeof CONECTA_API_URL === 'string' ? CONECTA_API_URL : 'http://localhost:8080/api',
};
