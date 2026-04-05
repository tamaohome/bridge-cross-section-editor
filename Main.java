import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

public class Main extends JFrame {
    private final ISectionBridge bridge = new ISectionBridge();

    JPanel mainPane;
    SummaryTableModel summaryTableModel;     // 一覧テーブルモデル
    SummaryTable summaryTable;               // 一覧テーブル
    DetailPanel detailPanel;                 // 詳細パネル
    PreviewPanel previewPanel;               // プレビューパネル
    AverageSectionPanel averageSectionPanel; // 平均断面パネル
    StatusBar statusBar;                     // ステータスバー

    /**
     * メイン処理
     */
    public static void main(String[] args) {
        JFrame w = new Main("BridgeCrossSectionEditor: 橋梁断面エディタ");
        w.setSize(960, 540);
        w.setVisible(true);

        // 終了時の動作
        w.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        w.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                openExitDialog(); // 終了ダイアログを表示
            }
        });
    }

    /**
     * メインクラス
     */
    public Main(String title) {
        super(title);
        mainPane = (JPanel) getContentPane();

        // メニューバーを追加
        setJMenuBar(new MenuBar());

        // インナーパネルを追加 (一覧テーブル、平均断面表示パネル用)
        JPanel innerPanel = new JPanel();
        innerPanel.setLayout(new BorderLayout());
        mainPane.add(innerPanel, BorderLayout.CENTER);

        // 一覧テーブルを追加
        summaryTableModel = new SummaryTableModel(bridge);
        summaryTable = new SummaryTable(summaryTableModel);
        JScrollPane scrollPane = new JScrollPane(summaryTable);
        innerPanel.add(scrollPane, BorderLayout.CENTER);

        // 平均断面表示パネルを追加
        averageSectionPanel = new AverageSectionPanel();
        innerPanel.add(averageSectionPanel, BorderLayout.NORTH);

        // 詳細パネルを追加
        detailPanel = new DetailPanel();
        mainPane.add(detailPanel, BorderLayout.EAST);


        // ステータスバーを追加
        statusBar = new StatusBar();
        mainPane.add(statusBar, BorderLayout.SOUTH);

        // デモンストレーション用
        setSampleSections("""
                7200,280,2200,36,19,28
                6600,280,2200,19,12,22
                6600,280,2200,28,12,25
                7200,280,2200,32,22,36
                """);

        // 一覧テーブルの1行目をフォーカス
        if (0 < summaryTable.getRowCount()) {
            summaryTable.setRowSelectionInterval(0, 0);
        }
    }

    /**
     * メニューバークラス
     */
    class MenuBar extends JMenuBar {
        MenuBar() {
            super();

            JMenu fileMenu = new JMenu("ファイル");
            fileMenu.add(new JMenuItem(new OpenAction()));
            fileMenu.add(new JMenuItem(new SaveAction()));
            fileMenu.addSeparator();
            fileMenu.add(new JMenuItem(new ExitAction()));
            this.add(fileMenu);

            JMenu editMenu = new JMenu("編集");
            editMenu.add(new JMenuItem(new InsertAction()));
            editMenu.add(new JMenuItem(new RemoveAction()));
            this.add(editMenu);

            JMenu helpMenu = new JMenu("ヘルプ");
            helpMenu.add(new JMenuItem(new VersionAction()));
            this.add(helpMenu);
        }
    }

    /**
     * 平均断面表示パネルクラス
     */
    class AverageSectionPanel extends JPanel {
        AverageField bridgeLengthField = new AverageField();
        AverageField averageAreaField = new AverageField();
        AverageField averageIyField = new AverageField();
        AverageField averageIzField = new AverageField();

        AverageSectionPanel() {
            setLayout(new FlowLayout());
            add(new JLabel("支間長"));
            add(bridgeLengthField);
            add(new JLabel("  平均断面積"));
            add(averageAreaField);
            add(new JLabel("  平均面内剛度"));
            add(averageIyField);
            add(new JLabel("  平均面外剛度"));
            add(averageIzField);
            update();
        }

        /**
         * 値を全て更新する
         */
        void update() {
            // Console.log("AverageSectionPanel.update()");
            bridgeLengthField.setValue(bridge.getBridgeLength(), 0);
            averageAreaField.setValue(bridge.getAverageArea(), 6);
            averageIyField.setValue(bridge.getAverageIy(), 6);
            averageIzField.setValue(bridge.getAverageIz(), 6);
            repaint();
        }

        // 平均値フィールド
        static class AverageField extends JTextField {
            AverageField() {
                super();
                setEditable(false); // 編集不可にする
                setPreferredSize(new Dimension(90, 20)); // サイズ変更
                setHorizontalAlignment(JTextField.RIGHT); // 右揃え

                // フォーカス時に全選択状態にする
                addFocusListener(new FocusAdapter() {
                    @Override
                    public void focusGained(FocusEvent e) {
                        selectAll();
                    }
                });
            }

            /**
             * 小数点6桁までをテキストフィールドに表示
             *
             * @param value Double
             * @param keta  int 小数点の表示桁数
             */
            void setValue(Double value, int keta) {
                String format = "%." + keta + "f";
                setText(String.format(format, value));
            }
        }
    }

    /**
     * ステータスバークラス
     */
    class StatusBar extends JToolBar {
        private String message = "";
        JTextField textField = new JTextField(message);

        StatusBar() {
            this.setFloatable(false); // ツールバーのドラッグ移動を無効化

            this.textField.setEditable(false);  // テキスト編集を無効化
            this.textField.setBorder(null);     // ボーターを消去
            this.textField.setOpaque(false);    // 背景を透明化

            this.add(textField);
        }

        public void setMessage(String message) {
            this.message = message;
            this.textField.setText(message);
            this.repaint();
        }

        public String getMessage() {
            return message;
        }
    }

    /**
     * 一覧テーブルモデルクラス
     */
    class SummaryTableModel extends DefaultTableModel {
        SummaryTableModel(ISectionBridge bridge) {
            super(new String[]{
                    "ブロック長 [mm]",
                    "フランジ幅 [mm]",
                    "上フランジ [mm]",
                    "ウェブ [mm]",
                    "下フランジ [mm]",
                    "断面積 [m2]",
                    "面内剛度 [m4]",
                    "面外剛度 [m4]"
            }, 0);
        }

        // セルを全て編集不可にする
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }

        /**
         * テーブルを再描画
         */
        public void update() {
            setRowCount(0); // テーブルをクリア

            // 行毎にセルの値をセット
            for (int i = 0; i < bridge.sections.size(); i++) {
                ISection section = bridge.get(i);
                addRow(new Object[]{
                        section.getLength(),
                        section.getWidth(),
                        section.getTuf(),
                        section.getTw(),
                        section.getTlf(),
                        formatedNumber((double) section.getArea() / 1_000_000),
                        formatedNumber((double) section.getIy() / 1_000_000_000_000L),
                        formatedNumber((double) section.getIz() / 1_000_000_000_000L)
                });
            }

            fireTableDataChanged();       // テーブルを再描画
            previewPanel.repaint();       // プレビューパネルを更新
            averageSectionPanel.update(); // 平均断面パネルを更新
        }

        /**
         * 小数点6桁にフォーマットした文字列を返す
         */
        private String formatedNumber(Object num) {
            try {
                if (num instanceof Double) {
                    return String.format("%.6f", (Double) num);
                } else if (num instanceof Integer) {
                    return String.format("%d", (Integer) num);
                } else {
                    return num.toString();
                }
            } catch (RuntimeException e) {
                return "0.000000";
            }
        }

        /**
         * 選択行の ISection を返す
         *
         * @param selectedRow 選択行の行番号
         * @return ISection
         */
        public ISection getSectionAt(int selectedRow) {
            return bridge.get(selectedRow);
        }
    }

    /**
     * 一覧テーブルクラス
     */
    class SummaryTable extends JTable {
        private int lastSelectedRow = -1; // 最後の選択行の行番号

        SummaryTable(DefaultTableModel tableModel) {
            super(tableModel);

            // ヘッダーを中央揃えにする
            DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer();
            headerRenderer.setHorizontalAlignment(JLabel.CENTER);
            for (int i = 0; i < getColumnModel().getColumnCount(); i++) {
                getColumnModel().getColumn(i).setHeaderRenderer(headerRenderer);
            }

            // テーブルの罫線を設定
            setShowGrid(true);
            setGridColor(Color.LIGHT_GRAY);

            // 行選択時の動作
            getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) {
                    int selectedRow = getSelectedRow();
                    if (selectedRow != -1) {
                        String statusMessage = "断面 " + (selectedRow + 1) + " が選択されました";
                        statusBar.setMessage(statusMessage);
                    }

                    detailPanel.update();   // 詳細パネルを更新
                    previewPanel.repaint(); // プレビューパネルを更新
                }
            });

            // 行の複数選択を禁止する
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        }


        // テーブルの色を設定
        @Override
        public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
            Component c = super.prepareRenderer(renderer, row, column);

            c.setForeground(Color.BLACK); // テキスト色は常に黒色

            if (row == getSelectedRow()) {
                c.setBackground(Color.ORANGE); // 背景色をオレンジに変更
            } else if (getSelectedRow() == -1 && row == lastSelectedRow) {
                c.setBackground(Color.ORANGE); // 背景色をオレンジに変更
            } else {
                c.setBackground(Color.WHITE); // 背景色を白色に変更
            }

            return c;
        }

        /**
         * 選択中の ISection を返す
         *
         * @return ISection
         */
        public ISection getSelectedSection() {
            int selectedRow = getLastSelectedRow();

            if (selectedRow == -1) return null; // 選択行が取得できない場合 null を返す

            return ((SummaryTableModel) getModel()).getSectionAt(selectedRow);
        }

        /**
         * 最後の選択行の番号を返す
         *
         * @return int
         */
        public int getLastSelectedRow() {
            // 選択行が取得できる場合、最後の選択行を更新
            int selectedRow = getSelectedRow();
            if (selectedRow != -1) lastSelectedRow = selectedRow;
            return lastSelectedRow;
        }
    }

    /**
     * 詳細パネルクラス
     */
    class DetailPanel extends JPanel {
        NumericField lengthField = new NumericField(); // ブロック長
        NumericField widthField = new NumericField();  // フランジ幅
        NumericField heightField = new NumericField(); // 桁高

        NumericField tufField = new NumericField();  // U.Flg 板厚
        NumericField twField = new NumericField();   // Web 板厚
        NumericField tlfField = new NumericField();  // L.FLg 板厚

        NumericField[] fields = {lengthField, widthField, heightField, tufField, twField, tlfField};

        DetailPanel() {
            this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            this.setPreferredSize(new Dimension(180, 0)); // パネル幅

            // ブロック形状入力フィールド
            JPanel sizePanel = new JPanel();
            sizePanel.setBorder(new TitledBorder("ブロック形状"));
            sizePanel.add(new LabelFieldPair("ブロック長", lengthField));
            sizePanel.add(new LabelFieldPair("幅", widthField));
            sizePanel.add(new LabelFieldPair("桁高", heightField));
            this.add(sizePanel);

            // 板厚入力フィールド
            JPanel tPanel = new JPanel();
            tPanel.setBorder(new TitledBorder("板厚"));
            tPanel.add(new LabelFieldPair("上フランジ", tufField));
            tPanel.add(new LabelFieldPair("ウェブ", twField));
            tPanel.add(new LabelFieldPair("下フランジ", tlfField));
            this.add(tPanel);

            // 断面プレビューパネル
            previewPanel = new PreviewPanel();
            this.add(previewPanel);

            // 選択行の ISection を更新するため、アクションリスナを追加
            for (NumericField field : fields) {
                // Enterキーが押された場合
                // field.addActionListener(e -> updateSection());
                // 任意のキーが押された場合
                field.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent e) {
                        updateSelectedSection();
                    }
                });
            }
        }

        /**
         * テキストフィールドの値を更新
         */
        void update() {
            ISection section = summaryTable.getSelectedSection();

            if (section == null) return; // 断面が選択されていない場合、処理を中止

            lengthField.setText(String.valueOf(section.getLength()));
            widthField.setText(String.valueOf(section.getWidth()));
            heightField.setText(String.valueOf(section.getHeight()));
            tufField.setText(String.valueOf(section.getTuf()));
            twField.setText(String.valueOf(section.getTw()));
            tlfField.setText(String.valueOf(section.getTlf()));
        }

        /**
         * テキストフィールドの値を基に、選択行の ISection の値を更新
         */
        private void updateSelectedSection() {
            ISection section = summaryTable.getSelectedSection();
            if (section == null) return;

            section.setLength(Double.parseDouble(lengthField.getText()));
            section.setWidth(Double.parseDouble(widthField.getText()));
            section.setHeight(Double.parseDouble(heightField.getText()));
            section.setTuf(Double.parseDouble(tufField.getText()));
            section.setTw(Double.parseDouble(twField.getText()));
            section.setTlf(Double.parseDouble(tlfField.getText()));

            // Console.log("updateSection():");
            summaryTableModel.update();
            previewPanel.repaint();
        }

        // ラベルとテキストフィールドを横並びにするパネル
        class LabelFieldPair extends JPanel {
            LabelFieldPair(String text, JTextField textField) {
                this.setLayout(new BorderLayout());
                JLabel label = new JLabel(text);
                label.setPreferredSize(new Dimension(80, 22));

                this.add(label, BorderLayout.WEST);
                this.add(textField, BorderLayout.CENTER);

                this.setMaximumSize(new Dimension(0, 22));
            }
        }
    }

    /**
     * 数値フィールド
     */
    static class NumericField extends JTextField {
        String validValues = "0123456789-+.";

        public NumericField() {
            enableEvents(AWTEvent.KEY_EVENT_MASK);
            setPreferredSize(new Dimension(60, 20)); // サイズ変更
            setHorizontalAlignment(JTextField.RIGHT); // 右揃え

            // フォーカス時に全選択状態にする
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    selectAll();
                }
            });
        }

        // 数字以外の入力を無効化
        @Override
        protected void processKeyEvent(KeyEvent e) {
            char chr = e.getKeyChar();
            int code = e.getKeyCode();
            if (code == 0 && validValues.indexOf(chr) == -1) {
                e.consume();
            }
            super.processKeyEvent(e);
        }
    }

    // プレビューパネル
    class PreviewPanel extends JPanel {
        PreviewPanel() {
            this.setBorder(new TitledBorder("プレビュー"));
            this.setPreferredSize(new Dimension(0, 160));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g); // 背景を再描画
            updateSection(g);
        }

        /**
         * 矩形を描画する
         */
        private void drawPlate(Graphics g, int x, int y, int width, int height) {
            g.setColor(Color.WHITE);
            g.fillRect(x, y, width, height);
            g.setColor(Color.GRAY);
            g.drawRect(x, y, width, height);
        }

        /**
         * 断面を描画する
         */
        private void drawSection(Graphics g, ISection section) {
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            int width = (int) section.getWidth() / 4;
            int height = 140;

            // 板厚を取得
            int tuf = (int) section.getTuf() / 2;
            int tw = (int) section.getTw() / 2;
            int tlf = (int) section.getTlf() / 2;

            // 矩形を描画
            drawPlate(g, cx - tw / 2, cy - height / 2, tw, height); // ウェブ
            drawPlate(g, cx - width / 2, cy + height / 2, width, tuf); // 上フランジ
            drawPlate(g, cx - width / 2, cy - height / 2, width, tlf); // 下フランジ
        }

        /**
         * 断面を再描画
         */
        void updateSection(Graphics g) {
            ISection section = summaryTable.getSelectedSection();
            if (section != null) {
                drawSection(g, section);
            }
        }
    }

    /**
     * 終了ダイアログを開く
     */
    private static void openExitDialog() {
        int response = JOptionPane.showConfirmDialog(
                null,
                "終了しますか？",
                "終了の確認",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );

        if (response == JOptionPane.OK_OPTION) {
            System.exit(0);
        }
    }

    /**
     * バージョン情報ダイアログを開く
     */
    private void openVersionDialog() {
        JOptionPane.showMessageDialog(
                null,
                "CrossSectionEditor: 橋梁断面エディタ",
                "このアプリについて",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    // ファイルオープン時の動作
    class OpenAction extends AbstractAction {
        OpenAction() {
            putValue(Action.NAME, "開く");
            putValue(Action.SHORT_DESCRIPTION, "開く");
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("テキスト文書 (*.txt)", "txt");
            fileChooser.setFileFilter(filter);
            int returnValue = fileChooser.showOpenDialog(null);

            // ファイル選択がキャンセルされた場合、処理を中止する
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                Console.log("ファイルオープンが中止されました");
                return;
            }

            // 選択されたファイルをオープンする
            File selectedFile = fileChooser.getSelectedFile();
            bridge.open(selectedFile.getPath());

            // 一覧テーブルの描画を更新する
            summaryTableModel.update();

            Console.log("ファイルを開きました:", selectedFile);
        }
    }

    // ファイル保存時の動作
    class SaveAction extends AbstractAction {
        SaveAction() {
            putValue(Action.NAME, "保存");
            putValue(Action.SHORT_DESCRIPTION, "保存");
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser fileChooser = new JFileChooser();
            FileNameExtensionFilter filter = new FileNameExtensionFilter("テキスト文書 (*.txt)", "txt");
            fileChooser.setFileFilter(filter);
            int returnValue = fileChooser.showSaveDialog(null);

            // ファイル選択がキャンセルされた場合、処理を中止する
            if (returnValue != JFileChooser.APPROVE_OPTION) {
                Console.log("ファイル保存が中止されました");
                return;
            }

            // 選択されたファイルに保存する
            File selectedFile = fileChooser.getSelectedFile();
            bridge.save(selectedFile.getPath());

            Console.log("ファイルを保存しました:", selectedFile);
        }
    }

    // 終了時の動作
    class ExitAction extends AbstractAction {
        ExitAction() {
            putValue(Action.NAME, "終了");
            putValue(Action.SHORT_DESCRIPTION, "終了");
        }

        public void actionPerformed(ActionEvent e) {
            openExitDialog();
        }
    }

    // 断面挿入時の動作
    class InsertAction extends AbstractAction {
        InsertAction() {
            putValue(Action.NAME, "行の挿入");
            putValue(Action.SHORT_DESCRIPTION, "行の挿入");
        }

        public void actionPerformed(ActionEvent e) {
            int selectedRow = summaryTable.getLastSelectedRow();
            if (selectedRow != -1) {
                bridge.insert(selectedRow);
                statusBar.setMessage(selectedRow + 1 + " 番目に断面を挿入します");
                summaryTableModel.update();
            }
        }
    }

    // 断面削除時の動作
    class RemoveAction extends AbstractAction {
        RemoveAction() {
            putValue(Action.NAME, "行の削除");
            putValue(Action.SHORT_DESCRIPTION, "行の削除");
        }

        public void actionPerformed(ActionEvent e) {
            int selectedRow = summaryTable.getLastSelectedRow();
            if (selectedRow != -1) {
                bridge.remove(selectedRow);
                statusBar.setMessage(selectedRow + 1 + " 番目の断面を削除します");
                summaryTableModel.update();
            }
        }
    }

    // バージョン情報を表示する動作
    class VersionAction extends AbstractAction {
        VersionAction() {
            putValue(Action.NAME, "このアプリについて");
            putValue(Action.SHORT_DESCRIPTION, "このアプリについて");
        }

        public void actionPerformed(ActionEvent e) {
            openVersionDialog();
        }
    }

    /**
     * サンプルデータを断面にセット
     */
    private void setSampleSections(String sampleData) {
        String[] lines = sampleData.split("\n");

        for (String line : lines) {
            String[] values = line.split(",");
            double length = Double.parseDouble(values[0]);
            double width = Double.parseDouble(values[1]);
            double height = Double.parseDouble(values[2]);
            double tuf = Double.parseDouble(values[3]);
            double tw = Double.parseDouble(values[4]);
            double tlf = Double.parseDouble(values[5]);
            bridge.add(length, width, height, tuf, tw, tlf);

            summaryTableModel.update();   // テーブルを再描画

        }
    }
}
