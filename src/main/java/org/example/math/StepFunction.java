package org.example.math;

import org.example.math.Interval.Bound;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.*;

import static org.example.math.Interval.Bound.unboundedAbove;
import static org.example.math.Interval.Bound.unboundedBelow;

/**
 * Represents a "step function", i.e. a function whose domain can be partitioned into a finite number of
 * {@link Interval}s such that the function is constant on each interval.
 * <p>
 * The {@link #apply(Object)} method throws {@code NullPointerException} on {@code null} inputs, but may have
 * {@code null} outputs (to represent partial functions).
 *
 * @param <X> the type of the domain.
 * @param <V> the type of the values.
 */
public final class StepFunction<X, V> implements Function<X, V> {

    // needed for equality testing, because Comparator.nullsFirst(..) does not override #equals()
    private final Comparator<? super X> xComparator;
    // invariants:
    // - all elements of the list are non-null
    // - all keys are non-null except the first one which represents -infinity
    // - the non-null keys are in ascending order w.r.t. xComparator
    private final List<Map.Entry<X, V>> entries;

    private StepFunction(List<Map.Entry<X, V>> entries, Comparator<? super X> xComparator) {
        this.xComparator = Objects.requireNonNull(xComparator);

        // stateful predicate to merge steps with equal values by filtering out intermediate entries
        Predicate<Map.Entry<X, V>> notEqualToPreviousValue = new Predicate<>() {
            Object last = new Object(); // sentinel value that definitely never occurs in the given values

            @Override
            public boolean test(Map.Entry<X, V> current) {
                if (!Objects.equals(last, current.getValue())) {
                    last = current.getValue();
                    return true;
                }
                return false;
            }
        };
        this.entries = entries.stream().filter(notEqualToPreviousValue).toList();
    }

    /**
     * @param <X> the type of the domain
     * @return the constant zero function
     * @see #constant(Object)
     */
    public static <X extends Comparable<? super X>> StepFunction<X, BigDecimal> zero() {
        return constant(BigDecimal.ZERO);
    }

    /**
     * @param <X> the type of the domain
     * @return the constant one function
     * @see #constant(Object)
     */
    public static <X extends Comparable<? super X>> StepFunction<X, BigDecimal> one() {
        return constant(BigDecimal.ONE);
    }

    /**
     * @param value the value of the function
     * @param <X>   the type of the domain
     * @return the constant function with the given value
     */
    public static <X extends Comparable<? super X>, V> StepFunction<X, V> constant(V value) {
        return constant(value, Comparator.naturalOrder());
    }

    /**
     * @param value       the value of the function
     * @param xComparator the order of the domain
     * @param <X>         the type of the domain
     * @return the constant function with the given value where the domain is ordered by the given comparator.
     */
    public static <X, V> StepFunction<X, V> constant(V value, Comparator<? super X> xComparator) {
        Objects.requireNonNull(xComparator);
        return new StepFunction<>(List.of(newEntry(null, value)), xComparator);
    }

    /**
     * @param interval an interval
     * @param value    a value
     * @param <X>      the type of the domain
     * @return a step function that has the value {@link BigDecimal#ZERO} outside of the given interval and
     * {@code value} within.
     */
    public static <X> StepFunction<X, BigDecimal> singleStep(Interval<X> interval, BigDecimal value) {
        return singleStep(interval, value, BigDecimal.ZERO);
    }

    /**
     * @param interval     an interval
     * @param valueInside  a value
     * @param valueOutside a value
     * @param <X>          the type of the domain
     * @return a step function that has the value {@code valueOutside} outside of the given interval and
     * {@code valueInside} within.
     */
    public static <X, V> StepFunction<X, V> singleStep(Interval<X> interval, V valueInside, V valueOutside) {
        Objects.requireNonNull(interval);

        // if the interval is empty, the returned function is a constant
        if (interval.isEmpty() || Objects.equals(valueInside, valueOutside)) {
            return constant(valueOutside, interval.comparator());
        }

        List<Map.Entry<X, V>> steps = new ArrayList<>(3);
        Optional<X> intervalBegin = interval.start().getValue();
        if (intervalBegin.isEmpty()) {
            // if the interval starts at -infinity, then the first step is from -infinity to the end of the interval
            // and has the inside-value
            steps.add(newEntry(null, valueInside));
        } else {
            // if the interval doesn't start at -infinity, the first step is from -infinity to the beginning of the
            // interval
            steps.add(newEntry(null, valueOutside));
            // second step: from the beginning to the end of the interval
            steps.add(newEntry(intervalBegin.get(), valueInside));
        }

        // if the interval doesn't end at +infinity we need another step after the interval with the outside-value
        interval.end().getValue().ifPresent(x -> steps.add(newEntry(x, valueOutside)));
        return new StepFunction<>(steps, interval.comparator());
    }

