package expression.generic;

import base.function.TriFunction;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class ExprNode {
    private ExprNode() {
    }

    public abstract <R> R get(
            Function<Integer, R> constant,
            Function<String, R> variable,
            BiFunction<String, ExprNode, R> unary,
            TriFunction<String, ExprNode, ExprNode, R> binary
    );

    public abstract <R> R getRecursive(
            Function<Integer, R> constant,
            Function<String, R> variable,
            BiFunction<String, R, R> unary,
            TriFunction<String, R, R, R> binary
    );

    public static ExprNode constant(final int value) {
        return new ExprNode() {
            @Override
            public <R> R get(
                    final Function<Integer, R> constant,
                    final Function<String, R> variable,
                    final BiFunction<String, ExprNode, R> unary,
                    final TriFunction<String, ExprNode, ExprNode, R> binary
            ) {
                return constant.apply(value);
            }

            @Override
            public <R> R getRecursive(
                    final Function<Integer, R> constant,
                    final Function<String, R> variable,
                    final BiFunction<String, R, R> unary,
                    final TriFunction<String, R, R, R> binary
            ) {
                return constant.apply(value);
            }
        };
    }

    public static ExprNode variable(final String name) {
        return new ExprNode() {
            @Override
            public <R> R get(
                    final Function<Integer, R> constant,
                    final Function<String, R> variable,
                    final BiFunction<String, ExprNode, R> unary,
                    final TriFunction<String, ExprNode, ExprNode, R> binary
            ) {
                return variable.apply(name);
            }

            @Override
            public <R> R getRecursive(
                    final Function<Integer, R> constant,
                    final Function<String, R> variable,
                    final BiFunction<String, R, R> unary,
                    final TriFunction<String, R, R, R> binary
            ) {
                return variable.apply(name);
            }
        };
    }

    public static ExprNode unary(final String name, final ExprNode arg) {
        return new ExprNode() {
            @Override
            public <R> R get(
                    final Function<Integer, R> constant,
                    final Function<String, R> variable,
                    final BiFunction<String, ExprNode, R> unary,
                    final TriFunction<String, ExprNode, ExprNode, R> binary
            ) {
                return unary.apply(name, arg);
            }

            @Override
            public <R> R getRecursive(
                    final Function<Integer, R> constant,
                    final Function<String, R> variable,
                    final BiFunction<String, R, R> unary,
                    final TriFunction<String, R, R, R> binary
            ) {
                return unary.apply(name, arg.getRecursive(constant, variable, unary, binary));
            }
        };
    }

    public static ExprNode binary(final String name, final ExprNode arg1, final ExprNode arg2) {
        return new ExprNode() {
            @Override
            public <R> R get(
                    final Function<Integer, R> constant,
                    final Function<String, R> variable,
                    final BiFunction<String, ExprNode, R> unary,
                    final TriFunction<String, ExprNode, ExprNode, R> binary
            ) {
                return binary.apply(name, arg1, arg2);
            }

            @Override
            public <R> R getRecursive(
                    final Function<Integer, R> constant,
                    final Function<String, R> variable,
                    final BiFunction<String, R, R> unary,
                    final TriFunction<String, R, R, R> binary
            ) {
                return binary.apply(
                        name,
                        arg1.getRecursive(constant, variable, unary, binary),
                        arg2.getRecursive(constant, variable, unary, binary)
                );
            }
        };
    }
}
