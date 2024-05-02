package xuan.cat.fartherviewdistance.code.branch.v120_6;

import net.minecraft.nbt.CompoundTag;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_120_6_NBT implements BranchNBT {

    protected CompoundTag tag;

    public Branch_120_6_NBT() { this.tag = new CompoundTag(); }

    public Branch_120_6_NBT(final CompoundTag tag) { this.tag = tag; }

    public CompoundTag getNMSTag() { return this.tag; }

    @Override
    public String toString() { return this.tag.toString(); }
}
