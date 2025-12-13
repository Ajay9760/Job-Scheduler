import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';

const RealTimeMonitor = () => {
  const [stats, setStats] = useState({
    totalJobs: 0,
    activeJobs: 0,
    totalExecutions: 0,
    runningExecutions: 0,
    executions24h: 0,
    failures24h: 0
  });
  const [recentInstances, setRecentInstances] = useState([]);
  const [alerts, setAlerts] = useState([]);
  const [isConnected, setIsConnected] = useState(true);
  const intervalRef = useRef(null);

  useEffect(() => {
    fetchData();

    // Poll every 3 seconds
    intervalRef.current = setInterval(fetchData, 3000);

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, []);

  const fetchData = async () => {
    try {
      // Fetch dashboard stats
      const statsResponse = await axios.get('/api/statistics/dashboard');
      setStats(statsResponse.data.data);

      // Fetch recent instances
      const instancesResponse = await axios.get('/api/instances/recent', {
        params: { limit: 10 }
      });
      setRecentInstances(instancesResponse.data.data.content || []);

      // Check for alerts (failures in last hour)
      if (statsResponse.data.data.failures24h > 0) {
        checkForAlerts();
      }

      setIsConnected(true);
    } catch (error) {
      console.error('Failed to fetch real-time data:', error);
      setIsConnected(false);
    }
  };

  const checkForAlerts = async () => {
    try {
      const response = await axios.get('/api/instances/failed', {
        params: { page: 0, size: 5 }
      });

      const newAlerts = response.data.data.content
        .filter(instance => {
          const failedTime = new Date(instance.completedAt);
          const hourAgo = new Date(Date.now() - 3600000);
          return failedTime > hourAgo;
        })
        .map(instance => ({
          id: instance.id,
          message: `Job "${instance.jobName}" failed`,
          time: instance.completedAt,
          severity: 'error'
        }));

      setAlerts(newAlerts);
    } catch (error) {
      console.error('Failed to fetch alerts:', error);
    }
  };

  const getStatusIndicator = (status) => {
    const colors = {
      SUCCESS: 'bg-green-500',
      FAILED: 'bg-red-500',
      RUNNING: 'bg-blue-500 animate-pulse',
      PENDING: 'bg-yellow-500',
    };
    return colors[status] || 'bg-gray-500';
  };

  const formatTimeAgo = (dateString) => {
    const seconds = Math.floor((new Date() - new Date(dateString)) / 1000);
    if (seconds < 60) return `${seconds}s ago`;
    const minutes = Math.floor(seconds / 60);
    if (minutes < 60) return `${minutes}m ago`;
    const hours = Math.floor(minutes / 60);
    return `${hours}h ago`;
  };

  return (
    <div className="space-y-6">
      {/* Connection Status */}
      <div className={`flex items-center justify-between p-4 rounded-lg ${
        isConnected ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'
      }`}>
        <div className="flex items-center space-x-3">
          <div className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`} />
          <span className={`font-medium ${isConnected ? 'text-green-800' : 'text-red-800'}`}>
            {isConnected ? 'Connected - Live Updates' : 'Disconnected'}
          </span>
        </div>
        <span className="text-sm text-gray-600">
          Refreshing every 3s
        </span>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-6 gap-4">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 text-white p-4 rounded-lg shadow-lg">
          <div className="text-2xl font-bold">{stats.totalJobs}</div>
          <div className="text-sm opacity-90">Total Jobs</div>
        </div>

        <div className="bg-gradient-to-br from-green-500 to-green-600 text-white p-4 rounded-lg shadow-lg">
          <div className="text-2xl font-bold">{stats.activeJobs}</div>
          <div className="text-sm opacity-90">Active Jobs</div>
        </div>

        <div className="bg-gradient-to-br from-purple-500 to-purple-600 text-white p-4 rounded-lg shadow-lg">
          <div className="text-2xl font-bold">{stats.totalExecutions}</div>
          <div className="text-sm opacity-90">Total Executions</div>
        </div>

        <div className="bg-gradient-to-br from-yellow-500 to-yellow-600 text-white p-4 rounded-lg shadow-lg relative">
          <div className="text-2xl font-bold">{stats.runningExecutions}</div>
          <div className="text-sm opacity-90">Running Now</div>
          {stats.runningExecutions > 0 && (
            <div className="absolute top-2 right-2 w-2 h-2 bg-white rounded-full animate-ping" />
          )}
        </div>

        <div className="bg-gradient-to-br from-indigo-500 to-indigo-600 text-white p-4 rounded-lg shadow-lg">
          <div className="text-2xl font-bold">{stats.executions24h}</div>
          <div className="text-sm opacity-90">Last 24h</div>
        </div>

        <div className="bg-gradient-to-br from-red-500 to-red-600 text-white p-4 rounded-lg shadow-lg">
          <div className="text-2xl font-bold">{stats.failures24h}</div>
          <div className="text-sm opacity-90">Failures 24h</div>
        </div>
      </div>

      {/* Alerts */}
      {alerts.length > 0 && (
        <div className="bg-red-50 border border-red-200 rounded-lg p-4">
          <div className="flex items-center space-x-2 mb-3">
            <span className="text-lg">🚨</span>
            <h3 className="font-semibold text-red-800">Recent Alerts</h3>
          </div>
          <div className="space-y-2">
            {alerts.map((alert) => (
              <div key={alert.id} className="flex items-center justify-between bg-white p-3 rounded">
                <span className="text-sm text-gray-800">{alert.message}</span>
                <span className="text-xs text-gray-500">{formatTimeAgo(alert.time)}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Recent Activity */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold mb-4">Recent Activity</h3>

        {recentInstances.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            No recent activity
          </div>
        ) : (
          <div className="space-y-3">
            {recentInstances.map((instance) => (
              <div
                key={instance.id}
                className="flex items-center justify-between p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition"
              >
                <div className="flex items-center space-x-3">
                  <div className={`w-3 h-3 rounded-full ${getStatusIndicator(instance.status)}`} />
                  <div>
                    <div className="font-medium text-gray-800">{instance.jobName}</div>
                    <div className="text-xs text-gray-500">
                      Instance #{instance.id} • {instance.status}
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-sm text-gray-600">{formatTimeAgo(instance.createdAt)}</div>
                  {instance.durationFormatted && (
                    <div className="text-xs text-gray-500">{instance.durationFormatted}</div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* System Health */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold mb-4">System Health</h3>
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">API Service</span>
            <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
              Healthy
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">Scheduler Service</span>
            <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
              Healthy
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">Worker Service</span>
            <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
              Healthy
            </span>
          </div>
          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-600">Database</span>
            <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
              Connected
            </span>
          </div>
        </div>
      </div>
    </div>
  );
};

export default RealTimeMonitor;