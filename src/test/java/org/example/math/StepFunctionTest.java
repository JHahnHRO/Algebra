package org.example.math;

import static java.math.BigDecimal.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.example.math.StepFunction.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.function.BinaryOperator;

import org.example.math.Interval.Bound;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class StepFunctionTest {
    static final BinaryOperator<StepFunction<Integer, BigDecimal>> ADDITION = StepFunction.pointwise(BigDecimal::add);

    @Nested
    class TestConstants {

        @Test
        void constantZeroIsZero() {
            var zero = constant(ZERO);

            assertThat(zero.isConstant()).isTrue();
            assertThat(zero.isConstant(ZERO)).isTrue();
            assertThat(zero.values()).containsExactly(ZERO);
        }

        @Test
        void constantOneIsNotZero() {
            StepFunction<Integer, BigDecimal> zero = constant(ONE);

            assertThat(zero.isConstant()).isTrue();
            assertThat(zero.isConstant(ONE)).isTrue();
            assertThat(zero.values()).containsExactly(ONE);
        }

        @Test
        void simpleStepIsNotZero() {
            var f = singleStep(Interval.of(0, 1), BigDecimal.valueOf(42));

            assertThat(f.isConstant()).isFalse();
            assertThat(f.values()).containsExactly(ZERO, BigDecimal.valueOf(42), ZERO);
        }

        @Test
        void stepFunctionWithEmptyIntervalIsZero() {
            var f = singleStep(Interval.of(1, 0), BigDecimal.valueOf(42));

            assertThat(f.isConstant()).isTrue();
            assertThat(f.isConstant(ZERO)).isTrue();
            assertThat(f.values()).containsExactly(ZERO);
        }
    }

    @Nested
    class TestAddition {

        private final StepFunction<Integer, BigDecimal> f1 = singleStep(Interval.of(1, 5), BigDecimal.valueOf(42));
        private final StepFunction<Integer, BigDecimal> f2 = singleStep(Interval.of(3, 7), BigDecimal.valueOf(5));
        private final StepFunction<Integer, BigDecimal> f3 = singleStep(Interval.of(1, 4), BigDecimal.valueOf(-40));
        private final StepFunction<Integer, BigDecimal> f4 = singleStep(Interval.of(5, 7), BigDecimal.valueOf(-5));
        private final StepFunction<Integer, BigDecimal> f5 = singleStep(Interval.of(-3, -2), BigDecimal.valueOf(1));

        @Test
        void addZero() {
            var f = singleStep(Interval.of(0, 1), BigDecimal.valueOf(42));

            StepFunction<Integer, BigDecimal> zero = zero();
            assertThat(ADDITION.apply(f, zero)).as("f+0=f").isEqualTo(f);
            assertThat(ADDITION.apply(zero, f)).as("0+f=f").isEqualTo(f);
        }

        @Test
        void overlappingIntervals() {
            var f1plusf2 = ADDITION.apply(f1, f2);

            assertSoftly(softly -> {
                softly.assertThat(f1plusf2.values())
                      .containsExactly(ZERO, BigDecimal.valueOf(42), BigDecimal.valueOf(47), BigDecimal.valueOf(5),
                              ZERO);

                softly.assertThat(f1plusf2.apply(0)).as("(f_1+f_2)(0)= 0").isEqualTo(ZERO);
                softly.assertThat(f1plusf2.apply(1)).as("(f_1+f_2)(1)=42").isEqualTo(BigDecimal.valueOf(42));
                softly.assertThat(f1plusf2.apply(2)).as("(f_1+f_2)(2)=42").isEqualTo(BigDecimal.valueOf(42));
                softly.assertThat(f1plusf2.apply(3)).as("(f_1+f_2)(3)=47").isEqualTo(BigDecimal.valueOf(47));
                softly.assertThat(f1plusf2.apply(4)).as("(f_1+f_2)(4)=47").isEqualTo(BigDecimal.valueOf(47));
                softly.assertThat(f1plusf2.apply(5)).as("(f_1+f_2)(5)= 5").isEqualTo(BigDecimal.valueOf(5));
                softly.assertThat(f1plusf2.apply(6)).as("(f_1+f_2)(6)= 5").isEqualTo(BigDecimal.valueOf(5));
                softly.assertThat(f1plusf2.apply(7)).as("(f_1+f_2)(7)= 0").isEqualTo(ZERO);
                softly.assertThat(f1plusf2.apply(7)).as("(f_1+f_2)(8)= 0").isEqualTo(ZERO);
            });
        }

        @Test
        void intervalsWithSameStart() {
            var sumF1toF3 = ADDITION.apply(ADDITION.apply(f1, f2), f3);

            assertSoftly(softly -> {

                softly.assertThat(sumF1toF3.values())
                      .containsExactly(ZERO, TWO, BigDecimal.valueOf(7), BigDecimal.valueOf(47), BigDecimal.valueOf(5),
                              ZERO);

                softly.assertThat(sumF1toF3.apply(0)).as("(f_1+f_2+f_3)(0)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF3.apply(1)).as("(f_1+f_2+f_3)(1)= 2").isEqualTo(TWO);
                softly.assertThat(sumF1toF3.apply(2)).as("(f_1+f_2+f_3)(2)= 2").isEqualTo(TWO);
                softly.assertThat(sumF1toF3.apply(3)).as("(f_1+f_2+f_3)(3)= 7").isEqualTo(BigDecimal.valueOf(7));
                softly.assertThat(sumF1toF3.apply(4)).as("(f_1+f_2+f_3)(4)=47").isEqualTo(BigDecimal.valueOf(47));
                softly.assertThat(sumF1toF3.apply(5)).as("(f_1+f_2+f_3)(5)= 5").isEqualTo(BigDecimal.valueOf(5));
                softly.assertThat(sumF1toF3.apply(6)).as("(f_1+f_2+f_3)(6)= 5").isEqualTo(BigDecimal.valueOf(5));
                softly.assertThat(sumF1toF3.apply(7)).as("(f_1+f_2+f_3)(7)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF3.apply(7)).as("(f_1+f_2+f_3)(8)= 0").isEqualTo(ZERO);
            });
        }

        @Test
        void intervalsWithSameEnd() {
            StepFunction<Integer, BigDecimal> s1 = ADDITION.apply(f1, f2);
            StepFunction<Integer, BigDecimal> s3 = ADDITION.apply(s1, f3);
            var sumF1toF4 = ADDITION.apply(s3, f4);

            assertSoftly(softly -> {
                softly.assertThat(sumF1toF4.values())
                      .containsExactly(ZERO, TWO, BigDecimal.valueOf(7), BigDecimal.valueOf(47), ZERO);

                softly.assertThat(sumF1toF4.apply(0)).as("(f_1+..+f_4)(0)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF4.apply(1)).as("(f_1+..+f_4)(1)= 2").isEqualTo(TWO);
                softly.assertThat(sumF1toF4.apply(2)).as("(f_1+..+f_4)(2)= 2").isEqualTo(TWO);
                softly.assertThat(sumF1toF4.apply(3)).as("(f_1+..+f_4)(3)= 7").isEqualTo(BigDecimal.valueOf(7));
                softly.assertThat(sumF1toF4.apply(4)).as("(f_1+..+f_4)(4)=47").isEqualTo(BigDecimal.valueOf(47));
                softly.assertThat(sumF1toF4.apply(5)).as("(f_1+..+f_4)(5)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF4.apply(6)).as("(f_1+..+f_4)(6)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF4.apply(7)).as("(f_1+..+f_4)(7)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF4.apply(7)).as("(f_1+..+f_4)(8)= 0").isEqualTo(ZERO);
            });
        }

        @Test
        void disjointIntervals() {
            StepFunction<Integer, BigDecimal> s1 = ADDITION.apply(f1, f2);
            StepFunction<Integer, BigDecimal> s3 = ADDITION.apply(s1, f3);
            StepFunction<Integer, BigDecimal> s4 = ADDITION.apply(s3, f4);
            var sumF1toF5 = ADDITION.apply(s4, f5);
            assertSoftly(softly -> {
                softly.assertThat(sumF1toF5.values())
                      .containsExactly(ZERO, ONE, ZERO, TWO, BigDecimal.valueOf(7), BigDecimal.valueOf(47), ZERO);

                softly.assertThat(sumF1toF5.apply(-4)).as("(f_1+..+f_5)(-4)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF5.apply(-3)).as("(f_1+..+f_5)(-3)= 0").isEqualTo(ONE);
                softly.assertThat(sumF1toF5.apply(-2)).as("(f_1+..+f_5)(-2)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF5.apply(-1)).as("(f_1+..+f_5)(-1)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF5.apply(0)).as("(f_1+..+f_5)( 0)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF5.apply(1)).as("(f_1+..+f_5)( 1)= 2").isEqualTo(TWO);
                softly.assertThat(sumF1toF5.apply(2)).as("(f_1+..+f_5)( 2)= 2").isEqualTo(TWO);
                softly.assertThat(sumF1toF5.apply(3)).as("(f_1+..+f_5)( 3)= 7").isEqualTo(BigDecimal.valueOf(7));
                softly.assertThat(sumF1toF5.apply(4)).as("(f_1+..+f_5)( 4)=47").isEqualTo(BigDecimal.valueOf(47));
                softly.assertThat(sumF1toF5.apply(5)).as("(f_1+..+f_5)( 5)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF5.apply(6)).as("(f_1+..+f_5)( 6)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF5.apply(7)).as("(f_1+..+f_5)( 7)= 0").isEqualTo(ZERO);
                softly.assertThat(sumF1toF5.apply(7)).as("(f_1+..+f_5)( 8)= 0").isEqualTo(ZERO);
            });
        }

        @Test
        void negation() {
            var result = ADDITION.apply(f1, f1.andThen(BigDecimal::negate));

            assertThat(result.isConstant(ZERO)).isTrue();
        }
    }

    @Nested
    class TestSupport {

        @Test
        void zero() {
            var f = StepFunction.zero();

            assertThat(f.support(ZERO)).isEmpty();
        }

        @Test
        void singleStep() {
            final Interval<Integer> interval = Interval.of(1, 3);
            var f = StepFunction.singleStep(interval, ONE);

            assertThat(f.support(ZERO)).containsExactly(interval);
        }

        @Test
        void twoAdjacentSteps() {
            StepFunction<Integer, BigDecimal> s1 = StepFunction.singleStep(Interval.of(1, 2), ONE);
            StepFunction<Integer, BigDecimal> s2 = StepFunction.singleStep(Interval.of(2, 3), BigDecimal.TWO);
            var f = ADDITION.apply(s1, s2);

            assertThat(f.support(ZERO)).containsExactly(Interval.of(1, 3));
        }

        @Test
        void sumOfTwoOverlappingSteps() {
            StepFunction<Integer, BigDecimal> s1 = StepFunction.singleStep(Interval.of(1, 3), ONE);
            StepFunction<Integer, BigDecimal> s2 = StepFunction.singleStep(Interval.of(2, 4), BigDecimal.TWO);
            var f = ADDITION.apply(s1, s2);
            // f equal to singleStep([1,2[, 1) + singleStep([2,3[, 3) + singleStep([3,4[, 2]

            assertThat(f.support(ZERO)).containsExactly(Interval.of(1, 4));
        }

        @Test
        void twoNonAdjacentSteps() {
            StepFunction<Integer, BigDecimal> s1 = StepFunction.singleStep(Interval.of(1, 2), ONE);
            StepFunction<Integer, BigDecimal> s2 = StepFunction.singleStep(Interval.of(3, 4), BigDecimal.TWO);
            var f = StepFunction.<Integer, BigDecimal>pointwise(BigDecimal::add).apply(s1, s2);

            assertThat(f.support(ZERO)).containsExactly(Interval.of(1, 2), Interval.of(3, 4));
        }

        @Test
        void constant() {
            StepFunction<Instant, BigDecimal> f = StepFunction.constant(ONE);

            assertThat(f.support(ZERO)).containsExactly(Interval.all());
        }

        @Test
        void almostConstantExceptForOneBit() {
            final Interval<Integer> leftPiece = Interval.of(Bound.unboundedBelow(), Bound.of(0));
            final Interval<Integer> rightPiece = Interval.of(Bound.of(1), Bound.unboundedAbove());

            var f = ADDITION.apply(StepFunction.singleStep(leftPiece, ONE), StepFunction.singleStep(rightPiece, ONE));

            assertThat(f.support(ZERO)).containsExactly(leftPiece, rightPiece);
        }
    }

    @Nested
    class TestPartialFunctions {

        @Test
        void everywhereUndefined() {
            StepFunction<Integer, Integer> function = StepFunction.singleStep(Interval.<Integer>all(), null, null);

            assertThat(function.apply(Integer.MIN_VALUE)).isNull();
            assertThat(function.apply(0)).isNull();
            assertThat(function.apply(Integer.MAX_VALUE)).isNull();
        }

        @Test
        void singleStepWithNull() {
            var interval = Interval.of(1, 3);

            StepFunction<Integer, BigDecimal> function1 = singleStep(interval, TWO, null);
            assertSoftly(softly -> {
                softly.assertThat(function1.apply(Integer.MIN_VALUE)).isNull();
                softly.assertThat(function1.apply(0)).isNull();
                softly.assertThat(function1.apply(1)).isEqualTo(TWO);
                softly.assertThat(function1.apply(2)).isEqualTo(TWO);
                softly.assertThat(function1.apply(3)).isNull();
                softly.assertThat(function1.apply(Integer.MAX_VALUE)).isNull();
            });

            StepFunction<Integer, BigDecimal> function2 = singleStep(interval, null, TWO);
            assertSoftly(softly -> {
                softly.assertThat(function2.apply(Integer.MIN_VALUE)).isEqualTo(TWO);
                softly.assertThat(function2.apply(0)).isEqualTo(TWO);
                softly.assertThat(function2.apply(1)).isNull();
                softly.assertThat(function2.apply(2)).isNull();
                softly.assertThat(function2.apply(3)).isEqualTo(TWO);
                softly.assertThat(function2.apply(Integer.MAX_VALUE)).isEqualTo(TWO);
            });

        }

        @Test
        void nullsOutsideSupport() {
            var interval = Interval.of(1, 3);
            var function = singleStep(interval, ONE, null);

            assertThat(function.support(ZERO)).containsExactly(interval);
        }
    }

    @Nested
    class TestPartitionValues {

        @Test
        void zero() {
            StepFunction<Instant, BigDecimal> f = StepFunction.zero();

            assertThat(f.asPartitionWithValues()).containsExactly(Map.entry(Interval.all(), BigDecimal.ZERO));
        }

        @Test
        void constant() {
            StepFunction<Instant, BigDecimal> f = StepFunction.constant(ONE);

            assertThat(f.asPartitionWithValues()).containsExactly(Map.entry(Interval.all(), BigDecimal.ONE));
        }

        @Test
        void nonConstant() {
            final Interval<Integer> leftPiece = Interval.of(Bound.unboundedBelow(), Bound.of(0));
            final Interval<Integer> rightPiece = Interval.of(Bound.of(1), Bound.unboundedAbove());

            var f = ADDITION.apply(StepFunction.singleStep(leftPiece, ONE), StepFunction.singleStep(rightPiece, TWO));

            assertThat(f.asPartitionWithValues()).containsExactly(
                    Map.entry(Interval.of(Bound.unboundedBelow(), Bound.of(0)), BigDecimal.ONE),
                    Map.entry(Interval.of(0, 1), ZERO),
                    Map.entry(Interval.of(Bound.of(1), Bound.unboundedAbove()), TWO));
        }
    }
}