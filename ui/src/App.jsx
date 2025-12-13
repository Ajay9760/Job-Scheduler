import React, { useEffect, useState } from 'react';
import {
  Calendar,
  Clock,
  PlayCircle,
  PauseCircle,
  XCircle,
  CheckCircle,
  TrendingUp,
  Activity,
  Zap,
  RefreshCw,
} from 'lucide-react';

import {
  login,
  fetchJobs,
  createJob,
  fetchStatusCounts,
  triggerJob,
  pauseJob,
  resumeJob,
  deleteJob,
} from './api';


/* -------------------- CONSTANTS -------------------- */

const STATUS_CONFIG = {
  PENDING: { color: 'bg-orange-500', icon: Clock },
  SCHEDULED: { color: 'bg-indigo-500', icon: Calendar },
  RUNNING: { color: 'bg-sky-500', icon: Activity },
  COMPLETED: { color: 'bg-green-500', icon: CheckCircle },
  FAILED: { color: 'bg-red-500', icon: XCircle },
  PAUSED: { color: 'bg-yellow-500', icon: PauseCircle },
};

/* -------------------- APP -------------------- */

export default function App() {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [activeTab, setActiveTab] = useState('dashboard');
  const [jobs, setJobs] = useState([]);
  const [statusCounts, setStatusCounts] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);

  const [loginForm, setLoginForm] = useState({
    username: 'admin',
    password: 'admin123',
  });

  const [filters, setFilters] = useState({
    status: 'ALL',
    method: 'ALL',
    search: '',
  });

  const [jobForm, setJobForm] = useState({
    name: '',
    description: '',
    targetUrl: '',
    httpMethod: 'GET',
    scheduleType: 'CRON',
    cronExpression: '0 * * * *',
    priority: 5,
    timeoutSeconds: 30,
    maxRetries: 3,
    webhookUrl: '',
  });

  /* -------------------- DATA LOADING -------------------- */

  useEffect(() => {
    if (!token) return;
    loadData();
    const id = setInterval(loadData, 5000);
    return () => clearInterval(id);
  }, [token]);

  async function loadData() {
    try {
      setLoading(true);
      const [jobsRes, statusRes] = await Promise.all([
        fetchJobs(token),
        fetchStatusCounts(token),
      ]);
      setJobs(Array.isArray(jobsRes) ? jobsRes : []);
      setStatusCounts(statusRes || {});
    } catch (e) {
      setError('Failed to load data');
    } finally {
      setLoading(false);
    }
  }

  /* -------------------- AUTH -------------------- */

  async function handleLogin(e) {
    e.preventDefault();
    try {
      setLoading(true);
      const res = await login(loginForm.username, loginForm.password);
      localStorage.setItem('token', res.token);
      setToken(res.token);
    } catch {
      setError('Invalid credentials');
    } finally {
      setLoading(false);
    }
  }

  function handleLogout() {
    localStorage.removeItem('token');
    setToken(null);
  }

  /* -------------------- JOB ACTIONS -------------------- */

  async function handleCreateJob(e) {
    e.preventDefault();
    try {
      setLoading(true);
      await createJob(token, jobForm);
      setShowCreateModal(false);
      await loadData();
      setSuccess('Job created');
    } catch {
      setError('Failed to create job');
    } finally {
      setLoading(false);
    }
  }

  /* -------------------- FILTER -------------------- */

  const filteredJobs = jobs.filter(j => {
    if (filters.status !== 'ALL' && j.status !== filters.status) return false;
    if (filters.method !== 'ALL' && j.httpMethod !== filters.method) return false;
    if (
      filters.search &&
      !`${j.name} ${j.targetUrl}`.toLowerCase().includes(filters.search.toLowerCase())
    ) return false;
    return true;
  });

  /* -------------------- LOGIN SCREEN -------------------- */

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-950">
        <form
          onSubmit={handleLogin}
          className="bg-slate-900 p-8 rounded-xl w-full max-w-md space-y-4"
        >
          <h1 className="text-white text-2xl font-bold text-center">Chronos</h1>

          <input
            className="w-full p-3 bg-slate-800 text-white rounded"
            placeholder="Username"
            value={loginForm.username}
            onChange={e => setLoginForm({ ...loginForm, username: e.target.value })}
          />

          <input
            type="password"
            className="w-full p-3 bg-slate-800 text-white rounded"
            placeholder="Password"
            value={loginForm.password}
            onChange={e => setLoginForm({ ...loginForm, password: e.target.value })}
          />

          <button
            disabled={loading}
            className="w-full bg-indigo-600 text-white py-3 rounded"
          >
            {loading ? 'Signing in…' : 'Sign In'}
          </button>

          {error && <p className="text-red-400 text-center">{error}</p>}
        </form>
      </div>
    );
  }

  /* -------------------- MAIN DASHBOARD -------------------- */

  return (
    <div className="min-h-screen bg-slate-950 text-white">
      <header className="p-4 border-b border-slate-800 flex justify-between">
        <span className="font-bold">Chronos</span>
        <div className="flex gap-3">
          <button onClick={() => setShowCreateModal(true)}>+ New Job</button>
          <button onClick={handleLogout}>Logout</button>
        </div>
      </header>

      <main className="p-6">
        {activeTab === 'jobs' && (
          <div className="grid md:grid-cols-3 gap-4">
            {loading ? (
              <RefreshCw className="animate-spin" />
            ) : (
              filteredJobs.map(job => {
                const Icon = STATUS_CONFIG[job.status]?.icon || Clock;
                return (
                  <div key={job.id} className="bg-slate-900 p-4 rounded">
                    <div className="flex justify-between">
                      <h3>{job.name}</h3>
                      <Icon />
                    </div>
                    <p className="text-sm text-slate-400">{job.targetUrl}</p>
                  </div>
                );
              })
            )}
          </div>
        )}
      </main>

      {/* CREATE JOB MODAL */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/70 flex items-center justify-center">
          <form
            onSubmit={handleCreateJob}
            className="bg-slate-900 p-6 rounded-xl w-full max-w-lg space-y-4"
          >
            <h2 className="text-xl font-bold">Create Job</h2>

            <input
              required
              className="w-full p-2 bg-slate-800 rounded"
              placeholder="Job Name"
              value={jobForm.name}
              onChange={e => setJobForm({ ...jobForm, name: e.target.value })}
            />

            <input
              required
              className="w-full p-2 bg-slate-800 rounded"
              placeholder="Target URL"
              value={jobForm.targetUrl}
              onChange={e => setJobForm({ ...jobForm, targetUrl: e.target.value })}
            />

            <button className="bg-indigo-600 w-full py-2 rounded">
              Create
            </button>
          </form>
        </div>
      )}
    </div>
  );
}