    @Override
    public V apply(X x) {
        Objects.requireNonNull(x);
        Comparator<Map.Entry<X, V>> comparator = Map.Entry.comparingByKey(new NullFirstComparator<>(xComparator));
        int i = Collections.binarySearch(entries, newEntry(x, null), comparator);
        int index = i >= 0 ? i : -i - 2;
        return entries.get(index).getValue();
    }

    /**
     * @return {@code true} iff this function is constant.
     */
    public boolean isConstant() {
        return entries.size() == 1;
    }

    /**
     * @param value a test value
     * @return {@code true} iff this function is constant equal to the given value.
     */
    public boolean isConstant(final V value) {
        return entries.equals(List.of(newEntry(null, value)));
    }

    /**
     * Returns the representation of this function as disjoint {@link Interval}s and values.
     *
     * @return a map {@link Interval} to this function's values
     */
    public SequencedMap<Interval<X>, V> asPartitionWithValues() {
        SequencedMap<Interval<X>, V> result = new LinkedHashMap<>();

        var iterator = entries.iterator();
        var currentEntry = iterator.next();
        do {
            Bound<X> lowerBound = currentEntry.getKey() == null ? unboundedBelow() : Bound.of(currentEntry.getKey());

            Bound<X> upperBound;
            Map.Entry<X, V> nextEntry;
            if (iterator.hasNext()) {
                nextEntry = iterator.next();
                upperBound = Bound.of(nextEntry.getKey());
            } else {
                nextEntry = null;
                upperBound = unboundedAbove();
            }
            result.putLast(new Interval<>(lowerBound, upperBound, xComparator), currentEntry.getValue());

            currentEntry = nextEntry;
        } while (currentEntry != null);

        return Collections.unmodifiableSequencedMap(result);
    }

    /**
     * @param partition a map defining intervals and values that the function should have on each of those intervals
     * @param <X>       the type of the domain
     * @param <V>       the type of the values
     * @return a StepFunction having the given values on the given intervals, undefined outside.
     * @throws NullPointerException     if {@code partition} is or contains {@code null} either as a key or as a value.
     * @throws IllegalArgumentException if the partition contains empty intervals, if it contains overlapping interval,
     *                                  or if the intervals do not all use the same comparator.
     */
    public static <X, V> StepFunction<X, V> fromPartitionWithValues(Map<Interval<X>, V> partition) {
        return fromPartitionWithValues(partition, null);
    }

    /**
     * @param partition    a map defining intervals and values that the function should have on each of those
     *                     intervals.
     * @param defaultValue the value the function takes outside the given intervals.
     * @param <X>          the type of the domain
     * @param <V>          the type of the values
     * @return a StepFunction having the given values on the given intervals, and {@code defaultValue} outside.
     * @throws NullPointerException     if {@code partition} is {@code null} or contains {@code null} as a key.
     * @throws IllegalArgumentException if the partition is empty, contains empty intervals, contains overlapping
     *                                  intervals, or if the intervals do not all use the same comparator.
     */
    public static <X, V> StepFunction<X, V> fromPartitionWithValues(Map<Interval<X>, V> partition, V defaultValue) {
        var iterator = partition.keySet().iterator();
        if (!iterator.hasNext()) {
            // It is tempting to return StepFunction.constant(null), the function that is undefined everywhere.
            // However, we cannot know which comparator to use in that case.
            throw new IllegalArgumentException("Partition is empty.");
        }

        Comparator<? super X> xComparator = iterator.next().comparator();

        // start with a StepFunction that is default everywhere
        List<Map.Entry<X, V>> values = List.of(newEntry(null, defaultValue));

        PartitionMerger<X, V, V, V> merger = new PartitionMerger<>((oldValue, newValue) -> {
            if (Objects.equals(oldValue, defaultValue)) {
                return newValue;
            } else if (Objects.equals(newValue, defaultValue)) {
                return oldValue;
            } else {
                throw new IllegalArgumentException("Intervals must not overlap");
            }
        });
        for (var entry : partition.entrySet()) {
            Interval<X> interval = entry.getKey();
            if (interval.isEmpty()) {
                throw new IllegalArgumentException("Intervals must not be empty.");
            }
            if (!xComparator.equals(interval.comparator())) {
                throw new IllegalArgumentException("Intervals must use compatible comparators");
            }
            V value = entry.getValue();
            List<Map.Entry<X, V>> additionalValues = StepFunction.singleStep(interval, value, defaultValue).entries;
            values = merger.merge(values, additionalValues, xComparator);
        }

        return new StepFunction<>(values, xComparator);
    }

