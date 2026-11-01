package dev.amblelabs.celestium.world.grid

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import io.netty.buffer.ByteBuf
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.codec.ByteBufCodecs
import net.minecraft.network.codec.StreamCodec
import net.minecraft.util.Mth
import net.minecraft.util.SimpleBitStorage
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Fluids
import net.minecraft.world.phys.Vec3
import java.util.function.Consumer
import java.util.function.Function
import java.util.stream.Collectors
import java.util.stream.IntStream
import java.util.stream.LongStream
import kotlin.math.max

class SubGridBlocks {
    val blockStates: Array<BlockState>
    val tickables: MutableList<BlockPos>
    val sizeX: Int
    val sizeY: Int
    val sizeZ: Int

    internal constructor(blockStates: Array<BlockState>, list: MutableList<BlockPos>, i: Int, j: Int, k: Int) {
        this.blockStates = blockStates
        this.tickables = list
        this.sizeX = i
        this.sizeY = j
        this.sizeZ = k
    }

    constructor(i: Int, j: Int, k: Int) {
        this.blockStates = Array(i * j * k) { EMPTY_BLOCK_STATE }
        this.tickables = ArrayList()
        this.sizeX = i
        this.sizeY = j
        this.sizeZ = k
    }

    fun countSolidBlocks(): Int {
        var count = 0
        for (state in this.blockStates) {
            if (!state.isAir) count++
        }
        return count
    }

    fun setBlockState(i: Int, j: Int, k: Int, blockState: BlockState?) {
        val l = this.index(i, j, k)
        check(l != -1) { "Block was out of bounds" }

        this.blockStates[l] = blockState!!
    }

    fun markTickable(blockPos: BlockPos) {
        this.tickables.add(blockPos)
    }

    fun tick(level: Level, vec3: Vec3, direction: Direction) {
        this.tickables.forEach { blockPos: BlockPos ->
            val blockState = this.getBlockState(blockPos.x, blockPos.y, blockPos.z)
            val block = blockState.block

            if (block is FlyingTickable) {
                block.flyingTick(
                    level,
                    this,
                    blockState,
                    blockPos,
                    vec3.add(blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble()),
                    direction
                )
            }
        }
    }

    fun getBlockState(i: Int, j: Int, k: Int): BlockState {
        val l = this.index(i, j, k)
        return if (l == NO_INDEX) EMPTY_BLOCK_STATE else this.blockStates[l]
    }

    fun getBlockState(blockPos: BlockPos): BlockState {
        return this.getBlockState(blockPos.x, blockPos.y, blockPos.z)
    }

    private fun index(i: Int, j: Int, k: Int): Int {
        return if (i >= 0 && j >= 0 && k >= 0 && i < this.sizeX && j < this.sizeY && k < this.sizeZ) (i + k * this.sizeX) * this.sizeY + j else -1
    }

    fun copy(): SubGridBlocks {
        return SubGridBlocks(
            this.blockStates.copyOf(),
            ArrayList(this.tickables),
            this.sizeX,
            this.sizeY,
            this.sizeZ
        )
    }

    fun place(blockPos: BlockPos, level: Level) {
        val pos = BlockPos.MutableBlockPos()

        for (z in 0..<this.sizeZ) {
            for (x in 0..<this.sizeX) {
                for (y in 0..<this.sizeY) {
                    pos.setWithOffset(blockPos, x, y, z)
                    var state = this.getBlockState(x, y, z)

                    if (!state.isAir) {
                        val fluidState = level.getFluidState(pos)

                        if (fluidState.`is`(Fluids.WATER)) state = state.trySetValue(
                            BlockStateProperties.WATERLOGGED, true
                        )

                        level.setBlock(pos, state, Block.UPDATE_KNOWN_SHAPE or Block.UPDATE_CLIENTS)
                    }
                }
            }
        }

        for (z in 0..<this.sizeZ) {
            for (x in 0..<this.sizeX) {
                for (y in 0..<this.sizeY) {
                    pos.setWithOffset(blockPos, x, y, z)
                    level.updateNeighborsAt(pos, this.getBlockState(x, y, z).block)
                }
            }
        }
    }

    private fun toStored(): Stored {
        val indices: Reference2IntMap<BlockState> = Reference2IntOpenHashMap()
        indices.defaultReturnValue(NO_INDEX)
        val palette: MutableList<BlockState> = ArrayList()
        val blocks = IntArray(this.blockStates.size)

        for (i in this.blockStates.indices) {
            val index = indices.putIfAbsent(this.blockStates[i], palette.size)
            if (index == NO_INDEX) {
                blocks[i] = palette.size
                palette.add(this.blockStates[i])
            } else {
                blocks[i] = index
            }
        }

        return Stored(
            this.sizeX,
            this.sizeY,
            this.sizeZ,
            palette,
            IntStream.of(*blocks),
            this.tickables.stream().mapToLong { obj: BlockPos? -> obj!!.asLong() })
    }

