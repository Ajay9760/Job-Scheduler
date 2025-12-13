import React, { useState, useEffect, useRef } from 'react';
import axios from 'axios';

const LogsViewer = ({ jobId, instanceId }) => {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [levelFilter, setLevelFilter] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [autoRefresh, setAutoRefresh] = useState(false);
  const logsEndRef = useRef(null);
  const intervalRef = useRef(null);

  useEffect(() => {
    fetchLogs();

    if (autoRefresh) {
      intervalRef.current = setInterval(fetchLogs, 3000);
    }

    return () => {
      if (intervalRef.current) {
        clearInterval(intervalRef.current);
      }
    };
  }, [jobId, instanceId, levelFilter, autoRefresh]);

  const fetchLogs = async () => {
    setLoading(true);
    try {
      let url;
      let params = {};

      if (instanceId) {
        url = `/api/logs/instance/${instanceId}`;
        if (levelFilter) params.level = levelFilter;
      } else if (jobId) {
        url = `/api/logs/job/${jobId}`;
        params = { page: 0, size: 100 };
        if (levelFilter) params.level = levelFilter;
      } else {
        url = '/api/logs/recent';
        params = { limit: 100 };
      }

      const response = await axios.get(url, { params });
      const fetchedLogs = Array.isArray(response.data.data)
        ? response.data.data
        : response.data.data.content || [];

      setLogs(fetchedLogs);
      setLoading(false);

      if (autoRefresh) {
        scrollToBottom();
      }
    } catch (error) {
      console.error('Failed to fetch logs:', error);
      setLoading(false);
    }
  };

  const scrollToBottom = () => {
    logsEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const exportLogs = async () => {
    if (!instanceId) {
      alert('Export is only available for instance logs');
      return;
    }

    try {
      const response = await axios.get(`/api/logs/instance/${instanceId}/export`, {
        responseType: 'blob'
      });

      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `logs-instance-${instanceId}.txt`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    } catch (error) {
      alert('Failed to export logs: ' + error.message);
    }
  };

  const getLevelIcon = (level) => {
    const icons = {
      DEBUG: '🔍',
      INFO: 'ℹ️',
      WARN: '⚠️',
      ERROR: '❌'
    };
    return icons[level] || 'ℹ️';
  };

  const getLevelColor = (level) => {
    const colors = {
      DEBUG: 'text-gray-600 bg-gray-50',
      INFO: 'text-blue-600 bg-blue-50',
      WARN: 'text-yellow-600 bg-yellow-50',
      ERROR: 'text-red-600 bg-red-50'
    };
    return colors[level] || 'text-gray-600 bg-gray-50';
  };

  const filteredLogs = logs.filter(log =>
    !searchTerm || log.message.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const formatTimestamp = (timestamp) => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString('en-US', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      fractionalSecondDigits: 3
    });
  };

  if (loading && logs.length === 0) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div>
      {/* Controls */}
      <div className="mb-4 flex items-center justify-between flex-wrap gap-4">
        <div className="flex items-center space-x-4">
          {/* Level Filter */}
          <select
            value={levelFilter}
            onChange={(e) => setLevelFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="">All Levels</option>
            <option value="DEBUG">Debug</option>
            <option value="INFO">Info</option>
            <option value="WARN">Warning</option>
            <option value="ERROR">Error</option>
          </select>

          {/* Search */}
          <input
            type="text"
            placeholder="Search logs..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
        </div>

        <div className="flex items-center space-x-3">
          {/* Auto Refresh Toggle */}
          <label className="flex items-center space-x-2 cursor-pointer">
            <input
              type="checkbox"
              checked={autoRefresh}
              onChange={(e) => setAutoRefresh(e.target.checked)}
              className="w-4 h-4 text-blue-600 rounded focus:ring-blue-500"
            />
            <span className="text-sm text-gray-700">Auto-refresh</span>
          </label>

          {/* Manual Refresh */}
          <button
            onClick={fetchLogs}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
          >
            🔄 Refresh
          </button>

          {/* Export */}
          {instanceId && (
            <button
              onClick={exportLogs}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition"
            >
              📥 Export
            </button>
          )}

          {/* Scroll to Bottom */}
          <button
            onClick={scrollToBottom}
            className="px-4 py-2 bg-gray-600 text-white rounded-lg hover:bg-gray-700 transition"
          >
            ⬇️ Bottom
          </button>
        </div>
      </div>

      {/* Logs Display */}
      <div className="bg-gray-900 rounded-lg p-4 h-[600px] overflow-y-auto font-mono text-sm">
        {filteredLogs.length === 0 ? (
          <div className="text-gray-400 text-center py-12">
            No logs found
          </div>
        ) : (
          filteredLogs.map((log) => (
            <div
              key={log.id}
              className={`py-2 px-3 mb-1 rounded ${getLevelColor(log.logLevel)} border-l-4 border-${log.levelColor}-500`}
            >
              <div className="flex items-start space-x-3">
                <span className="text-xs text-gray-500 whitespace-nowrap">
                  {formatTimestamp(log.createdAt)}
                </span>
                <span className="text-xs font-semibold">
                  {getLevelIcon(log.logLevel)} {log.logLevel}
                </span>
                <span className="flex-1 break-words">{log.message}</span>
              </div>

              {/* Details (if any) */}
              {log.details && Object.keys(log.details).length > 0 && (
                <div className="mt-2 pl-20 text-xs text-gray-600 bg-gray-100 p-2 rounded">
                  <pre className="whitespace-pre-wrap">
                    {JSON.stringify(log.details, null, 2)}
                  </pre>
                </div>
              )}
            </div>
          ))
        )}
        <div ref={logsEndRef} />
      </div>

      {/* Log Count */}
      <div className="mt-4 text-sm text-gray-600 text-center">
        Showing {filteredLogs.length} log{filteredLogs.length !== 1 ? 's' : ''}
        {searchTerm && ` matching "${searchTerm}"`}
      </div>
    </div>
  );
};

export default LogsViewer;