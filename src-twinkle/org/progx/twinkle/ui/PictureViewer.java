package org.progx.twinkle.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.vecmath.Point3f;
import javax.vecmath.Point3i;

import org.jdesktop.swingx.util.ShadowFactory;
import org.progx.jogl.CompositeGLPanel;
import org.progx.jogl.GLUtilities;
import org.progx.jogl.rendering.ReflectedQuad;
import org.progx.jogl.rendering.Renderable;
import org.progx.jogl.rendering.RenderableFactory;
import org.progx.math.equation.Equation;
import org.progx.twinkle.Debug;
import org.progx.twinkle.equation.AnimationEquation;

/**
 * PictureViewer
 *
 * @author Rick Wellman
 * @author aerith
 *
 */
public class PictureViewer extends CompositeGLPanel {

    // Action Keys
    public static final String KEY_ACTION_SHOW_PICTURE = "show";
    public static final String KEY_ACTION_NEXT_PICTURE = "next";
    public static final String KEY_ACTION_PREVIOUS_PICTURE = "previous";

    private static final boolean TWINKLE_ANTI_ALIAS = System.getProperty("twinkle.aa") == null;
    static {
        System.out.println("[TWINKLE] Use anti aliasing: " + TWINKLE_ANTI_ALIAS);
    }

    private static final boolean TWINKLE_TRANSPARENT = System.getProperty("twinkle.transparent") != null;
    static {
        System.out.println("[TWINKLE] Transparent: " + TWINKLE_TRANSPARENT);
    }

    // Quad Width
    private static final float QUAD_WIDTH = 65.0f;

    // Thumbnails
    private static final int THUMB_WIDTH = 48;
    private static final double SELECTED_THUMB_RATIO = 0.35;
    private static final double SELECTED_THUMB_EXTRA_WIDTH = THUMB_WIDTH * SELECTED_THUMB_RATIO;

    // Renderable
    private static final int INDEX_LEFT_PICTURE = 0;
    private static final int INDEX_SELECTED_PICTURE = 1;
    private static final int INDEX_NEXT_PICTURE = 2;
    private static final int INDEX_RIGHT_PICTURE = 3;
    private final Renderable[] renderables = new Renderable[4];
    private final Queue<Renderable> initQuadsQueue = new ConcurrentLinkedQueue<Renderable>();
    private final Queue<Renderable> disposeQuadsQueue = new ConcurrentLinkedQueue<Renderable>();

    private final List<Picture> pictures = Collections.synchronizedList(new ArrayList<Picture>());

    private final float camPosX = 0.0f;
    private final float camPosY = 0.0f;
    private final float camPosZ = 100.0f;

    private int picturesStripHeight = 0;

    private BufferedImage textImage = null;
    private BufferedImage nextTextImage = null; // small image that is next (none/null if last in album)
    private final Font textFont;
    private float textAlpha = 1.0f;
    private final Color grayColor = new Color(0xE1E1E1);

    private final ShadowFactory shadowFactory = new ShadowFactory(11, 1.0f, Color.BLACK);

    // Index of selected picture
    private int idxSelectedPicture = -1;

    // Index of next picture
    private int idxNextPicture = -1;

    private boolean pictureIsShowing = false;

    // ... used for animation
    private final Equation curve = new AnimationEquation(3.6, -1.0);
    //(2.8, -0.98); // original; ok, but a little klunky
    //(3.6, -1.0); // this is better
    //(6.0, -2.0); // experimental; this is terrible

    // A timer for animation sequences
    private Timer animator;

    private boolean antiAliasing = TWINKLE_ANTI_ALIAS;

    private boolean stopRendering;

    private final Object animLock = new Object();

    public PictureViewer() {
        super(!TWINKLE_TRANSPARENT, /*true*/false);

        this.setPreferredSize(new Dimension(640, 480));
        this.addMouseWheelListener(new MouseWheelDriver());
        this.setFocusable(true);

        registerActions();

        textFont = getFont().deriveFont(Font.BOLD, 32.0f);

        createButtons();
    }

