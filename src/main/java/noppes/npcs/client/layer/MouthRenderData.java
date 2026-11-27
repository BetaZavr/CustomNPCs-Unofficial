package noppes.npcs.client.layer;

import noppes.npcs.client.model.animation.EmotionFrame;

/**
 * Render data for mouth rendering.
 * Contains position, scale, rotation and color for each mouth part vertex.
 */
public class MouthRenderData {

    public boolean show = true;
    public boolean isRandom = false;

    // Upper lip vertices (4 vertices: TL, TR, BR, BL)
    // Each vertex: [offsetX, offsetY, scaleX, scaleY, rotation]
    public final float[][] upperLip = new float[4][5];
    // Lower lip vertices (4 vertices: TL, TR, BR, BL)
    public final float[][] lowerLip = new float[4][5];
    // Mouth cavity vertices (4 vertices: TL, TR, BR, BL)
    public final float[][] mouthCavity = new float[4][5];

    // Colors
    public final float[] upperLipColor = new float[] { 0.8f, 0.2f, 0.2f };
    public final float[] lowerLipColor = new float[] { 0.8f, 0.2f, 0.2f };
    public final float[] cavityColor = new float[] { 0.4f, 0.05f, 0.05f };

    // Random talk state
    public float randomTalkOffsetX = 0.0f;
    public float randomTalkOffsetY = 0.0f;
    public float randomTalkScaleX = 1.0f;
    public float randomTalkScaleY = 1.0f;
    public float randomTalkRot = 0.0f;
    public int randomTalkSeed = 0;

    public MouthRenderData() {
        // Initialize all vertices with default values
        for (int i = 0; i < 4; i++) {
            upperLip[i] = new float[] { 0.0f, 0.0f, 1.0f, 1.0f, 0.0f };
            lowerLip[i] = new float[] { 0.0f, 0.0f, 1.0f, 1.0f, 0.0f };
            mouthCavity[i] = new float[] { 0.0f, 0.0f, 1.0f, 1.0f, 0.0f };
        }
    }

    /**
     * Update mouth render data from emotion frame.
     * @param frame the current emotion frame (null = no emotion)
     * @param isRandom whether to use random talk animation
     * @param worldTime current world time for random talk variation
     */
    public void update(EmotionFrame frame, boolean isRandom, long worldTime) {
        this.isRandom = isRandom;

        if (frame == null) {
            show = false;
            return;
        }

        show = frame.showMouth;
        if (!show) { return; }

        if (isRandom) {
            updateRandomTalk(worldTime);
        } else {
            updateFromFrame(frame);
        }
    }

    private void updateFromFrame(EmotionFrame frame) {
        // Base mouth position (centered below eyes)
        float baseX = 0.0f;
        float baseY = 1.5f;

        // Apply global offset/scale/rotation from emotion frame
        float offsetX = baseX + frame.offsetMouth[0];
        float offsetY = baseY + frame.offsetMouth[1];
        float scaleX = frame.scaleMouth[0];
        float scaleY = frame.scaleMouth[1];
        float rot = frame.rotMouth;

        // Upper lip: positioned above the cavity, curves upward
        float ulWidth = 1.2f * scaleX;
        float ulHeight = 0.25f * scaleY;
        upperLip[0] = new float[] { offsetX - ulWidth * 0.5f, offsetY - ulHeight * 1.5f, scaleX, scaleY, rot }; // TL
        upperLip[1] = new float[] { offsetX + ulWidth * 0.5f, offsetY - ulHeight * 1.5f, scaleX, scaleY, rot }; // TR
        upperLip[2] = new float[] { offsetX + ulWidth * 0.4f, offsetY - ulHeight * 0.3f, scaleX, scaleY, rot }; // BR (inner)
        upperLip[3] = new float[] { offsetX - ulWidth * 0.4f, offsetY - ulHeight * 0.3f, scaleX, scaleY, rot }; // BL (inner)

        // Lower lip: positioned below the cavity, curves downward
        float llWidth = 1.2f * scaleX;
        float llHeight = 0.3f * scaleY;
        lowerLip[0] = new float[] { offsetX - llWidth * 0.4f, offsetY + llHeight * 0.3f, scaleX, scaleY, rot }; // TL (inner)
        lowerLip[1] = new float[] { offsetX + llWidth * 0.4f, offsetY + llHeight * 0.3f, scaleX, scaleY, rot }; // TR (inner)
        lowerLip[2] = new float[] { offsetX + llWidth * 0.5f, offsetY + llHeight * 1.5f, scaleX, scaleY, rot }; // BR
        lowerLip[3] = new float[] { offsetX - llWidth * 0.5f, offsetY + llHeight * 1.5f, scaleX, scaleY, rot }; // BL

        // Mouth cavity: between upper and lower lip
        float cavWidth = 1.0f * scaleX;
        float cavHeight = 0.4f * scaleY;
        mouthCavity[0] = new float[] { offsetX - cavWidth * 0.5f, offsetY - cavHeight * 0.5f, scaleX, scaleY, rot }; // TL
        mouthCavity[1] = new float[] { offsetX + cavWidth * 0.5f, offsetY - cavHeight * 0.5f, scaleX, scaleY, rot }; // TR
        mouthCavity[2] = new float[] { offsetX + cavWidth * 0.5f, offsetY + cavHeight * 0.5f, scaleX, scaleY, rot }; // BR
        mouthCavity[3] = new float[] { offsetX - cavWidth * 0.5f, offsetY + cavHeight * 0.5f, scaleX, scaleY, rot }; // BL
    }