    /**
     * @return the list of this function's values, ordered by the given order on the domain beginning with the value at
     * "-infinity" and ending with the value at "+infinity".
     */
    public List<V> values() {
        return entries.stream().map(Map.Entry::getValue).toList();
    }

    /**
     * Returns the subset of the domain where this step function has a value different from the given "zero" value,
     * expressed as a list of disjoint {@link Interval}s. The returned list is empty iff this function is {@code zero}
     * everywhere. The returned list contains the single, {@link Interval#all() all-encompassing interval} if this
     * function is non-{@code zero} everywhere.
     * <p>
     * Note that the support never contains intervals where this function does not have values, i.e. where
     * {@link #apply(Object)} would return {@code null}.
     *
     * @param zero the test value
     * @return the intervals where this function has a value different from the given zero value.
     */
    public List<Interval<X>> support(V zero) {
        Objects.requireNonNull(zero);
        return differentFrom(zero);
    }

    private List<Interval<X>> differentFrom(V v) {
        // We condense the function values down to a 3-valued Boolean: null if there is no value, true if the value
        // is v, false otherwise. The support is exactly the pre-image of false.
        StepFunction<X, Boolean> normalized = this.andThen(value -> value == null ? null : value.equals(v));
        return normalized.asPartitionWithValues().entrySet().stream()
                         // filter out intervals where this Function is either not defined or equal to v
                         .filter(entry -> entry.getValue() != null && !entry.getValue())
                         // then collect the remaining intervals in the domain
                         .map(Map.Entry::getKey).toList();
    }

    /**
     * Returns the domain of this function, i.e. the set of instances of {@code X} where this step function has a value,
     * expressed as a list of disjoint {@link Interval}s. The returned list is empty iff this function is {@code null}
     * everywhere. The returned list contains the single, {@link Interval#all() all-encompassing interval} if this
     * function is total, i.e. if it returns non-{@code null} for every input.
     *
     * @return the intervals where this function has a value.
     */
    public List<Interval<X>> domainOfDefinition() {
        return differentFrom(null);
    }

    @Override
    public <Y> StepFunction<X, Y> andThen(Function<? super V, ? extends Y> f) {
        Objects.requireNonNull(f);
        Function<V, Y> nullSafeF = v -> v == null ? null : f.apply(v);
        return new StepFunction<>(
                entries.stream().map(e -> newEntry(e.getKey(), nullSafeF.apply(e.getValue()))).toList(), xComparator);
    }


    @FunctionalInterface
    public interface EndoFunctor<X, Y> extends UnaryOperator<StepFunction<X, Y>>, Functor<X, Y, Y> {}

    /**
     * @param f   the operator
     * @param <X> the type of the domain
     * @param <Y> the type of the values
     * @return a {@link UnaryOperator} that, given a step-function {@code s}, returns the step-function that results
     * from applying the given operator to the values, i.e. the function {@code x->f(s(x))}.
     */
    public static <X, Y> EndoFunctor<X, Y> pointwise(UnaryOperator<Y> f) {
        return s -> s.andThen(f);
    }

    @FunctionalInterface
    public interface Functor<X, Y, Z> extends Function<StepFunction<X, Y>, StepFunction<X, Z>> {}

    /**
     * @param f   the operator
     * @param <X> the type of the domain
     * @param <Y> the type of the values
     * @return a {@link UnaryOperator} that, given a step-function {@code s}, returns the step-function that results
     * from applying the given operator to the values, i.e. the function {@code x->f(s(x))}.
     */
    public static <X, Y, Z> Functor<X, Y, Z> pointwise(Function<Y, Z> f) {
        return s -> s.andThen(f);
    }

    @FunctionalInterface
    public interface EndoBiFunctor<X, Y> extends BinaryOperator<StepFunction<X, Y>>, BiFunctor<X, Y, Y, Y> {}

    /**
     * @param f   the operator
     * @param <X> the type of the domain
     * @param <Y> the type of the values
     * @return a {@link BinaryOperator} that, given two step-functions {@code s1, s2}, returns the step-function that
     * results from applying the given operator to the values, i.e. the function {@code x->f(s1(x),s2(x))}.
     */
    public static <X, Y> EndoBiFunctor<X, Y> pointwise(BinaryOperator<Y> f) {
        final PartitionMerger<X, Y, Y, Y> partitionMerger = new PartitionMerger<>(f);
        return (s1, s2) -> new StepFunction<>(partitionMerger.merge(s1, s2), s1.xComparator);
    }

    @FunctionalInterface
    public interface BiFunctor<X, Y1, Y2, Z> extends BiFunction<StepFunction<X, Y1>, StepFunction<X, Y2>, StepFunction<X, Z>> {}

