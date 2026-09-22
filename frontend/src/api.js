const API_BASE = '';

function getAuthHeader() {
  const token = localStorage.getItem('token');
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export async function login(email, password) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Login failed' }));
    throw new Error(error.message || 'Login failed');
  }
  return res.json();
}

export async function register(email, password) {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Registration failed' }));
    throw new Error(error.message || 'Registration failed');
  }
  return res.json();
}

export async function uploadDocument(file) {
  const formData = new FormData();
  formData.append('file', file);

  const res = await fetch(`${API_BASE}/documents/upload`, {
    method: 'POST',
    headers: {
      ...getAuthHeader(),
    },
    body: formData,
  });

  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Upload failed' }));
    throw new Error(error.message || 'Upload failed');
  }
  return res.json();
}

export async function getMyDocuments(page = 0, size = 20) {
  const res = await fetch(`${API_BASE}/documents/my?page=${page}&size=${size}&sortBy=uploadedAt&sortDir=desc`, {
    headers: {
      ...getAuthHeader(),
    },
  });

  if (!res.ok) {
    if (res.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('userEmail');
      throw new Error('Unauthorized');
    }
    const error = await res.json().catch(() => ({ message: 'Failed to fetch documents' }));
    throw new Error(error.message || 'Failed to fetch documents');
  }
  return res.json();
}

export async function getAnalysis(id) {
  const res = await fetch(`${API_BASE}/documents/${id}/analysis`, {
    headers: {
      ...getAuthHeader(),
    },
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to fetch analysis' }));
    throw new Error(error.message || 'Failed to fetch analysis');
  }
  return res.json();
}

export async function downloadDocument(id, filename) {
  const res = await fetch(`${API_BASE}/documents/${id}/download`, {
    headers: {
      ...getAuthHeader(),
    },
  });
  if (!res.ok) {
    throw new Error('Failed to download document');
  }
  const blob = await res.blob();
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename || 'document.pdf';
  document.body.appendChild(a);
  a.click();
  window.URL.revokeObjectURL(url);
  document.body.removeChild(a);
}

export async function deleteDocument(id) {
  const res = await fetch(`${API_BASE}/documents/${id}`, {
    method: 'DELETE',
    headers: {
      ...getAuthHeader(),
    },
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: 'Failed to delete document' }));
    throw new Error(error.message || 'Failed to delete document');
  }
  return res.json();
}
