import { Routes } from '@angular/router';

// Lazy-loaded pages: each feature is downloaded only when it is first visited
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'search' },
  {
    path: 'search',
    title: 'Buscar · Technical Docs Search',
    loadComponent: () => import('./features/search/search-page').then((m) => m.SearchPage),
  },
  {
    path: 'upload',
    title: 'Cargar · Technical Docs Search',
    loadComponent: () => import('./features/upload/upload-page').then((m) => m.UploadPage),
  },
  {
    path: 'documents/:id',
    title: 'Documento · Technical Docs Search',
    loadComponent: () => import('./features/viewer/document-viewer-page').then((m) => m.DocumentViewerPage),
  },
  { path: '**', redirectTo: 'search' },
];
