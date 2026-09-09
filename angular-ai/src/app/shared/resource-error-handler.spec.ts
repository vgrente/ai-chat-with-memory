import { HttpErrorResponse } from '@angular/common/http';
import { ResourceErrorHandler, RetryConfig } from './resource-error-handler';

const TEST_CONFIG: RetryConfig = {
  maxRetries: 3,
  initialDelay: 100,
  backoffMultiplier: 2,
  maxDelay: 1000
};

function httpError(status: number, message?: string): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: message ? { message } : undefined });
}

describe('ResourceErrorHandler', () => {
  let handler: ResourceErrorHandler;

  beforeEach(() => {
    handler = new ResourceErrorHandler(TEST_CONFIG);
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('handleError', () => {
    it('classifies a network error (status 0) as retryable', () => {
      handler.handleError(httpError(0));

      expect(handler.error()?.isRetryable).toBe(true);
      expect(handler.error()?.message).toBe('Network error. Please check your internet connection.');
    });

    it('classifies a 5xx error as retryable', () => {
      handler.handleError(httpError(503));

      expect(handler.error()?.isRetryable).toBe(true);
      expect(handler.error()?.message).toBe('Server error. Please try again later.');
    });

    it('classifies a 404 as not retryable', () => {
      handler.handleError(httpError(404));

      expect(handler.error()?.isRetryable).toBe(false);
      expect(handler.error()?.message).toBe('Resource not found.');
    });

    it('classifies a generic (non-HTTP) error as retryable', () => {
      handler.handleError(new Error('boom'));

      expect(handler.error()?.isRetryable).toBe(true);
      expect(handler.error()?.message).toBe('boom');
    });

    it('records the retry count at the time of the error', () => {
      handler.setRetryCount(2);

      handler.handleError(httpError(500));

      expect(handler.error()?.retryCount).toBe(2);
    });
  });

  describe('canRetry', () => {
    it('is false when there is no error', () => {
      expect(handler.canRetry).toBe(false);
    });

    it('is false when the error is not retryable', () => {
      handler.handleError(httpError(404));

      expect(handler.canRetry).toBe(false);
    });

    it('is false once maxRetries has been reached', () => {
      handler.handleError(httpError(500));
      handler.setRetryCount(TEST_CONFIG.maxRetries);

      expect(handler.canRetry).toBe(false);
    });

    it('is true for a retryable error under the retry limit', () => {
      handler.handleError(httpError(500));

      expect(handler.canRetry).toBe(true);
    });
  });

  describe('retry', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    it('invokes the operation after the configured initial delay', () => {
      handler.handleError(httpError(500));
      const operation = vi.fn();

      handler.retry(operation);
      expect(operation).not.toHaveBeenCalled();

      vi.advanceTimersByTime(TEST_CONFIG.initialDelay);
      expect(operation).toHaveBeenCalledTimes(1);
    });

    it('increments the retry count immediately, before the delay elapses', () => {
      handler.handleError(httpError(500));

      handler.retry(() => {});

      expect(handler.retryCount()).toBe(1);
    });

    it('backs off exponentially on successive retries', () => {
      handler.handleError(httpError(500));
      const operation = vi.fn();

      handler.retry(operation); // retryCount 0 -> 1, delay = 100ms
      vi.advanceTimersByTime(99);
      expect(operation).not.toHaveBeenCalled();
      vi.advanceTimersByTime(1);
      expect(operation).toHaveBeenCalledTimes(1);

      handler.retry(operation); // retryCount 1 -> 2, delay = 200ms
      vi.advanceTimersByTime(199);
      expect(operation).toHaveBeenCalledTimes(1);
      vi.advanceTimersByTime(1);
      expect(operation).toHaveBeenCalledTimes(2);
    });

    it('caps the delay at maxDelay', () => {
      // A generous maxRetries keeps canRetry true at a high retry count, so this
      // test can isolate the maxDelay cap from the maxRetries limit above.
      const uncappedRetries = new ResourceErrorHandler({ ...TEST_CONFIG, maxRetries: 10 });
      // initialDelay * backoffMultiplier^5 = 3200ms, which exceeds maxDelay (1000ms)
      uncappedRetries.setRetryCount(5);
      uncappedRetries.handleError(httpError(500));
      const operation = vi.fn();

      uncappedRetries.retry(operation);
      vi.advanceTimersByTime(TEST_CONFIG.maxDelay);

      expect(operation).toHaveBeenCalledTimes(1);
    });

    it('does nothing when canRetry is false', () => {
      handler.handleError(httpError(404)); // not retryable
      const operation = vi.fn();

      handler.retry(operation);
      vi.advanceTimersByTime(10_000);

      expect(operation).not.toHaveBeenCalled();
      expect(handler.retryCount()).toBe(0);
    });

    it('cancels a pending retry when called again before it fires', () => {
      handler.handleError(httpError(500));
      const firstOperation = vi.fn();
      const secondOperation = vi.fn();

      handler.retry(firstOperation);
      vi.advanceTimersByTime(50);
      handler.retry(secondOperation);
      vi.advanceTimersByTime(200);

      expect(firstOperation).not.toHaveBeenCalled();
      expect(secondOperation).toHaveBeenCalledTimes(1);
    });
  });

  describe('reset', () => {
    it('clears the error and retry count', () => {
      handler.handleError(httpError(500));
      handler.setRetryCount(2);

      handler.reset();

      expect(handler.error()).toBeNull();
      expect(handler.retryCount()).toBe(0);
    });

    it('cancels a pending retry so the operation never runs', () => {
      vi.useFakeTimers();
      handler.handleError(httpError(500));
      const operation = vi.fn();
      handler.retry(operation);

      handler.reset();
      vi.advanceTimersByTime(10_000);

      expect(operation).not.toHaveBeenCalled();
    });
  });
});
