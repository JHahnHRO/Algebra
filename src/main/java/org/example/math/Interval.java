package org.example.math;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a (possibly infinite) half-open interval, i.e. a set of the form
 * <pre>{@code
 * [a,b[ := {x | a <= x < b }
 * }</pre>
 * where the order is defined by a comparator.
 * <p>
 * An interval {@code [a,b[} is {@link #isEmpty() empty} iff {@code a >= b} (according to the comparator). Any two empty
 * intervals are always considered {@link #equals(Object) equal}. Note that that means that
 * {@code i1.equals(i2) && !i1.start().equals(i2.start())} can be {@code true}! However, two non-empty Intervals are
 * equal precisely iff their comparators, start, and end are equal.
 *
 * @param start      the lower bound of this interval. It is contained in the interval.
 * @param end        the upper bound of this interval. It is not contained in the interval.
 * @param comparator the comparator that defines the order.
 * @param <T>        the type of elements.
 * @apiNote All methods and constructors throw {@link NullPointerException} if any of the arguments is {@code null}
 * unless otherwise stated.
 */
public record Interval<T>(Bound<T> start, Bound<T> end, Comparator<? super T> comparator) {

    private static final Interval<Comparable<Object>> EMPTY = new Interval<Comparable<Object>>(Bound.unboundedAbove(),
            Bound.unboundedBelow(), Comparator.naturalOrder());
    private static final Interval<Comparable<Object>> ALL = new Interval<Comparable<Object>>(Bound.unboundedBelow(),
            Bound.unboundedAbove(), Comparator.naturalOrder());

    /**
     * @param start      the lower bound of this interval. It is contained in the interval.
     * @param end        the upper bound of this interval. It is not contained in the interval.
     * @param comparator the comparator that defines the order.
     */
    public Interval {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
        Objects.requireNonNull(comparator);
    }

    /**
     * Constructs an interval with finite bounds.
     *
     * @param start      the lower bound of this interval. It is contained in the interval.
     * @param end        the upper bound of this interval. It is not contained in the interval.
     * @param comparator the comparator that defines the order.
     */
    public Interval(T start, T end, Comparator<? super T> comparator) {
        this(Bound.of(start), Bound.of(end), comparator);
    }

    /**
     * Convenience factory method that constructs an interval w.r.t. the
     * {@link Comparator#naturalOrder() natural order}.
     *
     * @param start the lower bound of the interval.
     * @param end   the upper bound of the interval.
     * @param <T>   the type of elements.
     * @return the interval with the given bounds.
     */
    public static <T extends Comparable<? super T>> Interval<T> of(T start, T end) {
        return new Interval<>(start, end, Comparator.naturalOrder());
    }

    /**
     * Convenience factory method that constructs an interval w.r.t. the
     * {@link Comparator#naturalOrder() natural order}.
     *
     * @param start the lower bound of the interval.
     * @param end   the upper bound of the interval.
     * @param <T>   the type of elements.
     * @return the interval with the given bounds.
     */
    public static <T extends Comparable<? super T>> Interval<T> of(Bound<T> start, Bound<T> end) {
        return new Interval<>(start, end, Comparator.naturalOrder());
    }

    /**
     * @param <T> the type of elements.
     *
     * @return the empty interval that does not {@link #contains(Object) contain} any values w.r.t. the natural order.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> Interval<T> empty() {
        return (Interval<T>) EMPTY;
    }

    /**
     * @param <T> the type of elements.
     *
     * @return The all-encompassing interval that {@link #contains(Object) contains} all instances of {@code T} w.r.t.
     * the natural order.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<? super T>> Interval<T> all() {
        return (Interval<T>) ALL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Interval<?> that)) {
            return false;
        }
        boolean bothEmpty = this.isEmpty() && that.isEmpty();
        // Mathematically speaking, it is questionable whether two empty sets are really equal if they have
        // non-compatible element types. The old-fashioned point of view (like ZFC and similar single-typed set
        // theories) would say this doesn't matter; there is one and only one empty set. A more modern point of view
        // would take element types into account and have one empty set for each type. This difference of opinions
        // usually does not matter, because there is already something very, very wrong somewhere if one is even
        // tempted to compare sets of strings with sets of integers. The single-typed theories allow these kinds of
        // questions, but they are never ever useful in any real argument. The typed theories simply make the
        // nonsensical questions truly invalid.
        //
        // However, in the Java ecosystem there is type erasure, and there can be only one answer: All empty sets must
        // be equal. Of course this class also cannot escape type erasure, but we could at least check the comparator if
        // we chose to.
        // Although this class does not implement the Set interface, we choose to be consistent with the
        // collections framework and return true if both Intervals are equal even if their comparators are different.
        return bothEmpty || (this.comparator.equals(that.comparator) && this.end.equals(that.end) && this.start.equals(
                that.start));
    }

    @Override
    public int hashCode() {
        return isEmpty() ? 0 : Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return "[" + start + ',' + end + '[';
    }

    /**
     * @return {@code true} iff {@code this.start} is equal (according to the comparator) to {@code this.end}
     */
    public boolean isEmpty() {
        return lte(end, start);
    }

    /**
     * @return {@code true} iff {@code t} is contained in this Interval.
     */
    public boolean contains(T t) {
        var b = Bound.of(t);
        return lte(start, b) && lt(b, end);
    }

    /**
     * @return {@code true} iff {@code that} is a subset of this Interval.
     */
    public boolean contains(Interval<T> that) {
        return (lte(this.start, that.start) && lte(that.end, this.end)) || that.isEmpty();
    }

    /**
     * @return the intersection of {@code this} and {@code that}, i.e. the interval that contains everything which is
     * contained in both of them. Note that this may be {@link #isEmpty() empty}.
     */
    public Interval<T> intersection(Interval<T> that) {
        var a = max(this.start, that.start);
        var b = min(this.end, that.end);

        return new Interval<>(a, b, this.comparator);
    }

    /**
     * @return {@code true} iff {@code !this.intersection(that).isEmpty()}
     */
    public boolean overlaps(Interval<T> that) {
        return !this.intersection(that).isEmpty();
    }

    /**
     * Returns the set-difference {@code this \ that}, i.e. the set of all elements that are contained in {@code this},
     * but not contained in {@code that}. The difference is a disjoint union of either 0,1, or 2 intervals.
     *
     * @param that another {@code Interval}.
     * @return a list of either 0,1, or 2 disjoint intervals whose union is exactly the set-theoretic difference between
     * {@code this} and {@code that}.
     */
    public List<Interval<T>> difference(Interval<T> that) {
        Interval<T> intersection = this.intersection(that);

        if (!intersection.isEmpty()) {
            boolean leftNonEmpty = lt(this.start, intersection.start);
            boolean rightNonEmpty = lt(intersection.end, this.end);
            if (leftNonEmpty && rightNonEmpty) {
                return List.of(
                        new Interval<>(this.start, intersection.start, comparator),
                        new Interval<>(intersection.end, this.end, comparator));
            }
            if (leftNonEmpty) {
                return List.of(new Interval<>(this.start, intersection.start, comparator));
            }
            if (rightNonEmpty) {
                return List.of(new Interval<>(intersection.end, this.end, comparator));
            }
            return List.of();
        } else {
            return List.of(this);
        }
    }

    /**
     * @param x the allowed minimum
     * @return an interval that starts with {@code x} if {@code this.start < x} and is otherwise unchanged, i.e. the
     * {@link #intersection(Interval) intersection} of {@code this} and {@code [x,+infinity[}.
     */
    public Interval<T> clampStart(T x) {
        return new Interval<>(max(this.start, Bound.of(x)), end, comparator);
    }

    /**
     * @param y the allowed maximum
     * @return an interval that ends with {@code y} if {@code y < this.end} and is otherwise unchanged, i.e. the
     * {@link #intersection(Interval) intersection} of {@code this} and {@code [-infinity,y[}.
     */
    public Interval<T> clampEnd(T y) {
        return new Interval<>(start, min(this.end, Bound.of(y)), comparator);
    }

    private Bound<T> min(Bound<T> a, Bound<T> b) {
        return lte(a, b) ? a : b;
    }

    private Bound<T> max(Bound<T> a, Bound<T> b) {
        return lt(a, b) ? b : a;
    }

    /**
     * @return {@code true} iff {@code a} is Less Than or Equal to {@code b}.
     */
    private boolean lte(Bound<T> a, Bound<T> b) {
        if (a == Infinity.NEGATIVE || b == Infinity.POSITIVE) {
            return true;
        }
        if (a == Infinity.POSITIVE || b == Infinity.NEGATIVE) {
            return false;
        }
        return comparator.compare(((Finite<T>) a).value, ((Finite<T>) b).value) <= 0;
    }

    /**
     * @return {@code true} iff {@code a} is strictly Less Than {@code b}.
     */
    private boolean lt(Bound<T> a, Bound<T> b) {
        if (a == Infinity.NEGATIVE) {
            return b != Infinity.NEGATIVE;
        }
        if (a == Infinity.POSITIVE || b == Infinity.NEGATIVE) {
            return false;
        }
        if (b == Infinity.POSITIVE) {
            return true;
        }
        return comparator.compare(((Finite<T>) a).value, ((Finite<T>) b).value) < 0;
    }

    public sealed interface Bound<T> {

        Optional<T> getValue();

        default boolean isInfinite() {
            return getValue().isEmpty();
        }

        static <T> Bound<T> of(T value) {
            return new Finite<>(value);
        }

        @SuppressWarnings("unchecked")
        static <T> Bound<T> unboundedBelow() {
            return (Bound<T>) Infinity.NEGATIVE;
        }

        @SuppressWarnings("unchecked")
        static <T> Bound<T> unboundedAbove() {
            return (Bound<T>) Infinity.POSITIVE;
        }

    }

    private record Finite<T>(T value) implements Bound<T> {
        public Finite {
            Objects.requireNonNull(value);
        }

        @Override
        public Optional<T> getValue() {
            return Optional.of(value);
        }

        @Override
        public String toString() {
            return value.toString();
        }
    }

    private enum Infinity implements Bound<Object> {
        NEGATIVE, POSITIVE;

        @Override
        public Optional<Object> getValue() {
            return Optional.empty();
        }

        @Override
        public String toString() {
            return this == NEGATIVE ? "-∞" : "+∞";
        }
    }
}
