import { MAX_FILE_SIZE_BYTES, titleFromFileName, validateFiles } from './file-validation';

function file(name: string, size = 10): File {
  const result = new File(['x'], name);
  Object.defineProperty(result, 'size', { value: size });
  return result;
}

describe('validateFiles', () => {
  it('accepts PDF, TXT and Markdown files', () => {
    expect(validateFiles([file('a.pdf'), file('b.TXT'), file('c.md')], 'batch')).toEqual([]);
  });

  it('rejects other formats', () => {
    expect(validateFiles([file('setup.exe')], 'single')[0].message).toContain('Formato no admitido');
  });

  it('rejects empty files', () => {
    expect(validateFiles([file('vacio.txt', 0)], 'single')[0].message).toBe('El archivo está vacío');
  });

  it('rejects files over 10 MB', () => {
    expect(validateFiles([file('grande.pdf', MAX_FILE_SIZE_BYTES + 1)], 'single')[0].message).toContain('10 MB');
  });

  it('rejects more than 20 files in a batch', () => {
    const files = Array.from({ length: 21 }, (_, index) => file(`doc-${index}.md`));
    expect(validateFiles(files, 'batch').map((problem) => problem.message)).toContain('Máximo 20 archivos por carga');
  });

  it('derives the title from the file name', () => {
    expect(titleFromFileName('Guía de la API.v2.md')).toBe('Guía de la API.v2');
  });
});
