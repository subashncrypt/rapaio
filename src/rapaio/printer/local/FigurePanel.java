/*
 * Apache License
 * Version 2.0, January 2004
 * http://www.apache.org/licenses/
 *
 * Copyright 2013 - 2022 Aurelian Tutuianu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package rapaio.printer.local;

import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.Serial;
import java.util.concurrent.ExecutionException;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import rapaio.graphics.Figure;
import rapaio.image.ImageTools;
import rapaio.printer.Printer;
import rapaio.printer.opt.POption;
import rapaio.printer.opt.POpts;

/**
 * @author <a href="mailto:padreati@yahoo.com">Aurelian Tutuianu</a>
 */
public class FigurePanel extends JPanel {

    @Serial
    private static final long serialVersionUID = 6956337145639708156L;

    protected Figure figure;
    protected volatile BufferedImage currentImage;
    protected SwingWorker<BufferedImage, Object> drawWorker;
    boolean forceRedraw = true;
    private final FigurePrinter printer;

    public FigurePanel() {
        this.figure = null;
        this.currentImage = null;
        this.printer = new FigurePrinter(this);
    }

    public FigurePanel(Figure figure) {
        this.figure = figure;
        this.printer = new FigurePrinter(this);
    }

    public FigurePanel(BufferedImage image) {
        this.figure = null;
        this.currentImage = image;
        this.printer = new FigurePrinter(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        final String drawingMessage = "Rendering Update...";
        FontMetrics fm = g.getFontMetrics();

        if (currentImage != null) {
            if (currentImage.getWidth() != getWidth() || currentImage.getHeight() != getHeight() || forceRedraw) {
                forceRedraw = false;
                if (drawWorker == null) {
                    createBackgroundImage();
                }

                g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
                g.drawString(drawingMessage, 3, getHeight() - fm.getHeight() / 2);
            } else {
                g.drawImage(currentImage, 0, 0, null);
            }
        } else if (currentImage == null) {
            if (drawWorker == null) {
                createBackgroundImage();
            }
            g.drawString(drawingMessage, getWidth() / 2 - fm.stringWidth(drawingMessage) / 2, getHeight() / 2 - fm.getHeight() / 2);
        }
    }

    /**
     * Creates a new worker to do the image rendering in the background.
     */
    private void createBackgroundImage() {
        drawWorker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() {
                if (figure == null) {
                    return currentImage;
                }
                return ImageTools.makeImage(figure, getWidth(), getHeight());
            }

            @Override
            protected void done() {

                SwingUtilities.invokeLater(() -> {
                    try {
                        currentImage = get();
                        drawWorker = null;
                        revalidate();
                        repaint();
                    } catch (InterruptedException | ExecutionException ex) {
                        JOptionPane.showMessageDialog(null, ex.getMessage());
                        ex.printStackTrace();
                    }
                });

            }
        };

        drawWorker.execute();
    }

    public void createUIComponents() {
    }

    public Printer getPrinter() {
        return printer;
    }

    static final class FigurePrinter implements Printer {

        private final FigurePanel panel;
        private POpts opts = POpts.defaults;

        public FigurePrinter(FigurePanel panel) {
            this.panel = panel;
        }

        @Override
        public Printer withGraphicShape(int width, int height) {
            return this;
        }

        @Override
        public int graphicWidth() {
            return panel.getWidth();
        }

        @Override
        public int graphicHeight() {
            return panel.getHeight();
        }

        @Override
        public POpts getOptions() {
            return null;
        }

        @Override
        public Printer withOptions(POption<?>... options) {
            opts = opts.bind(options);
            return this;
        }

        @Override
        public void print(String message) {

        }

        @Override
        public void println() {

        }

        @Override
        public void draw(Figure figure, int width, int height) {
            draw(figure);
        }

        @Override
        public void draw(Figure figure) {
            panel.figure = figure;
            panel.currentImage = null;
            panel.repaint();
        }
    }

}
