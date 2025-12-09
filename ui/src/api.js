const API_BASE = "http://localhost:8080/api";

// Auth

export async function login(username, password) {
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ username, password })
  });
  if (!res.ok) throw new Error("Login failed");
  return res.json();
}

// Jobs

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
  if (!res.ok) throw new Error("Failed to create job");
  return res.json();
}

// Analytics

export async function fetchStatusCounts(token) {
  const res = await fetch(`${API_BASE}/statistics/dashboard`, {
    headers: { 
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}` 
    }
  });
  if (!res.ok) throw new Error("Failed to fetch analytics");
  const data = await res.json();
  // Transform the response to match the expected format
  return {
    statusCounts: data.data // The dashboard endpoint returns data in a 'data' field
  };
}

// Job Control

export async function startJob(token, jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}/start`, {
    method: "POST",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error("Failed to start job");
  return res.json();
}

export async function stopJob(token, jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}/stop`, {
    method: "POST",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error("Failed to stop job");
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
  if (!res.ok) throw new Error("Failed to delete job");
  return res.json();
}