    /**
     * @param f    the operator
     * @param <X>  the type of the domain
     * @param <Y1> the type of the left values
     * @param <Y2> the type of the right values
     * @param <Z>  the type of the right values
     * @return a {@link BinaryOperator} that, given two step-functions {@code s1, s2}, returns the step-function that
     * results from applying the given operator to the values, i.e. the function {@code x->f(s1(x),s2(x))}.
     */
    public static <X, Y1, Y2, Z> BiFunctor<X, Y1, Y2, Z> pointwise(BiFunction<Y1, Y2, Z> f) {
        final PartitionMerger<X, Y1, Y2, Z> partitionMerger = new PartitionMerger<>(f);
        return (s1, s2) -> new StepFunction<>(partitionMerger.merge(s1, s2), s1.xComparator);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof StepFunction<?, ?> that && this.xComparator.equals(that.xComparator) && this.entries.equals(
                that.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xComparator, entries);
    }

    /**
     * Merges two step-functions by creating a common refinement of the two partitions and applying a BiFunction to both
     * values on each part.
     */
    private record PartitionMerger<X, Y1, Y2, Z>(BiFunction<Y1, Y2, Z> action) {

        List<Map.Entry<X, Z>> merge(StepFunction<X, Y1> a, StepFunction<X, Y2> b) {
            final Comparator<? super X> comparator = a.xComparator;
            if (!comparator.equals(b.xComparator)) {
                throw new IllegalArgumentException("Operation not applicable to incompatible step-functions");
            }
            return merge(a.entries, b.entries, comparator);
        }

        List<Map.Entry<X, Z>> merge(List<Map.Entry<X, Y1>> a, List<Map.Entry<X, Y2>> b,
                                    Comparator<? super X> comparator) {
            return new Iteration<>(a, b, new NullFirstComparator<>(comparator), action).result;
        }

        /**
         * Isolates a single iteration of the merger for thread-safety.
         */
        private static class Iteration<X, Y1, Y2, Z> {

            private final Iterator<Map.Entry<X, Y1>> aIter;
            private final Iterator<Map.Entry<X, Y2>> bIter;

            private Map.Entry<X, Y1> aEntry;
            private Map.Entry<X, Y2> bEntry;

            private final List<Map.Entry<X, Z>> result;

            Iteration(List<Map.Entry<X, Y1>> a, List<Map.Entry<X, Y2>> b, Comparator<? super X> comparator,
                      BiFunction<Y1, Y2, Z> action) {

                // we iterate in decreasing order through both maps
                aIter = a.reversed().iterator();
                bIter = b.reversed().iterator();

                // both maps are non-empty; they contain null as lowest key
                aEntry = aIter.next();
                bEntry = bIter.next();

                result = new ArrayList<>(a.size() + b.size());

                // zig-zag algorithm
                boolean moreToDo = true;
                while (moreToDo) {
                    Z currentValue = action.apply(aEntry.getValue(), bEntry.getValue());

                    int comp = comparator.compare(aEntry.getKey(), bEntry.getKey());
                    X currentKey;
                    if (comp < 0) {
                        // A < B => use B key & decrease B
                        currentKey = bEntry.getKey();
                        moreToDo = decreaseB();
                    } else if (comp == 0) {
                        // A == B => use any key & decrease both
                        //
                        // Note that both maps have null as their lowest key, so we will inevitably end the loop with
                        // this branch.
                        currentKey = aEntry.getKey();
                        boolean moreA = decreaseA();
                        boolean moreB = decreaseB();
                        moreToDo = moreA && moreB;
                    } else {
                        // B < A => use A key & decrease A
                        currentKey = aEntry.getKey();
                        moreToDo = decreaseA();
                    }
                    result.addFirst(newEntry(currentKey, currentValue));
                }
            }

            private boolean decreaseA() {
                if (aIter.hasNext()) {
                    aEntry = aIter.next();
                    return true;
                } else {
                    return false;
                }
            }

            private boolean decreaseB() {
                if (bIter.hasNext()) {
                    bEntry = bIter.next();
                    return true;
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * internal replacement for {@link Comparator#nullsFirst(Comparator)} to override equals.
     *
     * @param inner
     * @param <T>
     */
    private record NullFirstComparator<T>(Comparator<T> inner) implements Comparator<T> {

        @Override
        public int compare(T o1, T o2) {
            if (o1 == null) {
                return o2 == null ? 0 : -1;
            }
            if (o2 == null) {
                return 1;
            }
            return inner.compare(o1, o2);
        }

    }

    private static <K, Y> Map.Entry<K, Y> newEntry(K key, Y value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
