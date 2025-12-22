import React, { useState, useEffect } from 'react';
import axios from 'axios';

const JobHealthCard = ({ jobId }) => {
  const [health, setHealth] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchHealth();
    const interval = setInterval(fetchHealth, 30000); // Refresh every 30s
    return () => clearInterval(interval);
  }, [jobId]);

  const fetchHealth = async () => {
    try {
      const response = await axios.get(`/api/statistics/job/${jobId}/health`);
      setHealth(response.data.data);
      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch health:', error);
      setLoading(false);
    }
  };

  const getHealthColor = (status) => {
    switch (status) {
      case 'EXCELLENT': return 'text-green-600 bg-green-50 border-green-200';
      case 'GOOD': return 'text-blue-600 bg-blue-50 border-blue-200';
      case 'FAIR': return 'text-yellow-600 bg-yellow-50 border-yellow-200';
      case 'POOR': return 'text-red-600 bg-red-50 border-red-200';
      default: return 'text-gray-600 bg-gray-50 border-gray-200';
    }
  };

  const getHealthIcon = (status) => {
    switch (status) {
      case 'EXCELLENT': return '💚';
      case 'GOOD': return '💙';
      case 'FAIR': return '💛';
      case 'POOR': return '❤️';
      default: return '🩶';
    }
  };

  const getScoreColor = (score) => {
    if (score >= 90) return 'text-green-600';
    if (score >= 75) return 'text-blue-600';
    if (score >= 50) return 'text-yellow-600';
    return 'text-red-600';
  };

  if (loading) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-4 bg-gray-200 rounded w-3/4"></div>
          <div className="h-8 bg-gray-200 rounded"></div>
        </div>
      </div>
    );
  }

  if (!health) {
    return (
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="text-center text-gray-500">No health data available</div>
      </div>
    );
  }

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-semibold mb-4">Job Health</h3>

      {/* Health Score Circle */}
      <div className="flex items-center justify-center mb-6">
        <div className="relative">
          <svg className="transform -rotate-90 w-32 h-32">
            <circle
              cx="64"
              cy="64"
              r="56"
              stroke="currentColor"
              strokeWidth="8"
              fill="none"
              className="text-gray-200"
            />
            <circle
              cx="64"
              cy="64"
              r="56"
              stroke="currentColor"
              strokeWidth="8"
              fill="none"
              strokeDasharray={`${2 * Math.PI * 56}`}
              strokeDashoffset={`${2 * Math.PI * 56 * (1 - health.healthScore / 100)}`}
              className={getScoreColor(health.healthScore)}
              strokeLinecap="round"
            />
          </svg>
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="text-center">
              <div className={`text-3xl font-bold ${getScoreColor(health.healthScore)}`}>
                {Math.round(health.healthScore)}
              </div>
              <div className="text-xs text-gray-600">Health Score</div>
            </div>
          </div>
        </div>
      </div>

      {/* Health Status Badge */}
      <div className="flex justify-center mb-6">
        <div className={`px-6 py-3 rounded-full border-2 font-semibold ${getHealthColor(health.healthStatus)}`}>
          <span className="mr-2">{getHealthIcon(health.healthStatus)}</span>
          {health.healthStatus}
        </div>
      </div>

      {/* Metrics */}
      <div className="space-y-4">
        {/* Success Rate */}
        <div>
          <div className="flex justify-between text-sm mb-1">
            <span className="text-gray-600">Success Rate</span>
            <span className="font-semibold">{health.successRate?.toFixed(1)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className="bg-green-500 h-2 rounded-full transition-all"
              style={{ width: `${health.successRate}%` }}
            />
          </div>
        </div>

        {/* Average Duration */}
        <div>
          <div className="flex justify-between text-sm mb-1">
            <span className="text-gray-600">Avg Duration</span>
            <span className="font-semibold">
              {health.avgDurationMs < 1000
                ? `${health.avgDurationMs}ms`
                : `${(health.avgDurationMs / 1000).toFixed(2)}s`}
            </span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className="bg-blue-500 h-2 rounded-full transition-all"
              style={{ width: `${Math.min((health.avgDurationMs / 10000) * 100, 100)}%` }}
            />
          </div>
        </div>

        {/* Consecutive Failures */}
        {health.consecutiveFailures > 0 && (
          <div className="p-3 bg-red-50 border border-red-200 rounded-lg">
            <div className="flex items-center justify-between">
              <div className="flex items-center space-x-2">
                <span className="text-red-600 text-lg">⚠️</span>
                <span className="text-sm font-medium text-red-800">
                  Consecutive Failures
                </span>
              </div>
              <span className="text-lg font-bold text-red-600">
                {health.consecutiveFailures}
              </span>
            </div>
          </div>
        )}
      </div>

      {/* Recommendations */}
      {health.healthScore < 75 && (
        <div className="mt-6 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
          <div className="text-sm font-semibold text-yellow-800 mb-2">
            💡 Recommendations:
          </div>
          <ul className="text-sm text-yellow-700 space-y-1 list-disc list-inside">
            {health.successRate < 80 && (
              <li>Success rate is low - check error logs</li>
            )}
            {health.consecutiveFailures > 3 && (
              <li>Multiple consecutive failures detected - investigate immediately</li>
            )}
            {health.avgDurationMs > 30000 && (
              <li>Execution time is high - consider optimization</li>
            )}
          </ul>
        </div>
      )}

      {/* Last Updated */}
      <div className="mt-4 text-xs text-gray-500 text-center">
        Last updated: {new Date().toLocaleTimeString()}
      </div>
    </div>
  );
};

export default JobHealthCard;