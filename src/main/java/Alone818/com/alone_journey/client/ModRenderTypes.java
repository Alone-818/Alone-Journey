package Alone818.com.alone_journey.client;

import Alone818.com.alone_journey.Alone_journey;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

import java.util.OptionalDouble;

/**
 * 模组自定义渲染类型
 *
 * THICK_LINES：加粗线框（线宽4像素，用于拉线引导线）
 */
public class ModRenderTypes extends RenderType {

    // 永不实例化，仅用于访问受保护的静态状态与 RenderType.create
    private ModRenderTypes() {
        super(null, null, null, 0, false, false, null, null);
    }

    public static final RenderType THICK_LINES = RenderType.create(
            Alone_journey.MODID + ":thick_lines",
            DefaultVertexFormat.POSITION_COLOR_NORMAL,
            VertexFormat.Mode.LINES,
            256, false, false,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_LINES_SHADER)
                    .setLineState(new LineStateShard(OptionalDouble.of(4.0D)))
                    .setLayeringState(VIEW_OFFSET_Z_LAYERING)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setOutputState(ITEM_ENTITY_TARGET)
                    .setWriteMaskState(COLOR_DEPTH_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
}
