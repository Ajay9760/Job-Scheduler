import React, { useState, useEffect } from 'react';
import axios from 'axios';
import InstanceDetailsModal from './InstanceDetailsModal';

const InstancesList = ({ jobId }) => {
  const [instances, setInstances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [statusFilter, setStatusFilter] = useState('');
  const [selectedInstance, setSelectedInstance] = useState(null);
  const [showModal, setShowModal] = useState(false);

  useEffect(() => {
    fetchInstances();
  }, [jobId, page, statusFilter]);

  const fetchInstances = async () => {
    setLoading(true);
    try {
      const url = jobId
        ? `/api/instances/job/${jobId}`
        : '/api/instances';

      const params = {
        page,
        size: 20,
        ...(statusFilter && { status: statusFilter })
      };

      const response = await axios.get(url, { params });
      setInstances(response.data.data.content);
      setTotalPages(response.data.data.totalPages);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch instances:', error);
      setLoading(false);
    }
  };

  const handleRetry = async (instanceId) => {
    try {
      await axios.post(`/api/jobs/instances/${instanceId}/retry`);
      alert('Retry scheduled successfully!');
      fetchInstances();
    } catch (error) {
      alert('Failed to retry: ' + error.response?.data?.error);
    }
  };

  const handleStop = async (instanceId) => {
    if (!window.confirm('Are you sure you want to stop this instance?')) return;

    try {
      await axios.post(`/api/jobs/instances/${instanceId}/stop`);
      alert('Instance stopped successfully!');
      fetchInstances();
    } catch (error) {
      alert('Failed to stop instance: ' + error.response?.data?.error);
    }
  };

  const viewDetails = (instance) => {
    setSelectedInstance(instance);
    setShowModal(true);
  };

  const getStatusBadge = (status) => {
    const colors = {
      SUCCESS: 'bg-green-100 text-green-800',
      FAILED: 'bg-red-100 text-red-800',
      RUNNING: 'bg-blue-100 text-blue-800',
      PENDING: 'bg-yellow-100 text-yellow-800',
      TIMEOUT: 'bg-orange-100 text-orange-800',
      CANCELLED: 'bg-gray-100 text-gray-800',
    };

    return (
      <span className={`px-3 py-1 rounded-full text-xs font-semibold ${colors[status] || 'bg-gray-100 text-gray-800'}`}>
        {status}
      </span>
    );
  };

  const formatDate = (dateString) => {
    if (!dateString) return 'N/A';
    const date = new Date(dateString);
    return date.toLocaleString();
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div>
      {/* Filters */}
      <div className="mb-6 flex items-center space-x-4">
        <label className="text-sm font-medium text-gray-700">Filter by Status:</label>
        <select
          value={statusFilter}
          onChange={(e) => setStatusFilter(e.target.value)}
          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value="">All Statuses</option>
          <option value="SUCCESS">Success</option>
          <option value="FAILED">Failed</option>
          <option value="RUNNING">Running</option>
          <option value="PENDING">Pending</option>
          <option value="TIMEOUT">Timeout</option>
          <option value="CANCELLED">Cancelled</option>
        </select>
        <button
          onClick={fetchInstances}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          🔄 Refresh
        </button>
      </div>

      {/* Instances Table */}
      {instances.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          No instances found
        </div>
      ) : (
        <div className="overflow-x-auto">
          <table className="min-w-full bg-white border border-gray-200 rounded-lg">
            <thead className="bg-gray-50">
              <tr>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  ID
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Status
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Scheduled Time
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Duration
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Retry Count
                </th>
                <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-200">
              {instances.map((instance) => (
                <tr key={instance.id} className="hover:bg-gray-50 transition">
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-900">
                    #{instance.id}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap">
                    {getStatusBadge(instance.status)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {formatDate(instance.scheduledTime)}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {instance.durationFormatted || 'N/A'}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                    {instance.retryCount || 0}
                  </td>
                  <td className="px-6 py-4 whitespace-nowrap text-sm space-x-2">
                    <button
                      onClick={() => viewDetails(instance)}
                      className="text-blue-600 hover:text-blue-800 font-medium"
                    >
                      View
                    </button>
                    {instance.status === 'FAILED' && (
                      <button
                        onClick={() => handleRetry(instance.id)}
                        className="text-green-600 hover:text-green-800 font-medium"
                      >
                        Retry
                      </button>
                    )}
                    {instance.status === 'RUNNING' && (
                      <button
                        onClick={() => handleStop(instance.id)}
                        className="text-red-600 hover:text-red-800 font-medium"
                      >
                        Stop
                      </button>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="mt-6 flex items-center justify-between">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
          >
            ← Previous
          </button>
          <span className="text-sm text-gray-600">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 bg-gray-600 text-white rounded hover:bg-gray-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
          >
            Next →
          </button>
        </div>
      )}

      {/* Instance Details Modal */}
      {showModal && selectedInstance && (
        <InstanceDetailsModal
          instance={selectedInstance}
          onClose={() => {
            setShowModal(false);
            setSelectedInstance(null);
          }}
          onRefresh={fetchInstances}
        />
      )}
    </div>
  );
};

export default InstancesList;