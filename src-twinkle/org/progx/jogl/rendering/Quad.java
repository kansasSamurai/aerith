package org.progx.jogl.rendering;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import org.progx.jogl.Texture;

/**
 * Quad - A quadrilateral for rendering on 3D surfaces;
 * includes properties for Textures, Alpha, etc.
 *
 * @author Rick Wellman
 * @author aerith
 */
public class Quad extends Renderable {

    // texture
    protected BufferedImage textureImage = null;
    protected Texture texture = null;
    protected Rectangle textureCrop = null;

    // geometry
    protected float width, height;

    // alpha
    protected float alpha = 1.0f;

    public Quad(float x, float y, float z,
                float width, float height,
                BufferedImage textureImage) {
        super(x, y, z);
        setDimension(width, height);
        setTextureImage(textureImage);
    }

    public void setDimension(float width, float height) {
        this.width = width;
        this.height = height;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getAlpha() {
        return alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
    }

    public void setTextureImage(BufferedImage textureImage) {
        if (textureImage == null) {
            throw new IllegalArgumentException("Quad texture cannot be null.");
        }

        this.textureImage = textureImage;
        setTextureCrop(null);
    }

    public Rectangle getTextureCrop() {
        return textureCrop;
    }

    public void setTextureCrop(Rectangle textureCrop) {
        if (textureCrop == null) {
            textureCrop = new Rectangle(0, 0,
                                        textureImage.getWidth(),
                                        textureImage.getHeight());
        }

        this.textureCrop = textureCrop;
    }

    @Override public void init(GL gl) {
        texture = Texture.getInstance(gl, textureImage, true);
    }

    @Override public void dispose(GL gl) {
        texture.dispose(gl);
        textureImage = null;
    }

    // rendering
    @Override public void render(GL gl, boolean antiAliased) {
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

        if (alpha < 1.0f) {
            gl.glEnable(GL.GL_BLEND);
            if (!antiAliased) {
                gl.glBlendFunc(GL.GL_ONE, GL.GL_ONE_MINUS_SRC_ALPHA);
            } else {
                gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
            }
        }
        gl.glEnable(GL.GL_TEXTURE_2D);
        texture.bind(gl);
        gl.getGL2().glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_MODE, GL2.GL_MODULATE);

        gl.getGL2().glBegin(GL2.GL_QUADS);

        // render solid/upright texture
        gl.getGL2().glColor4f(antiAliased ? 1 : alpha, antiAliased ? 1 : alpha, antiAliased ? 1 : alpha, alpha);
        gl.getGL2().glTexCoord2f(tx2, ty1);
        gl.getGL2().glVertex3f(thisx + width, thisy + height, thisz);
        gl.getGL2().glTexCoord2f(tx1, ty1);
        gl.getGL2().glVertex3f(thisx, thisy + height, thisz);
        gl.getGL2().glTexCoord2f(tx1, ty2);
        gl.getGL2().glVertex3f(thisx, thisy, thisz);
        gl.getGL2().glTexCoord2f(tx2, ty2);
        gl.getGL2().glVertex3f(thisx + width, thisy, thisz);

        gl.getGL2().glEnd();

        gl.glDisable(GL.GL_TEXTURE_2D);
        if (alpha < 1.0f) {
            //gl.glDisable(GL.GL_BLEND);
        }
    }
}