    private void updateRandomTalk(long worldTime) {
        // Generate deterministic random values based on world time
        // This creates a talking animation independent of eye emotion frames
        if (randomTalkSeed != (int) (worldTime / 10)) {
            randomTalkSeed = (int) (worldTime / 10);
            randomTalkOffsetX = (float) Math.sin(worldTime * 0.05) * 0.1f;
            randomTalkOffsetY = (float) Math.cos(worldTime * 0.08) * 0.15f;
            randomTalkScaleY = 1.0f + (float) Math.sin(worldTime * 0.12) * 0.3f; // Mouth opens/closes
            randomTalkRot = (float) Math.sin(worldTime * 0.06) * 5.0f;
        }

        // Base mouth position
        float baseX = 0.0f;
        float baseY = 1.5f;

        // Apply random talk transforms
        float offsetX = baseX + randomTalkOffsetX;
        float offsetY = baseY + randomTalkOffsetY;
        float scaleX = 1.0f;
        float scaleY = randomTalkScaleY;
        float rot = randomTalkRot;

        // Upper lip
        float ulWidth = 1.2f * scaleX;
        float ulHeight = 0.2f * scaleY;
        upperLip[0] = new float[] { offsetX - ulWidth * 0.5f, offsetY - ulHeight * 1.2f, scaleX, scaleY, rot };
        upperLip[1] = new float[] { offsetX + ulWidth * 0.5f, offsetY - ulHeight * 1.2f, scaleX, scaleY, rot };
        upperLip[2] = new float[] { offsetX + ulWidth * 0.4f, offsetY - ulHeight * 0.2f, scaleX, scaleY, rot };
        upperLip[3] = new float[] { offsetX - ulWidth * 0.4f, offsetY - ulHeight * 0.2f, scaleX, scaleY, rot };

        // Lower lip (more variation for talking)
        float llWidth = 1.2f * scaleX;
        float llHeight = 0.35f * scaleY;
        lowerLip[0] = new float[] { offsetX - llWidth * 0.4f, offsetY + llHeight * 0.2f, scaleX, scaleY, rot };
        lowerLip[1] = new float[] { offsetX + llWidth * 0.4f, offsetY + llHeight * 0.2f, scaleX, scaleY, rot };
        lowerLip[2] = new float[] { offsetX + llWidth * 0.5f, offsetY + llHeight * 1.2f, scaleX, scaleY, rot };
        lowerLip[3] = new float[] { offsetX - llWidth * 0.5f, offsetY + llHeight * 1.2f, scaleX, scaleY, rot };

        // Mouth cavity (opens/closes with speech)
        float cavWidth = 1.0f * scaleX;
        float cavHeight = 0.3f * scaleY;
        mouthCavity[0] = new float[] { offsetX - cavWidth * 0.5f, offsetY - cavHeight * 0.5f, scaleX, scaleY, rot };
        mouthCavity[1] = new float[] { offsetX + cavWidth * 0.5f, offsetY - cavHeight * 0.5f, scaleX, scaleY, rot };
        mouthCavity[2] = new float[] { offsetX + cavWidth * 0.5f, offsetY + cavHeight * 0.5f, scaleX, scaleY, rot };
        mouthCavity[3] = new float[] { offsetX - cavWidth * 0.5f, offsetY + cavHeight * 0.5f, scaleX, scaleY, rot };
    }

    /**
     * Draw a single quadrilateral (quad) using the vertex data.
     * Vertices are stored as: [0]=offsetX, [1]=offsetY, [2]=scaleX, [3]=scaleY, [4]=rotation
     * The quad is defined by 4 vertices in order: TL, TR, BR, BL
     */
    public void drawQuad(float[][] vertices, float[] color, float z) {
        if (vertices == null || color == null) { return; }

        float alpha = (color[0] + color[1] + color[2]) / 3.0f * 0.5f; // Semi-transparent
        if (alpha <= 0.0f) { alpha = 1.0f; }

        net.minecraft.client.renderer.GlStateManager.color(2.0f, 2.0f, 2.0f, 1.0f);
        net.minecraft.client.renderer.BufferBuilder buffer = 
            net.minecraft.client.renderer.Tessellator.getInstance().getBuffer();
        buffer.begin(7, net.minecraft.client.renderer.vertex.DefaultVertexFormats.POSITION_COLOR);

        for (int i = 0; i < 4; i++) {
            float[] v = vertices[i];
            float vx = v[0];
            float vy = v[1];
            buffer.pos(vx, vy, z)
                .color(color[0], color[1], color[2], alpha)
                .endVertex();
        }

        net.minecraft.client.renderer.Tessellator.getInstance().draw();
    }
}
