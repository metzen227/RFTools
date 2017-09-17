package mcjty.rftools.blocks.shaper;

import mcjty.lib.container.DefaultSidedInventory;
import mcjty.lib.container.InventoryHelper;
import mcjty.lib.entity.GenericTileEntity;
import mcjty.lib.network.Argument;
import mcjty.lib.tools.ItemStackTools;
import mcjty.rftools.blocks.builder.BuilderSetup;
import mcjty.rftools.items.builder.Shape;
import mcjty.rftools.items.builder.ShapeCardItem;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.BlockPos;
import org.apache.commons.io.output.ByteArrayOutputStream;

import java.util.Map;

public class ScannerTileEntity extends GenericTileEntity implements DefaultSidedInventory {

    public static final String CMD_SCAN = "scan";

    private InventoryHelper inventoryHelper = new InventoryHelper(this, ScannerContainer.factory, 2);

    @Override
    public InventoryHelper getInventoryHelper() {
        return inventoryHelper;
    }

    @Override
    public boolean isUsable(EntityPlayer player) {
        return canPlayerAccess(player);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        return stack.getItem() == BuilderSetup.shapeCardItem;
    }

    @Override
    public void readRestorableFromNBT(NBTTagCompound tagCompound) {
        super.readRestorableFromNBT(tagCompound);
        readBufferFromNBT(tagCompound, inventoryHelper);
    }

    @Override
    public void writeRestorableToNBT(NBTTagCompound tagCompound) {
        super.writeRestorableToNBT(tagCompound);
        writeBufferToNBT(tagCompound, inventoryHelper);
    }

    private void scan(int offsetX, int offsetY, int offsetZ) {
        ItemStack cardIn = inventoryHelper.getStackInSlot(ScannerContainer.SLOT_IN);
        if (ItemStackTools.isValid(cardIn)) {
            NBTTagCompound tagIn = cardIn.getTagCompound();
            if (tagIn == null) {
                tagIn = new NBTTagCompound();
                cardIn.setTagCompound(tagIn);
            }
            tagIn.setInteger("offsetX", offsetX);
            tagIn.setInteger("offsetY", offsetY);
            tagIn.setInteger("offsetZ", offsetZ);

            ItemStack cardOut = inventoryHelper.getStackInSlot(ScannerContainer.SLOT_OUT);
            if (ItemStackTools.isEmpty(cardOut)) {
                return;
            }
            if (!ShapeCardItem.getShape(cardOut).isScheme()) {
                ShapeCardItem.setShape(cardOut, Shape.SHAPE_SCHEME, true);
            }
            NBTTagCompound tagOut = cardOut.getTagCompound();
            int dimX = tagIn.getInteger("dimX");
            int dimY = tagIn.getInteger("dimY");
            int dimZ = tagIn.getInteger("dimZ");
            tagOut.setInteger("dimX", dimX);
            tagOut.setInteger("dimY", dimY);
            tagOut.setInteger("dimZ", dimZ);
            scanArea(tagOut, getPos().add(offsetX, offsetY, offsetZ), dimX, dimY, dimZ);
        }
    }

    private void scanArea(NBTTagCompound tagOut, BlockPos center, int dimX, int dimY, int dimZ) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        BlockPos tl = new BlockPos(center.getX() - dimX/2, center.getY() - dimY/2, center.getZ() - dimZ/2);
        Byte prev = null;
        int cnt = 0;
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();
        for (int y = tl.getY() ; y < tl.getY() + dimY ; y++) {
            for (int x = tl.getX() ; x < tl.getX() + dimX ; x++) {
                for (int z = tl.getZ() ; z < tl.getZ() + dimZ ; z++) {
                    mpos.setPos(x, y, z);
                    byte c;
                    if (getWorld().isAirBlock(mpos)) {
                        c = 0;
                    } else {
                        IBlockState state = getWorld().getBlockState(mpos);
                        c = 1;
                    }
                    if (prev == null) {
                        prev = c;
                        cnt = 1;
                    } else if (prev == c && cnt < 255) {
                        cnt++;
                    } else {
                        stream.write(cnt);
                        stream.write(prev);
                        prev = c;
                        cnt = 1;
                    }
                }
            }
        }
        if (prev != null) {
            stream.write(cnt);
            stream.write(prev);
        }
        tagOut.setByteArray("data", stream.toByteArray());
    }

    @Override
    public boolean execute(EntityPlayerMP playerMP, String command, Map<String, Argument> args) {
        boolean rc = super.execute(playerMP, command, args);
        if (rc) {
            return true;
        }
        if (CMD_SCAN.equals(command)) {
            scan(args.get("offsetX").getInteger(), args.get("offsetY").getInteger(), args.get("offsetZ").getInteger());
            return true;

        }
        return false;
    }

}