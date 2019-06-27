package org.wso2.ballerinalang.compiler.util;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NumericLiteralSupportTest {

  @Test
  public void testStripDiscriminator() {
    Assert.assertEquals(NumericLiteralSupport.stripDiscriminator("3"), "3");
    Assert.assertEquals(NumericLiteralSupport.stripDiscriminator("abc"), "abc");
    Assert.assertEquals(NumericLiteralSupport.stripDiscriminator("abcf"), "abc");
    Assert.assertEquals(NumericLiteralSupport.stripDiscriminator("abcF"), "abc");
    Assert.assertEquals(NumericLiteralSupport.stripDiscriminator("abcd"), "abc");
    Assert.assertEquals(NumericLiteralSupport.stripDiscriminator("abcD"), "abc");
  }

  @Test
  public void testIsHexLiteral() {
    Assert.assertTrue(NumericLiteralSupport.isHexLiteral("p\u0000p"));
    Assert.assertTrue(NumericLiteralSupport.isHexLiteral("P\u0000P"));
    Assert.assertFalse(NumericLiteralSupport.isHexLiteral("1"));
  }

  @Test
  public void testParseBigDecimal() {
    Assert.assertEquals(NumericLiteralSupport.parseBigDecimal(1d).toString(),
        "1.0");
    Assert.assertEquals(NumericLiteralSupport.parseBigDecimal(1D).toString(),
        "1.0");
    Assert.assertEquals(NumericLiteralSupport.parseBigDecimal(1).toString(),
        "1");
  }

  @Test
  public void testIsDecimalDiscriminated() {
    Assert.assertTrue(NumericLiteralSupport.isDecimalDiscriminated("dddddddd"));
    Assert.assertTrue(NumericLiteralSupport.isDecimalDiscriminated("DDDDDDDD"));
    Assert.assertFalse(NumericLiteralSupport.isDecimalDiscriminated("Bar"));
    Assert.assertFalse(NumericLiteralSupport.isDecimalDiscriminated("3"));
  }

  @Test
  public void testIsFloatDiscriminated() {
    Assert.assertTrue(NumericLiteralSupport.isFloatDiscriminated("ffffffff"));
    Assert.assertTrue(NumericLiteralSupport.isFloatDiscriminated("FFFFFFFF"));
    Assert.assertFalse(NumericLiteralSupport.isFloatDiscriminated("Bar"));
    Assert.assertFalse(NumericLiteralSupport.isFloatDiscriminated("2"));
  }
}
