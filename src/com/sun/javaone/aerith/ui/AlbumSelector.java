package com.sun.javaone.aerith.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.font.TextLayout;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.animation.timing.Animator;
import org.jdesktop.animation.timing.TimingTarget;
import org.jdesktop.animation.timing.interpolation.PropertySetter;
import org.jdesktop.fuse.InjectedResource;
import org.jdesktop.fuse.ResourceInjector;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.util.SwingWorker;

import com.flickr4java.flickr.people.User;
import com.flickr4java.flickr.photosets.Photoset;
import com.sun.javaone.aerith.g2d.GraphicsUtil;
import com.sun.javaone.aerith.g2d.ShadowFactory;
import com.sun.javaone.aerith.util.Bundles;

/**
 * AlbumSelector
 *
 * @author Rick Wellman
 * @author aerith
 */
public class AlbumSelector extends JPanel {
    // TODO: when scrolling, always select the first or last element in the view

    private final AlbumDetails albumDetails;
    private final AlbumList albumList;
    private final DefaultListModel albumListModel;

    private float shadowOffsetX;
    private float shadowOffsetY;

    private User contact;

    ////////////////////////////////////////////////////////////////////////////
    // THEME SPECIFIC FIELDS
    ////////////////////////////////////////////////////////////////////////////
    @InjectedResource
    private Color selectionColor, listColor, detailsColor, listItemColor,
                  screenshotColor, screenshotTextColor;

    @InjectedResource
    private float selectionOpacity, selectionBorderOpacity, listOpacity,
                  listBorderOpacity, listUnselectedItemOpacity;

    @InjectedResource
    private Font listItemFont, listSelectedItemFont, screenshotFont;

    @InjectedResource(key="Common.shadowColor")
    private Color shadowColor;

    @InjectedResource(key="Common.shadowOpacity")
    private float shadowOpacity;

    @InjectedResource
    private int shadowDistance;

    @InjectedResource(key="Common.shadowDirection")
    private int shadowDirection;

    @InjectedResource
    private Font detailsBigFont, detailsSmallFont;

    @InjectedResource
    private float screenshotOpacity;

    @InjectedResource
    private BufferedImage starOn, starOff;

    @InjectedResource
    private Dimension screenshotDimension;

    /**
     * Transparent JPanel(JxPanel) w/ BorderLayout
     *
     */
    AlbumSelector() {
        super(new BorderLayout(0, 0));

        ResourceInjector.get().inject(this);

        setOpaque(false);
//      ResourceInjector.get().inject(this);
        computeShadow();

        albumListModel = new DefaultListModel();
        albumList = new AlbumList(albumListModel);
        albumDetails = new AlbumDetails();

        add(BorderLayout.CENTER, albumDetails);
        add(BorderLayout.EAST, albumList);
    }

    private void computeShadow() {
        double rads = Math.toRadians(shadowDirection);
        shadowOffsetX = (float) Math.cos(rads) * shadowDistance;
        shadowOffsetY = (float) Math.sin(rads) * shadowDistance;
    }

    void removeAllAlbums() {
        albumListModel.removeAllElements();
    }

    void addAlbum(Photoset album) {
        if (albumListModel.size() >= 9) {
            return;
        }
        albumListModel.addElement(album);
    }

    void defaultSelection() {
        if (albumListModel.size() > 0) {
            albumList.setSelectedIndex(0);
        } else {
            albumDetails.showAlbumDetails(null);
        }
    }

    /**
     * Customized JList component
     */
    private class AlbumList extends JList {
        private AlbumList(ListModel model) {
            super(model);
            setOpaque(false);
            setFont(listItemFont);
            setCellRenderer(new AlbumsListCellRenderer());

            addListSelectionListener(new ListSelectionListener() {
                @Override public void valueChanged(ListSelectionEvent e) {
                    final Rectangle bounds = getCellBounds(e.getFirstIndex(), e.getLastIndex());
                    if (bounds == null) {
                        return;
                    }

                    albumDetails.repaint(albumDetails.getWidth() - 20,
                                        bounds.y - 6,
                                        20,
                                        bounds.height + 12);

                    if (!e.getValueIsAdjusting()) {
                        Photoset album = (Photoset) getSelectedValue();
                        albumDetails.showAlbumDetails(album);
                    }
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            final Dimension parentSize = super.getPreferredSize();
            final Dimension containerSize = AlbumSelector.this.getSize();
            return new Dimension(containerSize.width / 3, parentSize.height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // == List background ==
            // Save, then adjust, graphics composite
            Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, listOpacity));

            g2.setColor(listColor);

            final RoundRectangle2D background = new RoundRectangle2D.Double(
                    3.0,
                    3.0,
                    (double) getWidth() - 18.0,
                    (double) getHeight() - 6.0,
                    10,
                    10);
            g2.fill(background);

            // == List border ==
            // Adjust graphics composite for border
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, listBorderOpacity));

            final Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(3.0f));
            g2.draw(background);

