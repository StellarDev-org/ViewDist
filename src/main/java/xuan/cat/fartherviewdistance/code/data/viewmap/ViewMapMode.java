package xuan.cat.fartherviewdistance.code.data.viewmap;

import java.util.function.Consumer;
import java.util.function.Function;
import xuan.cat.fartherviewdistance.code.data.viewmap.IntX15ViewMap;
import xuan.cat.fartherviewdistance.code.data.viewmap.LongX31ViewMap;
import xuan.cat.fartherviewdistance.code.data.viewmap.LongXInfinitelyViewMap;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewMap;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewMapMode;
import xuan.cat.fartherviewdistance.code.data.viewmap.ViewShape;

@SuppressWarnings("unused")
public enum ViewMapMode {
    X15(IntX15ViewMap::new, 15), X31(LongX31ViewMap::new, 31), X63(viewShape -> new LongXInfinitelyViewMap(viewShape, 2), 63),
    X127(viewShape -> new LongXInfinitelyViewMap(viewShape, 4), 127), X383(viewShape -> new LongXInfinitelyViewMap(viewShape, 6), 383);

    private final int extend;
    private final Function<ViewShape, ViewMap> create;

    private ViewMapMode(final Function<ViewShape, ViewMap> create, final int extend) {
        this.extend = extend;
        this.create = create;
    }

    public ViewMap createMap(final ViewShape viewShape) { return (ViewMap) this.create.apply(viewShape); }

    public int getExtend() { return this.extend; }

    // $FF: synthetic method
    private static ViewMapMode[] $values() { return new ViewMapMode[] { X15, X31, X63, X127, X383 }; }
}