    @JvmRecord
    private data class Stored(
        val sizeX: Int,
        val sizeY: Int,
        val sizeZ: Int,
        val palette: MutableList<BlockState>,
        val blocks: IntStream,
        val tickables: LongStream
    ) {
        companion object {
            val CODEC: Codec<Stored?> =
                RecordCodecBuilder.create { instance: RecordCodecBuilder.Instance<Stored> ->
                    instance.group(
                        Codec.INT.fieldOf("size_x").forGetter(Stored::sizeX),
                        Codec.INT.fieldOf("size_y").forGetter(Stored::sizeY),
                        Codec.INT.fieldOf("size_z").forGetter(Stored::sizeZ),
                        BlockState.CODEC.listOf().fieldOf("palette").forGetter(Stored::palette),
                        Codec.INT_STREAM.fieldOf("blocks").forGetter(Stored::blocks),
                        Codec.LONG_STREAM.optionalFieldOf("tickables", LongStream.empty())
                            .forGetter(Stored::tickables)
                    ).apply(
                        instance, ::Stored
                    )
                }
        }
    }

    companion object {
        private val EMPTY_BLOCK_STATE = Blocks.AIR.defaultBlockState()
        private const val NO_INDEX = -1

        private val PALETTE_STREAM_CODEC: StreamCodec<ByteBuf, MutableList<BlockState>> =
            ByteBufCodecs.idMapper(
                Block.BLOCK_STATE_REGISTRY
            ).apply(ByteBufCodecs.list())

        private val TICKABLES_STREAM_CODEC: StreamCodec<ByteBuf, MutableList<BlockPos>> =
            BlockPos.STREAM_CODEC.apply(
                ByteBufCodecs.list()
            )

        val STREAM_CODEC: StreamCodec<RegistryFriendlyByteBuf, SubGridBlocks> =
            object : StreamCodec<RegistryFriendlyByteBuf, SubGridBlocks> {
                override fun decode(buffer: RegistryFriendlyByteBuf): SubGridBlocks {
                    val sizeX = buffer.readVarInt()
                    val sizeY = buffer.readVarInt()
                    val sizeZ = buffer.readVarInt()
                    val palette: MutableList<BlockState> = PALETTE_STREAM_CODEC.decode(buffer)

                    val expectedSize = sizeX * sizeY * sizeZ
                    val storage = SimpleBitStorage(bitsFor(palette.size), expectedSize, buffer.readLongArray())

                    val blockStates: Array<BlockState> = Array(expectedSize) { i ->
                        palette[storage.get(i)]
                    }

                    val tickables: MutableList<BlockPos> = ArrayList(TICKABLES_STREAM_CODEC.decode(buffer))
                    return SubGridBlocks(blockStates, tickables, sizeX, sizeY, sizeZ)
                }

                override fun encode(buffer: RegistryFriendlyByteBuf, blocks: SubGridBlocks) {
                    buffer.writeVarInt(blocks.sizeX)
                    buffer.writeVarInt(blocks.sizeY)
                    buffer.writeVarInt(blocks.sizeZ)
                    val indices: Reference2IntMap<BlockState> = Reference2IntOpenHashMap()
                    indices.defaultReturnValue(NO_INDEX)
                    val palette: MutableList<BlockState> = ArrayList()

                    for (state in blocks.blockStates) {
                        if (indices.putIfAbsent(state, palette.size) == NO_INDEX) {
                            palette.add(state)
                        }
                    }

                    PALETTE_STREAM_CODEC.encode(buffer, palette)
                    val storage = SimpleBitStorage(bitsFor(palette.size), blocks.blockStates.size)

                    for (i in blocks.blockStates.indices) {
                        storage.set(i, indices.getInt(blocks.blockStates[i]))
                    }

                    buffer.writeLongArray(storage.raw)
                    TICKABLES_STREAM_CODEC.encode(buffer, blocks.tickables)
                }
            }

        val CODEC: Codec<SubGridBlocks> = Stored.CODEC.xmap(
            Function { stored: Stored -> fromStored(stored) },
            Function { obj: SubGridBlocks -> obj.toStored() })

        private fun bitsFor(paletteSize: Int): Int {
            return max(1, Mth.ceillog2(paletteSize))
        }

        private fun fromStored(stored: Stored): SubGridBlocks {
            val expectedSize = stored.sizeX * stored.sizeY * stored.sizeZ
            val indices = stored.blocks.toArray()

            if (indices.size != expectedSize) {
                return SubGridBlocks(stored.sizeX, stored.sizeY, stored.sizeZ)
            }

            val blockStates: Array<BlockState> = Array(indices.size) { i ->
                val index = indices[i]
                if (index in stored.palette.indices)
                    stored.palette[index] else EMPTY_BLOCK_STATE
            }

            return SubGridBlocks(
                blockStates,
                stored.tickables.mapToObj(BlockPos::of)
                    .collect(
                        Collectors.toList()
                    ),  // TODO: does this need to be an array list?
                stored.sizeX, stored.sizeY, stored.sizeZ
            )
        }
    }
}

