import React, { useState, useEffect } from 'react';
import { Calendar, Clock, PlayCircle, PauseCircle, XCircle, CheckCircle, AlertCircle, TrendingUp, Activity, Zap, RefreshCw } from 'lucide-react';

const API_BASE = "http://localhost:8080";

// API Functions
const api = {
  login: async (username, password) => {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) throw new Error("Login failed");
    return res.json();
  },

  fetchJobs: async (token) => {
    const res = await fetch(`${API_BASE}/jobs`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!res.ok) throw new Error("Failed to fetch jobs");
    return res.json();
  },

  createJob: async (token, job) => {
    const res = await fetch(`${API_BASE}/jobs`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`
      },
      body: JSON.stringify(job)
    });
    if (!res.ok) throw new Error("Failed to create job");
    return res.json();
  },

  fetchStatusCounts: async (token) => {
    const res = await fetch(`${API_BASE}/analytics/status-counts`, {
      headers: { Authorization: `Bearer ${token}` }
    });
    if (!res.ok) throw new Error("Failed to fetch analytics");
    return res.json();
  }
};

const STATUS_CONFIG = {
  PENDING: { color: 'bg-orange-500', icon: Clock, label: 'Pending' },
  SCHEDULED: { color: 'bg-indigo-500', icon: Calendar, label: 'Scheduled' },
  RUNNING: { color: 'bg-sky-500', icon: Activity, label: 'Running' },
  COMPLETED: { color: 'bg-green-500', icon: CheckCircle, label: 'Completed' },
  FAILED: { color: 'bg-red-500', icon: XCircle, label: 'Failed' },
  CANCELLED: { color: 'bg-gray-500', icon: PauseCircle, label: 'Cancelled' },
  PAUSED: { color: 'bg-yellow-500', icon: PauseCircle, label: 'Paused' }
};

function App() {
  const [token, setToken] = useState(null);
  const [activeTab, setActiveTab] = useState('dashboard');
  const [jobs, setJobs] = useState([]);
  const [statusCounts, setStatusCounts] = useState({});
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [loginForm, setLoginForm] = useState({ username: 'admin', password: 'admin123' });
  const [filters, setFilters] = useState({ status: 'ALL', method: 'ALL', search: '' });
  const [jobForm, setJobForm] = useState({
    name: '',
    targetUrl: '',
    httpMethod: 'GET',
    priority: 5,
    timeoutSeconds: 30,
    webhookUrl: '',
    recurringInterval: ''
  });

  useEffect(() => {
    if (!token) return;
    loadData();
    const interval = setInterval(loadData, 5000);
    return () => clearInterval(interval);
  }, [token]);

  const loadData = async () => {
    if (!token) return;
    try {
      const [jobsRes, statusRes] = await Promise.all([
        api.fetchJobs(token),
        api.fetchStatusCounts(token)
      ]);
      setJobs(jobsRes);
      setStatusCounts(statusRes);
    } catch (err) {
      console.error(err);
    }
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await api.login(loginForm.username, loginForm.password);
      setToken(res.token);
    } catch (err) {
      setError('Invalid credentials');
    } finally {
      setLoading(false);
    }
  };

  const handleCreateJob = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      await api.createJob(token, jobForm);
      setShowCreateModal(false);
      setJobForm({
        name: '',
        targetUrl: '',
        httpMethod: 'GET',
        priority: 5,
        timeoutSeconds: 30,
        webhookUrl: '',
        recurringInterval: ''
      });
      await loadData();
    } catch (err) {
      setError('Failed to create job');
    } finally {
      setLoading(false);
    }
  };

  const filteredJobs = jobs.filter(j => {
    if (filters.status !== 'ALL' && j.status !== filters.status) return false;
    if (filters.method !== 'ALL' && j.httpMethod !== filters.method) return false;
    if (filters.search && !`${j.name} ${j.targetUrl}`.toLowerCase().includes(filters.search.toLowerCase())) {
      return false;
    }
    return true;
  });

  // Login Screen
  if (!token) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-slate-950 via-indigo-950 to-slate-900 flex items-center justify-center p-4">
        <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0icmdiYSgyNTUsMjU1LDI1NSwwLjAzKSIgc3Ryb2tlLXdpZHRoPSIxIi8+PC9wYXR0ZXJuPjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI2dyaWQpIi8+PC9zdmc+')] opacity-40"></div>

        <div className="relative w-full max-w-md">
          <div className="absolute inset-0 bg-gradient-to-r from-indigo-500 to-purple-500 rounded-3xl blur-2xl opacity-20"></div>
          <div className="relative bg-slate-900/80 backdrop-blur-xl border border-slate-700/50 rounded-2xl p-8 shadow-2xl">
            <div className="flex items-center justify-center mb-8">
              <div className="w-16 h-16 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl flex items-center justify-center shadow-lg shadow-indigo-500/50">
                <Clock className="w-8 h-8 text-white" />
              </div>
            </div>

            <h1 className="text-3xl font-bold text-center mb-2 bg-gradient-to-r from-white to-slate-300 bg-clip-text text-transparent">
              Chronos
            </h1>
            <p className="text-slate-400 text-center mb-8">Job Scheduler System</p>

            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Username</label>
                <input
                  type="text"
                  className="w-full px-4 py-3 bg-slate-800/50 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 text-white"
                  value={loginForm.username}
                  onChange={(e) => setLoginForm({ ...loginForm, username: e.target.value })}
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-300 mb-2">Password</label>
                <input
                  type="password"
                  className="w-full px-4 py-3 bg-slate-800/50 border border-slate-700 rounded-xl focus:outline-none focus:ring-2 focus:ring-indigo-500 text-white"
                  value={loginForm.password}
                  onChange={(e) => setLoginForm({ ...loginForm, password: e.target.value })}
                />
              </div>

              <button
                type="submit"
                disabled={loading}
                className="w-full py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-medium rounded-xl hover:from-indigo-700 hover:to-purple-700 transition-all shadow-lg shadow-indigo-500/30 disabled:opacity-50"
              >
                {loading ? 'Signing in...' : 'Sign In'}
              </button>

              {error && <p className="text-red-400 text-sm text-center">{error}</p>}
            </form>
          </div>
        </div>
      </div>
    );
  }

  // Main Dashboard
  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-950 via-indigo-950 to-slate-900">
      <div className="absolute inset-0 bg-[url('data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iNjAiIGhlaWdodD0iNjAiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PHBhdHRlcm4gaWQ9ImdyaWQiIHdpZHRoPSI2MCIgaGVpZ2h0PSI2MCIgcGF0dGVyblVuaXRzPSJ1c2VyU3BhY2VPblVzZSI+PHBhdGggZD0iTSAxMCAwIEwgMCAwIDAgMTAiIGZpbGw9Im5vbmUiIHN0cm9rZT0icmdiYSgyNTUsMjU1LDI1NSwwLjAzKSIgc3Ryb2tlLXdpZHRoPSIxIi8+PC9wYXR0ZXJuPjwvZGVmcz48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBmaWxsPSJ1cmwoI2dyaWQpIi8+PC9zdmc+')] opacity-40"></div>

      <div className="relative">
        {/* Header */}
        <header className="border-b border-slate-800/50 bg-slate-900/50 backdrop-blur-xl">
          <div className="max-w-7xl mx-auto px-6 py-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-8">
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-indigo-500/50">
                    <Clock className="w-5 h-5 text-white" />
                  </div>
                  <span className="text-xl font-bold text-white">Chronos</span>
                </div>

                <nav className="hidden md:flex gap-2">
                  {['dashboard', 'jobs', 'analytics'].map(tab => (
                    <button
                      key={tab}
                      onClick={() => setActiveTab(tab)}
                      className={`px-4 py-2 rounded-lg font-medium capitalize transition-all ${
                        activeTab === tab
                          ? 'bg-indigo-500 text-white shadow-lg shadow-indigo-500/30'
                          : 'text-slate-400 hover:text-white hover:bg-slate-800'
                      }`}
                    >
                      {tab}
                    </button>
                  ))}
                </nav>
              </div>

              <div className="flex items-center gap-3">
                <button
                  onClick={() => setShowCreateModal(true)}
                  className="px-4 py-2 bg-gradient-to-r from-indigo-600 to-purple-600 text-white rounded-lg font-medium hover:from-indigo-700 hover:to-purple-700 transition-all shadow-lg shadow-indigo-500/30"
                >
                  + New Job
                </button>
                <button
                  onClick={() => setToken(null)}
                  className="px-4 py-2 text-slate-400 hover:text-white transition-colors"
                >
                  Logout
                </button>
              </div>
            </div>
          </div>
        </header>

        {/* Main Content */}
        <main className="max-w-7xl mx-auto px-6 py-8">
          {/* Dashboard Tab */}
          {activeTab === 'dashboard' && (
            <div className="space-y-6">
              {/* Stats Grid */}
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {Object.entries(STATUS_CONFIG).map(([status, config]) => {
                  const Icon = config.icon;
                  const count = statusCounts[status] || 0;

                  return (
                    <div key={status} className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/50 rounded-xl p-6 hover:border-slate-700 transition-all">
                      <div className="flex items-center justify-between mb-3">
                        <div className={`w-10 h-10 ${config.color} rounded-lg flex items-center justify-center`}>
                          <Icon className="w-5 h-5 text-white" />
                        </div>
                        <span className="text-2xl font-bold text-white">{count}</span>
                      </div>
                      <p className="text-slate-400 text-sm font-medium">{config.label}</p>
                    </div>
                  );
                })}
              </div>

              {/* System Health */}
              <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/50 rounded-xl p-6">
                <div className="flex items-center justify-between mb-4">
                  <h2 className="text-xl font-bold text-white">System Health</h2>
                  <div className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
                    <span className="text-sm text-slate-400">Operational</span>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="bg-slate-800/50 rounded-lg p-4">
                    <div className="flex items-center gap-3 mb-2">
                      <TrendingUp className="w-5 h-5 text-green-500" />
                      <span className="text-slate-400 text-sm">Success Rate</span>
                    </div>
                    <p className="text-2xl font-bold text-white">
                      {jobs.length > 0 ? Math.round(((statusCounts.COMPLETED || 0) / jobs.length) * 100) : 0}%
                    </p>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-4">
                    <div className="flex items-center gap-3 mb-2">
                      <Activity className="w-5 h-5 text-sky-500" />
                      <span className="text-slate-400 text-sm">Active Jobs</span>
                    </div>
                    <p className="text-2xl font-bold text-white">
                      {(statusCounts.RUNNING || 0) + (statusCounts.SCHEDULED || 0)}
                    </p>
                  </div>

                  <div className="bg-slate-800/50 rounded-lg p-4">
                    <div className="flex items-center gap-3 mb-2">
                      <Zap className="w-5 h-5 text-yellow-500" />
                      <span className="text-slate-400 text-sm">Total Jobs</span>
                    </div>
                    <p className="text-2xl font-bold text-white">{jobs.length}</p>
                  </div>
                </div>
              </div>

              {/* Recent Jobs */}
              <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/50 rounded-xl p-6">
                <h2 className="text-xl font-bold text-white mb-4">Recent Jobs</h2>
                <div className="space-y-2">
                  {jobs.slice(0, 5).map(job => {
                    const config = STATUS_CONFIG[job.status];
                    const Icon = config?.icon || Clock;

                    return (
                      <div key={job.id} className="flex items-center justify-between p-4 bg-slate-800/50 rounded-lg hover:bg-slate-800 transition-colors">
                        <div className="flex items-center gap-4">
                          <div className={`w-8 h-8 ${config?.color || 'bg-gray-500'} rounded-lg flex items-center justify-center`}>
                            <Icon className="w-4 h-4 text-white" />
                          </div>
                          <div>
                            <p className="text-white font-medium">{job.name}</p>
                            <p className="text-slate-400 text-sm">{job.targetUrl}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-4">
                          <span className="text-sm text-slate-400">{job.httpMethod}</span>
                          <span className={`px-3 py-1 rounded-full text-xs font-medium ${config?.color || 'bg-gray-500'} text-white`}>
                            {job.status}
                          </span>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          )}

          {/* Jobs Tab */}
          {activeTab === 'jobs' && (
            <div className="space-y-6">
              {/* Filters */}
              <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/50 rounded-xl p-6">
                <h2 className="text-xl font-bold text-white mb-4">Filters</h2>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-slate-400 mb-2">Status</label>
                    <select
                      className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      value={filters.status}
                      onChange={(e) => setFilters({ ...filters, status: e.target.value })}
                    >
                      <option value="ALL">All Statuses</option>
                      {Object.keys(STATUS_CONFIG).map(s => (
                        <option key={s} value={s}>{s}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-400 mb-2">Method</label>
                    <select
                      className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      value={filters.method}
                      onChange={(e) => setFilters({ ...filters, method: e.target.value })}
                    >
                      <option value="ALL">All Methods</option>
                      {['GET', 'POST', 'PUT', 'DELETE'].map(m => (
                        <option key={m} value={m}>{m}</option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-slate-400 mb-2">Search</label>
                    <input
                      type="text"
                      placeholder="Search by name or URL..."
                      className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                      value={filters.search}
                      onChange={(e) => setFilters({ ...filters, search: e.target.value })}
                    />
                  </div>
                </div>
              </div>

              {/* Jobs Table */}
              <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/50 rounded-xl overflow-hidden">
                <div className="p-6 border-b border-slate-800">
                  <div className="flex items-center justify-between">
                    <h2 className="text-xl font-bold text-white">All Jobs</h2>
                    <span className="text-slate-400">
                      Showing {filteredJobs.length} of {jobs.length}
                    </span>
                  </div>
                </div>

                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-slate-800/50">
                      <tr>
                        <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase">ID</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase">Name</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase">Target URL</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase">Method</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase">Status</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase">Priority</th>
                        <th className="px-6 py-3 text-left text-xs font-medium text-slate-400 uppercase">Next Run</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-slate-800">
                      {filteredJobs.length === 0 ? (
                        <tr>
                          <td colSpan="7" className="px-6 py-8 text-center text-slate-400">
                            No jobs match the current filters
                          </td>
                        </tr>
                      ) : (
                        filteredJobs.map(job => {
                          const config = STATUS_CONFIG[job.status];

                          return (
                            <tr key={job.id} className="hover:bg-slate-800/50 transition-colors">
                              <td className="px-6 py-4 text-slate-300">{job.id}</td>
                              <td className="px-6 py-4 text-white font-medium">{job.name}</td>
                              <td className="px-6 py-4 text-slate-400 text-sm max-w-xs truncate">{job.targetUrl}</td>
                              <td className="px-6 py-4">
                                <span className="px-2 py-1 bg-slate-700 text-slate-300 text-xs rounded">
                                  {job.httpMethod}
                                </span>
                              </td>
                              <td className="px-6 py-4">
                                <span className={`px-3 py-1 ${config?.color || 'bg-gray-500'} text-white text-xs rounded-full font-medium`}>
                                  {job.status}
                                </span>
                              </td>
                              <td className="px-6 py-4 text-slate-300">{job.priority}</td>
                              <td className="px-6 py-4 text-slate-400 text-sm">
                                {job.nextRunAt ? new Date(job.nextRunAt).toLocaleString() : '-'}
                              </td>
                            </tr>
                          );
                        })
                      )}
                    </tbody>
                  </table>
                </div>
              </div>
            </div>
          )}

          {/* Analytics Tab */}
          {activeTab === 'analytics' && (
            <div className="bg-slate-900/50 backdrop-blur-xl border border-slate-800/50 rounded-xl p-6">
              <h2 className="text-xl font-bold text-white mb-4">Analytics Dashboard</h2>
              <p className="text-slate-400">Analytics and detailed monitoring coming soon...</p>
            </div>
          )}
        </main>
      </div>

      {/* Create Job Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4 z-50" onClick={() => setShowCreateModal(false)}>
          <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 max-w-2xl w-full max-h-[90vh] overflow-y-auto" onClick={(e) => e.stopPropagation()}>
            <h2 className="text-2xl font-bold text-white mb-6">Create New Job</h2>

            <form onSubmit={handleCreateJob} className="space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-2">Job Name *</label>
                  <input
                    type="text"
                    required
                    className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    value={jobForm.name}
                    onChange={(e) => setJobForm({ ...jobForm, name: e.target.value })}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-2">HTTP Method</label>
                  <select
                    className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    value={jobForm.httpMethod}
                    onChange={(e) => setJobForm({ ...jobForm, httpMethod: e.target.value })}
                  >
                    <option value="GET">GET</option>
                    <option value="POST">POST</option>
                    <option value="PUT">PUT</option>
                    <option value="DELETE">DELETE</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-400 mb-2">Target URL *</label>
                <input
                  type="url"
                  required
                  placeholder="https://api.example.com/endpoint"
                  className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  value={jobForm.targetUrl}
                  onChange={(e) => setJobForm({ ...jobForm, targetUrl: e.target.value })}
                />
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-2">Priority (0-10)</label>
                  <input
                    type="number"
                    min="0"
                    max="10"
                    className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    value={jobForm.priority}
                    onChange={(e) => setJobForm({ ...jobForm, priority: Number(e.target.value) })}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-2">Timeout (sec)</label>
                  <input
                    type="number"
                    min="1"
                    className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    value={jobForm.timeoutSeconds}
                    onChange={(e) => setJobForm({ ...jobForm, timeoutSeconds: Number(e.target.value) })}
                  />
                </div>

                <div>
                  <label className="block text-sm font-medium text-slate-400 mb-2">Recurring</label>
                  <select
                    className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                    value={jobForm.recurringInterval}
                    onChange={(e) => setJobForm({ ...jobForm, recurringInterval: e.target.value })}
                  >
                    <option value="">One-time</option>
                    <option value="HOURLY">Hourly</option>
                    <option value="DAILY">Daily</option>
                    <option value="WEEKLY">Weekly</option>
                    <option value="MONTHLY">Monthly</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-slate-400 mb-2">Webhook URL (optional)</label>
                <input
                  type="url"
                  placeholder="https://your-app.com/webhook"
                  className="w-full px-4 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  value={jobForm.webhookUrl}
                  onChange={(e) => setJobForm({ ...jobForm, webhookUrl: e.target.value })}
                />
                <p className="text-xs text-slate-500 mt-1">Receive notifications when job completes or fails</p>
              </div>

              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={() => setShowCreateModal(false)}
                  className="flex-1 px-4 py-2 bg-slate-800 text-slate-300 rounded-lg hover:bg-slate-700 transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading}
                  className="flex-1 px-4 py-2 bg-gradient-to-r from-indigo-600 to-purple-600 text-white rounded-lg hover:from-indigo-700 hover:to-purple-700 transition-all shadow-lg shadow-indigo-500/30 disabled:opacity-50"
                >
                  {loading ? 'Creating...' : 'Create Job'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Error Toast */}
      {error && (
        <div className="fixed bottom-4 right-4 bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg flex items-center gap-2 z-50">
          <AlertCircle className="w-5 h-5" />
          <span>{error}</span>
          <button onClick={() => setError('')} className="ml-2 hover:opacity-80">×</button>
        </div>
      )}
    </div>
  );
}

export default App;