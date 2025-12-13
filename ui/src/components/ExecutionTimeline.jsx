import React, { useState, useEffect } from 'react';
import axios from 'axios';

const ExecutionTimeline = ({ jobId, limit = 20 }) => {
  const [instances, setInstances] = useState([]);
  const [loading, setLoading] = useState(true);
  const [selectedInstance, setSelectedInstance] = useState(null);

  useEffect(() => {
    fetchInstances();
  }, [jobId]);

  const fetchInstances = async () => {
    try {
      const response = await axios.get(`/api/instances/job/${jobId}`, {
        params: { page: 0, size: limit }
      });
      setInstances(response.data.data.content || []);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch instances:', error);
      setLoading(false);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'SUCCESS': return 'bg-green-500';
      case 'FAILED': return 'bg-red-500';
      case 'RUNNING': return 'bg-blue-500';
      case 'PENDING': return 'bg-yellow-500';
      case 'TIMEOUT': return 'bg-orange-500';
      case 'CANCELLED': return 'bg-gray-500';
      default: return 'bg-gray-400';
    }
  };

  const getStatusIcon = (status) => {
    switch (status) {
      case 'SUCCESS': return '✓';
      case 'FAILED': return '✗';
      case 'RUNNING': return '↻';
      case 'PENDING': return '⋯';
      case 'TIMEOUT': return '⏱';
      case 'CANCELLED': return '⊘';
      default: return '?';
    }
  };

  const formatDate = (dateString) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);

    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
    return date.toLocaleDateString();
  };

  const getDurationWidth = (durationMs) => {
    if (!durationMs) return 0;
    const maxDuration = Math.max(...instances.map(i => i.durationMs || 0));
    return Math.min((durationMs / maxDuration) * 100, 100);
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <div className="flex items-center justify-between mb-6">
        <h3 className="text-lg font-semibold">Execution Timeline</h3>
        <button
          onClick={fetchInstances}
          className="text-sm text-blue-600 hover:text-blue-800 font-medium"
        >
          🔄 Refresh
        </button>
      </div>

      {instances.length === 0 ? (
        <div className="text-center py-12 text-gray-500">
          No executions yet
        </div>
      ) : (
        <div className="space-y-2">
          {instances.map((instance, index) => (
            <div
              key={instance.id}
              onClick={() => setSelectedInstance(instance)}
              className="relative group cursor-pointer"
            >
              {/* Timeline Line */}
              {index < instances.length - 1 && (
                <div className="absolute left-4 top-8 w-0.5 h-full bg-gray-200 -z-10" />
              )}

              <div className="flex items-start space-x-4 p-3 hover:bg-gray-50 rounded-lg transition">
                {/* Status Indicator */}
                <div className={`w-8 h-8 rounded-full ${getStatusColor(instance.status)} flex items-center justify-center text-white font-bold flex-shrink-0`}>
                  {getStatusIcon(instance.status)}
                </div>

                {/* Instance Info */}
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between mb-1">
                    <span className="text-sm font-medium text-gray-800">
                      Instance #{instance.id}
                    </span>
                    <span className="text-xs text-gray-500">
                      {formatDate(instance.scheduledTime)}
                    </span>
                  </div>

                  {/* Duration Bar */}
                  {instance.durationMs && (
                    <div className="mb-2">
                      <div className="flex items-center justify-between text-xs text-gray-600 mb-1">
                        <span>Duration: {instance.durationFormatted}</span>
                        {instance.retryCount > 0 && (
                          <span className="text-orange-600">
                            🔄 Retry {instance.retryCount}
                          </span>
                        )}
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-1.5">
                        <div
                          className={`h-1.5 rounded-full ${
                            instance.status === 'SUCCESS' ? 'bg-green-500' : 'bg-red-500'
                          }`}
                          style={{ width: `${getDurationWidth(instance.durationMs)}%` }}
                        />
                      </div>
                    </div>
                  )}

                  {/* Status Details */}
                  <div className="text-xs text-gray-600">
                    {instance.status === 'SUCCESS' && instance.httpStatusCode && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded bg-green-100 text-green-800">
                        HTTP {instance.httpStatusCode}
                      </span>
                    )}
                    {instance.status === 'FAILED' && instance.errorMessage && (
                      <span className="inline-flex items-center text-red-600 truncate max-w-md">
                        Error: {instance.errorMessage.substring(0, 50)}...
                      </span>
                    )}
                    {instance.status === 'RUNNING' && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded bg-blue-100 text-blue-800 animate-pulse">
                        In Progress...
                      </span>
                    )}
                    {instance.status === 'PENDING' && (
                      <span className="inline-flex items-center px-2 py-0.5 rounded bg-yellow-100 text-yellow-800">
                        Waiting to execute
                      </span>
                    )}
                  </div>
                </div>

                {/* Hover Details */}
                <div className="opacity-0 group-hover:opacity-100 transition-opacity">
                  <button className="text-blue-600 hover:text-blue-800 text-xs font-medium">
                    View Details →
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Legend */}
      <div className="mt-6 pt-4 border-t border-gray-200">
        <div className="text-xs font-semibold text-gray-700 mb-2">Status Legend:</div>
        <div className="flex flex-wrap gap-3">
          {['SUCCESS', 'FAILED', 'RUNNING', 'PENDING', 'TIMEOUT', 'CANCELLED'].map((status) => (
            <div key={status} className="flex items-center space-x-2">
              <div className={`w-3 h-3 rounded-full ${getStatusColor(status)}`} />
              <span className="text-xs text-gray-600">{status}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Summary Stats */}
      <div className="mt-4 pt-4 border-t border-gray-200 grid grid-cols-3 gap-4">
        <div className="text-center">
          <div className="text-2xl font-bold text-gray-800">
            {instances.filter(i => i.status === 'SUCCESS').length}
          </div>
          <div className="text-xs text-gray-600">Successful</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-gray-800">
            {instances.filter(i => i.status === 'FAILED').length}
          </div>
          <div className="text-xs text-gray-600">Failed</div>
        </div>
        <div className="text-center">
          <div className="text-2xl font-bold text-gray-800">
            {instances.filter(i => i.status === 'RUNNING').length}
          </div>
          <div className="text-xs text-gray-600">Running</div>
        </div>
      </div>
    </div>
  );
};

export default ExecutionTimeline;