/*******************************************************************************
 * Copyright (c) 20178 Microsoft Research. All rights reserved.
 *
 * The MIT License (MIT)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
 * AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 * Contributors:
 *   Markus Alexander Kuppe - initial API and implementation
 ******************************************************************************/
package tlc2.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import tlc2.tool.EvalException;
import tlc2.value.Enumerable;
import tlc2.value.EnumerableValue;
import tlc2.value.IntValue;
import tlc2.value.IntervalValue;
import tlc2.value.SetEnumValue;
import tlc2.value.StringValue;
import tlc2.value.Value;

public class RandomizationTest {
	
	@BeforeClass
	public static void setup() {
		// Make test repeatable by setting random seed always to same value. 
		EnumerableValue.setRandom(15041980L);
	}

	/* RandomSubsetSetProbability */

	@Test
	public void testV1Valid() {
		final Enumerable randomSubset = (Enumerable) Randomization.RandomSubsetSetProbability(IntValue.gen(42), new StringValue("0.1"),
				new IntervalValue(1, 42));

		assertNotNull(randomSubset);
		assertEquals(42, randomSubset.size());
	}

	@Test
	public void testV1Negative() {
		final Value v1 = IntValue.gen(-42);
		try {
			Randomization.RandomSubsetSetProbability(v1, new StringValue("0.1"), new IntervalValue(1, 42));
		} catch (final EvalException ee) {
			assertTrue(ee.getMessage().contains("-42"));
			return;
		}
		fail();
	}

	@Test
	public void testV1Zero() {
		final Value v1 = IntValue.gen(0);
		final Enumerable randomSubset = (Enumerable) Randomization.RandomSubsetSetProbability(v1, new StringValue("0.1"),
				new IntervalValue(1, 42));

		assertNotNull(randomSubset);
		assertEquals(0, randomSubset.size());
	}
	
	@Test
	public void testV1NoIntValue() {
		final Value v1 = new StringValue("52");
		try {
			Randomization.RandomSubsetSetProbability(v1, new StringValue("0.1"), new IntervalValue(1, 42));
		} catch (final EvalException ee) {
			assertTrue(ee.getMessage().contains("\"52\""));
			return;
		}
		fail();
	}

	@Test
	public void testV2Zero() {
		final Enumerable randomSubset = (Enumerable) Randomization.RandomSubsetSetProbability(IntValue.gen(23), new StringValue("0"),
				new IntervalValue(1, 42));
		assertEquals(1, randomSubset.size());
		// empty set is only member
		assertTrue(randomSubset.member(new SetEnumValue()));
	}

	@Test
	public void testV2Negative() {
		try {
			Randomization.RandomSubsetSetProbability(IntValue.gen(23), new StringValue("-1"), new IntervalValue(1, 42));
		} catch (final EvalException ee) {
			assertTrue(ee.getMessage().contains("-1"));
			return;
		}
		fail();
	}

	@Test
	public void testV2Larger1() {
		try {
			Randomization.RandomSubsetSetProbability(IntValue.gen(23), new StringValue("1.1"), new IntervalValue(1, 42));
		} catch (final EvalException ee) {
			assertTrue(ee.getMessage().contains("1.1"));
			return;
		}
		fail();
	}
	
	@Test
	public void testV3Empty() {
		try {
			Randomization.RandomSubsetSetProbability(IntValue.gen(42), new StringValue("1E-1"), new SetEnumValue());
		} catch (final EvalException ee) {
			assertTrue(ee.getMessage().contains("2^0"));
			return;
		}
		fail();
	}
	
	@Test
	public void testV3AstronomicallyLarge() {
		final Enumerable randomSubset = (Enumerable) Randomization.RandomSubsetSetProbability(IntValue.gen(42), new StringValue("1E-1"),
				new IntervalValue(1, 256));

		assertNotNull(randomSubset);
		assertEquals(42, randomSubset.size());
	}
	
	@Test
	public void testV3isInfinite() {
		try {
			Randomization.RandomSubsetSetProbability(IntValue.gen(42), new StringValue("1E-1"), Naturals.Nat());
		} catch (final EvalException ee) {
			assertTrue(ee.getMessage().contains(
					"The third argument of RandomSubsetSetProbability should be a finite set, but instead it is:\nNat"));
			return;
		}
		fail();
	}
	
	/* RandomSubsetSet */

	@Test
	public void testRSSV2Zero() {
		final Enumerable randomSubset = (Enumerable) Randomization.RandomSubsetSet(IntValue.gen(23), IntValue.gen(0),
				new IntervalValue(1, 42));
		assertEquals(1, randomSubset.size());
		// empty set is only member
		assertTrue(randomSubset.member(new SetEnumValue()));
	}

	@Test
	public void testRSSV2Negative() {
		try {
			Randomization.RandomSubsetSetProbability(IntValue.gen(23), IntValue.gen(-1), new IntervalValue(1, 42));
		} catch (final EvalException ee) {
			assertTrue(ee.getMessage().contains("-1"));
			return;
		}
		fail();
	}
	
	@Test
	public void testRSSV2Cardinality() {
		final Enumerable randomSubset = (Enumerable) Randomization.RandomSubsetSet(IntValue.gen(32), IntValue.gen(5),
				new IntervalValue(1, 5));
		assertEquals(1, randomSubset.size());
		// With probability 1 (n = 5), the operator - due to collisions - only generates
		// a single subset which is the input set.
		assertTrue(randomSubset.member(new IntervalValue(1, 5)));
	}
	
	@Test
	public void testRSSV2TwiceCardinality() {
		try {
			Randomization.RandomSubsetSet(IntValue.gen(23), IntValue.gen(10), new IntervalValue(1, 5));
		} catch (final EvalException ee) {
			assertTrue(ee.getMessage().contains(
					"The second argument of RandomSubsetSetProbability should be a string literal does not represent a parsable probability, but instead it is:\n"
							+ "\"2.0\""));
			return;
		}
		fail();
	}
}