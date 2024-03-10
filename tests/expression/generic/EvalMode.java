package expression.generic;

import base.function.TernaryOperator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.IntFunction;
import java.util.function.UnaryOperator;

public record EvalMode<T>(
        IntFunction<T> constant,
        Map<String, UnaryOperator<TernaryOperator<T>>> monadics,
        Map<String, BinaryOperator<TernaryOperator<T>>> dyadics
) {
    /* package-private */ static class Builder<T> {
        private final IntFunction<T> constant;
        private final Map<String, UnaryOperator<TernaryOperator<T>>> monadics = new HashMap<>();
        private final Map<String, BinaryOperator<TernaryOperator<T>>> dyadics = new HashMap<>();

        private Builder(final IntFunction<T> constant) {
            this.constant = constant;
        }

        public Builder<T> add(final String name, final UnaryOperator<T> unary) {
            monadics.put(
                    name,
                    expr -> (x, y, z) -> unary.apply(expr.apply(x, y, z))
            );
            return this;
        }

        public Builder<T> add(final String name, final BinaryOperator<T> binary) {
            dyadics.put(
                    name,
                    (expr1, expr2) -> (x, y, z) -> binary.apply(expr1.apply(x, y, z), expr2.apply(x, y, z))
            );
            return this;
        }

        public EvalMode<T> build() {
            return new EvalMode<>(constant, Collections.unmodifiableMap(monadics), Collections.unmodifiableMap(dyadics));
        }
    }
    public static <T> Builder<T> builder(final IntFunction<T> constant) {
        return new Builder<>(constant);
    }
}