    public boolean isAntiAliasing() {
        return antiAliasing;
    }

    public void setAntiAliasing(boolean antiAliasing) {
        this.antiAliasing = antiAliasing && TWINKLE_ANTI_ALIAS;
        repaint();
    }

    public void addPicture(final String name, final BufferedImage image) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override public void run() {

                final Picture picture = new Picture(name, image);

                pictures.add(picture);
                final int size = pictures.size();
                if (size == 1) {
                    initQuadsQueue.add(createQuad(INDEX_SELECTED_PICTURE, 0));
                } else if (size - 1 == idxSelectedPicture + 1) {
                    initQuadsQueue.add(createQuad(INDEX_NEXT_PICTURE, 1));
                } else if (size - 1 == idxNextPicture + 1) {
                    initQuadsQueue.add(createQuad(INDEX_RIGHT_PICTURE, 2));
                }

                final float ratio = picture.getRatio();
                picturesStripHeight = Math.max(picturesStripHeight, (int) (THUMB_WIDTH + SELECTED_THUMB_EXTRA_WIDTH / ratio));

                getActionMap().get(KEY_ACTION_SHOW_PICTURE).setEnabled(idxSelectedPicture >= 0);
                getActionMap().get(KEY_ACTION_NEXT_PICTURE).setEnabled(idxSelectedPicture < size - 1);
                getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE).setEnabled(idxSelectedPicture > 0);

