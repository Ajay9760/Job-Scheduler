import React, { useState, useEffect } from 'react';
import axios from 'axios';

const WebhookManager = ({ jobId, currentWebhookUrl, onUpdate }) => {
  const [webhookUrl, setWebhookUrl] = useState(currentWebhookUrl || '');
  const [testing, setTesting] = useState(false);
  const [testResult, setTestResult] = useState(null);
  const [webhookHistory, setWebhookHistory] = useState([]);
  const [showPayloadPreview, setShowPayloadPreview] = useState(false);

  useEffect(() => {
    if (jobId) {
      fetchWebhookHistory();
    }
  }, [jobId]);

  const fetchWebhookHistory = async () => {
    // This would fetch webhook delivery history from backend
    // For now, mock data
    setWebhookHistory([]);
  };

  const testWebhook = async () => {
    if (!webhookUrl) {
      alert('Please enter a webhook URL');
      return;
    }

    setTesting(true);
    setTestResult(null);

    try {
      const response = await axios.post('/api/webhooks/test', {
        url: webhookUrl
      });

      setTestResult({
        success: true,
        message: 'Webhook test successful!',
        statusCode: response.status
      });
    } catch (error) {
      setTestResult({
        success: false,
        message: error.response?.data?.error || 'Webhook test failed',
        statusCode: error.response?.status
      });
    } finally {
      setTesting(false);
    }
  };

  const saveWebhook = async () => {
    try {
      await axios.put(`/api/jobs/${jobId}`, {
        webhookUrl: webhookUrl || null
      });

      alert('Webhook URL saved successfully!');
      if (onUpdate) onUpdate();
    } catch (error) {
      alert('Failed to save webhook URL: ' + error.response?.data?.error);
    }
  };

  const samplePayload = {
    event: "job.success",
    timestamp: "2024-12-02T10:30:00Z",
    job: {
      id: jobId,
      name: "Sample Job",
      description: "Sample job description"
    },
    instance: {
      id: 12345,
      status: "SUCCESS",
      scheduledTime: "2024-12-02T10:30:00Z",
      startedAt: "2024-12-02T10:30:01Z",
      completedAt: "2024-12-02T10:30:05Z",
      durationMs: 4000,
      httpStatusCode: 200,
      responseBody: '{"status": "ok"}',
      retryCount: 0
    }
  };

  return (
    <div className="space-y-6">
      {/* Webhook URL Configuration */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold mb-4">Webhook Configuration</h3>

        <div className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700 mb-2">
              Webhook URL
            </label>
            <input
              type="url"
              value={webhookUrl}
              onChange={(e) => setWebhookUrl(e.target.value)}
              placeholder="https://your-domain.com/webhook"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <p className="mt-2 text-sm text-gray-500">
              Enter a URL to receive notifications when job executions complete
            </p>
          </div>

          <div className="flex space-x-3">
            <button
              onClick={testWebhook}
              disabled={!webhookUrl || testing}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              {testing ? '⏳ Testing...' : '🧪 Test Webhook'}
            </button>
            <button
              onClick={saveWebhook}
              disabled={!webhookUrl}
              className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 disabled:opacity-50 disabled:cursor-not-allowed transition"
            >
              💾 Save
            </button>
            {webhookUrl && (
              <button
                onClick={() => {
                  setWebhookUrl('');
                  setTestResult(null);
                }}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition"
              >
                🗑️ Clear
              </button>
            )}
          </div>

          {/* Test Result */}
          {testResult && (
            <div className={`p-4 rounded-lg border ${
              testResult.success
                ? 'bg-green-50 border-green-200'
                : 'bg-red-50 border-red-200'
            }`}>
              <div className="flex items-center space-x-2">
                <span className="text-lg">{testResult.success ? '✅' : '❌'}</span>
                <span className={`font-medium ${
                  testResult.success ? 'text-green-800' : 'text-red-800'
                }`}>
                  {testResult.message}
                </span>
              </div>
              {testResult.statusCode && (
                <div className="mt-2 text-sm text-gray-600">
                  HTTP Status: {testResult.statusCode}
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Webhook Events */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold mb-4">Webhook Events</h3>
        <div className="space-y-3">
          <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
            <div>
              <div className="font-medium text-gray-800">job.success</div>
              <div className="text-sm text-gray-600">Triggered when a job completes successfully</div>
            </div>
            <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
              Active
            </span>
          </div>
          <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
            <div>
              <div className="font-medium text-gray-800">job.failed</div>
              <div className="text-sm text-gray-600">Triggered when a job execution fails</div>
            </div>
            <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
              Active
            </span>
          </div>
          <div className="flex items-center justify-between p-3 bg-gray-50 rounded">
            <div>
              <div className="font-medium text-gray-800">job.timeout</div>
              <div className="text-sm text-gray-600">Triggered when a job execution times out</div>
            </div>
            <span className="px-3 py-1 bg-green-100 text-green-800 rounded-full text-xs font-semibold">
              Active
            </span>
          </div>
        </div>
      </div>

      {/* Payload Preview */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-lg font-semibold">Payload Preview</h3>
          <button
            onClick={() => setShowPayloadPreview(!showPayloadPreview)}
            className="text-blue-600 hover:text-blue-800 font-medium text-sm"
          >
            {showPayloadPreview ? 'Hide' : 'Show'} Example
          </button>
        </div>

        {showPayloadPreview && (
          <div className="bg-gray-900 text-gray-100 rounded-lg p-4 overflow-x-auto">
            <pre className="text-sm font-mono">
              {JSON.stringify(samplePayload, null, 2)}
            </pre>
          </div>
        )}

        <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
          <div className="flex items-start space-x-2">
            <span className="text-blue-600 text-lg">ℹ️</span>
            <div className="text-sm text-blue-800">
              <div className="font-medium mb-1">Headers Included:</div>
              <ul className="list-disc list-inside space-y-1">
                <li><code className="bg-blue-100 px-2 py-0.5 rounded">Content-Type: application/json</code></li>
                <li><code className="bg-blue-100 px-2 py-0.5 rounded">User-Agent: Chronos-Job-Scheduler/1.0</code></li>
                <li><code className="bg-blue-100 px-2 py-0.5 rounded">X-Chronos-Event: job.success</code></li>
              </ul>
            </div>
          </div>
        </div>
      </div>

      {/* Webhook History */}
      <div className="bg-white rounded-lg shadow-md p-6">
        <h3 className="text-lg font-semibold mb-4">Delivery History</h3>
        {webhookHistory.length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            No webhook deliveries yet
          </div>
        ) : (
          <div className="space-y-2">
            {webhookHistory.map((delivery, index) => (
              <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded">
                <div>
                  <div className="font-medium text-gray-800">{delivery.event}</div>
                  <div className="text-sm text-gray-600">{delivery.timestamp}</div>
                </div>
                <span className={`px-3 py-1 rounded-full text-xs font-semibold ${
                  delivery.success
                    ? 'bg-green-100 text-green-800'
                    : 'bg-red-100 text-red-800'
                }`}>
                  {delivery.statusCode}
                </span>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};

export default WebhookManager;