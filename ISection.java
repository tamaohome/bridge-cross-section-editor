public class ISection {
    private static int lastId = 0;
    final int id;

    final Plate uFlg;       // U.Flg
    final Plate web;        // Web
    final Plate lFlg;       // L.Flg

    private double length;  // ブロック長
    private double width;   // Flg 幅
    private double height;  // 桁高

    private double tuf;     // U.Flg 板厚
    private double tw;      // Web   板厚
    private double tlf;     // L.Flg 板厚

    ISection() {
        this.id = ++lastId; // ユニークIDを付与
        this.length = 0;

        // Plate インスタンスの生成
        this.uFlg = new Plate(this, Plate.Type.U_FLG);
        this.web = new Plate(this, Plate.Type.WEB);
        this.lFlg = new Plate(this, Plate.Type.L_FLG);

        setSection(0, 0, 0, 0, 0);
    }

    ISection(double length, double width, double height, double tuf, double tw, double tlf) {
        this.id = ++lastId; // ユニークIDを付与
        this.length = length;

        // Plate インスタンスの生成
        this.uFlg = new Plate(this, Plate.Type.U_FLG);
        this.web = new Plate(this, Plate.Type.WEB);
        this.lFlg = new Plate(this, Plate.Type.L_FLG);

        setSection(width, height, tuf, tw, tlf);
    }

    ISection copy() {
        return new ISection(this.length, this.width, this.height, this.tuf, this.tw, this.tlf);
    }

    /**
     * 断面を一括してセット
     */
    void setSection(double width, double height, double tuf, double tw, double tlf) {
        setWidth(width);
        setHeight(height);
        setT(tuf, tw, tlf);
    }

    /**
     * ブロック長をセット
     */
    void setLength(double length) {
        this.length = length;
    }

    /**
     * フランジ幅をセット
     */
    void setWidth(double width) {
        this.width = Math.max(0, width);
    }

    /**
     * 桁高をセット
     */
    void setHeight(double height) {
        this.height = Math.max(0, height);
    }

    /**
     * 板厚をセット
     */
    void setT(double tuf, double tw, double tlf) {
        setTuf(tuf);
        setTw(tw);
        setTlf(tlf);
    }

    void setTuf(double tuf) {
        this.tuf = Math.max(0, tuf);
    }

    void setTw(double tw) {
        this.tw = Math.max(0, tw);
    }

    void setTlf(double tlf) {
        this.tlf = Math.max(0, tlf);
    }

    double getLength() {
        return this.length;
    }

    double getWidth() {
        return this.width;
    }

    double getHeight() {
        return this.height;
    }

    double getTuf() {
        return this.tuf;
    }

    double getTw() {
        return this.tw;
    }

    double getTlf() {
        return this.tlf;
    }

    /**
     * 断面積[mm2]を返す
     */
    long getArea() {
        return uFlg.getArea() + web.getArea() + lFlg.getArea();
    }

    /**
     * 面内剛度[mm4]を返す
     */
    long getIy() {
        return uFlg.getAy2() + web.getAy2() + lFlg.getAy2();
    }

    /**
     * 面外剛度[mm4]を返す
     */
    long getIz() {
        return uFlg.getAz2() + web.getAz2() + lFlg.getAz2();
    }

    @Override
    public String toString() {
        return String.format(
                "ISection (L=%.1fmm, h=%.1fmm, w=%.1fmm, tuf=%.1fmm, tw=%.1fmm, tlf=%.1fmm)",
                length, height, width, tuf, tw, tlf
        );
    }

    /**
     * テストコード
     */
    public static void main(String[] args) {
        // 鈑桁断面を生成する
        ISection sec1 = new ISection(7600, 340, 2200, 32, 12, 22);

        // 鈑桁断面をコピーして生成する
        ISection sec2 = sec1.copy();

        Console.log(sec1, sec1.uFlg, sec1.web, sec1.lFlg);
        Console.log("断面積[m2]:", (double) sec1.getArea() / 1_000_000);
        Console.log("面内剛度[m4]:", (double) sec1.getIy() / 1_000_000_000_000L);
        Console.log("面外剛度[m4]:", (double) sec1.getIz() / 1_000_000_000_000L);
    }
}