                repaint();
            }
        });
        // repaint(); // RW Q: Should this be here? I have commented out in the meantime
    }

    public void dispose() {
        stopRendering = true;
        System.out.println("dispose()");

        // dispose of quads queue
        for (Renderable renderable : renderables) {
            if (renderable != null) {
                disposeQuadsQueue.add(renderable);
            }
        }

        // dispose of pictures (and their buffered images)
        for (Picture picture : pictures) {
            picture.getImage().flush();
        }
        pictures.clear();

        repaint();
    }

    public void showSelectedPicture() {
        if (animator != null && animator.isRunning()) {
            System.out.println("WARN: Animator is running");
            return;
        }

        pictureIsShowing = !pictureIsShowing;
        ((ShowPictureAction) getActionMap().get(KEY_ACTION_SHOW_PICTURE)).toggleName();

        animator = new Timer(1000 / 60, new ZoomAnimation());
        animator.start();
    }

    public void nextPicture() {
        int size = pictures.size() - 1;
        if (idxSelectedPicture < size) {
            showPicture(true);
        }
    }

    public void previousPicture() {
        if (idxSelectedPicture > 0) {
            showPicture(false);
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        super.init(drawable);

        final GL gl = drawable.getGL();
        initQuads(gl);
    }

    private void registerActions() {
        KeyStroke stroke;
        Action action;

        InputMap inputMap = getInputMap();
        ActionMap actionMap = getActionMap();

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
        inputMap.put(stroke, KEY_ACTION_NEXT_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
        inputMap.put(stroke, KEY_ACTION_NEXT_PICTURE);

        action = new NextPictureAction();
        actionMap.put(KEY_ACTION_NEXT_PICTURE, action);

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
        inputMap.put(stroke, KEY_ACTION_PREVIOUS_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
        inputMap.put(stroke, KEY_ACTION_PREVIOUS_PICTURE);

        action = new PreviousPictureAction();
        actionMap.put(KEY_ACTION_PREVIOUS_PICTURE, action);

        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0);
        inputMap.put(stroke, KEY_ACTION_SHOW_PICTURE);
        stroke = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        inputMap.put(stroke, KEY_ACTION_SHOW_PICTURE);

        action = new ShowPictureAction();
        actionMap.put(KEY_ACTION_SHOW_PICTURE, action);
    }

    private void createButtons() {
        ControlPanel buttonsPanel = new ControlPanel();

        ControlButton button;
        button = new ControlButton(getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE));
        buttonsPanel.add(button);
        button = new ControlButton(getActionMap().get(KEY_ACTION_SHOW_PICTURE));
        buttonsPanel.add(button);
        button = new ControlButton(getActionMap().get(KEY_ACTION_NEXT_PICTURE));
        buttonsPanel.add(button);

        setLayout(new GridBagLayout());
        add(Box.createGlue(), new GridBagConstraints(0, 0,
                                                     2, 1,
                                                     0.0, 1.0,
                                                     GridBagConstraints.LINE_START,
                                                     GridBagConstraints.VERTICAL,
                                                     new Insets(0, 0, 0, 0),
                                                     0, 0));
        add(buttonsPanel, new GridBagConstraints(0, 1,
                                                 1, 1,
                                                 0.0, 0.0,
                                                 GridBagConstraints.LINE_START,
                                                 GridBagConstraints.NONE,
                                                 new Insets(0, 13, 13, 0),
                                                 0, 0));
        add(Box.createHorizontalGlue(), new GridBagConstraints(1, 1,
                                                               1, 1,
                                                               1.0, 0.0,
                                                               GridBagConstraints.LINE_START,
                                                               GridBagConstraints.HORIZONTAL,
                                                               new Insets(0, 0, 0, 0),
                                                               0, 0));
    }

    private void showPicture(final boolean next) {
        System.out.println("showPicture(), next             > " + next);

        if (animator != null && animator.isRunning()) {
            System.out.println("WARN: Animator is running");
            return;
        }

        System.out.println("showPicture(), pictureIsShowing > " +pictureIsShowing);
        if (pictureIsShowing) {
            new Thread(new Runnable() {
                /** @noinspection BusyWait*/
                @Override public void run() {
                    showSelectedPicture();
                    while (animator.isRunning()) {
                        synchronized(animLock) {
                            try {
                               animLock.wait();
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    }
                    showPicture(next);
                    while (animator.isRunning()) {
                        synchronized(animLock) {
                            try {
                               animLock.wait();
                            } catch (InterruptedException ie) {
                                ie.printStackTrace();
                            }
                        }
                    }
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            showSelectedPicture();
                        }
                    });
                }
            }).start();
            return;
        }

        animator = new Timer(2, new SlideAnimation(next)); // 10 := original value
        animator.start();
    }

    private Renderable createQuad(int index, int pictureNumber) {
        final Picture picture = pictures.get(pictureNumber);
        if (picture == null || index > renderables.length) {
            return null;
        }

        float ratio = picture.getRatio();
        int width = (int) QUAD_WIDTH;
        int height = (int) (QUAD_WIDTH / ratio);
        if (ratio < 1.0f) {
            height = (int) (QUAD_WIDTH / 1.5);
            width = (int) (height * ratio);
        }

        final Renderable quad = RenderableFactory.createReflectedQuad(0.0f, 0.0f, 0.0f,
                                                                width, height,
                                                                picture.getImage(), null,
                                                                picture.getName());
        renderables[index] = quad;

        switch (index) {
            case INDEX_SELECTED_PICTURE:
                idxSelectedPicture = pictureNumber;
                quad.setPosition(-7.0f, 0.0f, 0.0f);
                quad.setRotation(0, 30, 0);
                textImage = generateTextImage(picture);
                break;
            case INDEX_NEXT_PICTURE:
                idxNextPicture = pictureNumber;
                quad.setScale(0.5f, 0.5f, 0.5f);
                quad.setPosition(36.0f, -height / 2.0f, 30.0f);
                quad.setRotation(0, -20, 0);
                break;
            case INDEX_RIGHT_PICTURE:
                quad.setScale(0.5f, 0.5f, 0.5f);
                quad.setPosition(196.0f, -height / 2.0f, 30.0f);
                quad.setRotation(0, -20, 0);
                break;
            case INDEX_LEFT_PICTURE:
                quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f, 0.0f, 0.0f);
                quad.setRotation(0, 30, 0);
                break;
            default:
                break;
        }

        return quad;
    }

    private BufferedImage generateTextImage(Picture picture) {

        final FontRenderContext context = getFontMetrics(textFont).getFontRenderContext();
        //Graphics2D globalGraphics = (Graphics2D) getGraphics();
        //globalGraphics.setFont(textFont);
        //FontRenderContext context = (globalGraphics).getFontRenderContext();
        GlyphVector vector = textFont.createGlyphVector(context, picture.getName());
        Rectangle bounds = vector.getPixelBounds(context, 0.0f, 0.0f);
        TextLayout layout = new TextLayout(picture.getName(), textFont, context);

        BufferedImage image = new BufferedImage((int) (bounds.getWidth()),
                                                (int) (layout.getAscent() + layout.getDescent() + layout.getLeading()),
                                                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
        layout.draw(g2, 0, layout.getAscent());
        g2.dispose();

        final BufferedImage shadow = shadowFactory.createShadow(image);
        final BufferedImage composite = new BufferedImage(shadow.getWidth(),
                                                    shadow.getHeight(),
                                                    BufferedImage.TYPE_INT_ARGB);
        g2 = composite.createGraphics();
        g2.drawImage(shadow, null,
                     -1 - (shadow.getWidth() - image.getWidth()) / 2,
                     2 - (shadow.getHeight() - image.getHeight()) / 2);
        g2.drawImage(image, null, 0, 0);
        g2.dispose();

        shadow.flush();
        image.flush();

        return composite;
    }

    @Override
    protected void render2DBackground(Graphics g) {
        // NOTE: with antialiasing on the accum buffer creates a black backround
//        if (!antiAliasing) {
//            float h = getHeight() * 0.55f;
//
//            GradientPaint paint = new GradientPaint(0.0f, h, Color.BLACK,
//                                                    0.0f, getHeight(), new Color(0x4C4C4C));
//            Graphics2D g2 = (Graphics2D) g;
//            Paint oldPaint = g2.getPaint();
//            g2.setPaint(paint);
//            g2.fillRect(0, 0, getWidth(), getHeight());
//            g2.setPaint(oldPaint);
//        }

        if (!TWINKLE_TRANSPARENT) {
            g.setColor(Color.BLACK);
            Rectangle clip = g.getClipBounds();
            g.fillRect(clip.x, clip.y, clip.width, clip.height);
        }
    }

    @Override
    protected void render2DForeground(Graphics g) {
        final Graphics2D g2 = (Graphics2D) g;
        setupForegroundGraphics(g2);

        //paintPicturesStrip(g2);
        paintInfo(g2);
    }

    private void paintInfo(Graphics2D g2) {
        g2.setColor(Color.WHITE);

        if (Debug.isDebug()) {
            g2.drawString("X: " + camPosX, 5, 15);
            g2.drawString("Y: " + camPosY, 5, 30);
            g2.drawString("Z: " + camPosZ, 5, 45);
        }

        if (textImage != null) {
            Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, textAlpha));
            g2.drawImage(textImage, null,
                         (getWidth() - textImage.getWidth()) / 2,
                         (int) (getHeight() - textFont.getSize() * 1.7));
            g2.setComposite(composite);
        }
    }

    private static void setupForegroundGraphics(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    }

    @Override
    protected void render3DScene(final GL gl, final GLU glu) {
        //System.out.println("render3DScene()");
        if (stopRendering) {
            //System.out.println("render3DScene(), stopRendering");
            initAndDisposeQuads(gl);
            return;
        }

        initScene(gl);
        initAndDisposeQuads(gl);

        final Renderable scene = new Renderable() {
            @Override public Point3f getPosition() {
                return null;
            }

            @Override public void render(GL gl, boolean antiAliased) {
                setupCamera(gl, glu);
                renderItems(gl, antiAliased);
            }

            @Override public void init(GL gl) {
            }
        };

        if (isAntiAliasing()) {
            GLUtilities.renderAntiAliased(gl, scene);
        } else {
            scene.render(gl, false);
        }
    }

    private void initQuads(GL gl) {
        for (Renderable item: renderables) {
            if (item != null) {
                item.init(gl);
            }
        }
    }

    private void initAndDisposeQuads(final GL gl) {
        //System.out.println("initAndDisposeQuads()");

        while (!initQuadsQueue.isEmpty()) {
            final Renderable quad = initQuadsQueue.poll();
            if (quad != null) {
                quad.init(gl);
            }
        }

        while (!disposeQuadsQueue.isEmpty()) {
            final Renderable quad = disposeQuadsQueue.poll();
            if (quad != null) {
                quad.dispose(gl);
            }
        }
    }

    private static void initScene(GL gl) {
        //System.out.println("initScene()");
        // [1] gl.glMatrixMode(GL.GL_MODELVIEW));
        // [1] gl.glLoadIdentity();
        gl.getGL2().glMatrixMode(GL2.GL_MODELVIEW);
        gl.getGL2().glLoadIdentity();
    }

    private void setupCamera(GL gl, GLU glu) {
        glu.gluLookAt(camPosX, camPosY, camPosZ,
                      0.0f, 0.0f, 0.0f,
                      0.0f, 1.0f, 0.0f);
        gl.getGL2().glTranslatef(0.0f, -1.0f, 0.0f);
    }

    private void renderItems(GL gl, boolean antiAliased) {
        for (Renderable renderable: renderables) {
            setAndRender(gl, renderable, antiAliased);
        }
    }

    private static void setAndRender(GL gl, Renderable renderable, boolean antiAliased) {
        if (renderable == null) {
            return;
        }

        final Point3f pos = renderable.getPosition();
        final Point3i rot = renderable.getRotation();
        final Point3f scale = renderable.getScale();

        gl.getGL2().glPushMatrix();
            gl.getGL2().glScalef(scale.x, scale.y, scale.z);
            gl.getGL2().glTranslatef(pos.x, pos.y + 4.0f, pos.z);
            gl.getGL2().glRotatef(rot.x, 1.0f, 0.0f, 0.0f);
            gl.getGL2().glRotatef(rot.y, 0.0f, 1.0f, 0.0f);
            gl.getGL2().glRotatef(rot.z, 0.0f, 0.0f, 1.0f);
            renderable.render(gl, antiAliased);
        gl.getGL2().glPopMatrix();
    }

    private final class ZoomAnimation implements ActionListener {

        private static final int ANIM_DELAY = 800 * 3; // 400 := original value

        private final long start;

        private ZoomAnimation() {
            start = System.currentTimeMillis();
        }

        @Override public void actionPerformed(ActionEvent e) {
            System.out.println("ZoomAnimation");

            long elapsed = System.currentTimeMillis() - start;
            if (elapsed >= ANIM_DELAY) {
                Timer timer = (Timer) e.getSource();
                timer.stop();
                synchronized (animLock) {
                    animLock.notifyAll();
                }
            } else {
                double factor = (double) elapsed / (double) ANIM_DELAY;
                animateQuads(curve.compute(factor));
            }
            repaint();
        }

        private void animateQuads(double factor) {
            if (!pictureIsShowing) {
                factor = 1.0 - factor;
            }
            System.out.println("animateQuads(), p > " + pictureIsShowing + " < factor > " + Double.toString(factor));

            Renderable quad = renderables[INDEX_SELECTED_PICTURE];
            Point3f position = quad.getPosition();

            quad.setRotation(0, (int) (30.0 * (1.0 - factor)), 0);
            quad.setPosition((float) (-7.0f * (1.0 - factor)),
                             position.y,
                             (float) (30.0 * factor));

            quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) {
                position = quad.getPosition();
                quad.setPosition(36.0f + (float) (120.0f * factor),
                                 position.y,
                                 position.z);
            }
        }
    }

    private final class SlideAnimation implements ActionListener {

        private static final int ANIM_DELAY = 800 * 10;

        // If true, animate to the next pic.  If false, animate to the prev pic.
        private final boolean next;

        private final long start;

        private SlideAnimation(boolean next) {
            this.next = next;
            this.start =  System.currentTimeMillis();

            final int idxToShow = next ? idxNextPicture : (idxSelectedPicture - 1);
            final boolean discriminator = next
                    ? (idxNextPicture < pictures.size())
                    : (idxSelectedPicture > 0);
            nextTextImage =  discriminator ? generateTextImage(pictures.get(idxToShow)) : null;
// Replaced this logic with the logic above
//            if (next) {
//                if (idxNextPicture < pictures.size()) {
//                    nextTextImage = generateTextImage(pictures.get(idxNextPicture));
//                } else {
//                    nextTextImage = null;
//                }
//            } else {
//                if (idxSelectedPicture > 0) {
//                    nextTextImage = generateTextImage(pictures.get(idxSelectedPicture - 1));
//                } else {
//                    nextTextImage = null;
//                }
//            }
        }

        @Override public void actionPerformed(ActionEvent e) {
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed >= ANIM_DELAY) {
                final Timer timer = (Timer) e.getSource();
                timer.stop();

                if (next) {
                    selectNextPicture();
                } else {
                    selectPreviousPicture();
                }

                Action action;
                action = getActionMap().get(KEY_ACTION_NEXT_PICTURE);
                action.setEnabled(idxSelectedPicture < pictures.size() - 1);

                action = getActionMap().get(KEY_ACTION_PREVIOUS_PICTURE);
                action.setEnabled(idxSelectedPicture > 0 && pictures.size() > 1);

                synchronized (animLock) {
                    animLock.notifyAll();
                }
            } else {
                final double factor = (double) elapsed / (double) ANIM_DELAY;
                final double curvedFactor = curve.compute(factor);

                if (next) {
                    animateQuadsNext(curvedFactor);
                } else {
                    animateQuadsPrevious(1.0 - curvedFactor);
                }

                setTextAlpha(elapsed, factor);
            }

            repaint();
        }

        private void animateQuadsNext(double factor) {
            // System.out.println("AQN > " + Double.toString(factor));

            Renderable quad;
            Point3f position;

            quad = renderables[INDEX_SELECTED_PICTURE];
            position = quad.getPosition();
            quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f * (float) factor, position.y, position.z);

            final ReflectedQuad reflected = (ReflectedQuad) renderables[INDEX_NEXT_PICTURE];
            if (reflected != null) {
                final float scale = 0.5f + 0.5f * (float) factor; // RW Q: Why 0.5f + 0.5f? Why not 1.0f?

                reflected.setScale(scale, scale, scale);
                reflected.setRotation(0, (int) (-20.0 + 50.0 * factor), 0);
                reflected.setPosition((float) (36.0f - 43.0f * factor),
                                      -reflected.getHeight() * (1.0f - scale),
                                      (float) (30.0 * (1.0 - factor)));
            }

            quad = renderables[INDEX_RIGHT_PICTURE];
            if (quad != null) {
                position = quad.getPosition();
                quad.setPosition(36.0f + 160.0f * (float) (1.0 - factor), position.y, position.z);
            }
        }

        private void animateQuadsPrevious(double factor) {
            // System.out.println("AQP > " + Double.toString(factor));

            final float scale = 0.5f + 0.5f * (float) factor;

            final ReflectedQuad reflected = (ReflectedQuad) renderables[INDEX_SELECTED_PICTURE];
            reflected.setScale(scale, scale, scale);
            reflected.setRotation(0, (int) (-20.0 + 50.0 * factor), 0);
            reflected.setPosition((float) (36.0f - 43.0f * factor),
                                  -reflected.getHeight() * (1.0f - scale),
                                  (float) (30.0 * (1.0 - factor)));

            Renderable quad;
            quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) {
                Point3f position = quad.getPosition();
                quad.setPosition(36.0f + 160.0f * (float) (1.0 - factor), position.y, position.z);
            }

            quad = renderables[INDEX_LEFT_PICTURE];
            if (quad != null) {
                Point3f position = quad.getPosition();
                quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f * (float) factor, position.y, position.z);
            }
        }

        private void setTextAlpha(long elapsed, double factor) {
            if (elapsed < ANIM_DELAY / 2.0) {
                textAlpha = (float) (1.0 - 2.0 * factor);
            } else {
                textAlpha = (float) ((factor - 0.5) * 2.0);
                if (textAlpha > 1.0f) {
                    textAlpha = 1.0f;
                }
            }
            if (textAlpha < 0.1f) {
                textAlpha = 0.1f;
                textImage = nextTextImage;
            }
        }

        private void selectPreviousPicture() {
            idxSelectedPicture--;
            idxNextPicture--;

            if (renderables[INDEX_RIGHT_PICTURE] != null) {
                disposeQuadsQueue.add(renderables[INDEX_RIGHT_PICTURE]);
            }

            Renderable quad;
            quad = renderables[INDEX_NEXT_PICTURE];
            if (quad != null) {
                renderables[INDEX_RIGHT_PICTURE] = quad;
                quad.setScale(0.5f, 0.5f, 0.5f);
                quad.setPosition(196.0f, -((ReflectedQuad) quad).getHeight() / 2.0f, 30.0f);
                quad.setRotation(0, -20, 0);
            }

            quad = renderables[INDEX_SELECTED_PICTURE];
            renderables[INDEX_NEXT_PICTURE] = quad;

            nextTextImage = generateTextImage(pictures.get(idxNextPicture));

            quad = renderables[INDEX_LEFT_PICTURE];
            renderables[INDEX_SELECTED_PICTURE] = quad;

            textImage = generateTextImage(pictures.get(idxSelectedPicture));

            if (idxSelectedPicture > 0) {
                initQuadsQueue.add(createQuad(INDEX_LEFT_PICTURE, idxSelectedPicture - 1));
            } else {
                renderables[INDEX_LEFT_PICTURE] = null;
            }
        }

        private void selectNextPicture() {
            idxSelectedPicture++;
            idxNextPicture++;

            if (renderables[INDEX_LEFT_PICTURE] != null) {
                disposeQuadsQueue.add(renderables[INDEX_LEFT_PICTURE]);
            }

            Renderable quad;
            quad = renderables[INDEX_SELECTED_PICTURE];
            quad.setPosition(-7.0f - QUAD_WIDTH * 2.0f, 0.0f, 0.0f);
            quad.setRotation(0, 30, 0);
            renderables[INDEX_LEFT_PICTURE] = quad;

            quad = renderables[INDEX_NEXT_PICTURE];
            renderables[INDEX_SELECTED_PICTURE] = quad;

            textImage = generateTextImage(pictures.get(idxSelectedPicture));

            if (idxNextPicture < pictures.size()) {
                quad = renderables[INDEX_RIGHT_PICTURE];
                renderables[INDEX_NEXT_PICTURE] = quad;
                nextTextImage = generateTextImage(pictures.get(idxNextPicture));
            } else {
                renderables[INDEX_NEXT_PICTURE] = null;
            }

            if (idxNextPicture < pictures.size() - 1) {
                initQuadsQueue.add(createQuad(INDEX_RIGHT_PICTURE, idxNextPicture + 1));
            } else {
                renderables[INDEX_RIGHT_PICTURE] = null;
            }
        }
    }

    private final class NextPictureAction extends AbstractAction {
        public NextPictureAction() {
            super("Next");
            ImageIcon nextIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-button.png"));
            ImageIcon nextIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-button-pressed.png"));
            ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-next-disabled-button.png"));

            setEnabled(false);

            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", nextIconPressed);
            putValue(Action.LARGE_ICON_KEY, nextIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "next");
            putValue(Action.SHORT_DESCRIPTION, "Show next picture");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            nextPicture();
        }
    }

    private final class PreviousPictureAction extends AbstractAction {
        public PreviousPictureAction() {
            super("Previous");
            ImageIcon previousIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-button.png"));
            ImageIcon previousIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-button-pressed.png"));
            ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-previous-disabled-button.png"));

            setEnabled(false);

            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", previousIconPressed);
            putValue(Action.LARGE_ICON_KEY, previousIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "previous");
            putValue(Action.SHORT_DESCRIPTION, "Show previous picture");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            previousPicture();
        }
    }

    private final class ShowPictureAction extends AbstractAction {
        private final ImageIcon showIconActive;
        private final ImageIcon showIconAll;
        private final ImageIcon showIconPressed;
        private final ImageIcon showIconAllPressed;

        public ShowPictureAction() {
            super(pictureIsShowing ? "Show All" : "Show Picture");

            showIconActive = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-button.png"));
            showIconPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-button-pressed.png"));
            showIconAll = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-all-button.png"));
            showIconAllPressed = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-all-button-pressed.png"));

            setEnabled(false);

            final ImageIcon disabledIcon = new ImageIcon(PictureViewer.class.getResource("images/pictureviewer-show-disabled-button.png"));
            putValue("disabledIcon", disabledIcon);
            putValue("pressedIcon", pictureIsShowing ? showIconAllPressed : showIconPressed);
            putValue(Action.LARGE_ICON_KEY, pictureIsShowing ? showIconAll : showIconActive);
            putValue(Action.ACTION_COMMAND_KEY, "show");
            putValue(Action.SHORT_DESCRIPTION, "Show selected picture");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            showSelectedPicture();
        }

        public void toggleName() {
            putValue(Action.NAME, pictureIsShowing ? "Show All" : "Show Picture");
            putValue(Action.LARGE_ICON_KEY, pictureIsShowing ? showIconAll : showIconActive);
            putValue("pressedIcon", pictureIsShowing ? showIconAllPressed : showIconPressed);
        }

    }

    private final class ControlButton extends JButton implements PropertyChangeListener {
        public ControlButton(Action action) {
            super(action);
            getAction().addPropertyChangeListener(this);

            setPressedIcon((Icon) getAction().getValue("pressedIcon"));
            setDisabledIcon((Icon) getAction().getValue("disabledIcon"));
            setRolloverIcon((Icon) getAction().getValue("disabledIcon"));

            setForeground(grayColor);
            setFocusable(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            // setOpaque(false); // possibly transparent (depending on paint())
            // ^^ current swing javadocs say to not set this if setting setContentAreaFilled() to false
            setMargin(new Insets(0, 0, 0, 0));
            setText(""); // no text
            setHideActionText(true); // no text
        }

        @Override
        public void setToolTipText(String text) {
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if ("pressedIcon".equals(evt.getPropertyName())) {
                setPressedIcon((Icon) evt.getNewValue());
            }
        }
    }

    private static final class ControlPanel extends JPanel {
        private BufferedImage background;

        public ControlPanel() {
            super(new FlowLayout(FlowLayout.CENTER, 2, 2));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (background == null) {
                createBackground();
            }

            g.drawImage(background, 0, 0, null);
        }

        private void createBackground() {
            System.out.println("createBackground()");

            background = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);

            final Insets insets = getInsets();
            final RoundRectangle2D rect = new RoundRectangle2D.Double(
                    insets.left, insets.top,
                    getWidth() - insets.right - insets.left,
                    getHeight() - insets.bottom - insets.top,
                    14, 14);

            final Graphics2D g2 = background.createGraphics(); {
            g2.setColor(Color.WHITE);
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.fill(rect); } g2.dispose();
        }
    }

    private final class MouseWheelDriver implements MouseWheelListener {
        @Override public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.getWheelRotation() > 0) {
                nextPicture();
            } else {
                previousPicture();
            }
        }
    }
}