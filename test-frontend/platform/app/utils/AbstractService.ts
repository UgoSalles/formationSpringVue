/**
 * Base des services d'accès à l'API : encapsule CRUD et recherche paginée du platform, sur le client
 * `$api` (ofetch). Contrairement à l'ancienne version axios, les méthodes renvoient directement la
 * donnée typée (ofetch parse la réponse), pas un objet `{ data }`. Les composants n'utilisent que
 * les méthodes publiques exposées par les services concrets qui en héritent.
 */
export abstract class AbstractService {
  protected abstract basePath: string

  private get api() {
    return useNuxtApp().$api
  }

  protected get<T>(path = '', opts: Record<string, unknown> = {}) {
    return this.api<T>(`${this.basePath}${path}`, { method: 'GET', ...opts })
  }

  protected head(path = '', opts: Record<string, unknown> = {}) {
    return this.api<void>(`${this.basePath}${path}`, { method: 'HEAD', ...opts })
  }

  protected post<T, D = unknown>(path = '', body?: D, opts: Record<string, unknown> = {}) {
    return this.api<T>(`${this.basePath}${path}`, { method: 'POST', body, ...opts })
  }

  protected patch<T, D = unknown>(path = '', body?: D, opts: Record<string, unknown> = {}) {
    return this.api<T>(`${this.basePath}${path}`, { method: 'PATCH', body, ...opts })
  }

  protected delete<T>(path = '', opts: Record<string, unknown> = {}) {
    return this.api<T>(`${this.basePath}${path}`, { method: 'DELETE', ...opts })
  }

  protected search<T, F = unknown>(filters: F, opts: Record<string, unknown> = {}) {
    return this.api<T>(`${this.basePath}/search`, { method: 'POST', body: filters, ...opts })
  }

  protected findOne<T>(id: string, opts: Record<string, unknown> = {}) {
    return this.api<T>(`${this.basePath}/${id}`, { method: 'GET', ...opts })
  }

  protected create<T, D = unknown>(data: D, opts: Record<string, unknown> = {}) {
    return this.api<T>(this.basePath, { method: 'POST', body: data, ...opts })
  }

  protected update<T, D = unknown>(id: string, data: D, opts: Record<string, unknown> = {}) {
    return this.api<T>(`${this.basePath}/${id}`, { method: 'PATCH', body: data, ...opts })
  }

  protected remove<T>(id: string, opts: Record<string, unknown> = {}) {
    return this.api<T>(`${this.basePath}/${id}`, { method: 'DELETE', ...opts })
  }
}
