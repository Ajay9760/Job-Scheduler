import React, { useState, useEffect } from 'react';
import axios from 'axios';

const BulkActionsPanel = ({ jobs, onRefresh }) => {
  const [selectedJobs, setSelectedJobs] = useState([]);
  const [action, setAction] = useState('');
  const [processing, setProcessing] = useState(false);

  const handleSelectAll = () => {
    if (selectedJobs.length === jobs.length) {
      setSelectedJobs([]);
    } else {
      setSelectedJobs(jobs.map(job => job.id));
    }
  };

  const handleSelectJob = (jobId) => {
    if (selectedJobs.includes(jobId)) {
      setSelectedJobs(selectedJobs.filter(id => id !== jobId));
    } else {
      setSelectedJobs([...selectedJobs, jobId]);
    }
  };

  const executeBulkAction = async () => {
    if (selectedJobs.length === 0) {
      alert('Please select at least one job');
      return;
    }

    if (!action) {
      alert('Please select an action');
      return;
    }

    const confirmMessage = `Are you sure you want to ${action} ${selectedJobs.length} job(s)?`;
    if (!window.confirm(confirmMessage)) return;

    setProcessing(true);

    try {
      switch (action) {
        case 'trigger':
          await axios.post('/api/jobs/bulk-trigger', { jobIds: selectedJobs });
          alert(`Successfully triggered ${selectedJobs.length} job(s)`);
          break;

        case 'pause':
          for (const jobId of selectedJobs) {
            await axios.post(`/api/jobs/${jobId}/pause`);
          }
          alert(`Successfully paused ${selectedJobs.length} job(s)`);
          break;

        case 'resume':
          for (const jobId of selectedJobs) {
            await axios.post(`/api/jobs/${jobId}/resume`);
          }
          alert(`Successfully resumed ${selectedJobs.length} job(s)`);
          break;

        case 'disable':
          for (const jobId of selectedJobs) {
            await axios.post(`/api/jobs/${jobId}/disable`);
          }
          alert(`Successfully disabled ${selectedJobs.length} job(s)`);
          break;

        case 'delete':
          for (const jobId of selectedJobs) {
            await axios.delete(`/api/jobs/${jobId}`);
          }
          alert(`Successfully deleted ${selectedJobs.length} job(s)`);
          break;

        default:
          alert('Unknown action');
      }

      setSelectedJobs([]);
      setAction('');
      if (onRefresh) onRefresh();
    } catch (error) {
      alert('Bulk action failed: ' + error.response?.data?.error);
    } finally {
      setProcessing(false);
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-semibold mb-4">Bulk Actions</h3>

      {/* Selection Summary */}
      <div className="mb-4 p-3 bg-blue-50 border border-blue-200 rounded-lg">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <input
              type="checkbox"
              checked={selectedJobs.length === jobs.length && jobs.length > 0}
              onChange={handleSelectAll}
              className="w-5 h-5 text-blue-600 rounded focus:ring-blue-500"
            />
            <span className="font-medium text-blue-800">
              {selectedJobs.length === 0
                ? 'No jobs selected'
                : `${selectedJobs.length} job(s) selected`}
            </span>
          </div>
          {selectedJobs.length > 0 && (
            <button
              onClick={() => setSelectedJobs([])}
              className="text-sm text-blue-600 hover:text-blue-800"
            >
              Clear Selection
            </button>
          )}
        </div>
      </div>

      {/* Job List with Checkboxes */}
      <div className="mb-4 max-h-96 overflow-y-auto border border-gray-200 rounded-lg">
        {jobs.map((job) => (
          <div
            key={job.id}
            className="flex items-center p-3 hover:bg-gray-50 border-b border-gray-100 last:border-b-0"
          >
            <input
              type="checkbox"
              checked={selectedJobs.includes(job.id)}
              onChange={() => handleSelectJob(job.id)}
              className="w-5 h-5 text-blue-600 rounded focus:ring-blue-500"
            />
            <div className="ml-3 flex-1">
              <div className="font-medium text-gray-800">{job.name}</div>
              <div className="text-sm text-gray-600">{job.description}</div>
            </div>
            <span className={`px-3 py-1 rounded-full text-xs font-semibold ${
              job.status === 'ACTIVE'
                ? 'bg-green-100 text-green-800'
                : 'bg-gray-100 text-gray-800'
            }`}>
              {job.status}
            </span>
          </div>
        ))}
      </div>

      {/* Action Selector */}
      <div className="space-y-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Select Action
          </label>
          <select
            value={action}
            onChange={(e) => setAction(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            disabled={selectedJobs.length === 0}
          >
            <option value="">Choose an action...</option>
            <option value="trigger">▶️ Trigger Now</option>
            <option value="pause">⏸️ Pause</option>
            <option value="resume">▶️ Resume</option>
            <option value="disable">🚫 Disable</option>
            <option value="delete">🗑️ Delete</option>
          </select>
        </div>

        <button
          onClick={executeBulkAction}
          disabled={selectedJobs.length === 0 || !action || processing}
          className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
        >
          {processing ? '⏳ Processing...' : '🚀 Execute Bulk Action'}
        </button>
      </div>

      {/* Action Descriptions */}
      <div className="mt-6 p-4 bg-gray-50 rounded-lg">
        <h4 className="text-sm font-semibold text-gray-700 mb-2">Action Descriptions:</h4>
        <ul className="text-sm text-gray-600 space-y-1">
          <li><strong>Trigger Now:</strong> Execute selected jobs immediately</li>
          <li><strong>Pause:</strong> Temporarily stop job executions</li>
          <li><strong>Resume:</strong> Restart paused jobs</li>
          <li><strong>Disable:</strong> Stop jobs permanently (can be re-enabled)</li>
          <li><strong>Delete:</strong> Permanently remove jobs and their history</li>
        </ul>
      </div>
    </div>
  );
};

export default BulkActionsPanel;