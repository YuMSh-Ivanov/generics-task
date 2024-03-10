package expression.generic;

import base.function.TernaryOperator;
import base.function.TriFunction;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runners.MethodSorters;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.IntStream;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GenericTabulatorTest {
    public static void main(String[] args) {
        final Result res = new JUnitCore().run(GenericTabulatorTest.class);
        for (final Failure fail : res.getFailures()) {
            System.err.println(fail.getTestHeader() + ": " + fail.getMessage());
        }
        if (!res.wasSuccessful()) {
            System.exit(1);
        }
    }

    private static final EvalMode<Integer> INTEGER = EvalMode
            .builder(x -> x)
            .add("-", x -> -x)
            .add("+", Integer::sum)
            .add("-", (x, y) -> x - y)
            .add("*", (x, y) -> x * y)
            .add("/", (x, y) -> x / y)
            .build();

    private static final EvalMode<BigInteger> BIG_INTEGER = EvalMode
            .builder(BigInteger::valueOf)
            .add("-", BigInteger::negate)
            .add("+", BigInteger::add)
            .add("-", BigInteger::subtract)
            .add("*", BigInteger::multiply)
            .add("/", BigInteger::divide)
            .build();

    private static final Map<String, EvalMode<?>> MODES = Map.of("i", INTEGER, "bi", BIG_INTEGER);

    private static String fullBraced(final ExprNode expression) {
        return expression.getRecursive(
                c -> new StringBuilder(c.toString()),
                StringBuilder::new,
                (s, sb) -> new StringBuilder(1 + s.length() + sb.length() + 1)
                        .append("(")
                        .append(s)
                        .append(sb)
                        .append(")"),
                (s, sb1, sb2) -> new StringBuilder(1 + sb1.length() + s.length() + sb2.length() + 1)
                        .append("(")
                        .append(sb1)
                        .append(s)
                        .append(sb2)
                        .append(")")
        ).toString();
    }

    private static <T> TriFunction<Integer, Integer, Integer, T> evaluator(final ExprNode expression, final EvalMode<T> mode) {
        return (a, b, c) -> expression.<TernaryOperator<T>>getRecursive(
                t -> (x, y, z) -> mode.constant().apply(t),
                name -> switch (name) {
                    case "x" -> (x, y, z) -> x;
                    case "y" -> (x, y, z) -> y;
                    case "z" -> (x, y, z) -> z;
                    default -> throw new IllegalArgumentException("Variable with name \"" + name + "\"");
                },
                (name, lambda) -> {
                    if (!mode.monadics().containsKey(name)) {
                        throw new IllegalArgumentException("Unary operation \"" + name + "\" is not supported. Supported ones are: " + mode.monadics());
                    } else {
                        return mode.monadics().get(name).apply(lambda);
                    }
                },
                (name, lambda1, lambda2) -> {
                    if (!mode.dyadics().containsKey(name)) {
                        throw new IllegalArgumentException("Binary operation \"" + name + "\" is not supported. Supported ones are: " + mode.dyadics());
                    } else {
                        return mode.dyadics().get(name).apply(lambda1, lambda2);
                    }
                }
        ).apply(mode.constant().apply(a), mode.constant().apply(b), mode.constant().apply(c));
    }

    private record RangeInclusive(int from, int to) implements Iterable<Integer> {
        public RangeInclusive {
            if (from > to) {
                throw new IllegalArgumentException("from [" + from + "] is greater than to [" + to + "]");
            }
        }

        public IntStream stream() {
            return IntStream.rangeClosed(from, to);
        }

        @Override
        public Iterator<Integer> iterator() {
            return new Iterator<>() {
                private int value = from;

                @Override
                public boolean hasNext() {
                    return value <= to;
                }

                @Override
                public Integer next() {
                    return value++;
                }
            };
        }
    }

    public void testValid(final ExprNode expression, final String modeName,
                          final RangeInclusive xRange, final RangeInclusive yRange, final RangeInclusive zRange) {
        final EvalMode<?> mode = MODES.get(modeName);

        final String repr = fullBraced(expression);
        final TriFunction<Integer, Integer, Integer, ?> evaluator = evaluator(expression, mode);
        final GenericTabulator tabulator = new GenericTabulator();
        final Object[][][] actual;
        try {
            actual = tabulator.tabulate(
                    modeName, repr,
                    xRange.from(), xRange.to(),
                    yRange.from(), yRange.to(),
                    zRange.from(), zRange.to()
            );
        } catch (final Exception e) {
            Assert.fail("Tabulation of \"" + repr + "\" ended with exception " + e);
            return;
        }
        final Object[][][] expected = xRange.stream().mapToObj(x ->
                        yRange.stream().mapToObj(y ->
                                        zRange.stream().mapToObj(z -> {
                                                    try {
                                                        return evaluator.apply(x, y, z);
                                                    } catch (final RuntimeException e) {
                                                        return null;
                                                    }
                                                })
                                                .toArray(Object[]::new))
                                .toArray(Object[][]::new))
                .toArray(Object[][][]::new);
        Assert.assertArrayEquals("Tabulation of \"" + repr + "\" in mode \"" + modeName + "\"", expected, actual);
    }

    private void testFixedRangesFixedModes(final ExprNode expression) {
        final RangeInclusive middleRange = new RangeInclusive(-5, 5);
        final RangeInclusive bottomRange = new RangeInclusive(Integer.MIN_VALUE, Integer.MIN_VALUE + 10);
        final RangeInclusive topRange = new RangeInclusive(Integer.MAX_VALUE - 10, Integer.MAX_VALUE);

        for (final String modeName : new String[]{"i", "bi"}) {
            testValid(expression, modeName, middleRange, middleRange, middleRange);
            testValid(expression, modeName, bottomRange, bottomRange, bottomRange);
            testValid(expression, modeName, topRange, topRange, topRange);

            testValid(expression, modeName, bottomRange, middleRange, topRange);
            testValid(expression, modeName, topRange, bottomRange, middleRange);
            testValid(expression, modeName, middleRange, topRange, bottomRange);
        }
    }

    @Test
    public void test1SimpleConstant() {
        testFixedRangesFixedModes(ExprNode.constant(0));
        testFixedRangesFixedModes(ExprNode.constant(145));
        testFixedRangesFixedModes(ExprNode.constant(-233));
        testFixedRangesFixedModes(ExprNode.constant(1100));
        testFixedRangesFixedModes(ExprNode.constant(-2048));
        testFixedRangesFixedModes(ExprNode.constant(Integer.MAX_VALUE - 134));
        testFixedRangesFixedModes(ExprNode.constant(Integer.MIN_VALUE + 17));
        testFixedRangesFixedModes(ExprNode.constant(Integer.MAX_VALUE));
        testFixedRangesFixedModes(ExprNode.constant(Integer.MIN_VALUE));
    }

    @Test
    public void test2SimpleVariable() {
        testFixedRangesFixedModes(ExprNode.variable("x"));
        testFixedRangesFixedModes(ExprNode.variable("y"));
        testFixedRangesFixedModes(ExprNode.variable("z"));
    }

    @Test
    public void test3SimpleOperations() {
        testFixedRangesFixedModes(ExprNode.unary("-", ExprNode.variable("x")));
        testFixedRangesFixedModes(ExprNode.unary("-", ExprNode.constant(7)));
        testFixedRangesFixedModes(ExprNode.unary("-", ExprNode.constant(Integer.MIN_VALUE)));

        testFixedRangesFixedModes(
                ExprNode.binary("+",
                        ExprNode.variable("z"),
                        ExprNode.constant(123)
                )
        );
        testFixedRangesFixedModes(
                ExprNode.binary("-",
                        ExprNode.variable("y"),
                        ExprNode.variable("z")
                )
        );
        testFixedRangesFixedModes(
                ExprNode.binary("*",
                        ExprNode.variable("y"),
                        ExprNode.constant(-0xCAFE)
                )
        );
        testFixedRangesFixedModes(
                ExprNode.binary("/",
                        ExprNode.variable("x"),
                        ExprNode.variable("x")
                )
        );
        testFixedRangesFixedModes(
                ExprNode.binary("/",
                        ExprNode.constant(Integer.MAX_VALUE - 3),
                        ExprNode.variable("y")
                )
        );
    }

    private final Random rng = new Random(8082475903752582983L);

    private ExprNode generateExpressionRec(final int depth, final Function<Integer, Double> stopProb) {
        if (rng.nextDouble(1) < stopProb.apply(depth)) {
            if (rng.nextBoolean()) {
                return ExprNode.constant(rng.nextInt());
            } else {
                return ExprNode.variable(switch (rng.nextInt(3)) {
                    case 0 -> "x";
                    case 1 -> "y";
                    default -> "z";
                });
            }
        } else {
            return switch (rng.nextInt(5)) {
                case 0 ->
                        ExprNode.binary("+", generateExpressionRec(depth + 1, stopProb), generateExpressionRec(depth + 1, stopProb));
                case 1 ->
                        ExprNode.binary("-", generateExpressionRec(depth + 1, stopProb), generateExpressionRec(depth + 1, stopProb));
                case 2 ->
                        ExprNode.binary("*", generateExpressionRec(depth + 1, stopProb), generateExpressionRec(depth + 1, stopProb));
                case 3 ->
                        ExprNode.binary("/", generateExpressionRec(depth + 1, stopProb), generateExpressionRec(depth + 1, stopProb));
                default -> ExprNode.unary("-", generateExpressionRec(depth + 1, stopProb));
            };
        }
    }

    private ExprNode generateExpression(final Function<Integer, Double> stopProb) {
        return generateExpressionRec(0, stopProb);
    }

    private RangeInclusive randomRange() {
        final int distance = rng.nextInt(3, 20);
        final int from = rng.nextInt(Integer.MIN_VALUE, Integer.MAX_VALUE - distance + 1);
        return new RangeInclusive(from, from + distance);
    }

    @Test
    public void testNRandom() {
        for (int i = 0; i < 200; i++) {
            final ExprNode expr = generateExpression(depth -> depth / 5.0);
            final RangeInclusive xRange = randomRange();
            final RangeInclusive yRange = randomRange();
            final RangeInclusive zRange = randomRange();
            testValid(expr, "i", xRange, yRange, zRange);
            testValid(expr, "bi", xRange, yRange, zRange);
        }

        for (int i = 0; i < 40; i++) {
            final ExprNode expr = generateExpression(depth -> -1.0 / (depth + 1) + 1 + 1 / 10.0);
            final RangeInclusive xRange = randomRange();
            final RangeInclusive yRange = randomRange();
            final RangeInclusive zRange = randomRange();
            testValid(expr, "i", xRange, yRange, zRange);
            testValid(expr, "bi", xRange, yRange, zRange);
        }

        for (int i = 0; i < 10; i++) {
            final ExprNode expr = generateExpression(depth -> depth / 20.0);
            final RangeInclusive xRange = randomRange();
            final RangeInclusive yRange = randomRange();
            final RangeInclusive zRange = randomRange();
            testValid(expr, "i", xRange, yRange, zRange);
            testValid(expr, "bi", xRange, yRange, zRange);
        }

        for (int i = 0; i < 20; i++) {
            final ExprNode expr = generateExpression(depth -> Math.pow(depth, 4.0) / Math.pow(15, 4.0));
            final RangeInclusive xRange = randomRange();
            final RangeInclusive yRange = randomRange();
            final RangeInclusive zRange = randomRange();
            testValid(expr, "i", xRange, yRange, zRange);
            testValid(expr, "bi", xRange, yRange, zRange);
        }
    }
}
