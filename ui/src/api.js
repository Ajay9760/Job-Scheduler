const API_BASE = "http://localhost:8080/api";

// Auth APIs
export async function login(username, password) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Login failed" }));
    throw new Error(error.message || "Login failed");
  }
  return res.json();
}

export async function register(username, password, roles = "USER") {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password, roles })
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Registration failed" }));
    throw new Error(error.message || "Registration failed");
  }
  return res.json();
}

// Jobs APIs
export async function fetchJobs(token) {
  const res = await fetch(`${API_BASE}/jobs`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error("Failed to fetch jobs");
  return res.json();
}

export async function createJob(token, job) {
  const res = await fetch(`${API_BASE}/jobs`, {
    method: "POST",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(job)
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Failed to create job" }));
    throw new Error(error.message || "Failed to create job");
  }
  return res.json();
}

export async function updateJob(token, jobId, updates) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}`, {
    method: "PUT",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify(updates)
  });
  if (!res.ok) throw new Error("Failed to update job");
  return res.json();
}

export async function deleteJob(token, jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}`, {
    method: "DELETE",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok && res.status !== 204) {
    const error = await res.json().catch(() => ({ message: "Failed to delete job" }));
    throw new Error(error.message || "Failed to delete job");
  }
  return res.status === 204 ? null : res.json();
}

// Job Control APIs
export async function triggerJob(token, jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}/trigger`, {
    method: "POST",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Failed to trigger job" }));
    throw new Error(error.message || "Failed to trigger job");
  }
  return res.json();
}

export async function pauseJob(token, jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}/pause`, {
    method: "POST",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Failed to pause job" }));
    throw new Error(error.message || "Failed to pause job");
  }
  return res.json();
}

export async function resumeJob(token, jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}/resume`, {
    method: "POST",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) {
    const error = await res.json().catch(() => ({ message: "Failed to resume job" }));
    throw new Error(error.message || "Failed to resume job");
  }
  return res.json();
}

// Job Instances APIs
export async function getJobInstances(token, jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}/instances`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error("Failed to fetch instances");
  return res.json();
}

export async function retryInstance(token, instanceId) {
  const res = await fetch(`${API_BASE}/jobs/instances/${instanceId}/retry`, {
    method: "POST",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error("Failed to retry instance");
  return res.json();
}

// Analytics APIs
export async function fetchStatusCounts(token) {
  const res = await fetch(`${API_BASE}/analytics/status-counts`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) {
    // Fallback: calculate from jobs if analytics endpoint doesn't exist
    const jobs = await fetchJobs(token);
    return jobs.reduce((acc, job) => {
      acc[job.status] = (acc[job.status] || 0) + 1;
      return acc;
    }, {});
  }
  return res.json();
}