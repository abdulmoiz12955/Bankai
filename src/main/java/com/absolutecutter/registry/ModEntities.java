package com.absolutecutter.registry;

import com.absolutecutter.AbsoluteCutterMod;
import com.absolutecutter.entity.GetsugaEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AbsoluteCutterMod.MODID);

    public static final RegistryObject<EntityType<GetsugaEntity>> GETSUGA = ENTITIES.register("getsuga",
            () -> EntityType.Builder.<GetsugaEntity>of(GetsugaEntity::new, MobCategory.MISC)
                    .sized(1.0F, 1.0F)
                    .clientTrackingRange(10)
                    .updateInterval(1)
                    .fireImmune()
                    .noSave()
                    .noSummon()
                    .build("getsuga"));

    private ModEntities() {}
}
