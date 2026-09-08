public class TokenBucket implements RateLimiter {
  //Incomplete, todo...

  @Override
  public boolean allowRequest(String userId) {
    return false;
  }

  @Override
  public long getRetryAfterMs(String userId) {
    return 0L;
  }

  //Testing
  public static void testingTokenBucket(String userId, int requestCount) {
    System.out.println("Todo...");
  }
}