            // Restore stroke
            g2.setStroke(stroke);

            // Restore composite
            g2.setComposite(composite);

            super.paintComponent(g);
        }
    }

    /**
     * Custom CellRenderer for AlbumList
     */
    private class AlbumsListCellRenderer extends DefaultListCellRenderer {

        private boolean isSelected;

        private int index;

        private final Icon[] ratings = new Icon[6];

        private AlbumsListCellRenderer() {
            super();
            setIcon(getRatingIcon(0));
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.height += starOn.getHeight() + 2;
            return size;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value,
                                                      int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            this.index = index;
            this.isSelected = isSelected;
            super.getListCellRendererComponent(list,
                                               ((Photoset) value).getTitle(),
                                               index, isSelected,
                                               cellHasFocus);

            setOpaque(false);
            setForeground(listItemColor);

            int top = index == 0 ? 19 : 5;
            int left = 10;
            int bottom = index == albumListModel.size() - 1 ? 19 : 5;
            int right = 10;
            setBorder(BorderFactory.createEmptyBorder(top, left, bottom, right));

            if (isSelected) {
                setFont(listSelectedItemFont);
            }

            setIcon(getRatingIcon(getRating((Photoset) value)));

            return this;
        }

        private int getRating(Photoset photoset) {
            int count = photoset.getPhotoCount();
            if (count < 100) {
                return count / 20;
            } else {
                return 5;
            }
        }

        private Icon getRatingIcon(int rating) {
            if (ratings[rating] == null) {
                int starWidth = starOn.getWidth();
                int starHeight = starOn.getHeight();

                BufferedImage image = new BufferedImage(starWidth * 5, starHeight,
                                                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = image.createGraphics();
                for (int i = 0; i < rating; i++) {
                    g2.drawImage(starOn, i * starWidth, 0, null);
                }
                for (int i = rating; i < 5; i++) {
                    g2.drawImage(starOff, i * starWidth, 0, null);
                }
                g2.dispose();

                ratings[rating] = new ImageIcon(image);
            }

            return ratings[rating];
        }

        @Override
        protected void paintComponent(Graphics g) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);

            if (isSelected) {
                paintSelection(g2);
            }
            final Point p = paintText(g2);
            paintIcon(g2, p.x, p.y);
        }

        private void paintIcon(Graphics2D g2, int x, int y) {
            getIcon().paintIcon(this, g2, x, y);
        }

        private void paintSelection(Graphics2D g2) {
            final Composite composite = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selectionOpacity));

            final Color color = g2.getColor();
            g2.setColor(selectionColor);

            double y = 2.0;
            double height = (double) getHeight() - 6.0;
            if (index == 0 || index == albumListModel.size() - 1) {
                height -= 14;
            }
            if (index == 0) {
                y += 14.0;
            }
            final RoundRectangle2D background;
            background = new RoundRectangle2D.Double(
                    -6.0, y, (double) getWidth() + 3.0,
                    height, 12, 12);
            g2.fill(background);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selectionBorderOpacity));
            final Stroke stroke = g2.getStroke();
            g2.setStroke(new BasicStroke(3.0f));
            g2.draw(background);

            // Restore graphics settings
            g2.setColor(color); // added by rlw; maybe it was left out on purpose?
            g2.setStroke(stroke);
            g2.setComposite(composite);
        }

        private Point paintText(Graphics2D g2) {
            final FontMetrics fm = getFontMetrics(getFont());
            int x = getInsets().left;
            int y = getInsets().top + fm.getAscent();

            final Composite composite = g2.getComposite();

            if (isSelected) {
                y -= 2;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, shadowOpacity));
            } else {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, listUnselectedItemOpacity));
            }

            g2.setColor(shadowColor);
            final TextLayout layout = new TextLayout(getText(), getFont(), g2.getFontRenderContext());
            layout.draw(g2,
                        x + (int) Math.ceil(shadowOffsetX),
                        y + (int) Math.ceil(shadowOffsetY));

            if (isSelected) {
                g2.setComposite(composite);
            }

            g2.setColor(getForeground());
            layout.draw(g2, x, y);

            if (!isSelected) {
                g2.setComposite(composite);
            }

            return new Point(x, y + fm.getDescent());
        }
    }

    public class AlbumDetails extends JPanel {

        private Photoset album;
        private Screenshot currentScreenshot;
        private JXPanel head, description;
        private Animator timerFadeIn = null;
        private Animator timerFadeOut = null;
        private final ShadowFactory factory;

        private AlbumDetails() {
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(10, 20, 17, 21));
            setLayout(new GridBagLayout());
            factory = new ShadowFactory(shadowDistance + 2, shadowOpacity, shadowColor);
        }

        private void showAlbumDetails(final Photoset album) {
            if (album == null) {
                return;
            }

            final Runnable action = new Runnable() {
                @Override public void run() {
                    removeAll();

                    AlbumDetails.this.album = album;

                    add(head = buildHead(),
                        new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0,
                                               GridBagConstraints.FIRST_LINE_START,
                                               GridBagConstraints.NONE,
                                               new Insets(0, 0, 0, 0), 0, 0));
                    add(description = buildText(album.getDescription()),
                        new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0,
                                               GridBagConstraints.LINE_START,
                                               GridBagConstraints.BOTH,
                                               new Insets(20, 0, 0, 0), 0, 0));
                    add(Box.createVerticalGlue(),
                        new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0,
                                               GridBagConstraints.CENTER,
                                               GridBagConstraints.BOTH,
                                               new Insets(0, 0, 0, 0), 0, 0));
                    add(currentScreenshot = buildScreenshot(album),
                        new GridBagConstraints(0, 3, 1, 1, 0.0, 0.0,
                                               GridBagConstraints.LINE_START,
                                               GridBagConstraints.NONE,
                                               new Insets(20, 0, 0, 0), 0, 0));
                    add(buildButtons(),
                        new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
                                               GridBagConstraints.NORTH,
                                               GridBagConstraints.NONE,
                                               new Insets(20, 0, 0, 0), 0, 0));

                    revalidate();
                    repaint();
                }
            };
            if (currentScreenshot != null) {
                currentScreenshot.startFadeOut();
                startFadeOut(action);
            } else {
                action.run();
                startFadeIn();
            }
        }

        private void startFadeOut(final Runnable action) {
            if (timerFadeOut != null && timerFadeOut.isRunning()) {
                timerFadeOut.stop();
            }
            timerFadeOut = PropertySetter.createAnimator(400,head,"alpha",1.0f, 0.01f);
            timerFadeOut.addTarget(new TimingTarget() {

                @Override public void end() {
                    action.run();
                    startFadeIn(); // [2]
                    // [2] The startFadeIn() call was causing a recursion bug
                    //     when we were using only one time for both fadeout and fadein.
                    //     Now that we are using separate timers, it works as desired.
                }

                @Override public void begin() {
                }

                @Override public void timingEvent(float arg2) {
                }

                @Override public void repeat() {
                }

            });
            timerFadeOut.setAcceleration(0.7f);
            timerFadeOut.setDeceleration(0.3f);
            timerFadeOut.start();
        }

        private void startFadeIn() {
            if (timerFadeIn != null && timerFadeIn.isRunning()) {
                timerFadeIn.stop();
            }
            timerFadeIn = PropertySetter.createAnimator(400,head,"alpha",0.01f, 1.0f);
            timerFadeIn.setAcceleration(0.7f);
            timerFadeIn.setDeceleration(0.3f);
            timerFadeIn.start();
        }

        private JComponent buildButtons() {
            JButton button;
            Box box = Box.createVerticalBox();

            button = buildButton(Bundles.getMessage(getClass(), "TXT_ShowNow"), true);
            button.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    JButton button = (JButton) e.getSource();

                    try {
                        Thread.sleep(68);
                    } catch (InterruptedException e1) {
                    }

                    button.getModel().setArmed(false);
                    button.getModel().setPressed(false);
                    button.paintImmediately(button.getBounds());

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            try {
                                TransitionManager.showSlideshow(album);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            box.add(button);
            box.add(Box.createVerticalStrut(16));

            button = buildButton(Bundles.getMessage(getClass(), "TXT_SeeOnline"), false);
            button.addActionListener(new ActionListener() {
                @Override public void actionPerformed(ActionEvent e) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override public void run() {
                            try {
                                final URI uri = new URI("http://www.flickr.com/photos/"
                                        + contact.getUsername()
                                        + "/sets/" + album.getId());
                                Desktop.getDesktop().browse(uri);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } catch (URISyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            });
            box.add(button);

            return box;
        }

        private JButton buildButton(String text, boolean main) {
            ActionButton button = new ActionButton(text);
            button.setMain(main);
            return button;
        }

        private Screenshot buildScreenshot(Photoset album) {
            try {
                return new Screenshot(new URL(album.getPrimaryPhoto().getMediumUrl()));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }

            return new Screenshot(null);
        }

        private JXPanel buildText(String text) {

            JTextArea area = new JTextArea(text) {
                @Override
                public void paint(Graphics g) {
                    final Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    super.paint(g);
                }
            };
            area.setEditable(false);
            area.setEnabled(false);
            area.setWrapStyleWord(true);
            area.setLineWrap(true);
            area.setFont(detailsSmallFont);
            area.setOpaque(false);
            area.setForeground(detailsColor);
            area.setDisabledTextColor(detailsColor);

            DropShadowPanel panel = createDropShadowPanel(new BorderLayout());
            panel.add(area);

            return wrapWithX(panel);
        }

        private JXPanel buildHead() {
            final DropShadowPanel panel = createDropShadowPanel(new GridBagLayout());

            panel.add(buildLabel(album.getTitle(), detailsBigFont),
                      new GridBagConstraints(0, 0, 1, 1, 0.7, 0.33,
                                             GridBagConstraints.LINE_START,
                                             GridBagConstraints.NONE,
                                             new Insets(0, 0, 0, 4), 0, 0));
            panel.add(buildLabel(Bundles.getMessage(AlbumSelector.class, "TXT_NbPhotos", album.getPhotoCount()),
                                 detailsSmallFont),
                      new GridBagConstraints(0, 1, 1, 1, 0.7, 0.33,
                                             GridBagConstraints.LINE_START,
                                             GridBagConstraints.NONE,
                                             new Insets(0, 0, 0, 0), 0, 0));
//            panel.add(buildLabel("http://www.flickr.com/photos/" + contact.getUsername() +
//                                 "/sets/" + album.getId(),
//                                 detailsSmallFont),
//                      new GridBagConstraints(0, 2, 1, 1, 0.7, 0.33,
//                                             GridBagConstraints.LINE_START,
//                                             GridBagConstraints.NONE,
//                                             new Insets(0, 0, 0, 0), 0, 0));

            return wrapWithX(panel);
        }

        private JXPanel wrapWithX(JComponent component) {
            final JXPanel xpanel = new JXPanel(new BorderLayout());
            xpanel.setOpaque(false);
            xpanel.setAlpha(0.01f);
            xpanel.add(component);
            return xpanel;
        }

        private DropShadowPanel createDropShadowPanel(LayoutManager2 layout) {
            final DropShadowPanel panel = new DropShadowPanel(factory, layout);
            panel.setDistance(shadowDistance);
            panel.setAngle(shadowDirection);
            return panel;
        }

        private JLabel buildLabel(final String name, final Font font) {
            final JLabel label = new JLabel(name) {
                @Override
                public void paint(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    super.paint(g);
                }
            };
            label.setForeground(detailsColor);
            label.setFont(font);
            return label;
        }

        @Override
        public void paint(Graphics g) {
            final Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            super.paint(g);
        }

        @Override
        protected void paintComponent(Graphics g) {
            final Graphics2D g2 = (Graphics2D) g;

            Composite composite = g2.getComposite(); {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selectionOpacity));

                final RoundRectangle2D background = new RoundRectangle2D.Double(3.0, 3.0,
                                                         (double) getWidth() - 10.0 - 3.0,
                                                         (double) getHeight() - 6.0,
                                                         12, 12);
                Area area = new Area(background);
                area.add(new Area(getSelectedRectangle()));
                g2.setColor(selectionColor);
                g2.fill(area);

                Stroke stroke = g2.getStroke(); {
                    g2.setStroke(new BasicStroke(3.0f));
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selectionBorderOpacity));
                    g2.draw(area);
                } g2.setStroke(stroke);
            } g2.setComposite(composite);
        }

        private Shape getSelectedRectangle() {
            int selectedIndex = albumList.getSelectedIndex();
            Rectangle bounds = albumList.getCellBounds(selectedIndex, selectedIndex);
            if (bounds == null) {
                return new RoundRectangle2D.Double(0.0, 0.0, 0.0, 0.0, 1.0, 1.0);
            }

            float x = getWidth() - 10.0f;
            float y = 2.0f + bounds.y;
            float height = bounds.height - 6.0f;

            if (selectedIndex == 0 || selectedIndex == albumListModel.size() - 1) {
                height -= 14.0f;
            }
            if (selectedIndex == 0) {
                y += 14.0f;
            }

            GeneralPath gp = new GeneralPath();
            gp.moveTo(x - 5.0f, y- 6.0f);
            gp.lineTo(x, y - 6.0f);
            gp.quadTo(x, y, x + 6.0f, y);
            gp.lineTo(x + 20.0f, y);
            gp.lineTo(x + 20.0f, y + height);
            gp.lineTo(x + 6.0f, y + height);
            gp.quadTo(x, y + height, x, y + height + 6.0f);
            gp.lineTo(x - 5.0f, y + height + 6.0f);

            return gp;
        }

        /**
         * Screenshot (of the image at Flickr URL)
         * Lower left corner of Album Selector
         */
        public class Screenshot extends JComponent {

            private float fade = 0.0f;
            private Image image = null;
            private String text;
            private Animator timer;

            private Screenshot(URL imageUrl) {
                this.text = Bundles.getMessage(getClass(), "TXT_LoadingPicture");

                setFont(screenshotFont);
                setForeground(screenshotTextColor);

                if (imageUrl != null) {
                    new ImageFetcher(imageUrl).execute();
                } else {
                    text = Bundles.getMessage(getClass(), "TXT_NoScreenshot");
                }

            }

            /**
             * Fetches images from Flickr using SwingWorker
             */
            private class ImageFetcher extends SwingWorker {

                private final URL imageUrl;

                private ImageFetcher(URL imageUrl) {
                    this.imageUrl = imageUrl;
                }

                @Override
                protected Object doInBackground() throws Exception {
                    try {
                        System.out.println("ImageFetcher > " + imageUrl.toExternalForm() + " < ...");
                        final BufferedImage img = ImageIO.read(imageUrl);
                        System.out.println("... finished > " + imageUrl.toExternalForm());
                        image = GraphicsUtil.createThumbnail(img, getPreferredSize().width - 12, getPreferredSize().height - 12);
                        System.out.println("ImageFetcher; thumbnail finished.");
                    } catch (IOException e) {
                        image = null;
                    } finally {
                        if (image == null) {
                            text = Bundles.getMessage(getClass(), "TXT_NoScreenshot");
                        }
                    }
                    return image;
                }

                @Override
                protected void done() {
                    System.out.println("ImageFetcher > " + imageUrl.toExternalForm() + " < done()");
                    if (image != null) {
                        startFadeIn();
                    } else {
                        System.out.println("ImageFetcher ; null image WARN");
                        repaint();
                    }
                }
            }

            private void startFadeIn() {
                if (timer != null && timer.isRunning()) {
                    timer.stop();
                }

                timer = PropertySetter.createAnimator(500, Screenshot.this, "fade",  fade, 1.0f );
                timer.setAcceleration(0.7f);
                timer.setDeceleration(0.3f);
                timer.start();
            }

            private void startFadeOut() {
                if (timer != null && timer.isRunning()) {
                    timer.stop();
                }

                timer = PropertySetter.createAnimator(500, Screenshot.this, "fade", fade, 0.0f );
                timer.setAcceleration(0.7f);
                timer.setDeceleration(0.3f);
                timer.start();
            }

            public void setFade(float fade) {
                this.fade = fade;
                repaint();
            }

            public float getFade() {
                return fade;
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(screenshotDimension);
            }

            @Override
            protected void paintComponent(Graphics g) {
                final RoundRectangle2D rect = new RoundRectangle2D.Double( 0, 0, getWidth(), getHeight(), 12, 12);

                final Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

                // Reusable object reference
                Composite composite;

                // Draw translucent fill
                composite = g2.getComposite(); {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, screenshotOpacity));
                    g2.setColor(screenshotColor);
                    g2.fill(rect);
                } g2.setComposite(composite);

                // Draw Image
                if (image != null) {
                    composite = g2.getComposite(); {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, fade));
                        g2.drawImage(image, 6, 6, null);
                    } g2.setComposite(composite);
                }

                // Draw Text
                composite = g2.getComposite(); {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - fade));
                    final FontMetrics fm = getFontMetrics(getFont());
                    final TextLayout layout = new TextLayout(text, getFont(), g2.getFontRenderContext());
                    final Rectangle2D bounds = layout.getBounds();
                    float x = (float) ((getWidth() - bounds.getWidth()) / 2.0f);
                    float y = (getHeight() - fm.getHeight()) / 2.0f;
                    y += fm.getAscent();

                    g2.setColor(getForeground());
                    layout.draw(g2, x, y);
                } g2.setComposite(composite);

            }
        }
    }

    public void setContact(User contact) {
        this.contact = contact;
    }
}
