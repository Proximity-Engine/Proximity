package dev.hephaestus.proximity.util;

import org.jetbrains.annotations.Nullable;

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

    public static <L> Either<L, ?> left(L left) {
        return new Either<>(left, null, true);
    }

    public static <R> Either<?, R> right(R right) {
        return new Either<>(null, right, false);
    }

    public static <L, R> Either<L, R> of(@Nullable L left, @Nullable R right) {
        if (left == null ^ right == null) {
            return new Either<>(left, right, right == null);
        }

        throw new RuntimeException();
    }
}
