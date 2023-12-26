package xuan.cat.fartherviewdistance.code.branch.v120_4;

import net.minecraft.nbt.CompoundTag;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_120_4_NBT implements BranchNBT {

  protected CompoundTag tag;

  public Branch_120_4_NBT() {
    this.tag = new CompoundTag();
  }

  public Branch_120_4_NBT(CompoundTag tag) {
    this.tag = tag;
  }

  public CompoundTag getNMSTag() {
    return tag;
  }

  @Override
  public String toString() {
    return tag.toString();
  }
}
