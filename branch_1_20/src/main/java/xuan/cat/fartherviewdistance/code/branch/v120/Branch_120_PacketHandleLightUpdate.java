package xuan.cat.fartherviewdistance.code.branch.v120;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

public final class Branch_120_PacketHandleLightUpdate {

  public Branch_120_PacketHandleLightUpdate() {}

  public void write(FriendlyByteBuf serializer, Branch_120_ChunkLight light) {
    List<byte[]> dataSky = new ArrayList<>();
    List<byte[]> dataBlock = new ArrayList<>();
    BitSet notSkyEmpty = new BitSet();
    BitSet notBlockEmpty = new BitSet();
    BitSet isSkyEmpty = new BitSet();
    BitSet isBlockEmpty = new BitSet();

    for (int index = 0; index < light.getArrayLength(); ++index) {
      saveBitSet(light.getSkyLights(), index, notSkyEmpty, isSkyEmpty, dataSky);
      saveBitSet(
        light.getBlockLights(),
        index,
        notBlockEmpty,
        isBlockEmpty,
        dataBlock
      );
    }

    serializer.writeBitSet(notSkyEmpty);
    serializer.writeBitSet(notBlockEmpty);
    serializer.writeBitSet(isSkyEmpty);
    serializer.writeBitSet(isBlockEmpty);
    serializer.writeCollection(dataSky, FriendlyByteBuf::writeByteArray);
    serializer.writeCollection(dataBlock, FriendlyByteBuf::writeByteArray);
  }

  private static void saveBitSet(
    byte[][] nibbleArrays,
    int index,
    BitSet notEmpty,
    BitSet isEmpty,
    List<byte[]> list
  ) {
    byte[] nibbleArray = nibbleArrays[index];
    if (nibbleArray != Branch_120_ChunkLight.EMPTY) {
      if (nibbleArray == null) {
        isEmpty.set(index);
      } else {
        notEmpty.set(index);
        list.add(nibbleArray);
      }
    }
  }
}
