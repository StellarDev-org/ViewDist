package xuan.cat.fartherviewdistance.code.branch.v120_2;

import net.minecraft.nbt.CompoundTag;
import xuan.cat.fartherviewdistance.api.branch.BranchNBT;

public final class Branch_120_2_NBT implements BranchNBT {

  protected CompoundTag tag;

  public Branch_120_2_NBT() {
    this.tag = new CompoundTag();
  }

  public Branch_120_2_NBT(CompoundTag tag) {
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
