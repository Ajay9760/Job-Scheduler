import React, { useState } from 'react';

const RetryConfigPanel = ({ retryPolicy, onChange }) => {
  const [policy, setPolicy] = useState(retryPolicy || {
    maxRetries: 3,
    backoffStrategy: 'EXPONENTIAL',
    initialDelay: 1000
  });

  const handleChange = (field, value) => {
    const updated = { ...policy, [field]: value };
    setPolicy(updated);
    if (onChange) onChange(updated);
  };

  const calculateDelays = () => {
    const delays = [];
    for (let i = 1; i <= policy.maxRetries; i++) {
      let delay;
      switch (policy.backoffStrategy) {
        case 'EXPONENTIAL':
          delay = Math.pow(2, i - 1);
          break;
        case 'LINEAR':
          delay = i * 10;
          break;
        case 'FIXED':
        default:
          delay = 30;
      }
      delays.push(delay);
    }
    return delays;
  };

  const delays = calculateDelays();

  return (
    <div className="bg-white rounded-lg shadow-md p-6">
      <h3 className="text-lg font-semibold mb-4">Retry Configuration</h3>

      <div className="space-y-6">
        {/* Max Retries */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Maximum Retries
          </label>
          <input
            type="number"
            min="0"
            max="10"
            value={policy.maxRetries}
            onChange={(e) => handleChange('maxRetries', parseInt(e.target.value))}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          />
          <p className="mt-1 text-sm text-gray-500">
            Number of times to retry a failed job (0-10)
          </p>
        </div>

        {/* Backoff Strategy */}
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-2">
            Backoff Strategy
          </label>
          <select
            value={policy.backoffStrategy}
            onChange={(e) => handleChange('backoffStrategy', e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="EXPONENTIAL">Exponential (2^n seconds)</option>
            <option value="LINEAR">Linear (n × 10 seconds)</option>
            <option value="FIXED">Fixed (30 seconds)</option>
          </select>
          <p className="mt-1 text-sm text-gray-500">
            How delays between retries should increase
          </p>
        </div>

        {/* Visual Representation */}
        {policy.maxRetries > 0 && (
          <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
            <div className="text-sm font-semibold text-blue-800 mb-3">
              Retry Schedule Preview:
            </div>
            <div className="space-y-2">
              {delays.map((delay, index) => (
                <div key={index} className="flex items-center space-x-3">
                  <div className="flex-shrink-0 w-20 text-sm text-blue-700 font-medium">
                    Retry {index + 1}:
                  </div>
                  <div className="flex-1">
                    <div className="flex items-center space-x-2">
                      <div className="bg-blue-600 h-2 rounded-full" style={{ width: `${(delay / Math.max(...delays)) * 100}%`, minWidth: '20px' }} />
                      <span className="text-sm text-blue-800 font-mono">
                        {delay}s delay
                      </span>
                    </div>
                  </div>
                </div>
              ))}
            </div>
            <div className="mt-3 text-xs text-blue-700">
              Total max retry time: {delays.reduce((a, b) => a + b, 0)}s
            </div>
          </div>
        )}

        {/* Strategy Descriptions */}
        <div className="bg-gray-50 rounded-lg p-4">
          <h4 className="text-sm font-semibold text-gray-700 mb-2">Strategy Explanations:</h4>
          <div className="space-y-2 text-sm text-gray-600">
            <div>
              <strong className="text-gray-800">Exponential:</strong> Delay doubles with each retry
              (1s, 2s, 4s, 8s...). Best for transient errors.
            </div>
            <div>
              <strong className="text-gray-800">Linear:</strong> Delay increases by a fixed amount
              (10s, 20s, 30s...). Predictable timing.
            </div>
            <div>
              <strong className="text-gray-800">Fixed:</strong> Same delay for all retries
              (30s, 30s, 30s...). Simplest approach.
            </div>
          </div>
        </div>

        {/* Enable/Disable Retries */}
        <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
          <div>
            <div className="font-medium text-gray-800">Enable Automatic Retries</div>
            <div className="text-sm text-gray-600">
              {policy.maxRetries > 0
                ? `Jobs will retry up to ${policy.maxRetries} times on failure`
                : 'Retries are disabled'}
            </div>
          </div>
          <label className="relative inline-flex items-center cursor-pointer">
            <input
              type="checkbox"
              checked={policy.maxRetries > 0}
              onChange={(e) => handleChange('maxRetries', e.target.checked ? 3 : 0)}
              className="sr-only peer"
            />
            <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
          </label>
        </div>

        {/* Best Practices */}
        <div className="bg-yellow-50 border border-yellow-200 rounded-lg p-4">
          <div className="flex items-start space-x-2">
            <span className="text-yellow-600 text-lg">💡</span>
            <div className="text-sm text-yellow-800">
              <div className="font-semibold mb-1">Best Practices:</div>
              <ul className="list-disc list-inside space-y-1">
                <li>Use 3-5 retries for most jobs</li>
                <li>Exponential backoff is recommended for API calls</li>
                <li>Set max retries to 0 for non-idempotent operations</li>
                <li>Monitor retry statistics to tune settings</li>
              </ul>
            </div>
          </div>
        </div>

        {/* Test Retry Logic */}
        <button
          onClick={() => alert('Retry logic test - would simulate a failure and retry sequence')}
          className="w-full px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition"
        >
          🧪 Test Retry Logic
        </button>
      </div>
    </div>
  );
};

export default RetryConfigPanel;