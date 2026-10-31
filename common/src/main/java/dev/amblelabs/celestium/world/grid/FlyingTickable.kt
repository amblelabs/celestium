package dev.amblelabs.celestium.world.grid

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

interface FlyingTickable {
    fun flyingTick(
        level: Level,
        blocks: SubGridBlocks,
        state: BlockState,
        pos: BlockPos,
        worldPos: Vec3,
        movement: Direction
    )
}