package xuan.cat.fartherviewdistance.code.data.viewmap;

import java.util.function.Consumer;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

@SuppressWarnings("unused")
public enum ViewShape {
    SQUARE((aX, aZ, bX, bZ, viewDistance) -> {
        final int minX = bX - viewDistance;
        final int minZ = bZ - viewDistance;
        final int maxX = bX + viewDistance;
        final int maxZ = bZ + viewDistance;
        return aX >= minX && aZ >= minZ && aX <= maxX && aZ <= maxZ;
    }), ROUND((aX, aZ, bX, bZ, viewDistance) -> {
        final int viewDiameter = viewDistance * viewDistance + viewDistance;
        final int distanceX = aX - bX;
        final int distanceZ = aZ - bZ;
        final int distance = distanceX * distanceX + distanceZ * distanceZ;
        return distance <= viewDiameter;
    }, (aX, aZ, bX, bZ, viewDistance) -> {
        final ViewShape.JudgeInside inside = (_aX, _aZ, _bX, _bZ, viewDiameterx) -> {
            final int distanceX = _aX - _bX;
            final int distanceZ = _aZ - _bZ;
            final int distance = distanceX * distanceX + distanceZ * distanceZ;
            return distance <= viewDiameterx;
        };
        final int viewDiameter = viewDistance * viewDistance + viewDistance;
        return (inside.test(aX, aZ, bX, bZ, viewDiameter) && inside.test(aX + 1, aZ, bX, bZ, viewDiameter)
                && inside.test(aX - 1, aZ, bX, bZ, viewDiameter) && inside.test(aX, aZ + 1, bX, bZ, viewDiameter)
                && inside.test(aX, aZ - 1, bX, bZ, viewDiameter));
    });

    private final ViewShape.JudgeInside judgeInside;
    private final ViewShape.JudgeInside judgeInsideEdge;

    private ViewShape(final ViewShape.JudgeInside judgeInside) { this(judgeInside, judgeInside); }

    private ViewShape(final ViewShape.JudgeInside judgeInside, final ViewShape.JudgeInside judgeInsideEdge) {
        this.judgeInside = judgeInside;
        this.judgeInsideEdge = judgeInsideEdge;
    }

    public boolean isInside(final int aX, final int aZ, final int bX, final int bZ, final int viewDistance) {
        return this.judgeInside.test(aX, aZ, bX, bZ, viewDistance);
    }

    public boolean isInsideEdge(final int aX, final int aZ, final int bX, final int bZ, final int viewDistance) {
        return this.judgeInsideEdge.test(aX, aZ, bX, bZ, viewDistance);
    }

    // $FF: synthetic method
    private static ViewShape[] $values() { return new ViewShape[] { SQUARE, ROUND }; }

    interface JudgeInside { boolean test(int var1, int var2, int var3, int var4, int var5); }
}
