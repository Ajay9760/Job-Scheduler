import React, { useState, useEffect } from 'react';
import axios from 'axios';
import LogsViewer from './LogsViewer';

const InstanceDetailsModal = ({ instance, onClose, onRefresh }) => {
  const [logs, setLogs] = useState([]);
  const [activeTab, setActiveTab] = useState('details');

  useEffect(() => {
    if (activeTab === 'logs') {
      fetchLogs();
    }
  }, [activeTab, instance.id]);

  const fetchLogs = async () => {
    try {
      const response = await axios.get(`/api/logs/instance/${instance.id}`);
      setLogs(response.data.data);
    } catch (error) {
      console.error('Failed to fetch logs:', error);
    }
  };

  const getStatusColor = (status) => {
    const colors = {
      SUCCESS: 'bg-green-100 text-green-800 border-green-300',
      FAILED: 'bg-red-100 text-red-800 border-red-300',
      RUNNING: 'bg-blue-100 text-blue-800 border-blue-300',
      PENDING: 'bg-yellow-100 text-yellow-800 border-yellow-300',
      TIMEOUT: 'bg-orange-100 text-orange-800 border-orange-300',
      CANCELLED: 'bg-gray-100 text-gray-800 border-gray-300',
    };
    return colors[status] || 'bg-gray-100 text-gray-800 border-gray-300';
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    return new Date(dateString).toLocaleString();
  };

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    alert('Copied to clipboard!');
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 p-4">
      <div className="bg-white rounded-lg shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 flex items-center justify-between">
          <div>
            <h2 className="text-2xl font-bold text-gray-800">Instance #{instance.id}</h2>
            <p className="text-sm text-gray-600 mt-1">{instance.jobName}</p>
          </div>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600 text-2xl font-bold"
          >
            ×
          </button>
        </div>

        {/* Status Badge */}
        <div className="px-6 py-3 bg-gray-50 border-b border-gray-200">
          <span className={`px-4 py-2 rounded-full text-sm font-semibold border ${getStatusColor(instance.status)}`}>
            {instance.status}
          </span>
        </div>

        {/* Tabs */}
        <div className="border-b border-gray-200">
          <nav className="flex px-6">
            {['details', 'response', 'logs'].map((tab) => (
              <button
                key={tab}
                onClick={() => setActiveTab(tab)}
                className={`py-3 px-4 border-b-2 font-medium text-sm transition ${
                  activeTab === tab
                    ? 'border-blue-500 text-blue-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700'
                }`}
              >
                {tab.charAt(0).toUpperCase() + tab.slice(1)}
              </button>
            ))}
          </nav>
        </div>

        {/* Content */}
        <div className="flex-1 overflow-y-auto p-6">
          {/* Details Tab */}
          {activeTab === 'details' && (
            <div className="space-y-6">
              {/* Timing Information */}
              <div>
                <h3 className="text-lg font-semibold mb-3">Timing</h3>
                <dl className="grid grid-cols-2 gap-4">
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Scheduled Time</dt>
                    <dd className="mt-1 text-sm text-gray-900">{formatDate(instance.scheduledTime)}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Started At</dt>
                    <dd className="mt-1 text-sm text-gray-900">{formatDate(instance.startedAt)}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Completed At</dt>
                    <dd className="mt-1 text-sm text-gray-900">{formatDate(instance.completedAt)}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Duration</dt>
                    <dd className="mt-1 text-sm text-gray-900">{instance.durationFormatted || 'N/A'}</dd>
                  </div>
                </dl>
              </div>

              {/* Execution Information */}
              <div>
                <h3 className="text-lg font-semibold mb-3">Execution</h3>
                <dl className="grid grid-cols-2 gap-4">
                  <div>
                    <dt className="text-sm font-medium text-gray-500">HTTP Status Code</dt>
                    <dd className="mt-1 text-sm text-gray-900">
                      {instance.httpStatusCode || 'N/A'}
                    </dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Retry Count</dt>
                    <dd className="mt-1 text-sm text-gray-900">{instance.retryCount || 0}</dd>
                  </div>
                </dl>
              </div>

              {/* Error Message (if failed) */}
              {instance.errorMessage && (
                <div>
                  <h3 className="text-lg font-semibold mb-3 text-red-600">Error Message</h3>
                  <div className="bg-red-50 border border-red-200 rounded-lg p-4">
                    <pre className="text-sm text-red-900 whitespace-pre-wrap font-mono">
                      {instance.errorMessage}
                    </pre>
                  </div>
                </div>
              )}

              {/* Metadata */}
              <div>
                <h3 className="text-lg font-semibold mb-3">Metadata</h3>
                <dl className="grid grid-cols-2 gap-4">
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Created At</dt>
                    <dd className="mt-1 text-sm text-gray-900">{formatDate(instance.createdAt)}</dd>
                  </div>
                  <div>
                    <dt className="text-sm font-medium text-gray-500">Updated At</dt>
                    <dd className="mt-1 text-sm text-gray-900">{formatDate(instance.updatedAt)}</dd>
                  </div>
                </dl>
              </div>
            </div>
          )}

          {/* Response Tab */}
          {activeTab === 'response' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-lg font-semibold">HTTP Response</h3>
                {instance.responseBody && (
                  <button
                    onClick={() => copyToClipboard(instance.responseBody)}
                    className="px-3 py-1 bg-blue-600 text-white rounded text-sm hover:bg-blue-700 transition"
                  >
                    📋 Copy
                  </button>
                )}
              </div>

              {instance.responseBody ? (
                <div className="bg-gray-900 text-gray-100 rounded-lg p-4 overflow-x-auto">
                  <pre className="text-sm font-mono whitespace-pre-wrap">
                    {instance.responseBody}
                  </pre>
                </div>
              ) : (
                <div className="text-center py-12 text-gray-500">
                  No response body available
                </div>
              )}

              {instance.httpStatusCode && (
                <div className="flex items-center space-x-2 text-sm">
                  <span className="font-medium text-gray-600">Status Code:</span>
                  <span className={`px-3 py-1 rounded font-mono ${
                    instance.httpStatusCode >= 200 && instance.httpStatusCode < 300
                      ? 'bg-green-100 text-green-800'
                      : 'bg-red-100 text-red-800'
                  }`}>
                    {instance.httpStatusCode}
                  </span>
                </div>
              )}
            </div>
          )}

          {/* Logs Tab */}
          {activeTab === 'logs' && (
            <LogsViewer instanceId={instance.id} />
          )}
        </div>

        {/* Footer */}
        <div className="px-6 py-4 bg-gray-50 border-t border-gray-200 flex justify-end space-x-3">
          <button
            onClick={onClose}
            className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 transition"
          >
            Close
          </button>
          {instance.status === 'FAILED' && (
            <button
              onClick={async () => {
                try {
                  await axios.post(`/api/jobs/instances/${instance.id}/retry`);
                  alert('Retry scheduled!');
                  onRefresh();
                  onClose();
                } catch (error) {
                  alert('Failed to retry: ' + error.response?.data?.error);
                }
              }}
              className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 transition"
            >
              🔄 Retry
            </button>
          )}
        </div>
      </div>
    </div>
  );
};

export default InstanceDetailsModal;