package com.sun.javaone.aerith.g2d;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 * Reflection - Image Reflection Utilities
 *
 * @author Rick Wellman
 * @author aerith
 */
public final class Reflection {

    /**
     * Create a reflected image.
     *
     * @param avatar
     * @param alphaMask
     * @return
     */
    public static BufferedImage createReflectedPicture(BufferedImage avatar, BufferedImage alphaMask) {

        final int avatarWidth = avatar.getWidth();
        final int avatarHeight = avatar.getHeight();
        final BufferedImage buffer = createReflection(avatar, avatarWidth, avatarHeight);

        applyAlphaMask(buffer, alphaMask, avatarHeight);

        return buffer;/*.getSubimage(0, 0, avatarWidth, avatarHeight * 3 / 2)*/
    }

    /**
     * Apply an alpha mask to an image.
     *
     * @param buffer
     * @param alphaMask
     * @param avatarHeight
     */
    private static void applyAlphaMask(BufferedImage buffer, BufferedImage alphaMask, int avatarHeight) {
        final Graphics2D g2 = buffer.createGraphics(); {
            g2.setComposite(AlphaComposite.DstOut);
            g2.drawImage(alphaMask, null, 0, avatarHeight);
        } g2.dispose();
    }

    /**
     * Create a reflected image.
     *
     * @param avatar
     * @param avatarWidth
     * @param avatarHeight
     * @return
     */
    private static BufferedImage createReflection(BufferedImage avatar, int avatarWidth, int avatarHeight) {

        final BufferedImage buffer = new BufferedImage(avatarWidth, avatarHeight * 5 / 3, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = buffer.createGraphics(); {
            g.drawImage(avatar, null, null);
            g.translate(0, avatarHeight * 2);

            final AffineTransform reflectTransform = AffineTransform.getScaleInstance(1.0, -1.0);
            g.drawImage(avatar, reflectTransform, null);
        } g.dispose();

        return buffer;
    }

    /**
     * Create a default gradient mask for reflections. 0.7f > 1.0f
     *
     * @param avatarWidth
     * @param avatarHeight
     * @return
     */
    public static BufferedImage createGradientMask(int avatarWidth, int avatarHeight) {
        return createGradientMask(avatarWidth, avatarHeight, 0.7f, 1.0f);
    }

    /**
     * Create a gradient mask for reflections.
     *
     * @param avatarWidth
     * @param avatarHeight
     * @param opacityStart
     * @param opacityEnd
     * @return
     */
    public static BufferedImage createGradientMask(int avatarWidth, int avatarHeight, float opacityStart, float opacityEnd) {

        final BufferedImage gradient = new BufferedImage(avatarWidth, avatarHeight, BufferedImage.TYPE_INT_ARGB);

        final Graphics2D g = gradient.createGraphics(); {
            final GradientPaint painter = new GradientPaint(0.0f, 0.0f,
                                                      new Color(1.0f, 1.0f, 1.0f, opacityStart),
                                                      0.0f, avatarHeight / 2.0f,
                                                      new Color(1.0f, 1.0f, 1.0f, opacityEnd));
            g.setPaint(painter);
            g.fill(new Rectangle2D.Double(0, 0, avatarWidth, avatarHeight));
        } g.dispose();

        return gradient;
    }

}
