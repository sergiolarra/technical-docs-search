import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  BatchMetadata,
  BatchUploadItem,
  DocumentDetail,
  DocumentMetadata,
  SearchParams,
  SearchResponse,
  UploadResponse,
} from '../models/document.model';

@Injectable({ providedIn: 'root' })
export class DocumentApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/documents';

  upload(file: File, metadata: DocumentMetadata): Observable<UploadResponse> {
    const form = new FormData();
    form.append('file', file);
    form.append('title', metadata.title);
    appendSharedMetadata(form, metadata);
    return this.http.post<UploadResponse>(this.baseUrl, form);
  }

  uploadBatch(files: File[], metadata: BatchMetadata): Observable<BatchUploadItem[]> {
    const form = new FormData();
    files.forEach((file) => form.append('files', file));
    appendSharedMetadata(form, metadata);
    return this.http.post<BatchUploadItem[]>(`${this.baseUrl}/batch`, form);
  }

  getById(id: string): Observable<DocumentDetail> {
    return this.http.get<DocumentDetail>(`${this.baseUrl}/${id}`);
  }

  search(params: SearchParams): Observable<SearchResponse> {
    let query = new HttpParams().set('q', params.q);
    if (params.category) {
      query = query.set('category', params.category);
    }
    if (params.tags?.length) {
      query = query.set('tags', params.tags.join(','));
    }
    if (params.page !== undefined) {
      query = query.set('page', params.page);
    }
    if (params.size !== undefined) {
      query = query.set('size', params.size);
    }
    return this.http.get<SearchResponse>(`${this.baseUrl}/search`, { params: query });
  }
}

function appendSharedMetadata(form: FormData, metadata: BatchMetadata): void {
  form.append('author', metadata.author);
  form.append('category', metadata.category);
  form.append('version', metadata.version);
  // One field per tag: Spring binds repeated fields to a List
  metadata.tags.forEach((tag) => form.append('tags', tag));
}
