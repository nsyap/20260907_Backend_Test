public class Testrun {

  public static void main(String[] args) {

    //Algorithm 1: Sliding Window Log
    String testUserId = "u123";
    int testCount = 103;
    SlidingWindowLog.testingSlidingWindowLog(testUserId, testCount);

    //Algorithm 2: Token Bucket
    String testUserId2 = "u456";
    int testCount2 = 105;
    TokenBucket.testingTokenBucket(testUserId2, testCount2);

  }
}
