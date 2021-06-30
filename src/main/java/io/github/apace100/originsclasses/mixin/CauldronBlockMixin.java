package io.github.apace100.originsclasses.mixin;

import io.github.apace100.originsclasses.power.ClassPowerTypes;
import net.minecraft.block.AbstractCauldronBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.CauldronBlock;
import net.minecraft.block.LeveledCauldronBlock;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PotionItem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.potion.PotionUtil;
import net.minecraft.potion.Potions;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.stream.Collectors;

@Mixin(AbstractCauldronBlock.class)
public abstract class CauldronBlockMixin {

    @Inject(method = "onUse", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void extendPotionDuration(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        if((Object)this instanceof LeveledCauldronBlock && ClassPowerTypes.LONGER_POTIONS.isActive(player)) {
            ItemStack stack = player.getStackInHand(hand);
            int level = state.get(LeveledCauldronBlock.LEVEL);
            if(stack.getItem() instanceof PotionItem && level > 0 && (!stack.hasTag() || !stack.getTag().getBoolean("IsExtendedByCleric"))) {
                ItemStack extended = new ItemStack(stack.getItem());
                NbtCompound tag = extended.getOrCreateTag().copyFrom(stack.getTag());
                tag.putString("OriginalName", Text.Serializer.toJson(stack.getName()));
                tag.putBoolean("IsExtendedByCleric", true);
                PotionUtil.setPotion(extended, Potions.EMPTY);
                Collection<StatusEffectInstance> customPotion = (PotionUtil.getCustomPotionEffects(stack).isEmpty() ? PotionUtil.getPotionEffects(stack) : PotionUtil.getCustomPotionEffects(stack)).stream().map(effect -> new StatusEffectInstance(effect.getEffectType(), effect.getDuration() * (effect.getEffectType().isInstant() ? 1 : 2), effect.getAmplifier(), effect.isAmbient(), effect.shouldShowParticles(), effect.shouldShowIcon())).collect(Collectors.toList());;
                PotionUtil.setCustomPotionEffects(extended, customPotion);
                tag.putInt("CustomPotionColor", PotionUtil.getColor(customPotion));
                world.setBlockState(pos, state.with(LeveledCauldronBlock.LEVEL, level - 1));
                stack.decrement(1);
                player.giveItemStack(extended);
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
}
