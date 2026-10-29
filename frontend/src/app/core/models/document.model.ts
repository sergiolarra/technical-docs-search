// Mirrors the backend API contract.

export type DocumentStatus = 'PROCESANDO' | 'INDEXADO' | 'ERROR';
export type DocumentFormat = 'PDF' | 'TXT' | 'MARKDOWN';

export interface DocumentMetadata {
  title: string;
  author: string;
  category: string;
  tags: string[];
  version: string;
}

/** Batch uploads share metadata; each title comes from its file name. */
export type BatchMetadata = Omit<DocumentMetadata, 'title'>;

export interface UploadResponse {
  id: string;
  title: string;
  originalFilename: string;
  status: DocumentStatus;
  createdAt: string;
}

export interface BatchUploadItem {
  originalFilename: string;
  accepted: boolean;
  id: string | null;
  status: DocumentStatus | null;
  error: string | null;
}

export interface DocumentDetail {
  id: string;
  title: string;
  author: string;
  category: string;
  tags: string[];
  version: string;
  originalFilename: string;
  format: DocumentFormat;
  contentType: string;
  size: number;
  status: DocumentStatus;
  errorMessage: string | null;
  content: string | null;
  createdAt: string;
  updatedAt: string;
}

/** Piece of highlighted text: fragments with match=true are wrapped in <mark>. */
export interface Fragment {
  text: string;
  match: boolean;
}

export interface SearchHit {
  id: string;
  title: string;
  titleHighlight: Fragment[];
  author: string;
  category: string;
  tags: string[];
  version: string;
  format: DocumentFormat;
  score: number;
  snippet: Fragment[];
  createdAt: string;
}

export interface SearchResponse {
  query: string;
  page: number;
  size: number;
  totalHits: number;
  totalPages: number;
  tookMs: number;
  results: SearchHit[];
}

export interface SearchParams {
  q: string;
  category?: string;
  tags?: string[];
  page?: number;
  size?: number;
}

/** Payload of the "document-status" server-sent event. */
export interface DocumentStatusEvent {
  documentId: string;
  title: string;
  status: DocumentStatus;
  errorMessage: string | null;
  occurredAt: string;
}
