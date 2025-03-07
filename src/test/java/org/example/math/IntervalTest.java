package org.example.math;

import org.example.math.Interval.Bound;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class IntervalTest {

    @Nested
    class TestConstructorAndFactoryMethods {
        @Test
        void givenNullArgument_thenThrow() {
            assertThatNullPointerException().isThrownBy(() -> Interval.of(0, null));
            assertThatNullPointerException().isThrownBy(() -> Interval.of(null, 1));
            assertThatNullPointerException().isThrownBy(() -> Interval.of(null, (Integer) null));
            assertThatNullPointerException().isThrownBy(() -> Interval.of(null, (Bound<Integer>) null));
        }

        @Test
        void givenNullComparator_thenThrowNPE() {
            assertThatNullPointerException().isThrownBy(() -> new Interval<Integer>(Bound.of(0), Bound.of(1), null));
        }

        @Test
        void givenStartEqualEnd_thenDoNotThrow() {
            assertThatNoException().isThrownBy(() -> Interval.of(0, 0));
        }

        @Test
        void givenStartLessThanEnd_thenDoNotThrow() {
            assertThatNoException().isThrownBy(() -> Interval.of(0, 1));
        }

        @Test
        void givenUnboundedBelow_thenDoNotThrow() {
            assertThatNoException().isThrownBy(() -> Interval.of(Bound.unboundedBelow(), Bound.of(0)));
        }

        @Test
        void givenUnboundedAbove_thenDoNotThrow() {
            assertThatNoException().isThrownBy(() -> Interval.of(Bound.of(0), Bound.unboundedAbove()));
        }

        @Test
        void allContainsAllPossibleValues() {
            Interval<DayOfWeek> all = Interval.all();
            assertSoftly(softly -> {
                softly.assertThat(all.contains(DayOfWeek.MONDAY)).isTrue();
                softly.assertThat(all.contains(DayOfWeek.TUESDAY)).isTrue();
                softly.assertThat(all.contains(DayOfWeek.WEDNESDAY)).isTrue();
                softly.assertThat(all.contains(DayOfWeek.THURSDAY)).isTrue();
                softly.assertThat(all.contains(DayOfWeek.FRIDAY)).isTrue();
                softly.assertThat(all.contains(DayOfWeek.SATURDAY)).isTrue();
                softly.assertThat(all.contains(DayOfWeek.SUNDAY)).isTrue();
            });
        }

        @Test
        void emptyIntervalDoesNotContainAnyValues() {
            Interval<DayOfWeek> all = Interval.empty();
            assertSoftly(softly -> {
                softly.assertThat(all.contains(DayOfWeek.MONDAY)).isFalse();
                softly.assertThat(all.contains(DayOfWeek.TUESDAY)).isFalse();
                softly.assertThat(all.contains(DayOfWeek.WEDNESDAY)).isFalse();
                softly.assertThat(all.contains(DayOfWeek.THURSDAY)).isFalse();
                softly.assertThat(all.contains(DayOfWeek.FRIDAY)).isFalse();
                softly.assertThat(all.contains(DayOfWeek.SATURDAY)).isFalse();
                softly.assertThat(all.contains(DayOfWeek.SUNDAY)).isFalse();
            });
        }
    }

    @Nested
    class TestEquals {
        @SuppressWarnings("EqualsWithItself")
        @Test
        void equalsItself() {
            var interval = Interval.of(1, 3);

            assertThat(interval.equals(interval)).isTrue();
        }

        @SuppressWarnings("ConstantValue")
        @Test
        void notEqualToNull() {
            var interval = Interval.of(1, 3);

            assertThat(interval.equals(null)).isFalse();
        }

        @Test
        void equalsOtherInstance() {
            var interval1 = new Interval<>(1, 3, Comparator.naturalOrder());
            var interval2 = new Interval<>(1, 3, Comparator.naturalOrder());

            assertThat(interval1).isEqualTo(interval2);
        }

        @Test
        void emptyIntervalsEqualEachOther() {
            var empty1 = Interval.of(1, 1);
            var empty2 = Interval.of(2, 2);
            var empty3 = Interval.of(4, 3);

            assertThat(empty1).isEqualTo(empty2);
            assertThat(empty2).isEqualTo(empty1);

            assertThat(empty1).isEqualTo(empty3);
            assertThat(empty3).isEqualTo(empty1);

            assertThat(empty2).isEqualTo(empty3);
            assertThat(empty3).isEqualTo(empty2);
        }

        @Test
        void emptyIntervalsWithDifferentComparatorsEqualEachOther() {
            var empty1 = new Interval<>(1, 1, Comparator.naturalOrder());
            var empty2 = new Interval<>(4, 3, Comparator.nullsFirst(Comparator.naturalOrder()));

            assertThat(empty1).isEqualTo(empty2);
            assertThat(empty2).isEqualTo(empty1);
        }

        @Test
        void emptyIntervalNotEqualToNonEmptyInterval() {
            var empty1 = Interval.of(1, 1);
            var interval = Interval.of(1, 2);

            assertThat(empty1).isNotEqualTo(interval);
            assertThat(interval).isNotEqualTo(empty1);
        }

        @Test
        void notEqualToIntervalWithEqualBoundariesButDifferentComparator() {
            var interval1 = new Interval<>(1, 3, Comparator.naturalOrder());
            var interval2 = new Interval<>(1, 3, Comparator.nullsLast(Comparator.naturalOrder()));

            assertThat(interval1).isNotEqualTo(interval2);
            assertThat(interval2).isNotEqualTo(interval1);
        }
    }

    @Nested
    class SetOperations {
        @Nested
        class Contains {
            @ParameterizedTest(name = "{2} \\in [{0},{1}[")
            @CsvSource(textBlock = """
                                   # a, b, x, is contained
                                     1, 3, 0, false
                                     1, 3, 1,  true
                                     1, 3, 2,  true
                                     1, 3, 3, false
                                     1, 3, 4, false
                                   # empty intervals should contain nothing
                                     1, 1, 0, false
                                     1, 1, 1, false
                                     1, 1, 2, false
                                     2, 1, 0, false
                                     2, 1, 1, false
                                     2, 1, 2, false
                                     2, 1, 3, false
                                   """)
            void containsPoint(int start, int end, int x, boolean result) {
                final Interval<Integer> interval = Interval.of(start, end);
                assertThat(interval.contains(x)).isEqualTo(result);
            }

            @ParameterizedTest
            @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE})
            void allContainsEverything(int x) {
                final Interval<Integer> interval = Interval.all();
                assertThat(interval.contains(x)).isTrue();
            }

            @ParameterizedTest
            @CsvSource(textBlock = """
                                   # x, is contained
                                   -2147483648, true
                                            -1, true
                                             0, false
                                             1, false
                                    2147483647, false
                                   """)
            void leftHalfOpenContainsHalfOfEverything(int x, boolean result) {
                final Interval<Integer> interval = Interval.of(Bound.unboundedBelow(), Bound.of(0));
                assertThat(interval.contains(x)).isEqualTo(result);
            }

            @ParameterizedTest
            @CsvSource(textBlock = """
                                   # x, is contained
                                   -2147483648, false
                                            -1, false
                                             0, true
                                             1, true
                                    2147483647, true
                                   """)
            void rightHalfOpenContainsHalfOfEverything(int x, boolean result) {
                final Interval<Integer> interval = Interval.of(Bound.of(0), Bound.unboundedAbove());
                assertThat(interval.contains(x)).isEqualTo(result);
            }
        }

        @ParameterizedTest(name = "[{2},{3}[ \\subseteq [{0},{1}[ ")
        @CsvSource(textBlock = """
                               #a, b,  c, d, contains
                               # interval of length 2 never contains interval of length 3
                                1, 3, -3, 0, false
                                1, 3, -2, 1, false
                                1, 3, -1, 2, false
                                1, 3,  0, 3, false
                                1, 3,  1, 4, false
                                1, 3,  2, 5, false
                                1, 3,  3, 6, false
                                1, 3,  4, 7, false
                               # interval of length 2 contains in interval of length 2 iff equal
                                1, 3, -2, 0, false
                                1, 3, -1, 1, false
                                1, 3,  0, 2, false
                                1, 3,  1, 3,  true
                                1, 3,  2, 4, false
                                1, 3,  3, 5, false
                                1, 3,  4, 6, false
                               # interval of length 2 sometimes contains interval of length 1
                                1, 3, -1, 0, false
                                1, 3,  0, 1, false
                                1, 3,  1, 2,  true
                                1, 3,  2, 3,  true
                                1, 3,  3, 4, false
                                1, 3,  4, 5, false
                               # interval of length 2 always contains interval of length 0
                                1, 3,  0, 0,  true
                                1, 3,  1, 0,  true
                                1, 3,  1, 1,  true
                                1, 3,  2, 0,  true
                                1, 3,  2, 1,  true
                                1, 3,  2, 2,  true
                                1, 3,  3, 0,  true
                                1, 3,  3, 1,  true
                                1, 3,  3, 2,  true
                                1, 3,  3, 3,  true
                                1, 3,  4, 0,  true
                                1, 3,  4, 1,  true
                                1, 3,  4, 2,  true
                                1, 3,  4, 3,  true
                                1, 3,  4, 4,  true
                               # interval of length 0 only contains interval of length 0, but no others
                                1, 1,  0, 0,  true
                                1, 1,  0, 1,  false
                                1, 1,  0, 2,  false
                                1, 1,  1, 0,  true
                                1, 1,  1, 1,  true
                                1, 1,  1, 2,  false
                                1, 1,  2, 0,  true
                                1, 1,  2, 1,  true
                                1, 1,  2, 2,  true
                                3, 1,  0, 0,  true
                                3, 1,  0, 1,  false
                                3, 1,  0, 2,  false
                                3, 1,  1, 0,  true
                                3, 1,  1, 1,  true
                                3, 1,  1, 2,  false
                                3, 1,  2, 0,  true
                                3, 1,  2, 1,  true
                                3, 1,  2, 2,  true
                                3, 1,  2, 3,  false
                                3, 1,  3, 0,  true
                                3, 1,  3, 1,  true
                                3, 1,  3, 2,  true
                                3, 1,  3, 3,  true
                                3, 1,  3, 4,  false
                               """)
        void contains(int start, int end, int c, int d, boolean result) {
            assertThat(Interval.of(start, end).contains(Interval.of(c, d))).isEqualTo(result);
        }

        @Nested
        class IntersectionAndClamping {

            @ParameterizedTest(name = "[1,3[ \\cap [{0},{1}[")
            @CsvSource(textBlock = """
                                   # [1,3[ \\cap [a,b[ = [c,d[
                                   # with empty string marking empty intervals
                                   #
                                   # a, b, c, d
                                   # interval of length 3
                                    -3, 0,  ,
                                    -2, 1,  ,
                                    -1, 2, 1, 2
                                     0, 3, 1, 3
                                     1, 4, 1, 3
                                     2, 5, 2, 3
                                     3, 6,  ,
                                     4, 7,  ,
                                   # interval of length 2
                                    -2, 0,  ,
                                    -1, 1,  ,
                                     0, 2, 1, 2
                                     1, 3, 1, 3
                                     2, 4, 2, 3
                                     3, 5,  ,
                                     4, 6,  ,
                                   # interval of length 1
                                    -1, 0,  ,
                                     0, 1,  ,
                                     1, 2, 1, 2
                                     2, 3, 2, 3
                                     3, 4,  ,
                                     4, 5,  ,
                                   # interval of length 0
                                     0, 0,  ,
                                     1, 1,  ,
                                     2, 2,  ,
                                     3, 3,  ,
                                     4, 4,  ,
                                   """)
            void intersection(int a, int b, Integer x, Integer y) {
                var intersection = Interval.of(1, 3).intersection(Interval.of(a, b));

                if (x != null && y != null) {
                    Interval<Integer> expected = Interval.of(x, y);
                    assertThat(intersection.isEmpty()).isFalse();
                    assertThat(intersection).isEqualTo(expected);
                } else {
                    assertThat(intersection.isEmpty()).isTrue();
                }
            }

            @ParameterizedTest(name = "[1,3[ \\cap [{0},+infinity[ = [{1},3[")
            @CsvSource(textBlock = """
                                   # with empty string marking empty intervals
                                   #
                                   # a, x
                                     0, 1
                                     1, 1
                                     2, 2
                                     3,
                                     4,
                                   """)
            void clampStart(int a, Integer x) {
                var clamped = Interval.of(1, 3).intersection(Interval.of(Bound.of(a), Bound.unboundedAbove()));
                var intersection = Interval.of(1, 3).clampStart(a);

                if (x != null) {
                    Interval<Integer> expected = Interval.of(x, 3);

                    assertThat(intersection.isEmpty()).isFalse();
                    assertThat(intersection).isEqualTo(expected);
                    assertThat(clamped).isEqualTo(expected);
                } else {
                    assertThat(intersection.isEmpty()).isTrue();
                }
            }

            @ParameterizedTest(name = "[1,3[ \\cap [+infinity,{0}[ = [1,{1}[")
            @CsvSource(textBlock = """
                                   # with empty string marking empty intervals
                                   #
                                   # b, x
                                     0,
                                     1,
                                     2, 2
                                     3, 3
                                     4, 3
                                   """)
            void clampEnd(int b, Integer x) {
                var clamped = Interval.of(1, 3).intersection(Interval.of(Bound.unboundedBelow(), Bound.of(b)));
                var intersection = Interval.of(1, 3).clampEnd(b);

                if (x != null) {
                    Interval<Integer> expected = Interval.of(1, x);

                    assertThat(intersection.isEmpty()).isFalse();
                    assertThat(intersection).isEqualTo(expected);
                    assertThat(clamped).isEqualTo(expected);
                } else {
                    assertThat(intersection.isEmpty()).isTrue();
                }
            }

            @Test
            void clampBoth() {
                assertThat(Interval.of(0, 4).clampStart(1).clampEnd(3)).isEqualTo(Interval.of(1, 3));
                assertThat(Interval.<Integer>all().clampStart(1).clampEnd(3)).isEqualTo(Interval.of(1, 3));
            }
        }

        @ParameterizedTest(name = "[1,3[ \\cap [{0},{1}[")
        @CsvSource(textBlock = """
                               # [1,3[ \\cap [a,b[ = [c,d[
                               #
                               # a, b, non-empty
                               # interval of length 3
                                -3, 0, false
                                -2, 1, false
                                -1, 2,  true
                                 0, 3,  true
                                 1, 4,  true
                                 2, 5,  true
                                 3, 6, false
                                 4, 7, false
                               # interval of length 2
                                -2, 0, false
                                -1, 1, false
                                 0, 2,  true
                                 1, 3,  true
                                 2, 4,  true
                                 3, 5, false
                                 4, 6, false
                               # interval of length 1
                                -1, 0, false
                                 0, 1, false
                                 1, 2,  true
                                 2, 3,  true
                                 3, 4, false
                                 4, 5, false
                               # interval of length 0
                                 0, 0, false
                                 1, 1, false
                                 2, 2, false
                                 3, 3, false
                                 4, 4, false
                               """)
        void overlaps(int a, int b, boolean result) {
            assertThat(Interval.of(1, 3).overlaps(Interval.of(a, b))).isEqualTo(result);
        }

        @ParameterizedTest(name = "[1,4[ \\ [{0},{1}[")
        @CsvSource(textBlock = """
                               # [1,4[ \\ [a,b[ = [a_1,b_1[ \\cup [a_2,b_2[
                               # empty intervals are marked with empty string in the text block and will be
                               # converted to null by JUnit.
                               #
                               # [a,b[  [a_1, b_1[  [a_2, b_2[
                                -4, 0,     ,     ,    1,    4
                                -3, 1,     ,     ,    1,    4
                                -2, 2,     ,     ,    2,    4
                                -1, 3,     ,     ,    3,    4
                                 0, 4,     ,     ,     ,
                                 1, 5,     ,     ,     ,
                                 2, 6,    1,    2,     ,
                                 3, 7,    1,    3,     ,
                                 4, 8,    1,    4,     ,
                                 5, 9,    1,    4,     ,
                                -3, 0,     ,     ,    1,    4
                                -2, 1,     ,     ,    1,    4
                                -1, 2,     ,     ,    2,    4
                                 0, 3,     ,     ,    3,    4
                                 1, 4,     ,     ,     ,
                                 2, 5,    1,    2,     ,
                                 3, 6,    1,    3,     ,
                                 4, 7,    1,    4,     ,
                                 5, 8,    1,    4,     ,
                                -2, 0,     ,     ,    1,    4
                                -1, 1,     ,     ,    1,    4
                                 0, 2,     ,     ,    2,    4
                                 1, 3,     ,     ,    3,    4
                                 2, 4,    1,    2,     ,
                                 3, 5,    1,    3,     ,
                                 4, 6,    1,    4,     ,
                                 5, 7,    1,    4,     ,
                                -1, 0,     ,     ,    1,    4
                                 0, 1,     ,     ,    1,    4
                                 1, 2,     ,     ,    2,    4
                                 2, 3,    1,    2,    3,    4
                                 3, 4,    1,    3,     ,
                                 4, 5,    1,    4,     ,
                                 5, 6,    1,    4,     ,
                               """)
        void difference(int a, int b, Integer a1, Integer b1, Integer a2, Integer b2) {

            final var expected = new ArrayList<>();
            if (a1 != null && b1 != null) {
                expected.add(Interval.of(a1, b1));
            }
            if (a2 != null && b2 != null) {
                expected.add(Interval.of(a2, b2));
            }

            assertThat(Interval.of(1, 4).difference(Interval.of(a, b))).isEqualTo(expected);
        }
    }

    @Nested
    class ToString {
        @Test
        void bounded() {
            assertThat(Interval.of(1, 3)).hasToString("[1,3[");
        }

        @Test
        void bothUnbounded() {
            assertThat(Interval.all()).hasToString("[-∞,+∞[");
        }

        @Test
        void unboundedBelow() {
            assertThat(Interval.of(Bound.unboundedBelow(), Bound.of(-1))).hasToString("[-∞,-1[");
        }

        @Test
        void unboundedAbove() {
            assertThat(Interval.of(Bound.of(1), Bound.unboundedAbove())).hasToString("[1,+∞[");
        }
    }
}