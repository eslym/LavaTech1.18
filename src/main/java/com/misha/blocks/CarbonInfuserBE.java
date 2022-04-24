package com.misha.blocks;

import com.misha.setup.Registration;
import com.misha.tools.CustomEnergyStorage;
import com.misha.setup.Registration;
import com.misha.tools.CustomEnergyStorage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.TileFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStack;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicInteger;

public class CarbonInfuserBE extends BlockEntity {

    public static int capacity = 25000;
    public int usage = 5;
    int transfer = 200;
    boolean hasPower = false;
    public static final int baseUsage = 30;
    static int basetime = 500;
    boolean sun = false;
    int yield = 1;
    int time = basetime;

    //private final TileFluidHandler fluidHandler= createFluid();
    private final ItemStackHandler itemHandler = createHandler();
    private final CustomEnergyStorage energyStorage = createEnergy();
    // Never create lazy optionals in getCapability. Always place them as fields in the tile entity:
    private final LazyOptional<IItemHandler> handler = LazyOptional.of(() -> itemHandler);
    private final LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> energyStorage);

    public int counter = 0;
    public int ccounter = 0;

    int carbon=0;
    public static int carbonCap=5000;

    public CarbonInfuserBE(BlockPos pos, BlockState state) {
        super(Registration.CARBONINFUSER_BE.get(), pos, state);
    }

    public void setRemoved() {
        super.setRemoved();
        // Don't forget to invalidate your caps when your block entity is removed
        handler.invalidate();
        energy.invalidate();
    }

    public static boolean isValid(ItemStack i) {
        return true;

    }

    public static boolean random(int chance) {
        int rand = (int) Math.round(Math.random() * 100);
        return rand < chance;
    }

    public boolean sunlight() {
        float time = level.getDayTime();
        return ((time > 23450 || time < 12540.0F) && level.canSeeSky(worldPosition));
    }

    public void tickServer(BlockState state) {
        ItemStack input = itemHandler.getStackInSlot(0);
        ItemStack coal = itemHandler.getStackInSlot(1);
        ItemStack output=itemHandler.getStackInSlot(2);

        if(!coal.isEmpty()){
            if(coal.getItem()==Items.COAL){
                if(carbon<=carbonCap-100) {
                    carbon += 100;
                    itemHandler.extractItem(1, 1, false);
                }
            }
            if(coal.getItem()==Blocks.COAL_BLOCK.asItem()){
                if(carbon<=carbonCap-900) {
                    carbon += 900;
                    itemHandler.extractItem(1, 1, false);
                }
            }
        }
        if(hasEnoughPowerToWork() && carbon>=100){
            if(input.is(ItemTags.COALS) || input.is(ItemTags.create(new ResourceLocation("forge:biomass")))){
                if(counter>=time && (output.getItem()==Items.COAL ||output.isEmpty()) && carbon>=100){
                    if(output.getItem()==Items.COAL){
                        ItemStack newItem = new ItemStack(Items.COAL, 2);
                        itemHandler.insertItem(2, newItem, false);

                    }
                    if(output.isEmpty()){
                        ItemStack newItem = new ItemStack(Items.COAL, 2);
                        itemHandler.setStackInSlot(2,newItem);

                    }
                    itemHandler.extractItem(0,1,false);
                    carbon-=100;
                    counter=0;
                }else{
                    counter++;
                }
            }

            if(input.is(Tags.Items.INGOTS_IRON)){
                if(counter>=time && (output.getItem()==Items.COAL ||output.isEmpty()) && carbon>=100){
                    if(output.getItem()==Items.COAL){
                        ItemStack newItem = new ItemStack(Items.COAL, 2);
                        itemHandler.insertItem(2, newItem, false);

                    }
                    if(output.isEmpty()){
                        ItemStack newItem = new ItemStack(Items.COAL, 2);
                        itemHandler.setStackInSlot(2,newItem);

                    }
                    itemHandler.extractItem(0,1,false);
                    carbon-=100;
                    counter=0;
                }else{
                    counter++;
                }
            }
        }


    }

    public int findNext(ItemStack stack) {
        for (int i = 0; i < 9; i++) {
            if (itemHandler.getStackInSlot(i + 1).getItem() == stack.getItem()) {
                return i + 1;
            }
        }
        return nextEmpty();
    }

    public int nextEmpty() {
        for (int i = 0; i < 9; i++) {
            if (itemHandler.getStackInSlot(i + 1).isEmpty()) {
                return i + 1;
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        return nextEmpty() != -1;
    }

    private boolean hasEnoughPowerToWork() {
        return energyStorage.getEnergyStored() >= usage;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("counter", counter);
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();

        ccounter = tag.getInt("counter");
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putInt("counter", counter);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
        ccounter = tag.getInt("counter");
    }


    @Override
    public void load(CompoundTag tag) {
        itemHandler.deserializeNBT(tag.getCompound("inv"));

        if (tag.contains("energy")) {
            energyStorage.deserializeNBT(tag.get("energy"));
        }
        counter = tag.getInt("counter");
        super.load(tag);
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        tag.put("inv", itemHandler.serializeNBT());
        tag.put("energy", energyStorage.serializeNBT());
        tag.putInt("counter", counter);

    }


    private ItemStackHandler createHandler() {
        return new ItemStackHandler(10) {

            @Override
            protected void onContentsChanged(int slot) {
                // To make sure the TE persists when the chunk is saved later we need to
                // mark it dirty every time the item handler changes
                setChanged();
            }

            @Override
            public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
                return isValid(stack);

            }

            @Nonnull
            @Override
            public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
                if ((stack.isEmpty())) {
                    return stack;
                }
                return super.insertItem(slot, stack, simulate);
            }
        };
    }


    private CustomEnergyStorage createEnergy() {
        return new CustomEnergyStorage(capacity, transfer) {
            @Override
            protected void onEnergyChanged() {

                boolean newHasPower = hasEnoughPowerToWork();
                if (newHasPower != hasPower) {
                    hasPower = newHasPower;
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
                }
                setChanged();
            }
        };
    }


    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return handler.cast();
        }
        if (cap == CapabilityEnergy.ENERGY) {
            return energy.cast();
        }
        return super.getCapability(cap, side);
    }


}