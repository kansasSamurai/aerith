package org.progx.jogl.rendering;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

/**
 * ReflectedQuad
 *
 * @author Rick Wellman
 * @author aerith
 */
public class ReflectedQuad extends Quad {

    // reflection
    protected float fadeDistance = 0.8f;
    protected float reflectionTransparency = 3.8f;

    public ReflectedQuad(float x, float y, float z,
                         float width, float height,
                         BufferedImage textureImage) {
        super(x, y, z, width, height, textureImage);
    }

    public float getFadeDistance() {
        return fadeDistance;
    }

    public void setFadeDistance(float fadeDistance) {
        this.fadeDistance = fadeDistance;
    }

    public float getReflectionTransparency() {
        return reflectionTransparency;
    }

    public void setReflectionTransparency(float reflectionTransparency) {
        this.reflectionTransparency = reflectionTransparency;
    }

    // rendering
    @Override
    public void render(GL gl, boolean antiAliased) {
        float thisalpha = 1.0f;

        float[] crop = texture.getSubImageTextureCoords(textureCrop.x,
                                                        textureCrop.y,
                                                        textureCrop.x + textureCrop.width,
                                                        textureCrop.y + textureCrop.height);
        float tx1 = crop[0];
        float ty1 = crop[1];
        float tx2 = crop[2];
        float ty2 = crop[3];

        float thisx = -width / 2.0f;
        float thisy = -height / 2.0f;
        float thisz = 0.0f;

        gl.glEnable(GL.GL_BLEND);
        if (!antiAliased) {
            gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
        } else {
            gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
        }
        gl.glEnable(GL.GL_TEXTURE_2D);
        texture.bind(gl);
        gl.getGL2().glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);

        gl.getGL2().glBegin(GL2.GL_QUADS);

        // render solid/upright texture
        gl.getGL2().glColor4f(antiAliased ? 1 : thisalpha, antiAliased ? 1 : thisalpha, antiAliased ? 1 : thisalpha, thisalpha);
        gl.getGL2().glTexCoord2f(tx2, ty1);
        gl.getGL2().glVertex3f(thisx + width, thisy + height, thisz);
        gl.getGL2().glTexCoord2f(tx1, ty1);
        gl.getGL2().glVertex3f(thisx, thisy + height, thisz);
        gl.getGL2().glTexCoord2f(tx1, ty2);
        gl.getGL2().glVertex3f(thisx, thisy, thisz);
        gl.getGL2().glTexCoord2f(tx2, ty2);
        gl.getGL2().glVertex3f(thisx + width, thisy, thisz);

        thisalpha /= reflectionTransparency;

        gl.getGL2().glColor4f(antiAliased ? 1 : thisalpha, antiAliased ? 1 : thisalpha, antiAliased ? 1 : thisalpha, thisalpha);
        gl.getGL2().glTexCoord2f(tx2, ty2);
        gl.getGL2().glVertex3f(thisx + width, thisy, thisz);
        gl.getGL2().glTexCoord2f(tx1, ty2);
        gl.getGL2().glVertex3f(thisx, thisy, thisz);
        gl.getGL2().glColor4f(0.0f, 0.0f, 0.0f, 0.0f);
        gl.getGL2().glTexCoord2f(tx1, ty2 * (1 - fadeDistance));
        gl.getGL2().glVertex3f(thisx, thisy - (height * fadeDistance), thisz);
        gl.getGL2().glTexCoord2f(tx2, ty2 * (1 - fadeDistance));
        gl.getGL2().glVertex3f(thisx + width, thisy - (height * fadeDistance), thisz);

        gl.getGL2().glEnd();

        gl.glDisable(GL.GL_TEXTURE_2D);
        //gl.glDisable(GL.GL_BLEND);
    }
}