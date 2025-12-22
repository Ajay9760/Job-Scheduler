import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { LineChart, Line, BarChart, Bar, PieChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

const StatisticsCharts = ({ jobId, statistics }) => {
  const [dailyStats, setDailyStats] = useState([]);
  const [durationDistribution, setDurationDistribution] = useState([]);
  const [loading, setLoading] = useState(true);
  const [timeRange, setTimeRange] = useState(7); // days

  useEffect(() => {
    fetchChartData();
  }, [jobId, timeRange]);

  const fetchChartData = async () => {
    setLoading(true);
    try {
      // Fetch daily statistics
      const dailyResponse = await axios.get(`/api/statistics/job/${jobId}/daily`, {
        params: { days: timeRange }
      });
      setDailyStats(dailyResponse.data.data);

      // Fetch duration distribution
      const durationResponse = await axios.get(`/api/statistics/job/${jobId}/duration-distribution`);
      const distribution = Object.entries(durationResponse.data.data).map(([key, value]) => ({
        name: key,
        value: value
      }));
      setDurationDistribution(distribution);

      setLoading(false);
    } catch (error) {
      console.error('Failed to fetch chart data:', error);
      setLoading(false);
    }
  };

  // Colors for charts
  const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884D8'];

  // Success rate pie chart data
  const successRateData = [
    { name: 'Success', value: statistics?.successfulRuns || 0, color: '#10B981' },
    { name: 'Failed', value: statistics?.failedRuns || 0, color: '#EF4444' },
    { name: 'Pending', value: statistics?.pendingRuns || 0, color: '#F59E0B' },
  ].filter(item => item.value > 0);

  if (loading) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  return (
    <div className="space-y-8">
      {/* Time Range Selector */}
      <div className="flex items-center space-x-4">
        <label className="text-sm font-medium text-gray-700">Time Range:</label>
        <select
          value={timeRange}
          onChange={(e) => setTimeRange(parseInt(e.target.value))}
          className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
        >
          <option value={7}>Last 7 days</option>
          <option value={14}>Last 14 days</option>
          <option value={30}>Last 30 days</option>
          <option value={90}>Last 90 days</option>
        </select>
      </div>

      {/* Key Metrics Cards */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <div className="bg-gradient-to-br from-blue-500 to-blue-600 text-white p-6 rounded-lg shadow-lg">
          <div className="text-3xl font-bold">{statistics?.totalRuns || 0}</div>
          <div className="text-sm opacity-90 mt-1">Total Executions</div>
        </div>
        <div className="bg-gradient-to-br from-green-500 to-green-600 text-white p-6 rounded-lg shadow-lg">
          <div className="text-3xl font-bold">{statistics?.successRate?.toFixed(1) || 0}%</div>
          <div className="text-sm opacity-90 mt-1">Success Rate</div>
        </div>
        <div className="bg-gradient-to-br from-purple-500 to-purple-600 text-white p-6 rounded-lg shadow-lg">
          <div className="text-3xl font-bold">{statistics?.avgDurationFormatted || 'N/A'}</div>
          <div className="text-sm opacity-90 mt-1">Avg Duration</div>
        </div>
        <div className="bg-gradient-to-br from-red-500 to-red-600 text-white p-6 rounded-lg shadow-lg">
          <div className="text-3xl font-bold">{statistics?.consecutiveFailures || 0}</div>
          <div className="text-sm opacity-90 mt-1">Consecutive Failures</div>
        </div>
      </div>

      {/* Daily Executions Trend */}
      <div className="bg-white p-6 rounded-lg shadow-md">
        <h3 className="text-lg font-semibold mb-4">Daily Execution Trend</h3>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={dailyStats}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="total" stroke="#3B82F6" strokeWidth={2} name="Total" />
            <Line type="monotone" dataKey="success" stroke="#10B981" strokeWidth={2} name="Success" />
            <Line type="monotone" dataKey="failed" stroke="#EF4444" strokeWidth={2} name="Failed" />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Success vs Failure Distribution */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold mb-4">Success vs Failure</h3>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={successRateData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                outerRadius={100}
                fill="#8884d8"
                dataKey="value"
              >
                {successRateData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </div>

        {/* Duration Distribution */}
        <div className="bg-white p-6 rounded-lg shadow-md">
          <h3 className="text-lg font-semibold mb-4">Duration Distribution</h3>
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={durationDistribution}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" />
              <YAxis />
              <Tooltip />
              <Bar dataKey="value" fill="#8B5CF6" />
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Average Duration Trend */}
      <div className="bg-white p-6 rounded-lg shadow-md">
        <h3 className="text-lg font-semibold mb-4">Average Duration Trend</h3>
        <ResponsiveContainer width="100%" height={300}>
          <LineChart data={dailyStats}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip
              formatter={(value) => {
                if (value < 1000) return `${value}ms`;
                return `${(value / 1000).toFixed(2)}s`;
              }}
            />
            <Legend />
            <Line type="monotone" dataKey="avgDuration" stroke="#8B5CF6" strokeWidth={2} name="Avg Duration (ms)" />
          </LineChart>
        </ResponsiveContainer>
      </div>

      {/* Performance Summary */}
      <div className="bg-white p-6 rounded-lg shadow-md">
        <h3 className="text-lg font-semibold mb-4">Performance Summary</h3>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="border-l-4 border-blue-500 pl-4">
            <div className="text-sm text-gray-600">Total Runs</div>
            <div className="text-2xl font-bold text-gray-800">{statistics?.totalRuns || 0}</div>
          </div>
          <div className="border-l-4 border-green-500 pl-4">
            <div className="text-sm text-gray-600">Successful</div>
            <div className="text-2xl font-bold text-green-600">{statistics?.successfulRuns || 0}</div>
          </div>
          <div className="border-l-4 border-red-500 pl-4">
            <div className="text-sm text-gray-600">Failed</div>
            <div className="text-2xl font-bold text-red-600">{statistics?.failedRuns || 0}</div>
          </div>
        </div>
      </div>

      {/* Last Execution Info */}
      {statistics?.lastRunAt && (
        <div className="bg-gray-50 p-6 rounded-lg">
          <h3 className="text-lg font-semibold mb-4">Last Execution</h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
            <div>
              <span className="text-gray-600">Last Run:</span>
              <span className="ml-2 font-medium">{new Date(statistics.lastRunAt).toLocaleString()}</span>
            </div>
            {statistics.lastSuccessAt && (
              <div>
                <span className="text-gray-600">Last Success:</span>
                <span className="ml-2 font-medium text-green-600">
                  {new Date(statistics.lastSuccessAt).toLocaleString()}
                </span>
              </div>
            )}
            {statistics.lastFailureAt && (
              <div>
                <span className="text-gray-600">Last Failure:</span>
                <span className="ml-2 font-medium text-red-600">
                  {new Date(statistics.lastFailureAt).toLocaleString()}
                </span>
              </div>
            )}
            {statistics.nextRunAt && (
              <div>
                <span className="text-gray-600">Next Run:</span>
                <span className="ml-2 font-medium text-blue-600">
                  {new Date(statistics.nextRunAt).toLocaleString()}
                </span>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default StatisticsCharts;