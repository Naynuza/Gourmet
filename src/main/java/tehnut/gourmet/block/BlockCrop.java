package tehnut.gourmet.block;

import net.minecraft.block.BlockCrops;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import tehnut.gourmet.core.RegistrarGourmet;
import tehnut.gourmet.core.data.Harvest;
import tehnut.gourmet.core.util.IHarvestContainer;

import java.util.Random;

public class BlockCrop extends BlockCrops implements IHarvestContainer {

    private final Harvest harvest;
    private final PropertyInteger age;
    private final BlockStateContainer realBlockState;
    private Item produce;
    private Item seed;

    public BlockCrop(Harvest harvest) {
        this.harvest = harvest;
        this.age = PropertyInteger.create("age", 0, harvest.getCropGrowth().getStages());
        this.realBlockState = new BlockStateContainer.Builder(this).add(age).build();

        setDefaultState(getBlockState().getBaseState().withProperty(age, 0));
    }

    // Have to bypass BlockCrops updateTick() to handle light checks on our own
    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        this.checkAndDropBlock(world, pos, state);

        if (!harvest.getCropGrowth().checkLight(world.getLightFromNeighbors(pos.up())))
            return;

        int age = getAge(state);
        if (age >= getMaxAge())
            return;

        float growthChance = getGrowthChance(this, world, pos);
        if (ForgeHooks.onCropsGrowPre(world, pos, state, rand.nextInt((int) (25.0F / growthChance) + 1) == 0)) {
            world.setBlockState(pos, state.cycleProperty(getAgeProperty()));
            ForgeHooks.onCropsGrowPost(world, pos, state, state.cycleProperty(getAgeProperty()));
        }
    }

    @Override
    public void getDrops(NonNullList<ItemStack> drops, IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        int age = getAge(state);
        Random rand = world instanceof World ? ((World) world).rand : new Random();

        if (age >= getMaxAge()) {
            for (int i = 0; i < harvest.getCropGrowth().getMaxSeedDrop() + fortune; ++i)
                if (rand.nextInt(2 * getMaxAge()) <= age)
                    drops.add(new ItemStack(getSeed()));

            for (int i = 0; i < harvest.getCropGrowth().getMaxProduceDrop() + fortune; ++i)
                if (rand.nextInt(2 * getMaxAge()) <= age)
                    drops.add(new ItemStack(getCrop()));
        } else {
            drops.add(new ItemStack(getSeed()));
        }
    }

    /*  For the future me:
     *  This is first called before we can create our custom blockstate with the correct property, so we have to dummy out
     *  to the default age property to avoid crashes.
     */
    @Override
    protected PropertyInteger getAgeProperty() {
        return realBlockState == null ? super.getAgeProperty() : age;
    }

    /*  For the future me:
     *  This is first called before we can create our custom blockstate with the correct properties, so we have to dummy
     *  out to the default blockstate to avoid crashes.
     */
    @Override
    public BlockStateContainer getBlockState() {
        return realBlockState == null ? super.getBlockState() : realBlockState;
    }

    @Override
    public int getMaxAge() {
        return harvest.getCropGrowth().getStages();
    }

    @Override
    protected Item getSeed() {
        return seed == null ? seed = RegistrarGourmet.getSeeds().get(harvest) : seed;
    }

    @Override
    protected Item getCrop() {
        return produce == null ? produce = RegistrarGourmet.getEdibles().get(harvest) : produce;
    }

    @Override
    public boolean canUseBonemeal(World worldIn, Random rand, BlockPos pos, IBlockState state) {
        return harvest.getCropGrowth().canFertilize();
    }

    @Override
    protected final BlockStateContainer createBlockState() {
        return new BlockStateContainer.Builder(this).add(AGE).build();
    }

    @Override
    public Harvest getHarvest() {
        return harvest;
    }
}
