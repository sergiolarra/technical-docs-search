export type UploadMode = 'single' | 'batch';

export const ALLOWED_EXTENSIONS = ['pdf', 'txt', 'md'] as const;
export const MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024;
export const MAX_BATCH_FILES = 20;

export interface FileProblem {
  fileName: string;
  message: string;
}

/**
 * Mirrors the backend rules so most mistakes are caught before uploading.
 * The backend still checks the real content type (magic bytes): a renamed executable is rejected there.
 */
export function validateFiles(files: File[], mode: UploadMode): FileProblem[] {
  const problems: FileProblem[] = [];
  if (mode === 'single' && files.length > 1) {
    problems.push({ fileName: '', message: 'Select a single file or use batch upload' });
  }
  if (files.length > MAX_BATCH_FILES) {
    problems.push({ fileName: '', message: `At most ${MAX_BATCH_FILES} files per upload` });
  }
  for (const file of files) {
    const extension = file.name.includes('.') ? file.name.split('.').pop()!.toLowerCase() : '';
    if (!(ALLOWED_EXTENSIONS as readonly string[]).includes(extension)) {
      problems.push({ fileName: file.name, message: 'Unsupported format (PDF, TXT or Markdown only)' });
    } else if (file.size === 0) {
      problems.push({ fileName: file.name, message: 'The file is empty' });
    } else if (file.size > MAX_FILE_SIZE_BYTES) {
      problems.push({ fileName: file.name, message: 'Exceeds the 10 MB limit' });
    }
  }
  return problems;
}

export function titleFromFileName(fileName: string): string {
  return fileName.replace(/\.[^.]+$/, '');
}
