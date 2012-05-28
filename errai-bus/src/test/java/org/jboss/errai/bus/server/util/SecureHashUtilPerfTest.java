package org.jboss.errai.bus.server.util;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Arrays;

import org.jboss.perfrunner.Axis;
import org.jboss.perfrunner.PerfRunner;
import org.jboss.perfrunner.Varying;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(PerfRunner.class)
public class SecureHashUtilPerfTest {

  private static final SecureRandom sha1prng;
  private static final SecureRandom yarrowprng;
  private static final SecureRandom nativeprng;

  static {
    try {
      for (Provider p : Security.getProviders()) {
        System.out.println("\n\n" + p.getName());
        p.list(System.out);
      }
      sha1prng = SecureRandom.getInstance("SHA1PRNG");
      yarrowprng = SecureRandom.getInstance("YarrowPRNG");
      nativeprng = SecureRandom.getInstance("NativePRNG");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testSHA1PRNG(@Varying(name="bytes", axis=Axis.X, from=1000, to=10000, step=100) int bytes) throws Exception {
    byte[] randomness = new byte[bytes];
    sha1prng.nextBytes(randomness);
    System.out.println(Arrays.toString(randomness));
  }

  @Test
  public void testYarrowPRNG(@Varying(name="bytes", axis=Axis.X, from=1000, to=10000, step=100) int bytes) throws Exception {
    byte[] randomness = new byte[bytes];
    yarrowprng.nextBytes(randomness);
    System.out.println(Arrays.toString(randomness));
  }

  @Test
  public void testNatibvePRNG(@Varying(name="bytes", axis=Axis.X, from=1000, to=10000, step=100) int bytes) throws Exception {
    byte[] randomness = new byte[bytes];
    nativeprng.nextBytes(randomness);
    System.out.println(Arrays.toString(randomness));
  }

  @Test
  public void testSecureHashUtil(@Varying(name="bytes", axis=Axis.X, from=1000, to=10000, step=100) int bytes) throws Exception {

    // my estimate is that we get 64 random bytes from every call
    int neededCalls = bytes / 64;

    for (int i = 0; i < neededCalls; i++) {
      System.out.println(SecureHashUtil.nextSecureHash());
    }
  }

}
