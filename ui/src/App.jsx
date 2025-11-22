import React, { useEffect, useState } from "react";
import { login, fetchJobs, createJob, fetchStatusCounts } from "./api";

const HTTP_METHODS = ["GET", "POST", "PUT", "DELETE"];
const STATUS_COLORS = {
  PENDING: "#f97316",
  SCHEDULED: "#6366f1",
  RUNNING: "#0ea5e9",
  COMPLETED: "#22c55e",
  FAILED: "#ef4444",
  CANCELLED: "#6b7280",
  PAUSED: "#eab308"
};

function App() {
  const [token, setToken] = useState(null);
  const [loginForm, setLoginForm] = useState({
    username: "admin",
    password: "admin123"
  });
  const [error, setError] = useState("");
  const [jobs, setJobs] = useState([]);
  const [statusCounts, setStatusCounts] = useState({});
  const [loading, setLoading] = useState(false);

  const [filters, setFilters] = useState({
    status: "ALL",
    method: "ALL",
    search: ""
  });

  const [showCreateModal, setShowCreateModal] = useState(false);
  const [jobForm, setJobForm] = useState({
    name: "",
    targetUrl: "",
    httpMethod: "GET",
    priority: 5,
    timeoutSeconds: 30,
    webhookUrl: ""
  });

  const handleLogin = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await login(loginForm.username, loginForm.password);
      setToken(res.token);
    } catch (err) {
      setError("Invalid credentials or server error.");
    } finally {
      setLoading(false);
    }
  };

  const loadData = async () => {
    if (!token) return;
    try {
      const [jobsRes, statusRes] = await Promise.all([
        fetchJobs(token),
        fetchStatusCounts(token)
      ]);
      setJobs(jobsRes);
      setStatusCounts(statusRes);
    } catch (err) {
      console.error(err);
      setError("Failed to load data from backend.");
    }
  };

  useEffect(() => {
    if (!token) return;
    loadData();
    const id = setInterval(loadData, 5000);
    return () => clearInterval(id);
  }, [token]);

  const handleCreateJob = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      await createJob(token, jobForm);
      setShowCreateModal(false);
      setJobForm({
        name: "",
        targetUrl: "",
        httpMethod: "GET",
        priority: 5,
        timeoutSeconds: 30,
        webhookUrl: ""
      });
      await loadData();
    } catch (err) {
      setError("Failed to create job.");
    } finally {
      setLoading(false);
    }
  };

  const filteredJobs = jobs.filter((j) => {
    if (filters.status !== "ALL" && j.status !== filters.status) return false;
    if (filters.method !== "ALL" && j.httpMethod !== filters.method) return false;
    if (
      filters.search &&
      !`${j.name} ${j.targetUrl}`.toLowerCase().includes(filters.search.toLowerCase())
    ) {
      return false;
    }
    return true;
  });

  if (!token) {
    return (
      <div className="app app--centered">
        <div className="card card--auth">
          <h1 className="app-title">Chronos Job Scheduler</h1>
          <p className="muted">Sign in to manage scheduled jobs</p>
          <form onSubmit={handleLogin} className="form">
            <label>
              Username
              <input
                className="input"
                value={loginForm.username}
                onChange={(e) =>
                  setLoginForm({ ...loginForm, username: e.target.value })
                }
              />
            </label>
            <label>
              Password
              <input
                className="input"
                type="password"
                value={loginForm.password}
                onChange={(e) =>
                  setLoginForm({ ...loginForm, password: e.target.value })
                }
              />
            </label>
            <button className="btn btn--primary" disabled={loading}>
              {loading ? "Signing in..." : "Login"}
            </button>
            {error && <p className="error">{error}</p>}
          </form>
        </div>
      </div>
    );
  }

  return (
    <div className="app">
      <header className="topbar">
        <div>
          <h1 className="app-title">Chronos Dashboard</h1>
          <p className="muted">Monitor and manage scheduled HTTP jobs</p>
        </div>
        <div className="topbar-right">
          <button
            className="btn btn--ghost"
            onClick={() => loadData()}
            disabled={loading}
          >
            Refresh
          </button>
          <button className="btn btn--danger" onClick={() => setToken(null)}>
            Logout
          </button>
        </div>
      </header>

      <div className="layout">
        <aside className="sidebar">
          <div className="card">
            <h2 className="card-title">Filters</h2>
            <label className="label">
              Status
              <select
                className="input"
                value={filters.status}
                onChange={(e) =>
                  setFilters({ ...filters, status: e.target.value })
                }
              >
                <option value="ALL">All</option>
                {Object.keys(STATUS_COLORS).map((s) => (
                  <option key={s} value={s}>
                    {s}
                  </option>
                ))}
              </select>
            </label>
            <label className="label">
              Method
              <select
                className="input"
                value={filters.method}
                onChange={(e) =>
                  setFilters({ ...filters, method: e.target.value })
                }
              >
                <option value="ALL">All</option>
                {HTTP_METHODS.map((m) => (
                  <option key={m} value={m}>
                    {m}
                  </option>
                ))}
              </select>
            </label>
            <label className="label">
              Search
              <input
                className="input"
                placeholder="Name or URL"
                value={filters.search}
                onChange={(e) =>
                  setFilters({ ...filters, search: e.target.value })
                }
              />
            </label>
          </div>

          <div className="card">
            <h2 className="card-title">Actions</h2>
            <button
              className="btn btn--primary btn--full"
              onClick={() => setShowCreateModal(true)}
            >
              + New Job
            </button>
          </div>
        </aside>

        <main className="main">
          <section className="cards-row">
            {Object.keys(STATUS_COLORS).map((status) => (
              <div key={status} className="card card--stat">
                <span className="stat-label">{status}</span>
                <span className="stat-value">
                  {statusCounts[status] ?? 0}
                </span>
              </div>
            ))}
          </section>

          <section className="card">
            <div className="card-header">
              <h2 className="card-title">Jobs</h2>
              <span className="muted">
                Showing {filteredJobs.length} of {jobs.length}
              </span>
            </div>
            <div className="table-wrapper">
              <table className="table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Name</th>
                    <th>Target</th>
                    <th>Method</th>
                    <th>Status</th>
                    <th>Priority</th>
                    <th>Next Run</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredJobs.length === 0 && (
                    <tr>
                      <td colSpan="7" style={{ textAlign: "center" }}>
                        No jobs match the current filters.
                      </td>
                    </tr>
                  )}
                  {filteredJobs.map((j) => (
                    <tr key={j.id}>
                      <td>{j.id}</td>
                      <td>{j.name}</td>
                      <td className="cell-url">{j.targetUrl}</td>
                      <td>
                        <span className="pill pill--method">
                          {j.httpMethod}
                        </span>
                      </td>
                      <td>
                        <span
                          className="pill pill--status"
                          style={{
                            backgroundColor: STATUS_COLORS[j.status] || "#6b7280"
                          }}
                        >
                          {j.status}
                        </span>
                      </td>
                      <td>
                        <span className="pill pill--priority">
                          {j.priority}
                        </span>
                      </td>
                      <td>
                        {j.nextRunAt
                          ? new Date(j.nextRunAt).toLocaleString()
                          : "-"}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </main>
      </div>

      {showCreateModal && (
        <div className="modal-backdrop" onClick={() => setShowCreateModal(false)}>
          <div
            className="modal"
            onClick={(e) => e.stopPropagation()}
          >
            <h2 className="card-title">Create New Job</h2>
            <form onSubmit={handleCreateJob} className="form">
              <label className="label">
                Name
                <input
                  className="input"
                  value={jobForm.name}
                  onChange={(e) =>
                    setJobForm({ ...jobForm, name: e.target.value })
                  }
                  required
                />
              </label>
              <label className="label">
                Target URL
                <input
                  className="input"
                  value={jobForm.targetUrl}
                  onChange={(e) =>
                    setJobForm({ ...jobForm, targetUrl: e.target.value })
                  }
                  placeholder="https://api.example.com/endpoint"
                  required
                />
              </label>
              <label className="label">
                HTTP Method
                <select
                  className="input"
                  value={jobForm.httpMethod}
                  onChange={(e) =>
                    setJobForm({ ...jobForm, httpMethod: e.target.value })
                  }
                >
                  {HTTP_METHODS.map((m) => (
                    <option key={m} value={m}>
                      {m}
                    </option>
                  ))}
                </select>
              </label>
              <div className="grid-two">
                <label className="label">
                  Priority (0-10)
                  <input
                    className="input"
                    type="number"
                    min="0"
                    max="10"
                    value={jobForm.priority}
                    onChange={(e) =>
                      setJobForm({
                        ...jobForm,
                        priority: Number(e.target.value)
                      })
                    }
                  />
                </label>
                <label className="label">
                  Timeout (sec)
                  <input
                    className="input"
                    type="number"
                    min="1"
                    value={jobForm.timeoutSeconds}
                    onChange={(e) =>
                      setJobForm({
                        ...jobForm,
                        timeoutSeconds: Number(e.target.value)
                      })
                    }
                  />
                </label>
              </div>
              <label className="label">
                Webhook URL (optional)
                <input
                  className="input"
                  placeholder="https://your-app.com/webhook"
                  value={jobForm.webhookUrl}
                  onChange={(e) =>
                    setJobForm({
                      ...jobForm,
                      webhookUrl: e.target.value
                    })
                  }
                />
              </label>
              <div className="modal-actions">
                <button
                  type="button"
                  className="btn btn--ghost"
                  onClick={() => setShowCreateModal(false)}
                >
                  Cancel
                </button>
                <button className="btn btn--primary" disabled={loading}>
                  {loading ? "Creating..." : "Create Job"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {error && <div className="toast toast--error">{error}</div>}
    </div>
  );
}

export default App;
