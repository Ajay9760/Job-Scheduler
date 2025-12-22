import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import StatisticsCharts from './StatisticsCharts';
import InstancesList from './InstancesList';
import LogsViewer from './LogsViewer';

const JobDetailsPage = () => {
  const { id } = useParams();
  const navigate = useNavigate();
  const [job, setJob] = useState(null);
  const [statistics, setStatistics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState('overview');
  const [showEditModal, setShowEditModal] = useState(false);

  useEffect(() => {
    fetchJobDetails();
    fetchStatistics();
  }, [id]);

  const fetchJobDetails = async () => {
    try {
      const response = await axios.get(`/api/jobs/${id}`);
      setJob(response.data.data);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch job:', error);
      setLoading(false);
    }
  };

  const fetchStatistics = async () => {
    try {
      const response = await axios.get(`/api/statistics/job/${id}`);
      setStatistics(response.data.data);
    } catch (error) {
      console.error('Failed to fetch statistics:', error);
    }
  };

  const handleTrigger = async () => {
    try {
      await axios.post(`/api/jobs/${id}/trigger`);
      alert('Job triggered successfully!');
      fetchJobDetails();
    } catch (error) {
      alert('Failed to trigger job: ' + error.response?.data?.error);
    }
  };

  const handlePause = async () => {
    try {
      await axios.post(`/api/jobs/${id}/pause`);
      alert('Job paused successfully!');
      fetchJobDetails();
    } catch (error) {
      alert('Failed to pause job: ' + error.response?.data?.error);
    }
  };

  const handleResume = async () => {
    try {
      await axios.post(`/api/jobs/${id}/resume`);
      alert('Job resumed successfully!');
      fetchJobDetails();
    } catch (error) {
      alert('Failed to resume job: ' + error.response?.data?.error);
    }
  };

  const handleDelete = async () => {
    if (!window.confirm('Are you sure you want to delete this job?')) return;

    try {
      await axios.delete(`/api/jobs/${id}`);
      alert('Job deleted successfully!');
      navigate('/');
    } catch (error) {
      alert('Failed to delete job: ' + error.response?.data?.error);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <h2 className="text-2xl font-bold text-gray-800">Job Not Found</h2>
          <button
            onClick={() => navigate('/')}
            className="mt-4 px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Back to Jobs
          </button>
        </div>
      </div>
    );
  }

  const getStatusColor = (status) => {
    switch (status) {
      case 'ACTIVE': return 'bg-green-100 text-green-800';
      case 'PAUSED': return 'bg-yellow-100 text-yellow-800';
      case 'INACTIVE': return 'bg-gray-100 text-gray-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="bg-white rounded-lg shadow-md p-6 mb-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <button
                onClick={() => navigate('/')}
                className="text-gray-600 hover:text-gray-800"
              >
                ← Back
              </button>
              <div>
                <h1 className="text-3xl font-bold text-gray-800">{job.name}</h1>
                <p className="text-gray-600 mt-1">{job.description}</p>
              </div>
            </div>
            <span className={`px-4 py-2 rounded-full text-sm font-semibold ${getStatusColor(job.status)}`}>
              {job.status}
            </span>
          </div>

          {/* Action Buttons */}
          <div className="flex space-x-3 mt-6">
            <button
              onClick={handleTrigger}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition"
            >
              ▶️ Trigger Now
            </button>
            {job.status === 'ACTIVE' ? (
              <button
                onClick={handlePause}
                className="px-4 py-2 bg-yellow-500 text-white rounded hover:bg-yellow-600 transition"
              >
                ⏸️ Pause
              </button>
            ) : (
              <button
                onClick={handleResume}
                className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600 transition"
              >
                ▶️ Resume
              </button>
            )}
            <button
              onClick={() => setShowEditModal(true)}
              className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 transition"
            >
              ✏️ Edit
            </button>
            <button
              onClick={handleDelete}
              className="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700 transition"
            >
              🗑️ Delete
            </button>
          </div>
        </div>

        {/* Tabs */}
        <div className="bg-white rounded-lg shadow-md mb-6">
          <div className="border-b border-gray-200">
            <nav className="flex space-x-8 px-6">
              {['overview', 'instances', 'logs', 'statistics'].map((tab) => (
                <button
                  key={tab}
                  onClick={() => setActiveTab(tab)}
                  className={`py-4 px-1 border-b-2 font-medium text-sm transition ${
                    activeTab === tab
                      ? 'border-blue-500 text-blue-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  }`}
                >
                  {tab.charAt(0).toUpperCase() + tab.slice(1)}
                </button>
              ))}
            </nav>
          </div>

          <div className="p-6">
            {/* Overview Tab */}
            {activeTab === 'overview' && (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div>
                  <h3 className="text-lg font-semibold mb-4">Job Configuration</h3>
                  <dl className="space-y-3">
                    <div>
                      <dt className="text-sm font-medium text-gray-500">URL</dt>
                      <dd className="mt-1 text-sm text-gray-900 font-mono bg-gray-50 p-2 rounded">
                        {job.httpUrl}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-sm font-medium text-gray-500">Method</dt>
                      <dd className="mt-1 text-sm text-gray-900">{job.httpMethod}</dd>
                    </div>
                    <div>
                      <dt className="text-sm font-medium text-gray-500">Schedule</dt>
                      <dd className="mt-1 text-sm text-gray-900">
                        {job.scheduleType}: {job.cronExpression}
                      </dd>
                    </div>
                    <div>
                      <dt className="text-sm font-medium text-gray-500">Timeout</dt>
                      <dd className="mt-1 text-sm text-gray-900">{job.timeoutSeconds}s</dd>
                    </div>
                  </dl>
                </div>

                <div>
                  <h3 className="text-lg font-semibold mb-4">Statistics</h3>
                  {statistics && (
                    <div className="grid grid-cols-2 gap-4">
                      <div className="bg-blue-50 p-4 rounded">
                        <div className="text-2xl font-bold text-blue-600">{statistics.totalRuns}</div>
                        <div className="text-sm text-gray-600">Total Runs</div>
                      </div>
                      <div className="bg-green-50 p-4 rounded">
                        <div className="text-2xl font-bold text-green-600">
                          {statistics.successRate?.toFixed(1)}%
                        </div>
                        <div className="text-sm text-gray-600">Success Rate</div>
                      </div>
                      <div className="bg-purple-50 p-4 rounded">
                        <div className="text-2xl font-bold text-purple-600">
                          {statistics.avgDurationFormatted}
                        </div>
                        <div className="text-sm text-gray-600">Avg Duration</div>
                      </div>
                      <div className="bg-red-50 p-4 rounded">
                        <div className="text-2xl font-bold text-red-600">{statistics.failedRuns}</div>
                        <div className="text-sm text-gray-600">Failed Runs</div>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            )}

            {/* Instances Tab */}
            {activeTab === 'instances' && <InstancesList jobId={id} />}

            {/* Logs Tab */}
            {activeTab === 'logs' && <LogsViewer jobId={id} />}

            {/* Statistics Tab */}
            {activeTab === 'statistics' && statistics && (
              <StatisticsCharts jobId={id} statistics={statistics} />
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default JobDetailsPage;