import React, { useState, useEffect } from 'react';
import { Calendar, Clock, PlayCircle, PauseCircle, XCircle, CheckCircle, AlertCircle, TrendingUp, Activity, Zap, RefreshCw } from 'lucide-react';
import { login, fetchJobs, createJob, fetchStatusCounts, triggerJob, pauseJob, resumeJob, deleteJob, getJobInstances } from './api';

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

// Jobs - FIXED to handle different response formats
export async function fetchJobs(token) {
  const res = await fetch(`${API_BASE}/jobs`, {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) throw new Error("Failed to fetch jobs");
  const data = await res.json();

  // Handle if response is wrapped in {data: [...]} or direct array
  return Array.isArray(data) ? data : (data.data || []);
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
    const error = await res.json();
    throw new Error(error.error || "Failed to create job");
  }
  return res.json();
}

export async function triggerJob(token, jobId) {
  const res = await fetch(`${API_BASE}/jobs/${jobId}/trigger`, {
    method: "POST",
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    }
  });
  if (!res.ok) {
    const error = await res.json();
    throw new Error(error.error || "Failed to trigger job");
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
    const error = await res.json();
    throw new Error(error.error || "Failed to pause job");
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
    const error = await res.json();
    throw new Error(error.error || "Failed to resume job");
  }
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
    const error = await res.json();
    throw new Error(error.error || "Failed to delete job");
  }
  return res.status === 204 ? null : res.json();
}

// Analytics - Calculate from jobs
export async function fetchStatusCounts(token) {
  try {
    const jobs = await fetchJobs(token);

    const statusCounts = jobs.reduce((acc, job) => {
      acc[job.status] = (acc[job.status] || 0) + 1;
      return acc;
    }, {});

    return statusCounts;
  } catch (error) {
    console.error('Failed to fetch status counts:', error);
    return {};
  }
}

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