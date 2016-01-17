package jeresources.profiling;

import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class ChunkGetter implements Runnable
{
    private World world;
    private BlockPos origin;
    private int x, z;

    public ChunkGetter(World world, BlockPos origin, int x, int z)
    {
        this.world = world;
        this.origin = origin;
        this.x = x;
        this.z = z;
    }

    @Override
    public void run()
    {
        Profiler.newChunkProfiler(this.world.getChunkFromBlockCoords(
                this.origin.add(this.x * ChunkProfiler.CHUNK_SIZE, 0, this.z * ChunkProfiler.CHUNK_SIZE)));
    }
}
