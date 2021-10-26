package dev.hephaestus.proximity.util;

import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class Either<L, R> {
    private final L left;
    private final R right;
    private final boolean isLeft;

    private Either(L left, R right, boolean isLeft) {
        this.left = left;
        this.right = right;
        this.isLeft = isLeft;
    }

    public L getLeft() {
        return this.left;
    }

    public R getRight() {
        return this.right;
    }

    public boolean isLeft() {
        return this.isLeft;
    }

    public static <L, R> Either<L, R> left(L left) {
        return new Either<>(left, null, true);
    }

    public static <L ,R> Either<L, R> right(R right) {
        return new Either<>(null, right, false);
    }

    public static <L, R> Either<L, R> of(@Nullable L left, @Nullable R right) {
        if (left == null ^ right == null) {
            return new Either<>(left, right, right == null);
        }

        throw new RuntimeException();
    }

    public <L2, R2> Either<L2, R2> branch(Function<L, L2> left, Function<R, R2> right) {
        return new Either<>(
                this.isLeft ? left.apply(this.left) : null,
                this.isLeft ? null : right.apply(this.right),
                this.isLeft
        );
    }

    public <T> T join(Function<L, T> left, Function<R, T> right) {
        return this.isLeft ? left.apply(this.left) : right.apply(this.right);
    }
}
