package base.function;

/**
 * Represents an operation upon three operands of the same type, producing a result
 * of the same type as the operands. This is a specialization of
 * {@link TriFunction} for the case where the operands and the result are all of
 * the same type.
 *
 * <p>This is a <a href="package-summary.html">functional interface</a>
 * whose functional method is {@link #apply(Object, Object, Object)}.
 *
 * @param <T> the type of the operands and result of the operator
 *
 * @see TriFunction
 * @see java.util.function.BinaryOperator
 * @see java.util.function.UnaryOperator
 *
 * @author Ivanov Timofey
 */
@FunctionalInterface
public interface TernaryOperator<T> extends TriFunction<T, T, T, T> {
}
