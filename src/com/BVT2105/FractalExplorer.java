package com.BVT2105;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.geom.Rectangle2D;


//Этот класс задает взаимодействие с фракталом
public class FractalExplorer {
    private int displaySize;
    private JImageDisplay imageDisplay;
    private FractalGenerator fractalGenerator;
    private Rectangle2D.Double range;
    // Добавим новые взаимодействия в пользовательский интерфейс
    private JComboBox comboBox;
    private int rowsRemaining;
    private JButton buttonReset;
    private JButton buttonSave;

    //Реализуем конструктор
    private FractalExplorer (int displaySize) {
        this.displaySize = displaySize;
        this.fractalGenerator = new Mandelbrot();
        this.range = new Rectangle2D.Double(0,0,0,0);
        fractalGenerator.getInitialRange(this.range);
    }
    //зададим несколько приятных значений по умолчанию
    public static void main(String[] args) {
        FractalExplorer fractalExplorer = new FractalExplorer(600);
        fractalExplorer.setGUI();
        fractalExplorer.drawFractal();
    }

    //Создадим интерфейс
    public void setGUI() {
        JFrame frame = new JFrame("Fractal Generator");
        JPanel jPanel_1 = new JPanel();
        JPanel jPanel_2 = new JPanel();
        JLabel label = new JLabel("Fractal: ");

        imageDisplay = new JImageDisplay(displaySize, displaySize);

        imageDisplay = new JImageDisplay(displaySize, displaySize);
        //клик по экрану
        imageDisplay.addMouseListener(new MouseListener());

        //Добавим реализацию выборщика фракталов
        comboBox = new JComboBox();
        comboBox.addItem(new Mandelbrot());
        comboBox.addItem(new Tricorn());
        comboBox.addItem(new BurningShip());
        comboBox.addActionListener(new ActionHandler());

        //Добавим реализацию кнопки сброса
        buttonReset = new JButton("Reset");
        buttonReset.setActionCommand("Reset");
        buttonReset.addActionListener(new ActionHandler());

        //Кнопку сохранения на диск
        buttonSave = new JButton("Save image");
        buttonSave.setActionCommand("Save");
        buttonSave.addActionListener(new ActionHandler());

        // Добавим все это в панели
        jPanel_1.add(label, BorderLayout.CENTER);
        jPanel_1.add(comboBox, BorderLayout.CENTER);
        jPanel_2.add(buttonReset, BorderLayout.CENTER);
        jPanel_2.add(buttonSave, BorderLayout.CENTER);

        frame.setLayout(new BorderLayout());
        frame.add(imageDisplay, BorderLayout.CENTER);
        frame.add(jPanel_1, BorderLayout.NORTH);
        frame.add(jPanel_2, BorderLayout.SOUTH);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.pack();
        frame.setVisible(true);
        frame.setResizable(false);
    }

    //функция рендеринга изображения
    // Привяжем фоновое выполнение к процессу рисования фракталов
    // Воспользуемся для этого методом execute, который запустит фоновый поток и задачу в фоновом режиме
    private void drawFractal() {
        // вызовем метод который включает или отключает кнопски с выпадающим списком в интерфейсе
        enableGUI(false);
        // установим rowsRemaining равному общему кол-ву строк, которые нужно нарисовать
        rowsRemaining = displaySize;
        for (int i = 0; i < displaySize; i++) {
            FractalWorker drawRow = new FractalWorker(i);
            drawRow.execute();
        }
    }

    // Включение/отключение взаимодействия с GUI через метод Swing
    public void enableGUI(boolean b) {
        buttonSave.setEnabled(b);
        buttonReset.setEnabled(b);
        comboBox.setEnabled(b);
    }


    //добавим функцию, которая обрабатывает нажатия кнопки
    public class ActionHandler implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            //Кнопка сброса сбрасывает изображение
            if (e.getActionCommand().equals("Reset")) {
                fractalGenerator.getInitialRange(range);
                drawFractal();
                //Кнопка сохранения сохраняет изображение
            } else if (e.getActionCommand().equals("Save")) {
                JFileChooser fileChooser = new JFileChooser();
                FileNameExtensionFilter fileFilter = new FileNameExtensionFilter("PNG Images", "png");
                fileChooser.setFileFilter(fileFilter);
                fileChooser.setAcceptAllFileFilterUsed(false);
                int t = fileChooser.showSaveDialog(imageDisplay);
                if (t == JFileChooser.APPROVE_OPTION) {
                    //Поместим реализацию сохранения в обработку исключение потому как сохранение может завершиться неудачей
                    try {
                        ImageIO.write(imageDisplay.getImage(), "png", fileChooser.getSelectedFile());
                    } catch (NullPointerException | IOException ee) {
                        JOptionPane.showMessageDialog(imageDisplay, ee.getMessage(), "Cannot save image", JOptionPane.ERROR_MESSAGE);
                    }
                }
            } else {
                fractalGenerator = (FractalGenerator) comboBox.getSelectedItem();
                range = new Rectangle2D.Double(0,0,0,0);
                fractalGenerator.getInitialRange(range);
                drawFractal();
            }
        }
    }

    //По нажатию на экран выберется другая(более малая) область. Реализуем здесь подключение клика мыши.
    public class MouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            double x = FractalGenerator.getCoord(range.x, range.x + range.width, displaySize, e.getX());
            double y = FractalGenerator.getCoord(range.y, range.y + range.width, displaySize, e.getY());
            fractalGenerator.recenterAndZoomRange(range, x, y, 0.5);
            drawFractal();
        }
    }
    // Реализуем функцию многопоточной обработки построения фракталов
    // Класс FractalWorker будет вычислять значение цвета для одной строки фрактала
    public class FractalWorker extends SwingWorker<Object, Object> {
        // Зададим два поля: координата строки и массив чисел для хранения вычисленных значений цвета каждого пикселя
        private int y_coord;
        private int[] rgb;
        // Зададим конструктор, который получает координату и сохраняет ее
        public FractalWorker(int y_coord) {
            this.y_coord = y_coord;
        }

        // Данный метод вызывается в фоновом потоке и отвечает за выполнение длительной задачи.
        @Override
        protected Object doInBackground() throws Exception {
            rgb = new int[displaySize];
            // Вместо того, чтоб рисовать в окне, цикл будет сохранять каждое RGB значение в элементе массива
            for (int i = 0; i < displaySize; i++) {
                int count = fractalGenerator.numIterations(FractalGenerator.getCoord(range.x, range.x + range.width, displaySize, i),
                        FractalGenerator.getCoord(range.y, range.y+range.width, displaySize, y_coord));
                if (count == -1)
                    rgb[i] = 0;
                else {
                    double hue = 0.7f + (float) count / 200f;
                    int rgbColor = Color.HSBtoRGB((float) hue, 1f, 1f);
                    rgb[i] = rgbColor;
                }
            }
            return null;
        }

        // Данный метод вызывается когда фоновая задача завершена и этот метод вызывается из потока обработки событий Swing
        @Override
        protected void done() {
            for (int i = 0; i < displaySize; i++) {
                imageDisplay.drawPixel(i, y_coord, rgb[i]);
            }
            // После вычисления строки сообщаем Swing перерисовать часть изображения, что была измененеа
            imageDisplay.repaint(0, 0, y_coord, displaySize, 1);
            // уменьшим значение rowdRemaining
            rowsRemaining--;
            // Если 0, то вызовем функцию enableGUI
            if (rowsRemaining == 0)
                enableGUI(true);
        }
    }
}

