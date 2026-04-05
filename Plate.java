record Plate(ISection iSection, Type plateType) {
    enum Type {U_FLG, L_FLG, WEB}

    /**
     * 部材の幅寸法[mm]を返す
     */
    double getWidth() {
        return switch (plateType) {
            case U_FLG, L_FLG -> iSection.getWidth();
            case WEB -> iSection.getTw();
            case null -> -1;
        };
    }

    /**
     * 部材の高さ寸法[mm]を返す
     */
    double getHeight() {
        return switch (plateType) {
            // Flg の場合、板厚を返す
            case U_FLG -> iSection.getTuf();
            case L_FLG -> iSection.getTlf();

            // Web の場合、桁高から U.Flg 板厚を控除
            case WEB -> iSection.getHeight() - iSection.getTuf();
            case null -> -1;
        };
    }

    /**
     * 開始点および終了点の座標 Point[] を返す
     */
    Point[] getPoints() {
        double width = iSection.getWidth();
        double height = iSection.getHeight();
        double tuf = iSection.getTuf();
        double tw = iSection.getTw();
        double tlf = iSection.getTlf();

        return switch (plateType) {
            case U_FLG -> Point.createPoints(-width / 2, height / 2, width / 2, height / 2 - tuf);
            case WEB -> Point.createPoints(-tw / 2, height / 2 - tuf, tw / 2, -height / 2);
            case L_FLG -> Point.createPoints(-width / 2, -height / 2, width / 2, -height / 2 - tlf);
        };
    }

    /**
     * 部材の中心軸を返す
     */
    Axis getAxis() {
        Point[] points = getPoints();
        double cy = (points[0].y() + points[1].y()) / 2;
        double cz = (points[0].z() + points[1].z()) / 2;
        return new Axis(cy, cz);
    }

    /**
     * 部材の面積[mm2]を返す
     */
    long getArea() {
        return Math.round(getWidth() * getHeight());
    }

    /**
     * 面内方向の断面1次モーメント[mm3]を返す
     */
    long getAy() {
        return Math.round(
                switch (plateType) {
                    case U_FLG, L_FLG -> getArea() * getAxis().y();
                    case WEB -> 0;
                }
        );
    }

    /**
     * 面内方向の断面2次モーメント[mm4]を返す
     */
    long getAy2() {
        return Math.round(
                switch (plateType) {
                    case U_FLG, L_FLG -> getAy() * getAxis().y();
                    case WEB -> (getWidth() * Math.pow(getHeight(), 3) / 12);
                }
        );
    }

    /**
     * 面外方向の断面2次モーメント[mm4]を返す
     */
    long getAz2() {
        return Math.round(getHeight() * Math.pow(getWidth(), 3) / 12);
    }

    @Override
    public String toString() {
        return switch (plateType) {
            case U_FLG -> "U.Flg(w=" + getWidth() + ", t=" + getHeight() + ")";
            case WEB -> "Web(h=" + getHeight() + ", t=" + getWidth() + ")";
            case L_FLG -> "L.Flg(w=" + getWidth() + ", t=" + getHeight() + ")";
            case null -> "null";
        };
    }
}
