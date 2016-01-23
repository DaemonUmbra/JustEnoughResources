package jeresources.profiling;

import jeresources.utils.MapKeys;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChunkProfiler implements Runnable
{
    private final World world;
    private final ProfilingTimer timer;
    private final List<Chunk> chunks;
    private final ProfiledDimensionData dimensionData;
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;

    public ChunkProfiler(World world, List<Chunk> chunks, ProfiledDimensionData dimensionData, ProfilingTimer timer)
    {
        this.world = world;
        this.chunks = chunks;
        this.dimensionData = dimensionData;
        this.timer = timer;
    }

    @Override
    public void run()
    {
        for (Chunk chunk : this.chunks)
            profileChunk(chunk);
    }

    private void profileChunk(Chunk chunk)
    {
        this.timer.startChunk(world.provider.getDimensionId());
        Map<String, Integer[]> temp = new HashMap<>();

        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        MovingObjectPosition movingObjectPosition = new MovingObjectPosition(new Vec3(0, 0, 0), EnumFacing.DOWN, blockPos);
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;

        final int maxY = chunk.getTopFilledSegment();
        for (int y = 0; y < maxY; y++)
            for (int x = 0; x < CHUNK_SIZE; x++)
                for (int z = 0; z < CHUNK_SIZE; z++)
                {
                    blockPos.set(x + chunk.xPosition * CHUNK_SIZE, y, z + chunk.zPosition * CHUNK_SIZE);
                    Block block = chunk.getBlock(x, y, z);
                    int meta = chunk.getBlockMetadata(blockPos);

                    ItemStack pickBlock = block.getPickBlock(movingObjectPosition, world, blockPos, player);
                    String key;
                    if (pickBlock == null)
                    {
                        key = block.getRegistryName() + ':' + meta;
                    } else
                    {
                        key = MapKeys.getKey(pickBlock);
                    }

                    if (!dimensionData.dropsMap.containsKey(key))
                    {
                        IBlockState blockState = block.getStateFromMeta(meta);
                        dimensionData.dropsMap.put(key, getDrops(block, world, blockPos, blockState));
                    }

                    if (!dimensionData.silkTouchMap.containsKey(key))
                    {
                        IBlockState blockState = block.getStateFromMeta(meta);
                        boolean canSilkTouch = block.canSilkHarvest(world, blockPos, blockState, player);
                        dimensionData.silkTouchMap.put(key, canSilkTouch);
                    }

                    Integer[] array = temp.get(key);
                    if (array == null)
                    {
                        array = new Integer[CHUNK_HEIGHT];
                        Arrays.fill(array, 0);
                    }
                    array[y]++;
                    temp.put(key, array);
                }

        for (Map.Entry<String, Integer[]> entry : temp.entrySet())
        {
            Integer[] array = dimensionData.distributionMap.get(entry.getKey());
            if (array == null)
            {
                array = new Integer[CHUNK_HEIGHT];
                Arrays.fill(array, 0);
            }
            for (int i = 0; i < CHUNK_HEIGHT; i++)
                array[i] += entry.getValue()[i];
            dimensionData.distributionMap.put(entry.getKey(), array);
        }

        this.timer.endChunk(world.provider.getDimensionId());
    }

    public static Map<String, Float> getDrops(Block block, IBlockAccess world, BlockPos pos, IBlockState state) {
        final int totalTries = 10000;

        final Map<String, Integer> dropsMap = new HashMap<>();
        for (int i = 0; i < totalTries; i++)
        {
            List<ItemStack> drops = block.getDrops(world, pos, state, 0);
            for (ItemStack drop : drops) {
                if (drop == null)
                    continue;
                String key = MapKeys.getKey(drop);
                Integer count = dropsMap.get(key);
                if (count != null) {
                    count += drop.stackSize;
                } else {
                    count = drop.stackSize;
                }
                dropsMap.put(key, count);
            }
        }

        final Map<String, Float> dropsMapAverage = new HashMap<>(dropsMap.size());
        for (Map.Entry<String, Integer> dropEntry : dropsMap.entrySet())
        {
            dropsMapAverage.put(dropEntry.getKey(), dropEntry.getValue() / (float) totalTries);
        }

        return dropsMapAverage;
    }
}