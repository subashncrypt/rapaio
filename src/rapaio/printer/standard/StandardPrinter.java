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

package rapaio.printer.standard;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.io.Console;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import javax.swing.JDialog;
import javax.swing.JFrame;

import rapaio.graphics.Figure;
import rapaio.printer.AbstractPrinter;
import rapaio.printer.local.FigurePanel;

/**
 * @author tutuianu
 */
public class StandardPrinter extends AbstractPrinter {

    private final Reader reader;
    private final PrintWriter writer;

    public StandardPrinter() {

        Console console = System.console();
        if (console != null) {
            reader = console.reader();
            writer = console.writer();
        } else {
            reader = new InputStreamReader(System.in);
            writer = new PrintWriter(System.out);
        }
        withGraphicShape(1200, 600);
    }

    @Override
    public void print(String message) {
        writer.print(message);
        writer.flush();
    }

    @Override
    public void println() {
        writer.println();
        writer.flush();
    }

    @Override
    public void draw(Figure figure, int width, int height) {
        FigurePanel figurePanel = new FigurePanel(figure);
        JFrame frame = new JFrame("rapaio graphic window");
        frame.setContentPane(figurePanel);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
//        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.setAutoRequestFocus(true);
        frame.setModalExclusionType(Dialog.ModalExclusionType.APPLICATION_EXCLUDE);

        frame.setSize(width, height);
        do {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
        } while (frame.isVisible());
    }
}
