public interface RateLimiter {

  boolean allowRequest(String userId);

  long getRetryAfterMs(String userId);

}
