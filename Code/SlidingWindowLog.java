import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class SlidingWindowLog implements RateLimiter {

  //The product team requires each user to be limited to 100 requests per minute.
  private static final int MAX_REQUESTS_LIMIT = 100;
  private static final int SETTING_TIME_IN_MINUTES = 1;

  //Constant
  private static final String CONST_TIMEZONE = "Asia/Kuala_Lumpur";

  //Returned message
  private static final String MSG_CHECKING_LIMIT = "UserID: %s under request: %s is allowed: %s%n";
  private static final String MSG_REMOVE_QUEUE_REQUESTS_STATUS = "Request that time " + SETTING_TIME_IN_MINUTES + " minute before - remove: %s%n";
  private static final String MSG_RETRY_AT = "UserID: %s retry after:: %s ms%n";

  //To get users, ConcurrentHashMap - handled access as per user
  private final Map<String, UserWindow> users = new ConcurrentHashMap<>();

  //Class UserWindow
  private static class UserWindow {

    //Using Queue (FIFO) - offer, peek, poll
    private final Queue<Long> requests = new ArrayDeque<>();
  }

  // RateLimiter must expose at minimum.
  // Returns true if the request is within the rate limit.
  @Override
  public boolean allowRequest(String userId) {

    //Get userId
    UserWindow userWindow = users.get(userId);
    if (userWindow == null) {
      userWindow = new UserWindow();
      users.put(userId, userWindow);
    }

    //Get current time millis
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of(CONST_TIMEZONE));
    long currentTimeInMs = now.toInstant().toEpochMilli();

    //synchronization, only one thread allowed by follow same userWindow
    synchronized (userWindow) {

      // Remove requests which in setting time (minute) before
      boolean isDeleteOldRequest = removeQueueRequests(userWindow.requests, currentTimeInMs);
      System.out.printf(MSG_REMOVE_QUEUE_REQUESTS_STATUS, isDeleteOldRequest);

      // Check maximum request limit
      if (userWindow.requests.size() >= MAX_REQUESTS_LIMIT) {
        return false;
      }

      // Add queue for current request
      userWindow.requests.offer(currentTimeInMs);

      return true;
    }
  }

  // Returns how many milliseconds
  // until the next request slot opens for the user.
  @Override
  public long getRetryAfterMs(String userId) {

    //Get userId
    UserWindow userWindow = users.get(userId);
    if (userWindow == null) {
      userWindow = new UserWindow();
      users.put(userId, userWindow);
    }

    //Get current time millis
    ZonedDateTime now = ZonedDateTime.now(ZoneId.of(CONST_TIMEZONE));
    long currentTimeInMs = now.toInstant().toEpochMilli();

    //synchronization, only one thread allowed by follow same userWindow
    synchronized (userWindow) {

      // Remove requests which in 1 minute before
      boolean isDeleteOldRequest = removeQueueRequests(userWindow.requests, currentTimeInMs);
      System.out.printf(MSG_REMOVE_QUEUE_REQUESTS_STATUS, isDeleteOldRequest);

      // If NOT more than maximum requests limit, returned ZERO Ms
      if (userWindow.requests.size() < MAX_REQUESTS_LIMIT) {
        return 0L;
      }

      // Get Oldest request
      Long oldestRequest = userWindow.requests.peek();

      if (oldestRequest == null) {
        return 0L;
      }

      // Calculate for retryAt
      long settingTimeInMs = SETTING_TIME_IN_MINUTES * 60L * 1000L;
      long retryAt = oldestRequest + settingTimeInMs;

      return Math.max(0, retryAt - currentTimeInMs);
    }
  }

  private boolean removeQueueRequests(Queue<Long> requests, long currentTimeInMs) {

    long settingTimeInMs = SETTING_TIME_IN_MINUTES * 60L * 1000L;
    long processingTime = currentTimeInMs - settingTimeInMs;

    if (!requests.isEmpty()) {
      while (!requests.isEmpty() && requests.peek() <= processingTime) {
        //remove queue for requests before
        requests.poll();
      }
      return true;

    } else {
      return false;
    }
  }

  //Testing
  public static void testingSlidingWindowLog(String userId, int requestCount) {
    RateLimiter limiter = new SlidingWindowLog();

    // requests 1 - 100 allowed
    for (int i = 0; i < requestCount; i++) {
      boolean isAllowed = limiter.allowRequest(userId);
      System.out.printf(MSG_CHECKING_LIMIT, userId, i + 1, isAllowed);

      if (!isAllowed) {
        // requests from 101 onwards only can retry at next request slot open
        System.out.printf(MSG_RETRY_AT, userId, limiter.getRetryAfterMs(userId));
      }
    }
  }
}

