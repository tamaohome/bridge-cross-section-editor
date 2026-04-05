import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class ISectionBridge implements Iterable<ISection> {
    ArrayList<ISection> sections = new ArrayList<>();
    String name;

    ISectionBridge() {
        this.name = "";
    }

    ISectionBridge(String name) {
        this.name = name;
    }

    public ISection get(int index) {
        return sections.get(index);
    }

    public ISection getFirst() {
        return sections.getFirst();
    }

    public ISection getLast() {
        return sections.getLast();
    }

    /**
     * 全断面のブロック長から支間長を算出して返す
     */
    double getBridgeLength() {
        return sections.stream().mapToDouble(ISection::getLength).sum();
    }

    /**
     * 平均断面積を算出して返す
     */
    double getAverageArea() {
        return sections.stream()
                .mapToDouble(section -> section.getArea() * section.getLength() / getBridgeLength() / 1_000_000)
                .sum();
    }

    /**
     * 平均面内剛度を算出して返す
     */
    double getAverageIy() {
        return sections.stream()
                .mapToDouble(section -> section.getIy() * section.getLength() / getBridgeLength() / 1_000_000_000_000L)
                .sum();
    }

    /**
     * 平均面外剛度を算出して返す
     */
    double getAverageIz() {
        return sections.stream()
                .mapToDouble(section -> section.getIz() * section.getLength() / getBridgeLength() / 1_000_000_000_000L)
                .sum();
    }

    /**
     * 断面を追加
     *
     * @param iSection ISection 断面
     */
    public void add(ISection iSection) {
        sections.add(iSection.copy());
    }

    /**
     * 断面を追加
     *
     * @param width  double フランジ幅
     * @param height double 桁高
     * @param tuf    double 上フランジ板厚
     * @param tw     double ウェブ板厚
     * @param tlf    double 下フランジ板厚
     */
    public void add(double length, double width, double height, double tuf, double tw, double tlf) {
        add(new ISection(length, width, height, tuf, tw, tlf));
    }

    /**
     * 断面を追加
     * フランジ幅、桁高は直前の断面と同一
     *
     * @param tuf double 上フランジ板厚
     * @param tw  double ウェブ板厚
     * @param tlf double 下フランジ板厚
     */
    public void add(double tuf, double tw, double tlf) {
        double length = getLast().getLength();
        double width = getLast().getWidth();   // 最後の断面のフランジ幅
        double height = getLast().getHeight(); // 最後の断面の桁高
        add(new ISection(length, width, height, tuf, tw, tlf));
    }

    /**
     * 断面を追加
     * フランジ幅、桁高、板厚は直前の断面と同一
     */
    public void add() {
        add(getLast());
    }

    /**
     * 断面をすべて削除
     */
    public void clear() {
        sections.clear();
    }

    /**
     * 橋梁データを外部ファイルより読み込む
     *
     * @param filename ファイルパス
     */
    public void open(String filename) {
        clear(); // 断面をリセット

        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            String line;
            br.readLine(); // ヘッダー行をスキップ

            // 各行から断面データを取得
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length == 6) {
                    double length = Double.parseDouble(values[0]);
                    double width = Double.parseDouble(values[1]);
                    double height = Double.parseDouble(values[2]);
                    double tuf = Double.parseDouble(values[3]);
                    double tw = Double.parseDouble(values[4]);
                    double tlf = Double.parseDouble(values[5]);
                    add(length, width, height, tuf, tw, tlf);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 橋梁データを外部ファイルに保存する
     *
     * @param filename ファイルパス
     */
    public void save(String filename) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filename))) {
            // ヘッダー行を追加
            bw.write("length,width,height,tuf,tw,tlf");
            bw.newLine();

            // 断面データを1行ずつ書き込む
            for (ISection section : sections) {
                bw.write(String.format("%.1f,%.1f,%.1f,%.1f,%.1f,%.1f",
                        section.getLength(),
                        section.getWidth(),
                        section.getHeight(),
                        section.getTuf(),
                        section.getTw(),
                        section.getTlf())
                );
                bw.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 指定番号の上に断面を挿入
     */
    public void insert(int index) throws IndexOutOfBoundsException {
        sections.add(index, new ISection());
    }

    /**
     * 指定番号の断面を削除
     *
     * @param index 断面番号
     */
    public void remove(int index) throws IndexOutOfBoundsException {
        sections.remove(index);
    }

    @Override
    public Iterator<ISection> iterator() {
        return sections.iterator();
    }

    /**
     * テストコード
     */
    public static void main(String[] args) {
        // 断面が空のインスタンスを生成
        ISectionBridge myBridge = new ISectionBridge();

        // add() の引数に ISection インスタンスを渡すと、断面が追加される
        ISection mySection = new ISection(7200, 280, 2200, 32, 19, 28);
        myBridge.add(mySection);

        // add() の引数に板厚を渡すと、幅と桁高がコピーされた断面が追加される
        myBridge.add(19, 12, 22);

        // add() の引数が空の場合、最後の断面がコピーされる
        myBridge.add();

        // 0番目の断面の上フランジ板厚を変更
        myBridge.get(0).setTuf(36);

        // 最後の断面の板厚を一括変更
        myBridge.getLast().setT(32, 22, 36);

        // イテレータとして振る舞う
        for (ISection section : myBridge) {
            Console.log(section);
        }

        // 橋長 (ブロック長の合計) を表示
        Console.log("橋長:", myBridge.getBridgeLength(), "mm");

        // 橋梁データをファイルに保存
        myBridge.save("sections2.txt");

        // 断面をリセット
        myBridge.clear();

        // 外部ファイルから橋梁データを読み込む
        String sectionsFile = "sections.txt";
        myBridge.open(sectionsFile);

        for (ISection section : myBridge) {
            Console.log(section);
        }
    }
}
